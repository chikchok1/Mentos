package com.example.personalfinance.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenManager(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val sharedPreferences = try {
        EncryptedSharedPreferences.create(
            "auth_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // 앱 재설치 시 Keystore 키가 변경되어 발생하는 SecurityException 대응 (기존 백업된 파일 삭제 후 재생성)
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        val file = java.io.File(context.filesDir.parent + "/shared_prefs/auth_prefs.xml")
        if (file.exists()) {
            file.delete()
        }
        EncryptedSharedPreferences.create(
            "auth_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    /**
     * 로그인 성공 직후 JWT의 sub(userId)를 명시적으로 저장.
     * GachaStore 등 사용자 식별이 필요한 곳에서 사용.
     */
    fun saveUserId(userId: String) {
        sharedPreferences.edit().putString("user_id", userId).apply()
    }

    fun getUserId(): String? = sharedPreferences.getString("user_id", null)

    fun getAccessToken(): String? = sharedPreferences.getString("access_token", null)
    fun getRefreshToken(): String? = sharedPreferences.getString("refresh_token", null)

    fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }

    // ── 토큰 만료 전용 (TokenAuthenticator에서만 호출) ──────────────────────────
    // clearTokens(로그아웃)과 구분하기 위해 별도 함수로 분리
    private val _tokenExpired = MutableStateFlow(false)
    val tokenExpired: StateFlow<Boolean> = _tokenExpired.asStateFlow()

    fun expireTokens() {
        sharedPreferences.edit().clear().apply()
        _tokenExpired.value = true
    }

    fun resetExpiredFlag() {
        _tokenExpired.value = false
    }
}
