package com.example.personalfinance.network

import android.content.Context
import com.example.personalfinance.data.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
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
            val authenticator = TokenAuthenticator(context, tokenManager) { authApi!! }

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
    private var transactionApi: TransactionApi? = null

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

    @Synchronized
    fun getTransactionApi(context: Context, tokenManager: TokenManager): TransactionApi {
        getAuthApi(context, tokenManager)
        if (transactionApi == null) {
            transactionApi = retrofit!!.create(TransactionApi::class.java)
        }
        return transactionApi!!
    }

    /** 로그아웃 시 호출 — 캐시된 API 인스턴스 및 Retrofit 초기화 */
    @Synchronized
    fun reset() {
        retrofit         = null
        authApi          = null
        gachaApi         = null
        classificationApi = null
        transactionApi   = null
    }
}
