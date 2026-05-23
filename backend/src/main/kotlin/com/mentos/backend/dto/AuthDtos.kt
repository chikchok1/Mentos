package com.mentos.backend.dto

data class SocialLoginRequest(
    val socialToken: String,
    val provider: String // "KAKAO" or "GOOGLE"
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)
