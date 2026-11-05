package com.smartfarm.app.data

/**
 * 작물 하나의 적정 생육 범위.
 *  - 온도: tempMin ~ tempMax (°C)
 *  - 토양 수분: soilMin ~ soilMax (%)
 */
data class CropStandard(
    val name: String,
    val tempMin: Int,
    val tempMax: Int,
    val soilMin: Int,
    val soilMax: Int
) {
    /** 이 온도가 적정 범위 안이면 true → 배지 "적정", 아니면 "주의" */
    fun isTemperatureNormal(value: Double) = value >= tempMin && value <= tempMax

    /** 이 토양 수분이 적정 범위 안이면 true */
    fun isSoilNormal(value: Int) = value in soilMin..soilMax
}

/**
 * 작물 목록 + 작물별 적정 범위표.
 * 대시보드 드롭다운의 작물 목록도 여기서 가져갑니다 (순서 = 드롭다운 순서).
 *
 * ⚠ 아래 숫자는 일반적인 재배 자료를 참고한 "예시 값"입니다.
 *   팀/백엔드에서 기준이 정해지면 숫자만 고치면 앱 전체(대시보드·온도·토양 화면)에 반영됩니다.
 *   작물을 추가하려면 줄을 하나 더 넣으면 드롭다운에도 자동으로 나타납니다.
 */
object CropStandards {
    val list = listOf(
        //           이름     온도 최소, 최대   수분 최소, 최대
        // 온도 = 밤 적온 하한 ~ 낮 적온 상한 (출처: 농촌진흥청·제주농업기술원 재배 자료)
        // 수분 = 예시 값 (서버/시뮬레이터 기준과 맞출 것)
        CropStandard("토마토", 17, 27, 30, 50),
        CropStandard("오이", 15, 28, 50, 70),
        CropStandard("고추", 18, 28, 40, 60),
        CropStandard("상추", 15, 20, 50, 70),
        CropStandard("딸기", 10, 20, 40, 60)
    )
}
