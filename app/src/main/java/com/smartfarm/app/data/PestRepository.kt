package com.smartfarm.app.data

import com.smartfarm.app.data.api.PestApi
import com.smartfarm.app.data.dto.DiagnosisDto
import com.smartfarm.app.data.dto.FcmTokenRequest
import com.smartfarm.app.data.dto.LatestCropImageResponse
import com.smartfarm.app.data.network.ApiClient

/**
 * 병해충 진단·사진·알림 토큰 API 를 부르는 저장소.
 * 결과를 Result 로 감싸서, 실패해도 예외로 앱이 꺼지지 않고 화면이 실패 처리를 고를 수 있게 합니다.
 */
class PestRepository(
    private val pestApi: PestApi = ApiClient.pestApi
) {
    companion object {
        /** 농장 ID (현재 농장 하나) */
        const val DEFAULT_FARM_ID = "greenhouse-01"
    }

    /** API-008 병해충 진단 이력 (최신순) */
    suspend fun getDiagnoses(farmId: String = DEFAULT_FARM_ID, size: Int = 20): Result<List<DiagnosisDto>> =
        try {
            val response = pestApi.getDiagnoses(farmId, size)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.diagnoses)
            } else {
                Result.failure(Exception("진단 이력 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    /** API-007-B 내 작물 최근 사진 */
    suspend fun getLatestCropImage(farmId: String = DEFAULT_FARM_ID): Result<LatestCropImageResponse> =
        try {
            val response = pestApi.getLatestCropImage(farmId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("최근 작물 사진 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    /** API-010 FCM 토큰 등록 */
    suspend fun registerFcmToken(token: String): Result<Unit> =
        try {
            val response = pestApi.registerFcmToken(FcmTokenRequest(token = token))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("FCM 토큰 등록 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
