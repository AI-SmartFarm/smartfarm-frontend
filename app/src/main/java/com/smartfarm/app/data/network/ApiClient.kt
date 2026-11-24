package com.smartfarm.app.data.network

import com.smartfarm.app.data.api.FarmApiService
import com.smartfarm.app.data.api.PestApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 백엔드 통신 설정 (Retrofit). 앱 전체에서 하나만 만들어 같이 씁니다.
 */
object ApiClient {

    /**
     * 백엔드 서버 주소. 서버 주소가 바뀌면 이 한 줄만 고치면 됩니다.
     *  - https 라서 포트 번호가 필요 없음
     *  - 끝의 "/" 는 Retrofit 규칙이라 꼭 붙여야 함
     */
    const val BASE_URL = "https://search-inflation-documentation-fellowship.trycloudflare.com/"

    /** 서버가 주는 상대 경로("/api/v1/images/34/file")를 전체 주소로 바꿈 (사진 로딩용) */
    fun fullUrl(path: String): String = BASE_URL.trimEnd('/') + "/" + path.trimStart('/')

    /** 요청·응답 JSON 을 Logcat("okhttp")에 출력 (연동 확인용) */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /** 센서·작물 API (대시보드 / 온도 로그 / 토양 수분 로그) */
    val farmApiService: FarmApiService by lazy { retrofit.create(FarmApiService::class.java) }

    /** 병해충 진단·사진·알림 토큰 API (내 작물 확인 / 병해충 알림) */
    val pestApi: PestApi by lazy { retrofit.create(PestApi::class.java) }
}
