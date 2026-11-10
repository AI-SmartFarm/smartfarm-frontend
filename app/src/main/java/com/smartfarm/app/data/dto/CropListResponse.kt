package com.smartfarm.app.data.dto

import com.google.gson.annotations.SerializedName

/**
 * 작물 목록 + 작물별 적정 범위  GET /api/v1/crops
 *
 * ⚠ 백엔드 명세에는 아직 없는 API 입니다 (프론트가 요청한 설계안).
 *   002-A 의 crop / temperature.targetMin·targetMax 와 같은 이름·모양으로 맞췄습니다.
 *   백엔드가 이름을 다르게 만들면 @SerializedName("...") 안의 글자만 고치면 됩니다.
 *
 * {
 *   "crops": [
 *     { "species": "Tomato", "nameKo": "토마토",
 *       "temperature":  { "targetMin": 17.0, "targetMax": 27.0 },
 *       "soilMoisture": { "targetMin": 30.0, "targetMax": 50.0 } }
 *   ]
 * }
 */
data class CropListResponse(
    @SerializedName("crops") val crops: List<CropDto>?
)

/** 작물 하나 */
data class CropDto(
    @SerializedName("species") val species: String?,          // 영문 코드 (Tomato)
    @SerializedName("nameKo") val nameKo: String?,            // 화면에 보일 이름 (토마토)
    @SerializedName("temperature") val temperature: TargetRangeDto?,
    @SerializedName("soilMoisture") val soilMoisture: TargetRangeDto?
)

/** 적정 범위 (002-A 의 targetMin / targetMax 와 같은 이름) */
data class TargetRangeDto(
    @SerializedName("targetMin") val targetMin: Double?,
    @SerializedName("targetMax") val targetMax: Double?
)
