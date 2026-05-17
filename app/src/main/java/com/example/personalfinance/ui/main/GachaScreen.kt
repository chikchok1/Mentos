package com.example.personalfinance.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.example.personalfinance.data.GachaStore
import com.example.personalfinance.data.TokenManager
import com.example.personalfinance.network.ApiClient
import com.example.personalfinance.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── 캡슐머신 카드 데이터 ──────────────────────────────────────────────────────────

private data class CapsuleMachineData(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val descriptions: List<String>,
    val buttonText: String,
    val accentColor: Color,
)

// ── GachaScreen ───────────────────────────────────────────────────────────────

@Composable
fun GachaScreen(navController: NavController) {
    val context    = LocalContext.current
    val gachaStore = remember { GachaStore(context) }
    val tokenManager = remember { TokenManager(context) }
    val gachaApi = remember { ApiClient.getGachaApi(context, tokenManager) }
    val coroutineScope = rememberCoroutineScope()

    // ── 출석 가챠 상태 (서버 연동) ────────────────────────────────────────────────────────
    var attendanceUsed by remember { mutableStateOf(false) }
    // 1초마다 갱신되는 카운트다운 문자열 (UI용 로컬 계산)
    var countdownText  by remember { mutableStateOf(gachaStore.timeUntilMidnight()) }

    // 서버 상태 조회 및 타이머 루프
    LaunchedEffect(Unit) {
        // 백엔드에서 출석 여부 조회
        try {
            val response = gachaApi.getAttendanceStatus()
            if (response.isSuccessful) {
                attendanceUsed = response.body()?.get("usedToday") ?: false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 카운트다운 타이머 갱신
        while (true) {
            delay(1_000)
            countdownText = gachaStore.timeUntilMidnight()
        }
    }

    // ── 다른 가챠 상태 (추후 서버 연동 예정) ─────────────────────────────────
    var adWatched by remember { mutableStateOf(false) }
    var coins     by remember { mutableStateOf(25) }

    val machines = listOf(
        CapsuleMachineData(
            id           = "attendance",
            icon         = Icons.Rounded.CardGiftcard,
            title        = "출석 캡슐머신",
            descriptions = listOf("매일 출석하고 보상 받으세요", "매일 00시 자동 초기화"),
            buttonText   = "출석하고 뽑기",
            accentColor  = Color(0xFFFF6B6B),
        ),
        CapsuleMachineData(
            id           = "ad",
            icon         = Icons.Rounded.PlayCircle,
            title        = "광고 캡슐머신",
            descriptions = listOf("광고 보고 보상 받으세요"),
            buttonText   = "광고 보고 뽑기",
            accentColor  = Color(0xFF4ECDC4),
        ),
        CapsuleMachineData(
            id           = "coin",
            icon         = Icons.Rounded.MonetizationOn,
            title        = "코인 캡슐머신",
            descriptions = listOf("코인 10개를 소모해서 보상받으세요"),
            buttonText   = "코인 사용하고 뽑기",
            accentColor  = Color(0xFFFFB800),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── 상단 헤더 ──────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "뒤로", tint = Gray600)
            }
            Text(
                text       = "가챠",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(48.dp))
        }
        HorizontalDivider(color = Gray100)

        // ── 본문 ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // 타이틀 영역
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text       = "캡슐을 뽑으면\n다양한 보상을 받을 수 있어요",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp,
                    color      = Gray900,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = "매일 새로운 보상을 받아보세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500,
                )
            }

            // 카드 목록
            machines.forEach { machine ->
                val available = when (machine.id) {
                    "attendance" -> !attendanceUsed
                    "ad"         -> !adWatched
                    "coin"       -> coins >= 10
                    else         -> false
                }
                val statusLabel = when (machine.id) {
                    "attendance" -> if (attendanceUsed) "사용 완료" else "사용 가능"
                    "ad"         -> if (adWatched) "시청 완료" else "시청 가능"
                    "coin"       -> "${coins}개 보유"
                    else         -> ""
                }
                // 출석 가챠가 사용된 경우에만 카운트다운 표시
                val cooldownText = if (machine.id == "attendance" && attendanceUsed) {
                    countdownText
                } else {
                    null
                }

                CapsuleMachineCard(
                    data         = machine,
                    available    = available,
                    statusLabel  = statusLabel,
                    cooldownText = cooldownText,
                    onAction     = {
                        when (machine.id) {
                            "attendance" -> {
                                coroutineScope.launch {
                                    try {
                                        val response = gachaApi.performAttendanceGacha()
                                        if (response.isSuccessful) {
                                            attendanceUsed = true
                                            android.widget.Toast.makeText(context, "출석 보상을 받았습니다!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "이미 처리되었거나 오류가 발생했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "네트워크 오류가 발생했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            "ad"   -> adWatched = true
                            "coin" -> if (coins >= 10) coins -= 10
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
            }

            // 하단 안내
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "보상은 랜덤으로 지급됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray400,
                )
            }
        }
    }
}

// ── CapsuleMachineCard ────────────────────────────────────────────────────────

@Composable
private fun CapsuleMachineCard(
    data: CapsuleMachineData,
    available: Boolean,
    statusLabel: String,
    cooldownText: String?,       // null이면 카운트다운 미표시
    onAction: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val shakeOffset by animateFloatAsState(
        targetValue   = if (pressed) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 500
            0f  at 0
            -8f at 60
            8f  at 120
            -6f at 180
            6f  at 240
            0f  at 300
        },
        label = "shake",
    )

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(500)
            pressed = false
        }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border    = BorderStroke(1.dp, Gray100),
    ) {
        Box {
            // ── 배경 일러스트 (우상단) ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(width = 112.dp, height = 128.dp)
                    .padding(8.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCapsuleMachine(color = data.accentColor, alpha = 0.35f)
                }
            }

            // ── 카드 본문 ─────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(24.dp)) {

                Row(verticalAlignment = Alignment.Top) {
                    // 아이콘 박스
                    Box(
                        modifier         = Modifier
                            .size(56.dp)
                            .offset(x = shakeOffset.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(data.accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = data.icon,
                            contentDescription = null,
                            tint               = data.accentColor,
                            modifier           = Modifier.size(28.dp),
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    // 텍스트
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = data.title,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = Gray900,
                        )
                        Spacer(Modifier.height(4.dp))
                        data.descriptions.forEach { line ->
                            Text(
                                text  = line,
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── 버튼 + 상태 뱃지 ──────────────────────────────────────────
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 뽑기 버튼
                    Button(
                        onClick  = {
                            if (available) {
                                pressed = true
                                onAction()
                            }
                        },
                        enabled  = available,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = data.accentColor,
                            contentColor           = Color.White,
                            disabledContainerColor = Gray100,
                            disabledContentColor   = Gray400,
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (available) 2.dp else 0.dp,
                        ),
                    ) {
                        Text(
                            text       = data.buttonText,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // 상태 뱃지
                    Box(
                        modifier         = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (available) data.accentColor.copy(alpha = 0.1f)
                                else Gray50
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (available) data.accentColor else Gray400,
                        )
                    }
                }

                // ── 카운트다운 표시 (출석 가챠 사용 후) ───────────────────────
                if (cooldownText != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFF3F3))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint               = Color(0xFFFF6B6B),
                            modifier           = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text  = "다음 뽑기까지  $cooldownText",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF6B6B),
                        )
                    }
                }
            }
        }
    }
}

