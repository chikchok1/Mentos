package com.example.personalfinance.network

import com.example.personalfinance.data.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // 발급받은 Access Token이 있다면 모든 API 요청의 Authorization 헤더에 추가
        tokenManager.getAccessToken()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
