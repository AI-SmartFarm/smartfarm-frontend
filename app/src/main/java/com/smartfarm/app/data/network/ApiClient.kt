package com.smartfarm.app.data.network

import com.smartfarm.app.data.api.FarmApiService
import com.smartfarm.app.data.api.PestApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // 💡 에뮬레이터에서 내 컴퓨터(로컬 서버/시뮬레이터)를 바라볼 때는 10.0.2.2 사용
    // 외부 백엔드 서버가 있다면 해당 IP/도메인으로 변경 (예: "http://192.168.0.10:8080/")
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // 통신 로그(요청/응답 JSON)를 Logcat에 출력해주는 설정
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

    val farmApiService: FarmApiService by lazy { // 주 담당자용 Api 파일
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FarmApiService::class.java)
    }

    // 보조(세민 님)용 (신규)
    val pestApi: PestApi by lazy {
        retrofit.create(PestApi::class.java)
    }

}