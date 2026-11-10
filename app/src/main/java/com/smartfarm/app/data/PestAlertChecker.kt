package com.smartfarm.app.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.smartfarm.app.data.dto.DiagnosisDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * "새로 생긴 병해충 진단이 있나?"를 진단 이력 API(API-008)로 확인하는 곳.
 *
 * 팝업 내용(병 이름, 가동 장치, 가동 시간)은 전부 API-008 응답에서 가져옵니다.
 *  → 백엔드와 푸시 형식을 따로 맞출 필요 없음. 푸시(FCM)는 "확인해 봐"라는 신호로만 씀.
 *
 * 부르는 곳
 *  - 앱이 화면에 나타날 때마다 (MainActivity.onStart)  ← FCM 이 없는 지금도 동작
 *  - FCM 푸시를 받았을 때 (백엔드가 API-010 을 만들면)
 *
 * 마지막으로 확인한 진단 번호(diagnosisId)를 휴대폰에 저장해서, 같은 진단으로 팝업이 두 번 뜨지 않게 합니다.
 */
object PestAlertChecker {

    private const val TAG = "PestAlert"
    private const val PREFS_NAME = "farm_settings"
    private const val KEY_LAST_SEEN_ID = "last_seen_diagnosis_id"

    private val repository = PestRepository()
    private val mutex = Mutex()   // 앱 시작 + 푸시가 동시에 와도 한 번만 확인

    /**
     * 새로 생긴 "감염" 진단이 있으면 팝업용 PestAlert, 없으면 null.
     *  - 처음 설치해서 저장된 번호가 없으면: 지금까지의 진단은 "이미 본 것"으로 치고 팝업 안 띄움
     *  - 감염이 아닌 새 진단(병 못 찾음)은 팝업 없이 번호만 갱신
     */
    suspend fun checkNewInfection(context: Context): PestAlert? = mutex.withLock {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val diagnoses = repository.getDiagnoses(size = 10).getOrElse {
            Log.w(TAG, "진단 이력 확인 실패: ${it.message}")
            return@withLock null
        }
        val newestId = diagnoses.maxOfOrNull { it.diagnosisId } ?: return@withLock null

        // 처음 실행: 기준점만 저장
        if (!prefs.contains(KEY_LAST_SEEN_ID)) {
            prefs.edit { putLong(KEY_LAST_SEEN_ID, newestId) }
            Log.d(TAG, "처음 실행 → 기준 진단 번호 $newestId 저장 (팝업 없음)")
            return@withLock null
        }

        val lastSeenId = prefs.getLong(KEY_LAST_SEEN_ID, 0L)
        if (newestId <= lastSeenId) return@withLock null   // 새 진단 없음

        prefs.edit { putLong(KEY_LAST_SEEN_ID, newestId) }

        // 새 진단 중 가장 최근의 "감염" 진단 (서버가 최신순으로 줌)
        val newInfection = diagnoses.firstOrNull { it.diagnosisId > lastSeenId && it.infected }
        Log.d(TAG, "새 진단 확인: 마지막 $lastSeenId → $newestId, 감염=${newInfection?.diseaseName}")
        newInfection?.toPestAlert()
    }

    /**
     * 진단 → 팝업 내용.
     * 명세: "순환팬 자동 가동 중, 60분 연속 가동"은 responses[0] 의 actuator, durationMinutes 로 그림.
     * responses 가 비어 있으면(예: 황화잎말이바이러스 = 알림만) deviceName 을 "" 로 → 팝업이 "자동 제어 없음"으로 표시
     */
    private fun DiagnosisDto.toPestAlert(): PestAlert {
        val response = responses.orEmpty().firstOrNull()
        return PestAlert(
            pestName = diseaseName ?: diseaseCode ?: "병해충",
            deviceName = when (response?.actuator) {
                null -> ""
                "circFan" -> "순환팬"
                "waterPump" -> "급수 펌프"
                else -> response?.actuator ?: ""
            },
            timerMinutes = response?.durationMinutes ?: 0
        )
    }
}
