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
 * 서버(GET /api/v1/crops)에서 받아오면 update() 로 서버 값으로 바뀝니다.
 * 서버가 꺼져 있거나 아직 API 가 없으면 아래 DEFAULT(앱 기본값)를 그대로 씁니다.
 */
object CropStandards {

    /**
     * 서버에서 못 받았을 때 쓰는 앱 기본값.
     * ⚠ 일반적인 재배 자료를 참고한 "예시 값"입니다.
     */
    private val DEFAULT = listOf(
        //           이름     온도 최소, 최대   수분 최소, 최대
        // 온도 = 밤 적온 하한 ~ 낮 적온 상한 (출처: 농촌진흥청·제주농업기술원 재배 자료)
        // 수분 = 예시 값 (서버/시뮬레이터 기준과 맞출 것)
        CropStandard("토마토", 17, 27, 30, 50),
        CropStandard("오이", 15, 28, 50, 70),
        CropStandard("고추", 18, 28, 40, 60),
        CropStandard("상추", 15, 20, 50, 70),
        CropStandard("딸기", 10, 20, 40, 60)
    )

    /** 지금 쓰는 목록. 처음엔 앱 기본값, 서버에서 받으면 서버 값 */
    var list: List<CropStandard> = DEFAULT
        private set

    /** 서버에서 받은 목록으로 교체 (비어 있으면 무시하고 지금 목록 유지) */
    fun update(newList: List<CropStandard>) {
        if (newList.isNotEmpty()) list = newList
    }

    /**
     * 상태 API(002-A)가 알려 준 "농장 작물의 적정 범위"로 그 작물 한 줄만 바꿈.
     * 예) 서버 작물이 토마토 22~26°C 면 → 목록의 토마토가 22~26 으로 (다른 작물은 그대로)
     * 목록에 없는 작물이면 맨 뒤에 추가.
     */
    fun applyServerStandard(server: CropStandard) {
        list = if (list.any { it.name == server.name }) {
            list.map { if (it.name == server.name) server else it }
        } else {
            list + server
        }
    }
}
