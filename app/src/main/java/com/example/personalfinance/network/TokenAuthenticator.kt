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
        // 동시에 여러 요청이 401을 받으면 이 블록이 동시에 실행될 수 있음 → synchronized로 직렬화
        synchronized(this) {
            // 이미 다른 스레드가 토큰을 갱신했는지 확인
            // 현재 저장된 토큰과 실패한 요청의 토큰이 다르면 → 이미 갱신된 것이므로 새 토큰으로 재시도
            val currentToken = tokenManager.getAccessToken()
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")

            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // Refresh Token이 없으면 복구 불가 → 만료 처리
            val refreshToken = tokenManager.getRefreshToken() ?: run {
                tokenManager.expireTokens()
                return null
            }

            return try {
                val refreshResponse = authApiProvider()
                    .refreshToken(RefreshTokenRequest(refreshToken))
                    .execute()

                if (refreshResponse.isSuccessful) {
                    val newTokens = refreshResponse.body()
                    if (newTokens != null) {
                        // 갱신 성공 → 새 토큰 저장 후 원래 요청 재시도
                        tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.accessToken}")
                            .build()
                    } else {
                        // 응답은 왔는데 바디가 없는 경우 → 만료 처리
                        tokenManager.expireTokens()
                        null
                    }
                } else {
                    // Refresh Token 자체가 만료되거나 서버에서 거부한 경우 → 만료 처리
                    tokenManager.expireTokens()
                    null
                }
            } catch (e: Exception) {
                // 네트워크 오류(오프라인 등) → 만료로 간주하지 않음, 그냥 요청 실패 처리
                e.printStackTrace()
                null
            }
        }
    }
}
