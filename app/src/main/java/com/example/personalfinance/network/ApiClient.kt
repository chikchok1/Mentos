package com.example.personalfinance.network

import android.content.Context
import com.example.personalfinance.data.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // TODO: 백엔드 서버의 실제 주소로 변경하세요. (안드로이드 에뮬레이터에서 로컬호스트 접근 시 10.0.2.2 사용)
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private var retrofit: Retrofit? = null
    private var authApi: AuthApi? = null

    fun getAuthApi(context: Context, tokenManager: TokenManager): AuthApi {
        if (authApi == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = AuthInterceptor(tokenManager)
            
            // Authenticator 내부에서 API Call이 필요하므로, provider 방식으로 넘깁니다.
            val authenticator = TokenAuthenticator(context, tokenManager) {
                authApi!!
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .authenticator(authenticator)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            authApi = retrofit!!.create(AuthApi::class.java)
        }
        return authApi!!
    }

    private var gachaApi: GachaApi? = null

    fun getGachaApi(context: Context, tokenManager: TokenManager): GachaApi {
        // AuthApi와 동일한 로직으로 Retrofit 초기화(이미 초기화되었다면 재사용)
        getAuthApi(context, tokenManager)
        if (gachaApi == null) {
            gachaApi = retrofit!!.create(GachaApi::class.java)
        }
        return gachaApi!!
    }
}
