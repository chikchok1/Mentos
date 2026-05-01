package com.example.personalfinance.network

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    
    // 카카오/구글 소셜 로그인 진행 (회원가입/로그인 동시 처리)
    @POST("api/auth/social-login")
    suspend fun socialLogin(
        @Body request: SocialLoginRequest
    ): Response<AuthResponse>

    // Access Token이 만료되었을 때 Refresh Token을 보내서 새 토큰 발급
    @POST("api/auth/refresh")
    fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Call<AuthResponse> // Authenticator 안에서 동기식(execute)으로 호출하기 위해 Call 반환
}
