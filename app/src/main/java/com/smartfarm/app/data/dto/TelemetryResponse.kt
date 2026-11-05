package com.smartfarm.app.data.dto

import com.google.gson.annotations.SerializedName

/** API-002-B 환경 데이터 추이 (그래프)  GET /api/v1/farms/{farmId}/telemetry?range=24h */
data class TelemetryResponse(
    @SerializedName("range") val range: String?,
    @SerializedName("intervalMinutes") val intervalMinutes: Int?,
    @SerializedName("points") val points: List<TelemetryPointDto>?
)

/** 그래프 점 하나. 그 구간에 센서값이 없으면 null 일 수 있음 → 그래프에서 제외 */
data class TelemetryPointDto(
    @SerializedName("time") val time: String?, // 예: "2026-10-28T10:00:00+09:00"
    @SerializedName("temperature") val temperature: Float?,
    @SerializedName("soilMoisture") val soilMoisture: Float?
)
