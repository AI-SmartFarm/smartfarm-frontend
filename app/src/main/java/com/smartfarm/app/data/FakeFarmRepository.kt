package com.smartfarm.app.data

/**
 * 가짜(더미) 데이터 저장소. 값은 디자인 시안과 똑같이 맞춰 두었습니다.
 *
 * 화면(Fragment)은 항상 이 함수들로 데이터를 받아옵니다.
 * → 나중에 보조 담당이 실제 Backend API를 부르는 Repository를 만들 때
 *   함수 이름만 똑같이 맞추면 화면 코드는 거의 안 고쳐도 됩니다.
 */
class FakeFarmRepository {

    // ===== 온도 로그 =====
    fun getCurrentTemperature(): Double = 24.5

    fun getTemperatureHistory() = ChartData(
        labels = listOf("10시", "11시", "12시", "13시", "14시", "15시", "16시", "17시"),
        values = listOf(25.0f, 26.0f, 24.8f, 25.6f, 24.6f, 24.9f, 25.5f, 24.5f)
    )

    fun getTemperatureLogs() = listOf(
        ActivityLog("09-10 13:00", "30°C 초과 환기팬 가동", ActivityType.WARNING),
        ActivityLog("09-12 11:00", "적정 온도 도달, 환기팬 정지", ActivityType.FAN)
    )

    // ===== 토양 수분 로그 =====
    fun getCurrentSoilMoisture(): Int = 32

    fun getSoilMoistureHistory() = ChartData(
        labels = listOf("10", "11", "12", "13", "14", "15", "16", "17시"),
        values = listOf(64f, 57f, 54f, 52f, 47f, 41f, 37f, 32f)
    )

    fun getSoilLogs() = listOf(
        ActivityLog("9-3 10:00", "토양 수분 함유량 기준치 미만, 자동급수 완료", ActivityType.WATERING),
        ActivityLog("9-10 14:00", "토양 수분 함유량 기준치 미만, 자동급수 완료", ActivityType.WATERING)
    )

    // ===== 내 작물 확인 =====
    fun getPestLogs() = listOf(
        PestLog("9/7", "흰가루병 발생, 순환팬 가동", "환경 습도 78% 감지 후 AI 스마트 순환팬 즉시 가동"),
        PestLog("8/24", "토마토잎곰팡이병 발생, 환기 가동", "토마토잎곰팡이병 감지, 순환팬 가동으로 과습 관리 조치"),
        PestLog("7/16", "잿빛곰팡이 초기 증상, 환기 조치", "잿빛 곰팡이 감지, 순환팬 가동")
    )

    // ===== 병해충 발생 알림 (팝업 테스트용) =====
    fun getLatestPestAlert() = PestAlert(
        pestName = "흰가루병",
        deviceName = "순환팬",
        timerMinutes = 60
    )
}
