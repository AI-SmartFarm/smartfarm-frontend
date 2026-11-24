package com.smartfarm.app.data

/**
 * 작물 하나의 적정 생육 범위.
 *  - 온도: tempMin ~ tempMax (°C)
 *  - 토양 수분: soilMin ~ soilMax (%)
 *  - species: 서버 작물 코드 (GET /api/v1/crops/{species} 에 사용)
 */
data class CropStandard(
    val name: String,
    val tempMin: Int,
    val tempMax: Int,
    val soilMin: Int,
    val soilMax: Int,
    val species: String = ""
) {
    /** 이 온도가 적정 범위 안이면 true → 배지 "적정", 아니면 "주의" */
    fun isTemperatureNormal(value: Double) = value >= tempMin && value <= tempMax

    /** 이 토양 수분이 적정 범위 안이면 true */
    fun isSoilNormal(value: Int) = value in soilMin..soilMax
}

/**
 * 작물 목록 + 작물별 적정 범위표. 대시보드 드롭다운 목록도 여기서 가져갑니다 (순서 = 드롭다운 순서).
 *
 * 처음에는 앱 기본값(DEFAULT)을 쓰고, 서버에서 기준값을 받으면 그 작물만 서버 값으로 바꿉니다.
 * 서버가 꺼져 있어도 앱 기본값으로 화면이 정상 표시됩니다.
 */
object CropStandards {

    /**
     * 서버에서 못 받았을 때 쓰는 앱 기본값.
     * 온도 = 밤 적온 하한 ~ 낮 적온 상한 (농촌진흥청·제주농업기술원 재배 자료 참고)
     * species 는 백엔드 작물 API 의 species 값과 같아야 합니다.
     */
    private val DEFAULT = listOf(
        //           이름     온도 최소, 최대   수분 최소, 최대
        CropStandard("토마토", 17, 27, 30, 50, species = "Tomato"),
        CropStandard("오이", 15, 28, 50, 70, species = "Cucumber"),
        CropStandard("고추", 18, 28, 40, 60, species = "Pepper"),
        CropStandard("상추", 15, 20, 50, 70, species = "Lettuce"),
        CropStandard("딸기", 10, 20, 40, 60, species = "Strawberry")
    )

    /** 지금 쓰는 목록 */
    var list: List<CropStandard> = DEFAULT
        private set

    /**
     * 서버에서 받은 작물 기준값으로 그 작물 한 줄만 바꿈 (다른 작물은 그대로).
     * 작물 코드(대소문자 무시) 또는 한글 이름이 같으면 같은 작물로 보고,
     * 목록에 없는 작물이면 맨 뒤에 추가합니다.
     */
    fun applyServerStandard(server: CropStandard) {
        fun sameCrop(it: CropStandard) =
            (server.species.isNotBlank() && it.species.equals(server.species, ignoreCase = true)) ||
                it.name == server.name

        list = if (list.any(::sameCrop)) {
            // 화면 이름과 저장된 선택이 바뀌지 않도록 이름·코드는 앱 것을 두고 숫자만 서버 값으로
            list.map { if (sameCrop(it)) server.copy(name = it.name, species = it.species) else it }
        } else {
            list + server
        }
    }
}
