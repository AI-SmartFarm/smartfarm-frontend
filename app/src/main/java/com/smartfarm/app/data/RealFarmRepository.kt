package com.smartfarm.app.data

import android.util.Log
import com.smartfarm.app.data.api.FarmApiService
import com.smartfarm.app.data.dto.ControlLogDto
import com.smartfarm.app.data.dto.CropDto
import com.smartfarm.app.data.dto.StatusResponse
import com.smartfarm.app.data.dto.TelemetryPointDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 실제 백엔드 API 를 부르는 저장소 (센서 쪽: 대시보드 / 온도 로그 / 토양 수분 로그).
 * 서버 응답(DTO)을 화면이 쓰는 모델(ChartData, ActivityLog ...)로 바꿔서 돌려줍니다.
 *
 * 실패하거나 5초 안에 응답이 없으면 예외 대신 null / 빈 목록을 돌려줍니다 → 화면은 "-" 또는 "기록 없음".
 * 자세한 실패 이유는 Logcat 에서 "SmartFarmAPI" 로 검색하세요.
 */
class RealFarmRepository(private val apiService: FarmApiService) {

    companion object {
        /** 명세: 지금은 농장 하나 */
        const val FARM_ID = "greenhouse-01"
        private const val TAG = "SmartFarmAPI"
        /** API 하나를 최대 몇 ms 기다릴지 */
        private const val TIMEOUT_MS = 5_000L
        private val LOG_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    }

    // ===================== 현재 센서값 (API-002-A) =====================

    /** 대시보드용: 온도·토양 수분을 한 번의 호출로 */
    suspend fun getCurrentSensor(): CurrentSensor? {
        val status = getStatus() ?: return null
        return CurrentSensor(
            temperature = status.temperature?.value,
            soilMoisture = status.soilMoisture?.value?.roundToInt()   // 서버는 32.0 → 앱은 32
        )
    }

    /** 온도 로그 화면용 현재 온도. 실패하거나 센서가 없으면 null */
    suspend fun getCurrentTemperature(): Double? = getStatus()?.temperature?.value

    /** 토양 수분 로그 화면용 현재 수분(%). 실패하거나 센서가 없으면 null */
    suspend fun getCurrentSoilMoisture(): Int? = getStatus()?.soilMoisture?.value?.roundToInt()

    private suspend fun getStatus(): StatusResponse? = safeCall("status") {
        val response = apiService.getStatus(FARM_ID)
        Log.d(TAG, "status HTTP ${response.code()} / ${response.body()}")
        if (response.isSuccessful) response.body() else null   // 센서값이 한 번도 없으면 404 → null
    }

    // ===================== 작물별 적정 범위 (GET /api/v1/crops, 백엔드에 요청한 설계안) =====================

    /**
     * 서버에서 작물 목록·적정 범위를 받아 CropStandards 를 서버 값으로 바꿈.
     * 성공하면 true (화면이 범위 글자를 다시 그리면 됨).
     * 실패·404(아직 API 없음)면 false → 앱 기본값(CropStandard.kt)을 그대로 사용.
     */
    suspend fun refreshCropStandards(): Boolean {
        val crops = safeCall("crops") {
            val response = apiService.getCrops()
            Log.d(TAG, "crops HTTP ${response.code()} / ${response.body()}")
            if (response.isSuccessful) response.body()?.crops?.mapNotNull { it.toCropStandard() }
            else null
        }
        if (crops.isNullOrEmpty()) return false

        CropStandards.update(crops)
        return true
    }

    /** 서버 작물 하나 → 앱 CropStandard. 이름이나 범위가 하나라도 없으면 그 작물은 뺌 */
    private fun CropDto.toCropStandard(): CropStandard? {
        val name = nameKo ?: species ?: return null
        val tempMin = temperature?.targetMin ?: return null
        val tempMax = temperature?.targetMax ?: return null
        val soilMin = soilMoisture?.targetMin ?: return null
        val soilMax = soilMoisture?.targetMax ?: return null
        // 화면 글자(%d)가 정수라 반올림 (서버 22.0 → 22)
        return CropStandard(
            name = name,
            tempMin = tempMin.roundToInt(),
            tempMax = tempMax.roundToInt(),
            soilMin = soilMin.roundToInt(),
            soilMax = soilMax.roundToInt()
        )
    }

