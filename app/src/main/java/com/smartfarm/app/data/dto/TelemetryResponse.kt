package com.smartfarm.app.data.dto

import com.google.gson.annotations.SerializedName

data class TelemetryResponse(
    @SerializedName("range") val range: String,
    @SerializedName("intervalMinutes") val intervalMinutes: Int,
    @SerializedName("points") val points: List<TelemetryPointDto>
)

data class TelemetryPointDto(
    @SerializedName("time") val time: String, // 예: "2026-10-28T10:00:00+09:00"
    @SerializedName("temperature") val temperature: Float,
    @SerializedName("soilMoisture") val soilMoisture: Float
)