package com.example.personalfinance.navigation

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
import com.example.personalfinance.ui.main.HomeScreen
import com.example.personalfinance.ui.main.LedgerScreen
import com.example.personalfinance.ui.main.MenuScreen
import com.example.personalfinance.ui.main.NewRecordScreen
import com.example.personalfinance.ui.main.NotificationDebugScreen
import com.example.personalfinance.ui.main.GachaScreen
import com.example.personalfinance.ui.auth.LoginScreen

// ── Routes ────────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Login     : Screen("login")
    object Home      : Screen("home")
    object Ledger    : Screen("ledger")
    object NewRecord : Screen("new_record")
    object Menu      : Screen("menu")
    object Gacha     : Screen("gacha")
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
                                    val request = com.example.personalfinance.network.SocialLoginRequest(kakaoToken, "KAKAO")
                                    val response = authApi.socialLogin(request)
                                    if (response.isSuccessful && response.body() != null) {
                                        val authData = response.body()!!
                                        tokenManager.saveTokens(authData.accessToken, authData.refreshToken)
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    } else {
                                        android.widget.Toast.makeText(context, "백엔드 로그인 실패", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
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
                                        val request = com.example.personalfinance.network.SocialLoginRequest(googleIdToken, "GOOGLE")
                                        val response = authApi.socialLogin(request)
                                        if (response.isSuccessful && response.body() != null) {
                                            val authData = response.body()!!
                                            tokenManager.saveTokens(authData.accessToken, authData.refreshToken)
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        } else {
                                            android.widget.Toast.makeText(context, "백엔드 로그인 실패", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
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
            MenuScreen(
                navController = navController,
                onNotificationDebugClick = {
                    navController.navigate(Screen.NotificationDebug.route)
                },
                onLogout = {
                    tokenManager.clearTokens()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true } // 모든 백스택을 지우고 로그인 화면으로
                    }
                }
            )      
        }
        composable(Screen.NotificationDebug.route) { NotificationDebugScreen(navController) }
        composable(Screen.Gacha.route)             { GachaScreen(navController)             }
    }
}
