package com.mentos.backend.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.fasterxml.jackson.databind.ObjectMapper

@Service
class OAuthService(
    @Value("\${app.oauth.google.client-id}") private val googleClientId: String
) {
    private val httpClient = OkHttpClient()
    private val objectMapper = ObjectMapper()

    fun verifyGoogleToken(idTokenString: String): OAuthUserInfo {
        val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
            .setAudience(listOf(googleClientId))
            .build()

        val idToken = verifier.verify(idTokenString) 
            ?: throw IllegalArgumentException("유효하지 않은 구글 ID 토큰입니다.")
        
        val payload = idToken.payload
        return OAuthUserInfo(
            socialId = payload.subject,
            email = payload.email,
            provider = "GOOGLE"
        )
    }

    fun verifyKakaoToken(accessToken: String): OAuthUserInfo {
        // 카카오 API를 호출해서 사용자 정보를 가져옴
        val request = Request.Builder()
            .url("https://kapi.kakao.com/v2/user/me")
            .header("Authorization", "Bearer $accessToken")
            .header("Content-type", "application/x-www-form-urlencoded;charset=utf-8")
            .build()

        val response = httpClient.newCall(request).execute()
        response.use { resp ->
        if (!resp.isSuccessful) {
            throw IllegalArgumentException("카카오 토큰 검증에 실패했습니다.")
        }

        val responseBody = resp.body?.string() ?: throw IllegalArgumentException("응답이 비어있습니다.")
        val json = objectMapper.readTree(responseBody)
        
        val socialId = json.get("id").asText()
        val email = json.path("kakao_account")
            .path("email")
            .takeUnless { it.isMissingNode || it.isNull }
            ?.asText()
            ?.takeIf { it.isNotBlank() }

        return OAuthUserInfo(
            socialId = socialId,
            email = email,
            provider = "KAKAO"
        )
        }
    }
}

data class OAuthUserInfo(
    val socialId: String,
    val email: String?,
    val provider: String
)
