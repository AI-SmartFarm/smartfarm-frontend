package com.smartfarm.app.data.api

import com.smartfarm.app.data.dto.TelemetryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FarmApiService {

    @GET("api/v1/farms/{farmId}/telemetry")
    suspend fun getTelemetry(
        @Path("farmId") farmId: String,
        @Query("range") range: String = "24h"
    ): Response<TelemetryResponse>


}