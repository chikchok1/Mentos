package com.example.personalfinance.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.personalfinance.notification.CardNotificationDiagnosticEntry
import com.example.personalfinance.data.UserStatsCalculator
import com.example.personalfinance.notification.CardNotificationDebugEntry
import com.example.personalfinance.notification.CardNotificationDebugStore
import com.example.personalfinance.notification.PaymentNotificationAnalysis
import com.example.personalfinance.notification.PaymentNotificationAnalysisStatus
import com.example.personalfinance.notification.PaymentNotificationAnalyzer
import com.example.personalfinance.notification.SamplePaymentNotification
import com.example.personalfinance.ui.theme.Gray100
import com.example.personalfinance.ui.theme.Gray200
import com.example.personalfinance.ui.theme.Gray500
import com.example.personalfinance.ui.theme.Gray600
import com.example.personalfinance.ui.theme.Gray700
import com.example.personalfinance.ui.theme.GreenSuccess
import com.example.personalfinance.ui.theme.RedDanger

@Composable
fun NotificationDebugScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestResult by CardNotificationDebugStore.latestResult.collectAsState()
    val recentDiagnostics by CardNotificationDebugStore.recentDiagnostics.collectAsState()
    var listenerEnabled by remember { mutableStateOf(context.isNotificationListenerEnabled()) }
    var canPostNotifications by remember {
        mutableStateOf(SamplePaymentNotification.canPostNotifications(context))
    }
    var manualPackageName by remember { mutableStateOf("com.example.personalfinance") }
    var manualTitle by remember { mutableStateOf("신한카드") }
    var manualText by remember { mutableStateOf("스타벅스 5,000원 승인") }
    var manualAnalysis by remember { mutableStateOf<PaymentNotificationAnalysis?>(null) }

    val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        canPostNotifications = SamplePaymentNotification.canPostNotifications(context)
        if (granted) {
            Toast.makeText(context, "테스트 알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "샘플 결제 알림 표시를 위해 테스트 알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = context.isNotificationListenerEnabled()
                canPostNotifications = SamplePaymentNotification.canPostNotifications(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "뒤로가기", tint = Gray600)
            }
            Text("결제 알림 테스트", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(48.dp))
        }

        HorizontalDivider(color = Gray100)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "알림 접근 권한은 Android 설정에서 사용자가 직접 켜야 합니다. 이 화면의 테스트는 앱이 직접 발생시킨 샘플 결제 알림만 파싱하며, 서버 전송은 하지 않습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray700
            )

            StatusPanel(listenerEnabled = listenerEnabled)

            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                Text("알림 접근 권한 설정 열기", modifier = Modifier.padding(start = 8.dp))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostNotifications) {
                PostNotificationPermissionPanel(
                    onRequestPermission = {
                        postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
            }

            Button(
                onClick = {
                    if (canPostNotifications) {
                        showSampleNotification(context)
                    } else {
                        Toast.makeText(
                            context,
                            "먼저 테스트 알림 권한을 허용해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Send, contentDescription = null)
                Text("샘플 결제 알림 발생", modifier = Modifier.padding(start = 8.dp))
            }

            ManualNotificationTestPanel(
                packageName = manualPackageName,
                onPackageNameChange = { manualPackageName = it },
                title = manualTitle,
                onTitleChange = { manualTitle = it },
                text = manualText,
                onTextChange = { manualText = it },
                analysis = manualAnalysis,
                onAnalyze = {
                    manualAnalysis = PaymentNotificationAnalyzer.analyze(
                        sourcePackage = manualPackageName,
                        title = manualTitle,
                        text = manualText
                    )
                }
            )

            LatestResultPanel(latestResult)

            RecentDiagnosticsPanel(recentDiagnostics)
        }
    }
}

@Composable
private fun PostNotificationPermissionPanel(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Gray200, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Android 13 이상에서는 샘플 결제 알림을 표시하려면 앱 알림 권한이 필요합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray700
        )
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Notifications, contentDescription = null)
            Text("테스트 알림 권한 요청", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun StatusPanel(listenerEnabled: Boolean) {
    val statusColor = if (listenerEnabled) GreenSuccess else RedDanger
    val statusText = if (listenerEnabled) "알림 접근 권한 켜짐" else "알림 접근 권한 꺼짐"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Gray200, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = statusColor)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(statusText, style = MaterialTheme.typography.titleMedium, color = statusColor)
            Text(
                "권한을 켠 뒤 샘플 알림을 발생시키면 Logcat과 아래 영역에 파싱 결과가 표시됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
    }
}

@Composable
private fun ManualNotificationTestPanel(
    packageName: String,
    onPackageNameChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    text: String,
    onTextChange: (String) -> Unit,
    analysis: PaymentNotificationAnalysis?,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Gray200, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("원문 파싱 테스트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = packageName,
            onValueChange = onPackageNameChange,
            label = { Text("패키지명") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("알림 제목") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text("알림 본문") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("원문 파싱 테스트")
        }

        ManualAnalysisResultPanel(analysis)
    }
}

@Composable
private fun ManualAnalysisResultPanel(analysis: PaymentNotificationAnalysis?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Gray200, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text("원문 테스트 결과", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        if (analysis == null) {
            Text("입력값으로 파싱 테스트를 실행해보세요.", style = MaterialTheme.typography.bodyMedium, color = Gray500)
            return@Column
        }

        ResultRow("상태", analysis.status.toDisplayText())
        ResultRow("점포명", analysis.parseResult.merchantName.ifBlank { "-" })
        ResultRow("금액", analysis.parseResult.amount?.let { "%,d원".format(it) } ?: "-")
        ResultRow("카테고리", analysis.category)
        ResultRow("획득 XP", analysis.earnedXP?.let { "${it}XP" } ?: "-")
        ResultRow("지출 반영 여부", "미반영")
    }
}

@Composable
private fun LatestResultPanel(entry: CardNotificationDebugEntry?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Gray200, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text("최근 파싱 결과", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        if (entry == null) {
            Text("아직 수신된 샘플 알림이 없습니다.", style = MaterialTheme.typography.bodyMedium, color = Gray500)
            return@Column
        }

        ResultRow("처리 상태", entry.handlingStatus.name)
        ResultRow("알림 유형", entry.notificationType.name)
        ResultRow("파싱 상태", entry.result.parseStatus.name)
        ResultRow("금액", entry.result.amount?.let { "%,d".format(it) } ?: "-")
        ResultRow("획득 XP", entry.result.amount?.let { UserStatsCalculator.calculateEarnedXP(it).toString() } ?: "-")
        ResultRow("카테고리", entry.category)
        ResultRow("가맹점", entry.result.merchantName.ifBlank { "-" })
        ResultRow("거래시각", entry.result.transactionDateTime?.toString() ?: "-")
        ResultRow("알림 제목", entry.title.ifBlank { "-" })
        ResultRow("알림 내용", entry.text.ifBlank { "-" })
        ResultRow("수신 앱", entry.sourcePackage)
        ResultRow("수신 시각", entry.receivedAt.toString())
    }
}

private fun PaymentNotificationAnalysisStatus.toDisplayText(): String =
    when (this) {
        PaymentNotificationAnalysisStatus.APPROVED -> "승인"
        PaymentNotificationAnalysisStatus.CANCELED -> "취소"
        PaymentNotificationAnalysisStatus.NEEDS_REVIEW -> "확인 필요"
        PaymentNotificationAnalysisStatus.PARSE_FAILED -> "파싱 실패"
    }

@Composable
private fun RecentDiagnosticsPanel(entries: List<CardNotificationDiagnosticEntry>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Gray200, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text("최근 감지 알림 진단", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Text("아직 감지된 알림이 없습니다.", style = MaterialTheme.typography.bodyMedium, color = Gray500)
            return@Column
        }

        entries.forEachIndexed { index, entry ->
            if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Gray100)
            }

            ResultRow("패키지", entry.packageName)
            ResultRow("제목", entry.title.ifBlank { "-" })
            ResultRow("상태", entry.status.name)
            ResultRow("이유", entry.reason)
            ResultRow("처리 여부", if (entry.handled) "처리됨" else "수신됨")
            ResultRow("수신 시각", entry.receivedAt.toString())
            entry.rawTextPreview?.let { preview ->
                ResultRow("본문 미리보기", preview)
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Gray500)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = Gray700,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

private fun showSampleNotification(context: Context) {
    val posted = SamplePaymentNotification.show(context)
    val message = if (posted) {
        "샘플 결제 알림을 발생시켰습니다."
    } else {
        "테스트 알림 권한이 필요합니다."
    }
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun Context.isNotificationListenerEnabled(): Boolean {
    val enabledListeners = Settings.Secure.getString(
        contentResolver,
        "enabled_notification_listeners"
    ).orEmpty()

    return enabledListeners.split(':').any { component ->
        component.contains(packageName, ignoreCase = true)
    }
}
