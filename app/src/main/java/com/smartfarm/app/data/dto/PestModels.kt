package com.smartfarm.app.data.dto

import com.google.gson.annotations.SerializedName

/** API-008 병해충 진단 이력  GET /api/v1/farms/{farmId}/diagnoses?size=20 */
data class DiagnosisListResponse(
    @SerializedName("diagnoses") val diagnoses: List<DiagnosisDto>
)

/** 진단 한 건 */
data class DiagnosisDto(
    @SerializedName("diagnosisId") val diagnosisId: Long,
    // 진단한 작물 코드 ("Tomato" 등, 작물 API 의 species 와 같은 값).
    // 백엔드가 아직 안 보내면 null → diseaseCode 앞부분("tomato-A" → tomato)으로 대신 판단
    @SerializedName("species") val species: String? = null,
    @SerializedName("diagnosedAt") val diagnosedAt: String,          // "2026-11-20T10:10:44+09:00"
    @SerializedName("infected") val infected: Boolean,              // false = 병을 찾지 못함
    @SerializedName("diseaseCode") val diseaseCode: String?,         // "tomato-A"
    @SerializedName("diseaseName") val diseaseName: String?,         // "잎곰팡이병"
    @SerializedName("severityLevel") val severityLevel: String?,     // 초기 / 중기 / 말기
    @SerializedName("severityLowConfidence") val severityLowConfidence: Boolean?,
    @SerializedName("confidence") val confidence: Double?,           // 탐지 점수 0~1
    @SerializedName("imageUrl") val imageUrl: String?,               // 서버 상대 경로. 없으면 null
    @SerializedName("responses") val responses: List<ActuatorResponseDto>?, // 자동 조치. 없으면 []
    @SerializedName("guide") val guide: String?                      // 예방·방제 안내 문구
)

/**
 * 이 진단의 작물 코드. 응답의 species 를 우선 쓰고, 없으면 diseaseCode 앞부분을 씀.
 * 둘 다 없으면 null (작물을 알 수 없음)
 */
fun DiagnosisDto.cropSpecies(): String? =
    species?.takeIf { it.isNotBlank() }
        ?: diseaseCode?.substringBefore('-')?.takeIf { it.isNotBlank() }

/** 이 진단이 지금 고른 작물(species)의 진단인지. 작물을 알 수 없는 진단은 걸러내지 않음 */
fun DiagnosisDto.isForCrop(selectedSpecies: String): Boolean {
    val crop = cropSpecies() ?: return true
    return selectedSpecies.isBlank() || crop.equals(selectedSpecies, ignoreCase = true)
}

/** 실제 병해충 감염인지: infected=true 이고, 병명·코드가 정상(healthy)을 뜻하지 않는 것 */
fun DiagnosisDto.isRealInfection(): Boolean {
    if (!infected) return false
    val code = diseaseCode.orEmpty()
    return !diseaseName.orEmpty().contains("정상") &&
        !code.contains("healthy", ignoreCase = true) &&
        !code.contains("normal", ignoreCase = true)
}

/** 진단으로 자동 실행된 조치 한 건 */
data class ActuatorResponseDto(
    @SerializedName("commandId") val commandId: String,
    @SerializedName("actuator") val actuator: String,                // circFan
    @SerializedName("action") val action: String,                    // on / off
    @SerializedName("durationMinutes") val durationMinutes: Int?,    // 60
    @SerializedName("result") val result: String                     // ACKED 등
)

/** API-007-B 최근 작물 사진  GET /api/v1/farms/{farmId}/images/latest */
data class LatestCropImageResponse(
    @SerializedName("imageId") val imageId: Long,
    @SerializedName("capturedAt") val capturedAt: String,
    @SerializedName("species") val species: String,
    @SerializedName("imageUrl") val imageUrl: String
)

/** API-010 FCM 토큰 등록 요청  POST /api/v1/fcm/token */
data class FcmTokenRequest(
    @SerializedName("token") val token: String,
    @SerializedName("deviceModel") val deviceModel: String = "Android"
)