    // ===================== 그래프 (API-002-B) =====================

    /** 온도 로그 그래프용 데이터 */
    suspend fun getTemperatureHistory(range: String = "24h"): ChartData =
        getHistory(range) { it.temperature }

    /** 토양 수분 로그 그래프용 데이터 */
    suspend fun getSoilMoistureHistory(range: String = "24h"): ChartData =
        getHistory(range) { it.soilMoisture }

    private suspend fun getHistory(
        range: String,
        pickValue: (TelemetryPointDto) -> Float?
    ): ChartData = safeCall("telemetry") {
        val response = apiService.getTelemetry(FARM_ID, range)
        Log.d(TAG, "telemetry HTTP ${response.code()} / ${response.body()}")
        val points = response.body()?.points
        if (response.isSuccessful && points != null) {
            // 값이 null 인 점은 빼고 그림
            val valid = points.mapNotNull { point -> pickValue(point)?.let { point.time to it } }
            ChartData(
                labels = valid.map { parseHourLabel(it.first) },
                values = valid.map { it.second }
            )
        } else null
    } ?: ChartData(emptyList(), emptyList())

    // ===================== 활동로그 (API-007-A) =====================

    /** 온도 로그 화면의 활동로그 (순환팬 제어) */
    suspend fun getTemperatureLogs(size: Int = 20): List<ActivityLog> = getControlLogs("TEMP", size)

    /** 토양 수분 로그 화면의 활동로그 (관수 제어) */
    suspend fun getSoilLogs(size: Int = 20): List<ActivityLog> = getControlLogs("SOIL", size)

    private suspend fun getControlLogs(type: String, size: Int): List<ActivityLog> =
        safeCall("control-logs($type)") {
            val response = apiService.getControlLogs(FARM_ID, type, size)
            Log.d(TAG, "control-logs($type) HTTP ${response.code()} / ${response.body()}")
            if (response.isSuccessful) response.body()?.logs.orEmpty().map { it.toActivityLog() }
            else null
        } ?: emptyList()

    /** 서버 로그 한 줄 → 화면의 활동로그 한 줄 */
    private fun ControlLogDto.toActivityLog() = ActivityLog(
        time = formatLogTime(time),
        message = message.orEmpty(),
        type = when (reason) {
            "TEMP_HIGH" -> ActivityType.WARNING              // ⚠ 기준 초과
            "SOIL_LOW", "SOIL_ENOUGH" -> ActivityType.WATERING // 💧 급수
            else -> if (actuator == "waterPump") ActivityType.WATERING else ActivityType.FAN
        }
    )

    // ===================== 공통 =====================

    /**
     * API 호출을 감싸서, 실패하면 null 을 돌려줌.
     * 단 화면을 나가서 취소된 경우(CancellationException)는 그대로 던져야
     * 화면이 사라진 뒤에 binding 을 건드리는 문제가 생기지 않음.
     */
    private suspend fun <T> safeCall(name: String, block: suspend () -> T?): T? {
        // TIMEOUT_MS 안에 응답이 없으면 기다리지 않고 null (서버가 꺼져 있을 때 화면이 오래 멈추지 않게)
        val result = withTimeoutOrNull(TIMEOUT_MS) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "$name 호출 실패: ${e.message}", e)
                null
            }
        }
        if (result == null) Log.w(TAG, "$name: 값 없음 (실패·404·시간 초과 ${TIMEOUT_MS / 1000}초)")
        return result
    }

    /** "2026-10-28T10:00:00+09:00" → "10시" (그래프 x축) */
    private fun parseHourLabel(timeIso: String?): String = try {
        "${ZonedDateTime.parse(timeIso).hour}시"
    } catch (e: Exception) {
        ""
    }

    /** "2026-10-28T13:00:00+09:00" → "10-28 13:00" (활동로그 시간) */
    private fun formatLogTime(timeIso: String?): String = try {
        ZonedDateTime.parse(timeIso).format(LOG_TIME_FORMAT)
    } catch (e: Exception) {
        timeIso.orEmpty()
    }
}
