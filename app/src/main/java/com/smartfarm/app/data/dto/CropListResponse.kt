package com.smartfarm.app.data.dto

import com.google.gson.annotations.SerializedName

/**
 * 작물 기준값 API 의 요청·응답 모양.
 *
 * GET /api/v1/crops/{species}   (예: /api/v1/crops/Tomato)
 * {
 *   "species": "Tomato", "nameKo": "토마토",
 *   "temperature":  { "min": 17.0, "max": 27.0 },
 *   "soilMoisture": { "min": 30.0, "max": 50.0 }
 * }
 */
data class CropDto(
    @SerializedName("species") val species: String?,          // 영문 코드 (Tomato)
    @SerializedName("nameKo") val nameKo: String?,            // 한글 이름 (토마토)
    @SerializedName("temperature") val temperature: TargetRangeDto?,
    @SerializedName("soilMoisture") val soilMoisture: TargetRangeDto?
)

/** 적정 범위. 작물 API 는 min / max, 상태 API 는 targetMin / targetMax 로 주므로 두 이름 모두 읽음 */
data class TargetRangeDto(
    @SerializedName(value = "min", alternate = ["targetMin"]) val min: Double?,
    @SerializedName(value = "max", alternate = ["targetMax"]) val max: Double?
)

/** 농장 작물 변경 요청  PUT /api/v1/farms/{farmId}/crop   { "species": "Cucumber" } */
data class CropChangeRequest(
    @SerializedName("species") val species: String
)
