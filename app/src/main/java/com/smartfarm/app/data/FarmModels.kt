package com.smartfarm.app.data

/**
 * 화면이 쓰는 데이터 모델 모음.
 * 서버 응답(DTO, data/dto 폴더)은 Repository 에서 이 모델로 바뀐 뒤 화면에 전달됩니다.
 */

/** 현재 센서값 (대시보드). 센서값이 없거나 불러오기에 실패하면 null */
data class CurrentSensor(
    val temperature: Double?,   // °C
    val soilMoisture: Int?      // %
)

/** 그래프 한 개 분량의 데이터. labels[i] 시각의 값이 values[i] */
data class ChartData(
    val labels: List<String>,   // x축 글자 ("10시", "11시" ...)
    val values: List<Float>     // y축 값
)

/** 활동로그 한 줄 (온도 로그 / 토양 수분 로그) */
data class ActivityLog(
    val time: String,           // "11-20 13:00"
    val message: String,        // 백엔드가 만든 문장 (예: "27.4°C 기준 초과, 순환팬 가동")
    val type: ActivityType      // 앞에 붙는 아이콘 종류
)

enum class ActivityType {
    WATERING,   // 급수
    WARNING,    // 기준 초과 경고
    FAN         // 순환팬
}

/** 병해충 발생 알림 팝업에 보여줄 내용 */
data class PestAlert(
    val pestName: String,       // "잎곰팡이병"
    val deviceName: String,     // "순환팬". 자동 대응이 없는 병이면 ""
    val timerMinutes: Int       // 60. 없으면 0
)

/** 서버의 장치 코드 → 화면에 보일 이름 (circFan → 순환팬) */
fun actuatorName(actuator: String?): String = when (actuator) {
    null, "" -> ""
    "circFan" -> "순환팬"
    "waterPump" -> "급수 펌프"
    else -> actuator.orEmpty()
}
