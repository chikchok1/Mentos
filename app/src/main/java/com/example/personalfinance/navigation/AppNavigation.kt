package com.example.personalfinance.navigation

import android.util.Base64
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.personalfinance.data.TokenManager
import com.example.personalfinance.data.UserStatsStore
import com.example.personalfinance.network.ApiClient
import org.json.JSONObject
import com.example.personalfinance.ui.main.HomeScreen
import com.example.personalfinance.ui.main.LedgerScreen
import com.example.personalfinance.ui.main.MenuScreen
import com.example.personalfinance.ui.main.NewRecordScreen
import com.example.personalfinance.ui.main.NotificationDebugScreen
import com.example.personalfinance.ui.main.GachaScreen
import com.example.personalfinance.ui.main.InventoryScreen
import com.example.personalfinance.ui.auth.LoginScreen

// ── Routes ────────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Login     : Screen("login")
    object Home      : Screen("home")
    object Ledger    : Screen("ledger")
    object NewRecord : Screen("new_record")
    object Menu      : Screen("menu")
    object Gacha     : Screen("gacha")
    object Inventory : Screen("inventory")
    object NotificationDebug : Screen("notification_debug")
}

// ── Navigation Host ───────────────────────────────────────────────────────────

@Composable
fun AppNavigation(tokenManager: TokenManager) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = tokenManager.getAccessToken()
        startDestination = if (token != null) Screen.Home.route else Screen.Login.route
        Log.i(TAG, "App start auth state: hasAccessToken=${token != null}, startDestination=$startDestination")
    }

    if (startDestination == null) {
        // Token 검증 중일 때 보여줄 스플래시 화면 또는 빈 화면
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination!!
    ) {
        composable(Screen.Login.route) { 
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

            val authApi = remember { com.example.personalfinance.network.ApiClient.getAuthApi(context, tokenManager) }

            LoginScreen(
                onKakaoLogin = {
                    com.example.personalfinance.ui.auth.SocialLoginHelper.loginWithKakao(
                        context = context,
                        onSuccess = { kakaoToken ->
                            coroutineScope.launch {
                                try {
                                    Log.i(TAG, "Requesting backend social-login provider=KAKAO")
                                    val request = com.example.personalfinance.network.SocialLoginRequest(kakaoToken, "KAKAO")
                                    val response = authApi.socialLogin(request)
                                    if (response.isSuccessful && response.body() != null) {
                                        Log.i(TAG, "Backend social-login succeeded provider=KAKAO")
                                        val authData = response.body()!!
                                        tokenManager.saveTokens(authData.accessToken, authData.refreshToken)
                                        // JWT sub(userId) 추출 후 명시적으로 저장
                                        extractUserIdFromJwt(authData.accessToken)?.let { tokenManager.saveUserId(it) }
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    } else {
                                        Log.w(TAG, "Backend social-login failed provider=KAKAO status=${response.code()}")
                                        android.widget.Toast.makeText(context, "백엔드 로그인 실패", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Backend social-login error provider=KAKAO", e)
                                    android.widget.Toast.makeText(context, "네트워크 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onFailure = { error ->
                            android.widget.Toast.makeText(context, "카카오 로그인 실패: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onGoogleLogin = {
                    activity?.let {
                        com.example.personalfinance.ui.auth.SocialLoginHelper.loginWithGoogle(
                            activity = it,
                            coroutineScope = coroutineScope,
                            onSuccess = { googleIdToken ->
                                coroutineScope.launch {
                                    try {
                                        Log.i(TAG, "Requesting backend social-login provider=GOOGLE")
                                        val request = com.example.personalfinance.network.SocialLoginRequest(googleIdToken, "GOOGLE")
                                        val response = authApi.socialLogin(request)
                                        if (response.isSuccessful && response.body() != null) {
                                            Log.i(TAG, "Backend social-login succeeded provider=GOOGLE")
                                            val authData = response.body()!!
                                            tokenManager.saveTokens(authData.accessToken, authData.refreshToken)
                                            // JWT sub(userId) 추출 후 명시적으로 저장
                                            extractUserIdFromJwt(authData.accessToken)?.let { tokenManager.saveUserId(it) }
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        } else {
                                            Log.w(TAG, "Backend social-login failed provider=GOOGLE status=${response.code()}")
                                            android.widget.Toast.makeText(context, "백엔드 로그인 실패", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Backend social-login error provider=GOOGLE", e)
                                        android.widget.Toast.makeText(context, "네트워크 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onFailure = { error ->
                                android.widget.Toast.makeText(context, "구글 로그인 실패: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            ) 
        }
        composable(Screen.Home.route)      { HomeScreen(navController)      }
        composable(Screen.Ledger.route)    { LedgerScreen(navController)    }
        composable(Screen.NewRecord.route) { NewRecordScreen(navController) }
        composable(Screen.Menu.route)      { 
            val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
            MenuScreen(
                navController = navController,
                onNotificationDebugClick = {
                    navController.navigate(Screen.NotificationDebug.route)
                },
                onLogout = {
                    UserStatsStore.getInstance(context).clearForLogout()
                    tokenManager.clearTokens()
                    ApiClient.reset()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true } // 모든 백스택을 지우고 로그인 화면으로
                    }
                }
            )      
        }
        composable(Screen.NotificationDebug.route) { NotificationDebugScreen(navController) }
        composable(Screen.Gacha.route)             { GachaScreen(navController)             }
        composable(Screen.Inventory.route)         { InventoryScreen(navController)         }
    }
}

// ── JWT Payload에서 sub(userId) 추출 ─────────────────────────────────────────

/**
 * JWT AccessToken의 payload를 Base64url 디코딩해 sub 필드를 반환.
 * 파싱 실패 시 null 반환.
 */
fun extractUserIdFromJwt(token: String): String? {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return null
        // JWT payload는 Base64url(패딩 없음) → 패딩 추가 후 디코딩
        val payload = parts[1].let { it + "=".repeat((4 - it.length % 4) % 4) }
        val json = JSONObject(String(Base64.decode(payload, Base64.URL_SAFE), Charsets.UTF_8))
        json.optString("sub").takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}

private const val TAG = "AppNavigation"
