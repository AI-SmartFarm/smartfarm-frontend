package com.smartfarm.app.data.network

import com.smartfarm.app.data.api.FarmApiService
import com.smartfarm.app.data.api.PestApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // 백엔드 테스트 서버 (Cloudflare 임시 터널 → 주소가 바뀌면 백엔드가 단톡에 공지, 여기 한 줄만 고치면 됨)
    // https 라 포트 번호 불필요. 끝의 "/" 는 Retrofit 규칙이라 꼭 붙여야 함
    // (내 PC 에서 백엔드를 직접 돌려 에뮬레이터로 볼 때는 "http://10.0.2.2:8080/")
    const val BASE_URL = "https://search-inflation-documentation-fellowship.trycloudflare.com/"

    /** 서버가 주는 상대 경로(예: "/api/v1/images/34/file")를 전체 주소로. 사진 로딩(Coil 등)에 사용 */
    fun fullUrl(path: String): String = BASE_URL.trimEnd('/') + "/" + path.trimStart('/')

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