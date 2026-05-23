package com.example.personalfinance.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SocialLoginHelper {
    private const val GOOGLE_WEB_CLIENT_ID_NOT_CONFIGURED_MESSAGE =
        "Google Web Client ID가 설정되지 않았습니다."
    
    // ==========================================
    // 카카오 로그인
    // ==========================================
    fun loginWithKakao(context: Context, onSuccess: (String) -> Unit, onFailure: (Throwable) -> Unit) {
        Log.i("KAKAO_LOGIN", "Kakao login clicked")

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("KAKAO_LOGIN", "카카오계정으로 로그인 실패", error)
                onFailure(error)
            } else if (token != null) {
                Log.i("KAKAO_LOGIN", "카카오계정으로 로그인 성공")
                // 백엔드에 토큰 전달 등 후속 처리
                onSuccess(token.accessToken)
            }
        }

        // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    Log.e("KAKAO_LOGIN", "카카오톡으로 로그인 실패", error)
                    // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                    // 의도적인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        onFailure(error)
                        return@loginWithKakaoTalk
                    }
                    // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                } else if (token != null) {
                    Log.i("KAKAO_LOGIN", "카카오톡으로 로그인 성공")
                    onSuccess(token.accessToken)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
        }
    }

    // ==========================================
    // 구글 로그인 (Credential Manager)
    // ==========================================
    fun loginWithGoogle(
        activity: Activity,
        coroutineScope: CoroutineScope,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val webClientId = com.example.personalfinance.BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (webClientId.isBlank()) {
            Log.w("GOOGLE_LOGIN", GOOGLE_WEB_CLIENT_ID_NOT_CONFIGURED_MESSAGE)
            onFailure(IllegalStateException(GOOGLE_WEB_CLIENT_ID_NOT_CONFIGURED_MESSAGE))
            return
        }

        val credentialManager = CredentialManager.create(activity)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            // .setNonce("가급적 서버에서 생성한 난수 사용 권장")
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        coroutineScope.launch(Dispatchers.Main) {
            try {
                val result: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = activity
                )
                
                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential && 
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    Log.i("GOOGLE_LOGIN", "구글 로그인 성공.")
                    onSuccess(idToken)
                } else {
                    Log.e("GOOGLE_LOGIN", "예상치 못한 인증 유형입니다.")
                    onFailure(Exception("Unexpected credential type"))
                }

            } catch (e: GetCredentialException) {
                Log.e("GOOGLE_LOGIN", "구글 로그인 실패: ${e.message}")
                onFailure(e)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("GOOGLE_LOGIN", "토큰 파싱 실패: ${e.message}")
                onFailure(e)
            }
        }
    }
}
