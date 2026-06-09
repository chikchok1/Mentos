package com.example.personalfinance.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.personalfinance.ui.theme.Gray100
import com.example.personalfinance.ui.theme.Gray500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // State
    var notiEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", false)) }
    var locEnabled by remember { mutableStateOf(prefs.getBoolean("location_enabled", false)) }

    // 기기 설정으로 이동하는 함수
    val openAppSettings = {
        Toast.makeText(context, "기기 설정에서 권한을 직접 해제해 주세요.", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    // 앱 화면으로 돌아올 때마다 실제 시스템 권한 상태와 동기화
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasNoti = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true
                
                val hasLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                
                notiEnabled = hasNoti
                locEnabled = hasLoc
                
                prefs.edit()
                    .putBoolean("notifications_enabled", hasNoti)
                    .putBoolean("location_enabled", hasLoc)
                    .apply()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allEnabled = notiEnabled && locEnabled

    // 개별 권한 요청 런처
    val notiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        notiEnabled = isGranted
        prefs.edit().putBoolean("notifications_enabled", isGranted).apply()
    }

    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        locEnabled = isGranted
        prefs.edit().putBoolean("location_enabled", isGranted).apply()
    }

    // 전체 권한 요청 런처
    val multiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val nGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true
        
        val lGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        notiEnabled = nGranted
        locEnabled = lGranted
        prefs.edit()
            .putBoolean("notifications_enabled", nGranted)
            .putBoolean("location_enabled", lGranted)
            .apply()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("권한 설정", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            // 전체 권한 허용
            PermissionItem(
                title = "전체 권한 허용",
                description = "모든 필수 권한을 한 번에 허용하거나 해제합니다.",
                isChecked = allEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        multiLauncher.launch(perms.toTypedArray())
                    } else {
                        // 권한 해제 시 설정 화면으로 이동
                        openAppSettings()
                    }
                },
                isPrimary = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Gray100)

            // 알림 권한
            PermissionItem(
                title = "알림 접근 권한 허용",
                description = "결제 내역 및 앱 푸시 알림을 받기 위해 필요합니다.",
                isChecked = notiEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notiLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notiEnabled = true
                            prefs.edit().putBoolean("notifications_enabled", true).apply()
                        }
                    } else {
                        // 권한 해제 시 설정 화면으로 이동
                        openAppSettings()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 위치 권한
            PermissionItem(
                title = "위치 접근 권한 허용",
                description = "위치 기반 혜택 및 결제 위치 기록에 필요합니다.",
                isChecked = locEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        locLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        // 권한 해제 시 설정 화면으로 이동
                        openAppSettings()
                    }
                }
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isPrimary: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = if (isPrimary) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Gray500,
                fontSize = 13.sp
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF534AB7),
            )
        )
    }
}
