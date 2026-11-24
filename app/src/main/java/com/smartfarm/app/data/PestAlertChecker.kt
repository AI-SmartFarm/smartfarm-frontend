package com.smartfarm.app.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.smartfarm.app.data.dto.DiagnosisDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * "새로 생긴 감염 진단이 있나?"를 진단 이력 API(API-008)로 확인합니다.
 *
 * 팝업 내용(병 이름, 가동 장치, 가동 시간)은 모두 API-008 응답에서 가져오고,
 * FCM 푸시는 "새 진단을 확인하라"는 신호로만 씁니다.
 *
 * 호출되는 때
 *  - 앱이 화면에 나타날 때마다 (MainActivity.onStart)
 *  - 앱을 보고 있는 중에 FCM 푸시를 받았을 때
 *
 * 마지막으로 확인한 진단 번호(diagnosisId)를 기기에 저장해서 같은 진단으로 팝업이 다시 뜨지 않게 합니다.
 */
object PestAlertChecker {

    private const val TAG = "PestAlert"
    private const val PREFS_NAME = "farm_settings"
    private const val KEY_LAST_SEEN_ID = "last_seen_diagnosis_id"

    private val repository = PestRepository()

    /** 앱 실행과 푸시가 동시에 와도 확인은 한 번에 하나만 (같은 팝업 중복 방지) */
    private val mutex = Mutex()

    /**
     * 새 감염 진단이 있으면 팝업용 PestAlert, 없으면 null.
     *  - 처음 설치 후 첫 확인: 기존 진단은 "이미 본 것"으로 기준만 저장하고 팝업 없음
     *  - 새 진단이 감염이 아니면(병을 못 찾음) 팝업 없이 번호만 갱신
     */
    suspend fun checkNewInfection(context: Context): PestAlert? = mutex.withLock {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val diagnoses = repository.getDiagnoses(size = 10).getOrElse {
            Log.w(TAG, "진단 이력 확인 실패: ${it.message}")
            return@withLock null
        }
        val newestId = diagnoses.maxOfOrNull { it.diagnosisId } ?: return@withLock null

        if (!prefs.contains(KEY_LAST_SEEN_ID)) {
            prefs.edit { putLong(KEY_LAST_SEEN_ID, newestId) }
            return@withLock null
        }

        val lastSeenId = prefs.getLong(KEY_LAST_SEEN_ID, 0L)
        if (newestId <= lastSeenId) return@withLock null   // 새 진단 없음

        prefs.edit { putLong(KEY_LAST_SEEN_ID, newestId) }

        // 새 진단 중 가장 최근의 감염 진단 (서버가 최신순으로 줌)
        val newInfection = diagnoses.firstOrNull { it.diagnosisId > lastSeenId && it.infected }
        Log.d(TAG, "새 진단: $lastSeenId → $newestId, 감염=${newInfection?.diseaseName}")
        newInfection?.toPestAlert()
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
