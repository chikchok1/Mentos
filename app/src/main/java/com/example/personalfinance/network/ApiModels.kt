package com.example.personalfinance.network

import com.google.gson.annotations.SerializedName

// 백엔드로 보낼 소셜 로그인 토큰 요청 객체
data class SocialLoginRequest(
    @SerializedName("socialToken") val socialToken: String,
    @SerializedName("provider") val provider: String // "KAKAO" or "GOOGLE"
)

// 백엔드로부터 받을 자체 JWT 응답 객체
data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
)

// 토큰 갱신 요청 객체
data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)
