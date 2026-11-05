package com.smartfarm.app.data.dto

import com.google.gson.annotations.SerializedName

// 1. API-008 병해충 진단 목록 응답 DTO
data class DiagnosisListResponse(
    @SerializedName("diagnoses") val diagnoses: List<DiagnosisDto>
)

// 병해충 진단 단건 DTO
data class DiagnosisDto(
    @SerializedName("diagnosisId") val diagnosisId: Long,
    @SerializedName("diagnosedAt") val diagnosedAt: String,
    @SerializedName("infected") val infected: Boolean,
    @SerializedName("diseaseCode") val diseaseCode: String?,
    @SerializedName("diseaseName") val diseaseName: String?,
    @SerializedName("severityLevel") val severityLevel: String?,
    @SerializedName("severityLowConfidence") val severityLowConfidence: Boolean?,
    @SerializedName("confidence") val confidence: Double?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("responses") val responses: List<ActuatorResponseDto>,
    @SerializedName("guide") val guide: String?
)

// 자동 제어 조치 내역 DTO
data class ActuatorResponseDto(
    @SerializedName("commandId") val commandId: String,
    @SerializedName("actuator") val actuator: String,
    @SerializedName("action") val action: String,
    @SerializedName("durationMinutes") val durationMinutes: Int?,
    @SerializedName("result") val result: String
)

// 2. API-007-B 최근 작물 사진 응답 DTO
data class LatestCropImageResponse(
    @SerializedName("imageId") val imageId: Long,
    @SerializedName("capturedAt") val capturedAt: String,
    @SerializedName("species") val species: String,
    @SerializedName("imageUrl") val imageUrl: String
)

// 3. API-010 FCM 토큰 등록 요청 DTO
data class FcmTokenRequest(
    @SerializedName("token") val token: String,
    @SerializedName("deviceModel") val deviceModel: String = "Android"
)

// 공통 메시지 응답 DTO
data class MessageResponse(
    @SerializedName("message") val message: String?
)