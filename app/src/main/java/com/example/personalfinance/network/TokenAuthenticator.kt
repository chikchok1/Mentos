package com.example.personalfinance.network

import android.content.Context
import com.example.personalfinance.data.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val context: Context,
    private val tokenManager: TokenManager,
    private val authApiProvider: () -> AuthApi // 순환 참조를 막기 위해 지연 주입(provider) 형태로 받음
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = tokenManager.getRefreshToken() ?: return null

        // 동기적으로 새로운 토큰을 발급받는 API 호출
        val call = authApiProvider().refreshToken(RefreshTokenRequest(refreshToken))
        
        try {
            val refreshResponse = call.execute()
            if (refreshResponse.isSuccessful) {
                val newTokens = refreshResponse.body()
                if (newTokens != null) {
                    // 서버로부터 새 Access Token을 정상적으로 받았다면 로컬에 저장
                    tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)

                    // 원래 실패했던 요청의 헤더만 새 토큰으로 교체하여 재시도
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Refresh Token도 만료되거나 검증에 실패한 경우
        // 로컬 토큰 완전 삭제
        tokenManager.clearTokens() 

        // UI navigation should be handled by the UI layer after it observes the cleared token state.

        return null
    }
}
