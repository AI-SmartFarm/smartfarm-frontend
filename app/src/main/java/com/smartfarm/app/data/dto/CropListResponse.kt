package com.smartfarm.app.data.dto

import com.google.gson.annotations.SerializedName

/**
 * 작물 하나의 기준값  GET /api/v1/crops/{species}   (예: /api/v1/crops/Tomato)
 *
 * {
 *   "species": "Tomato", "nameKo": "토마토",
 *   "temperature":  { "min": 17.0, "max": 27.0 },
 *   "soilMoisture": { "min": 30.0, "max": 50.0 }
 * }
 * 백엔드가 이름을 바꾸면 @SerializedName("...") 안의 글자만 고치면 됩니다.
 * (파일 이름은 예전 그대로 둠: 새 파일을 만들면 커밋에서 빠지기 쉬워서)
 */
data class CropDto(
    @SerializedName("species") val species: String?,          // 영문 코드 (Tomato)
    @SerializedName("nameKo") val nameKo: String?,            // 한글 이름 (토마토)
    @SerializedName("temperature") val temperature: TargetRangeDto?,
    @SerializedName("soilMoisture") val soilMoisture: TargetRangeDto?
)

/** 적정 범위. 백엔드는 min / max 로 줌 (status 처럼 targetMin / targetMax 로 와도 읽힘) */
data class TargetRangeDto(
    @SerializedName(value = "min", alternate = ["targetMin"]) val min: Double?,
    @SerializedName(value = "max", alternate = ["targetMax"]) val max: Double?
)

/**
 * 농장 작물 바꾸기 요청  PUT /api/v1/farms/{farmId}/crop
 * ⚠ 백엔드에 아직 없음 (프론트 제안 형식). 백엔드가 다르게 만들면 이름만 고치기.
 *   { "species": "Cucumber" }
 */
data class CropChangeRequest(
    @SerializedName("species") val species: String
)
