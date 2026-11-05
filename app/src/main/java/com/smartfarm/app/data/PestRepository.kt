package com.smartfarm.app.data

import com.smartfarm.app.data.api.PestApi
import com.smartfarm.app.data.dto.DiagnosisDto
import com.smartfarm.app.data.dto.FcmTokenRequest
import com.smartfarm.app.data.dto.LatestCropImageResponse
import com.smartfarm.app.data.network.ApiClient

class PestRepository(
    private val pestApi: PestApi = ApiClient.pestApi
) {
    // 1. API-008: 병해충 진단 이력 목록 조회
    suspend fun getDiagnoses(farmId: String = "greenhouse-01", size: Int = 20): Result<List<DiagnosisDto>> {
        return try {
            val response = pestApi.getDiagnoses(farmId, size)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.diagnoses)
            } else {
                Result.failure(Exception("진단 이력 조회 실패: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. API-007-B: 내 작물 최근 사진 조회
    suspend fun getLatestCropImage(farmId: String = "greenhouse-01"): Result<LatestCropImageResponse> {
        return try {
            val response = pestApi.getLatestCropImage(farmId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("최근 작물 사진 조회 실패: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. API-010: FCM 토큰 등록
    suspend fun registerFcmToken(token: String): Result<Unit> {
        return try {
            val response = pestApi.registerFcmToken(FcmTokenRequest(token = token))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("FCM 토큰 등록 실패: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}