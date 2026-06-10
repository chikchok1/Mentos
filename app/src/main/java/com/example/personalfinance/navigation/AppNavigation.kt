package com.example.personalfinance.navigation

import android.util.Base64
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.personalfinance.data.TokenManager
import com.example.personalfinance.data.CharacterAppearanceStore
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
import com.example.personalfinance.ui.main.ShopScreen
import com.example.personalfinance.ui.auth.LoginScreen
import com.example.personalfinance.ui.main.FriendComparisonScreen
import com.example.personalfinance.ui.main.FriendsScreen
import com.example.personalfinance.ui.main.GachaDebugScreen
import com.example.personalfinance.ui.main.ProfileScreen
import com.example.personalfinance.ui.main.PermissionSettingsScreen

// ── Routes ────────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Login     : Screen("login")
    object Home      : Screen("home")
    object Ledger    : Screen("ledger")
    object NewRecord : Screen("new_record")
    object Menu      : Screen("menu")
    object Gacha     : Screen("gacha")
    object Inventory : Screen("inventory")
    object Shop      : Screen("shop")
    object Profile   : Screen("profile")
    object Friends   : Screen("friends")
    object FriendComparison : Screen("friends/{friendId}/comparison") {
        fun route(friendId: Long): String = "friends/$friendId/comparison"
    }
    object NotificationDebug : Screen("notification_debug")
    object GachaDebug : Screen("gacha_debug")
    object PermissionSettings : Screen("permission_settings")
}

// ── Navigation Host ───────────────────────────────────────────────────────────

@Composable
fun AppNavigation(tokenManager: TokenManager) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val token = tokenManager.getAccessToken()
        startDestination = if (token != null) Screen.Home.route else Screen.Login.route
        Log.i(TAG, "App start auth state: hasAccessToken=${token != null}, startDestination=$startDestination")
        if (token != null) {
            // 앱 재시작 시(토큰 있음) 서버 데이터 복원
            // restoreFromServer: 거래 내역 기반 로컬 계산 레벨 적용
            // refreshServerStats: 서버 기준 레벨로 덮어씀 → 홈/프로필 레벨 일치
            withContext(Dispatchers.IO) {
                UserStatsStore.getInstance(context).refreshProfile()
                UserStatsStore.getInstance(context).restoreFromServer()
                UserStatsStore.getInstance(context).refreshServerStats()
                CharacterAppearanceStore.getInstance(context).restoreFromServer()
            }
        }
    }

    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    // ── 토큰 만료 감지 → 로그인 화면으로 강제 이동 ──────────────────────
    // clearTokens(로그아웃)는 여기서 반응하지 않음 — MenuScreen onLogout이 직접 처리
    val tokenExpired by tokenManager.tokenExpired.collectAsState()

    LaunchedEffect(tokenExpired) {
        if (tokenExpired) {
            tokenManager.resetExpiredFlag()
            UserStatsStore.getInstance(context).clearForLogout()
            CharacterAppearanceStore.getInstance(context).clearForLogout()
            ApiClient.reset()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
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
                                        extractUserIdFromJwt(authData.accessToken)?.let { tokenManager.saveUserId(it) }
                                        UserStatsStore.getInstance(context).refreshProfile()
                                        UserStatsStore.getInstance(context).restoreFromServer()
                                        CharacterAppearanceStore.getInstance(context).restoreFromServer()
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
                                            extractUserIdFromJwt(authData.accessToken)?.let { tokenManager.saveUserId(it) }
                                            UserStatsStore.getInstance(context).refreshProfile()
                                            UserStatsStore.getInstance(context).restoreFromServer()
                                            CharacterAppearanceStore.getInstance(context).restoreFromServer()
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
        composable(Screen.Home.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            var showPermissionDialog by remember { mutableStateOf(false) }

            val multiLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) {
                showPermissionDialog = false
            }

            LaunchedEffect(Unit) {
                val hasNoti = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true
                val hasLoc = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (!hasNoti || !hasLoc) {
                    showPermissionDialog = true
                }
            }
            
            HomeScreen(navController)

            if (showPermissionDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { androidx.compose.material3.Text("앱 이용을 위한 권한 안내", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                    text = {
                        androidx.compose.foundation.layout.Column {
                            androidx.compose.material3.Text("원활한 서비스 제공을 위해 다음 권한이 필요합니다.", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
                            androidx.compose.material3.Text("🔔 알림 허용", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            androidx.compose.material3.Text("알림을 허용해야 결제 내역을 실시간으로 추적할 수 있습니다.", color = androidx.compose.ui.graphics.Color.DarkGray, fontSize = 13.sp)
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                            androidx.compose.material3.Text("📍 위치 허용", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            androidx.compose.material3.Text("위치를 허용해야 결제 장소를 정확하게 파악하고 분류할 수 있습니다.", color = androidx.compose.ui.graphics.Color.DarkGray, fontSize = 13.sp)
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                            androidx.compose.material3.Text("지금 권한을 허용하시겠습니까?", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                val perms = mutableListOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                                multiLauncher.launch(perms.toTypedArray())
                            }
                        ) {
                            androidx.compose.material3.Text("권한 허용하기")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { showPermissionDialog = false }
                        ) {
                            androidx.compose.material3.Text("나중에", color = androidx.compose.ui.graphics.Color.Gray)
                        }
                    }
                )
            }
        }
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
                    CharacterAppearanceStore.getInstance(context).clearForLogout()
                    tokenManager.clearTokens()
                    ApiClient.reset()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )      
        }
        composable(Screen.Profile.route)              { ProfileScreen(navController)              }
        composable(Screen.Friends.route)              { FriendsScreen(navController)              }
        composable(Screen.FriendComparison.route) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId")?.toLongOrNull() ?: 0L
            FriendComparisonScreen(navController, friendId)
        }
        composable(Screen.NotificationDebug.route)    { NotificationDebugScreen(navController)    }
        composable(Screen.Gacha.route)                { GachaScreen(navController)                }
        composable(Screen.Inventory.route)            { InventoryScreen(navController)            }
        composable(Screen.Shop.route)                 { ShopScreen(navController)                 }
        composable(Screen.GachaDebug.route)           { GachaDebugScreen(navController)           }
        composable(Screen.PermissionSettings.route)   { PermissionSettingsScreen(navController)   }
    }
}

// ── JWT Payload에서 sub(userId) 추출 ─────────────────────────────────────────

fun extractUserIdFromJwt(token: String): String? {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return null
        val payload = parts[1].let { it + "=".repeat((4 - it.length % 4) % 4) }
        val json = JSONObject(String(Base64.decode(payload, Base64.URL_SAFE), Charsets.UTF_8))
        json.optString("sub").takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}

private const val TAG = "AppNavigation"
