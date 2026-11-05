package com.smartfarm.app.data.api

import com.smartfarm.app.data.dto.ControlLogResponse
import com.smartfarm.app.data.dto.StatusResponse
import com.smartfarm.app.data.dto.TelemetryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 센서 쪽 API (대시보드 / 온도 로그 / 토양 수분 로그) - 주담당
 * 병해충·사진 API 는 보조 담당의 별도 인터페이스에 둡니다 (같은 파일을 동시에 고치지 않기 위해)
 */
interface FarmApiService {

    /** API-002-A 현재 농장 상태 (현재 온도·토양 수분) */
    @GET("api/v1/farms/{farmId}/status")
    suspend fun getStatus(
        @Path("farmId") farmId: String
    ): Response<StatusResponse>

    /** API-002-B 환경 데이터 추이 (그래프) */
    @GET("api/v1/farms/{farmId}/telemetry")
    suspend fun getTelemetry(
        @Path("farmId") farmId: String,
        @Query("range") range: String = "24h"
    ): Response<TelemetryResponse>

    /** API-007-A 제어 활동 로그. type = TEMP(온도·순환팬) / SOIL(토양 수분·관수) */
    @GET("api/v1/farms/{farmId}/control-logs")
    suspend fun getControlLogs(
        @Path("farmId") farmId: String,
        @Query("type") type: String,
        @Query("size") size: Int = 20
    ): Response<ControlLogResponse>
}
