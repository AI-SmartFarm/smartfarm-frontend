package com.smartfarm.app.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.smartfarm.app.data.dto.DiagnosisDto
import com.smartfarm.app.data.dto.isForCrop
import com.smartfarm.app.data.dto.isRealInfection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.OffsetDateTime

/**
 * "새로 생긴 감염 진단이 있나?"를 진단 이력 API(API-008)로 확인합니다.
 *
 * 팝업 내용(병 이름, 가동 장치, 가동 시간)은 모두 API-008 응답에서 가져오고,
 * FCM 푸시는 "새 진단을 확인하라"는 신호로만 씁니다.
 *
 * 호출되는 때
 *  - 앱이 화면에 떠 있는 동안: 나타나자마자 한 번 + 15초마다 (MainActivity.startPestAlertPolling)
 *  - 앱을 보고 있는 중에 FCM 푸시를 받았을 때: 바로 + 3초 간격으로 몇 번 더 (진단 저장이 늦을 때 대비)
 *
 * 마지막으로 확인한 진단 번호(diagnosisId)를 기기에 저장해서 같은 진단으로 팝업이 다시 뜨지 않게 합니다.
 * 지금 고른 작물의 감염 진단만 팝업 대상입니다 (다른 작물 진단은 무시).
 * 확인 과정은 Logcat 의 "PestAlert" 태그로 볼 수 있습니다.
 */
object PestAlertChecker {

    private const val TAG = "PestAlert"
    private const val PREFS_NAME = "farm_settings"
    private const val KEY_LAST_SEEN_ID = "last_seen_diagnosis_id"

    /** 처음 실행이거나 서버 DB 가 초기화됐을 때, 이 시간(분) 안에 생긴 감염은 팝업으로 알려줌 */
    private const val RECENT_MINUTES = 10L

    private val repository = PestRepository()

    /** 앱 실행과 푸시가 동시에 와도 확인은 한 번에 하나만 (같은 팝업 중복 방지) */
    private val mutex = Mutex()

    /**
     * 새 감염 진단이 있으면 팝업용 PestAlert, 없으면 null.
     *  - 평소: 마지막으로 본 번호보다 큰(새) 진단 중, 고른 작물의 가장 최근 감염 진단
     *  - 처음 실행 / 서버 진단 번호가 줄어듦(DB 초기화): 기존 진단은 "이미 본 것"으로 처리하되,
     *    최근 10분 안에 생긴 감염이면 팝업 (설치 직후나 서버 재시작 직후에도 알림이 빠지지 않게)
     */
    suspend fun checkNewInfection(context: Context): PestAlert? = mutex.withLock {
        val appContext = context.applicationContext
        FarmSettings.init(appContext)   // 고른 작물 불러오기
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val diagnoses = repository.getDiagnoses(size = 20).getOrElse {
            Log.w(TAG, "진단 이력 확인 실패: ${it.message}")
            return@withLock null
        }
        val newestId = diagnoses.maxOfOrNull { it.diagnosisId } ?: run {
            Log.d(TAG, "진단 이력 없음")
            return@withLock null
        }

        // 지금 고른 작물의 실제 감염 진단만, 최신순
        val species = FarmSettings.currentStandard.species
        val infections = diagnoses
            .filter { it.isForCrop(species) && it.isRealInfection() }
            .sortedByDescending { it.diagnosisId }

        val hasLastSeen = prefs.contains(KEY_LAST_SEEN_ID)
        val lastSeenId = prefs.getLong(KEY_LAST_SEEN_ID, 0L)
        prefs.edit { putLong(KEY_LAST_SEEN_ID, newestId) }

        val newInfection = if (!hasLastSeen || newestId < lastSeenId) {
            // 처음 실행이거나 서버 진단 번호가 초기화됨 → 최근 10분 안의 감염만 알림
            Log.d(TAG, "기준 다시 잡음 (저장된 번호=${if (hasLastSeen) lastSeenId else "없음"}, 서버 최신=$newestId)")
            infections.firstOrNull { it.isRecent() }
        } else {
            if (newestId == lastSeenId) {
                Log.d(TAG, "새 진단 없음 (마지막 번호=$lastSeenId)")
                return@withLock null
            }
            infections.firstOrNull { it.diagnosisId > lastSeenId }
        }

        Log.d(
            TAG,
            "진단 확인: $lastSeenId → $newestId, 작물=$species, " +
                "팝업=${newInfection?.let { "${it.diseaseName}(#${it.diagnosisId})" } ?: "없음"}"
        )
        newInfection?.toPestAlert()
    }

    /** 진단 시각이 최근 RECENT_MINUTES 분 안인지 (시각을 못 읽으면 false) */
    private fun DiagnosisDto.isRecent(): Boolean = try {
        val minutesAgo = Duration.between(OffsetDateTime.parse(diagnosedAt), OffsetDateTime.now()).toMinutes()
        minutesAgo in -5..RECENT_MINUTES   // 휴대폰·서버 시계 차이 몇 분은 허용
    } catch (e: Exception) {
        false
    }

    /**
     * 진단 → 팝업 내용. 자동 조치는 responses[0] 의 장치·시간으로 표시하고,
     * 조치가 없는 병(예: 황화잎말이바이러스 = 알림만)은 deviceName 을 "" 로 넘겨 "자동 제어 없음"으로 표시합니다.
     */
    private fun DiagnosisDto.toPestAlert(): PestAlert {
        val response = responses.orEmpty().firstOrNull()
        return PestAlert(
            pestName = diseaseName ?: diseaseCode ?: "병해충",
            deviceName = actuatorName(response?.actuator),
            timerMinutes = response?.durationMinutes ?: 0
        )
    }
}
