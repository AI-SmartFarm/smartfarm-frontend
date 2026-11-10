package com.smartfarm.app.data.dto

import com.google.gson.annotations.SerializedName

/**
 * API-002-A 현재 농장 상태  GET /api/v1/farms/{farmId}/status
 *
 * 화면에 필요한 값만 받습니다. (devices, latestDiagnosis 등은 지금 쓰지 않아서 생략.
 *  JSON에 더 있는 필드는 Gson이 알아서 무시합니다)
 * crop + temperature/soilMoisture 의 targetMin·targetMax = 서버가 쓰는 "농장 작물의 적정 범위"
 */
data class StatusResponse(
    @SerializedName("farmId") val farmId: String?,
    @SerializedName("updatedAt") val updatedAt: String?,      // 마지막 센서값 시각
    @SerializedName("connected") val connected: Boolean?,     // 시뮬레이터가 30초 안에 값을 보냈는지
    @SerializedName("crop") val crop: StatusCropDto?,          // 농장에 심은 작물 (예: 토마토)
    @SerializedName("temperature") val temperature: SensorValueDto?,
    @SerializedName("soilMoisture") val soilMoisture: SensorValueDto?
)

/** 농장 작물  { "species": "Tomato", "nameKo": "토마토" } */
data class StatusCropDto(
    @SerializedName("species") val species: String?,
    @SerializedName("nameKo") val nameKo: String?
)

/** temperature / soilMoisture 한 칸. 센서가 없으면 value 가 null */
data class SensorValueDto(
    @SerializedName("value") val value: Double?,
    @SerializedName("targetMin") val targetMin: Double?,      // 서버 기준 적정 범위 → CropStandards 에 반영
    @SerializedName("targetMax") val targetMax: Double?,
    @SerializedName("state") val state: String?               // NORMAL / HIGH / LOW
)
