package com.smartfarm.app.data

import com.smartfarm.app.data.api.FarmApiService
import java.time.ZonedDateTime
import android.util.Log

class RealFarmRepository(private val apiService: FarmApiService) {

    /** 온도 로그 그래프용 데이터를 API에서 불러옵니다. */
    suspend fun getTemperatureHistory(farmId: String = "farm1", range: String = "24h"): ChartData {
        return try {
            val response = apiService.getTelemetry(farmId, range)

            Log.d("SmartFarmAPI", "==========================================")
            Log.d("SmartFarmAPI", "HTTP Code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
            Log.d("SmartFarmAPI", "Response Body: ${response.body()}")
            Log.d("SmartFarmAPI", "==========================================")


            if (response.isSuccessful && response.body() != null) {
                val points = response.body()!!.points
                val labels = points.map { parseHourLabel(it.time) }
                val values = points.map { it.temperature }
                ChartData(labels = labels, values = values)
            } else {
                ChartData(emptyList(), emptyList())
            }
        } catch (e: Exception) {
            ChartData(emptyList(), emptyList())
        }
    }

    /** 토양 수분 로그 그래프용 데이터를 API에서 불러옵니다. */
    suspend fun getSoilMoistureHistory(farmId: String = "farm1", range: String = "24h"): ChartData {
        return try {
            val response = apiService.getTelemetry(farmId, range)
            if (response.isSuccessful && response.body() != null) {
                val points = response.body()!!.points
                val labels = points.map { parseHourLabel(it.time) }
                val values = points.map { it.soilMoisture }
                ChartData(labels = labels, values = values)
            } else {
                ChartData(emptyList(), emptyList())
            }
        } catch (e: Exception) {
            ChartData(emptyList(), emptyList())
        }
    }

    /** ISO 8601 시간 문자열을 그래프 x축용 "10시" 형태로 변환 */
    private fun parseHourLabel(timeIso: String): String {
        return try {
            val zdt = ZonedDateTime.parse(timeIso)
            "${zdt.hour}시"
        } catch (e: Exception) {
            ""
        }
    }
}