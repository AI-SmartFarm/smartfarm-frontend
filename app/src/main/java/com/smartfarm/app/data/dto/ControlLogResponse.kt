package com.smartfarm.app.data.dto

import com.google.gson.annotations.SerializedName

/**
 * API-007-A 제어 활동 로그  GET /api/v1/farms/{farmId}/control-logs?type=TEMP&size=20
 */
data class ControlLogResponse(
    @SerializedName("logs") val logs: List<ControlLogDto>?
)

data class ControlLogDto(
    @SerializedName("commandId") val commandId: String?,
    @SerializedName("time") val time: String?,               // "2026-10-28T13:00:00+09:00"
    @SerializedName("actuator") val actuator: String?,       // circFan / waterPump
    @SerializedName("action") val action: String?,           // on / off
    @SerializedName("reason") val reason: String?,           // TEMP_HIGH, TEMP_NORMAL, SOIL_LOW, SOIL_ENOUGH ...
    @SerializedName("triggerValue") val triggerValue: Double?,
    @SerializedName("result") val result: String?,           // PENDING / DELIVERED / ACKED / REJECTED / EXPIRED
    @SerializedName("message") val message: String?          // 백엔드가 만든 문장 → 화면에 그대로 표시
)