// ── 캡슐머신 SVG → Canvas 변환 ────────────────────────────────────────────────

private fun DrawScope.drawCapsuleMachine(color: Color, alpha: Float) {
    val w  = size.width
    val h  = size.height
    val sx = w / 120f
    val sy = h / 140f

    fun x(v: Float) = v * sx
    fun y(v: Float) = v * sy
    fun r(v: Float) = v * sx

    drawOval(
        color   = Color.Black.copy(alpha = 0.08f * alpha),
        topLeft = Offset(x(25f), y(124f)),
        size    = Size(x(70f), y(12f)),
    )
    drawRoundRect(
        color        = color.copy(alpha = 0.9f * alpha),
        topLeft      = Offset(x(35f), y(95f)),
        size         = Size(x(50f), y(30f)),
        cornerRadius = CornerRadius(r(8f)),
    )
    drawRoundRect(
        color        = Color.White.copy(alpha = 0.3f * alpha),
        topLeft      = Offset(x(45f), y(100f)),
        size         = Size(x(30f), y(18f)),
        cornerRadius = CornerRadius(r(4f)),
    )
    drawRoundRect(
        color        = color.copy(alpha = 0.15f * alpha),
        topLeft      = Offset(x(20f), y(35f)),
        size         = Size(x(80f), y(65f)),
        cornerRadius = CornerRadius(r(12f)),
    )
    drawOval(
        color   = Color.White.copy(alpha = 0.25f * alpha),
        topLeft = Offset(x(30f), y(20f)),
        size    = Size(x(60f), y(65f)),
    )

    data class Cap(val cx: Float, val cy: Float, val cr: Float, val a: Float)
    listOf(
        Cap(50f, 65f, 8f, 0.6f),
        Cap(70f, 70f, 7f, 0.8f),
        Cap(60f, 55f, 6f, 0.5f),
        Cap(45f, 75f, 6f, 0.7f),
    ).forEach { cap ->
        drawCircle(
            color  = color.copy(alpha = cap.a * alpha),
            radius = r(cap.cr),
            center = Offset(x(cap.cx), y(cap.cy)),
        )
    }

    drawOval(
        color   = color.copy(alpha = 0.9f * alpha),
        topLeft = Offset(x(35f), y(27f)),
        size    = Size(x(50f), y(16f)),
    )
    drawOval(
        color   = Color.White.copy(alpha = 0.2f * alpha),
        topLeft = Offset(x(35f), y(27f)),
        size    = Size(x(50f), y(16f)),
    )
    drawCircle(
        color  = color.copy(alpha = 0.9f * alpha),
        radius = r(12f),
        center = Offset(x(60f), y(22f)),
    )
    drawCircle(
        color  = Color.White.copy(alpha = 0.6f * alpha),
        radius = r(4f),
        center = Offset(x(56f), y(18f)),
    )
    drawCircle(
        color  = color.copy(alpha = 0.9f * alpha),
        radius = r(10f),
        center = Offset(x(95f), y(70f)),
    )
    drawCircle(
        color  = Color.White.copy(alpha = 0.3f * alpha),
        radius = r(5f),
        center = Offset(x(95f), y(70f)),
    )
}
