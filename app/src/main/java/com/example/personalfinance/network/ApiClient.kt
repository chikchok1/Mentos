package com.example.personalfinance.network

import android.content.Context
import com.example.personalfinance.data.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // URL은 local.properties → BuildConfig.BASE_URL 에서 읽어오므로 코드에 하드코딩하지 않아도 됩니다.
    // 에뮬레이터: local.properties → BASE_URL=http://10.0.2.2:8080/
    // 실기기   : local.properties → BASE_URL=http://<PC_IP>:8080/
    private val BASE_URL = com.example.personalfinance.BuildConfig.BASE_URL

    private var retrofit: Retrofit? = null
    private var authApi: AuthApi? = null

    @Synchronized
    fun getAuthApi(context: Context, tokenManager: TokenManager): AuthApi {
        if (authApi == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                redactHeader("Authorization")
                level = if (com.example.personalfinance.BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
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
    private var classificationApi: ClassificationApi? = null

    @Synchronized
    fun getGachaApi(context: Context, tokenManager: TokenManager): GachaApi {
        getAuthApi(context, tokenManager)
        if (gachaApi == null) {
            gachaApi = retrofit!!.create(GachaApi::class.java)
        }
        return gachaApi!!
    }

    @Synchronized
    fun getClassificationApi(context: Context, tokenManager: TokenManager): ClassificationApi {
        getAuthApi(context, tokenManager)
        if (classificationApi == null) {
            classificationApi = retrofit!!.create(ClassificationApi::class.java)
        }
        return classificationApi!!
    }
}
