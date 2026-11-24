package com.smartfarm.app.data.api

import com.smartfarm.app.data.dto.DiagnosisListResponse
import com.smartfarm.app.data.dto.FcmTokenRequest
import com.smartfarm.app.data.dto.LatestCropImageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 병해충 진단·사진·알림 토큰 API (내 작물 확인 / 병해충 알림).
 */
interface PestApi {

    /** API-008 병해충 진단 이력 (최신순) */
    @GET("api/v1/farms/{farmId}/diagnoses")
    suspend fun getDiagnoses(
        @Path("farmId") farmId: String,
        @Query("size") size: Int = 20
    ): Response<DiagnosisListResponse>

    /** API-007-B 내 작물 최근 사진 */
    @GET("api/v1/farms/{farmId}/images/latest")
    suspend fun getLatestCropImage(
        @Path("farmId") farmId: String
    ): Response<LatestCropImageResponse>

    /** API-010 FCM 푸시 알림 토큰 등록 (응답은 성공 여부만 사용) */
    @POST("api/v1/fcm/token")
    suspend fun registerFcmToken(
        @Body request: FcmTokenRequest
    ): Response<Unit>
}
