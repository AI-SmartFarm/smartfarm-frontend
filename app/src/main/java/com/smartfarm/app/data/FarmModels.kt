package com.smartfarm.app.data

/**
 * 앱에서 주고받는 데이터의 "모양" 모음.
 * 나중에 Backend API 응답(JSON) 형식이 정해지면 필드 이름을 거기에 맞춰 고치면 됩니다.
 */

/** 그래프 한 개 분량의 데이터. labels[i] 시각의 값이 values[i] */
data class ChartData(
    val labels: List<String>,   // x축 글자 ("10시", "11시" ...)
    val values: List<Float>     // y축 값
)

/** 활동로그 한 줄 (온도 로그 / 토양 수분 로그) */
data class ActivityLog(
    val time: String,           // "09-10 13:00"
    val message: String,        // "30°C 초과 환기팬 가동"
    val type: ActivityType      // 앞에 붙는 아이콘 종류
)

enum class ActivityType {
    WATERING,   // 💧 급수
    WARNING,    // ⚠ 경고
    FAN         // 🌀 환기팬/순환팬
}

/** 병해충 로그 한 줄 (내 작물 확인) */
data class PestLog(
    val date: String,           // "9/7"
    val title: String,          // "흰가루병 발생, 순환팬 가동"
    val detail: String          // "환경 습도 78% 감지 후 ..."
)

/** 병해충 발생 알림 팝업에 보여줄 내용 */
data class PestAlert(
    val pestName: String,       // "흰가루병"
    val deviceName: String,     // "순환팬"
    val timerMinutes: Int       // 60
)

/** 적정 범위 기준값. 배지(적정/주의) 판단에 사용 */
object FarmStandard {
    const val TEMP_MIN = 20.0
    const val TEMP_MAX = 26.0
    const val SOIL_MIN = 30
    const val SOIL_MAX = 50

    fun isTemperatureNormal(value: Double) = value in TEMP_MIN..TEMP_MAX
    fun isSoilNormal(value: Int) = value in SOIL_MIN..SOIL_MAX
}
