package com.example.personalfinance.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.personalfinance.data.*
import com.example.personalfinance.network.ApiClient
import com.example.personalfinance.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

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
    val context        = LocalContext.current
    val gachaStore     = remember { GachaStore(context) }
    val coroutineScope = rememberCoroutineScope()
    val tokenManager   = remember { TokenManager(context) }
    val gachaApi       = remember { ApiClient.getGachaApi(context, tokenManager) }

    // ── 상태 ──────────────────────────────────────────────────────────────────
    var attendanceUsed    by remember { mutableStateOf(false) }
    var countdownText     by remember { mutableStateOf(gachaStore.timeUntilMidnight()) }
    var coins             by remember { mutableStateOf(0) }
    var gachaResult       by remember { mutableStateOf<GachaResult?>(null) }
    var showResultDialog  by remember { mutableStateOf(false) }
    var showProbDialog    by remember { mutableStateOf(false) }

    // 서버 상태 조회 및 타이머 루프
    LaunchedEffect(Unit) {
        try {
            val statusRes = gachaApi.getAttendanceStatus()
            if (statusRes.isSuccessful) {
                attendanceUsed = statusRes.body()?.get("usedToday") ?: false
            }
            val stateRes = gachaApi.getUserGachaState()
            if (stateRes.isSuccessful) {
                coins = (stateRes.body()?.get("coins") as? Number)?.toInt() ?: 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        while (true) {
            delay(1_000)
            countdownText = gachaStore.timeUntilMidnight()
        }
    }

    var adWatched by remember { mutableStateOf(false) }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFF8E1))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector        = Icons.Rounded.MonetizationOn,
                    contentDescription = null,
                    tint               = Color(0xFFFFB800),
                    modifier           = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text       = "$coins",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFFE65100)
                )
            }
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

            // ── 확률 보기 버튼 ────────────────────────────────────────────
            OutlinedButton(
                onClick  = { showProbDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                border   = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            ) {
                Icon(
                    imageVector        = Icons.Rounded.BarChart,
                    contentDescription = null,
                    tint               = Gray600,
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = "확률 보기",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray600,
                )
            }
            Spacer(Modifier.height(20.dp))

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
                val cooldownText = if (machine.id == "attendance" && attendanceUsed) countdownText else null

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
                                            val body       = response.body()
                                            val itemId     = body?.get("itemId") as? String
                                            val isDup      = body?.get("isDuplicate") as? Boolean ?: false
                                            val coinRew    = (body?.get("coinReward") as? Number)?.toInt() ?: 0
                                            val totalCoins = (body?.get("totalCoins") as? Number)?.toInt() ?: 0
                                            if (itemId != null) {
                                                val item = GachaItemPool.findById(itemId)
                                                if (item != null) {
                                                    gachaResult = if (isDup)
                                                        GachaResult.DuplicateCoin(item, coinRew)
                                                    else
                                                        GachaResult.NewItem(item)
                                                    coins          = totalCoins
                                                    attendanceUsed = true
                                                    showResultDialog = true
                                                }
                                            }
                                        } else {
                                            android.widget.Toast.makeText(context, "오류가 발생했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "네트워크 오류", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            "ad"   -> adWatched = true
                            "coin" -> {
                                coroutineScope.launch {
                                    try {
                                        val response = gachaApi.performCoinGacha()
                                        if (response.isSuccessful) {
                                            val body       = response.body()
                                            val itemId     = body?.get("itemId") as? String
                                            val isDup      = body?.get("isDuplicate") as? Boolean ?: false
                                            val coinRew    = (body?.get("coinReward") as? Number)?.toInt() ?: 0
                                            val totalCoins = (body?.get("totalCoins") as? Number)?.toInt() ?: 0
                                            if (itemId != null) {
                                                val item = GachaItemPool.findById(itemId)
                                                if (item != null) {
                                                    gachaResult = if (isDup)
                                                        GachaResult.DuplicateCoin(item, coinRew)
                                                    else
                                                        GachaResult.NewItem(item)
                                                    coins           = totalCoins
                                                    showResultDialog = true
                                                }
                                            }
                                        } else {
                                            val errBody = response.errorBody()?.string() ?: ""
                                            val msg = if (errBody.contains("부족")) "코인이 부족합니다 (필요: 10개)" else "오류가 발생했습니다."
                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "네트워크 오류", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
            }

            Box(
                modifier         = Modifier.fillMaxWidth().padding(vertical = 16.dp),
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

    // ── 가챠 결과 연출 ────────────────────────────────────────────────────────
    if (showResultDialog && gachaResult != null) {
        CinematicGachaReveal(
            result    = gachaResult!!,
            onDismiss = { showResultDialog = false }
        )
    }

    // ── 확률 보기 다이얼로그 ──────────────────────────────────────────────────
    if (showProbDialog) {
        GachaProbabilityDialog(onDismiss = { showProbDialog = false })
    }
}

// ── 확률 다이얼로그 ────────────────────────────────────────────────────────────

private data class GradeInfo(val name: String, val pct: String, val color: Color)

private val attendanceGrades = listOf(
    GradeInfo("Common",    "60%", Color(0xFF9E9E9E)),
    GradeInfo("Rare",      "25%", Color(0xFF1976D2)),
    GradeInfo("Unique",    "10%", Color(0xFF7B1FA2)),
    GradeInfo("Legendary",  "5%", Color(0xFFE65100)),
)

private val coinGrades = listOf(
    GradeInfo("Common",    "55%", Color(0xFF9E9E9E)),
    GradeInfo("Rare",      "30%", Color(0xFF1976D2)),
    GradeInfo("Unique",    "10%", Color(0xFF7B1FA2)),
    GradeInfo("Legendary",  "5%", Color(0xFFE65100)),
)

@Composable
private fun GachaProbabilityDialog(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }  // 0 = 출석, 1 = 코인

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // 제목
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = "확률 안내",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = Gray900,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "닫기", tint = Gray400)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 탭 (출석 가챠 / 코인 가챠)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(4.dp),
                ) {
                    listOf("🎁 출석 가챠", "🪙 코인 가챠").forEachIndexed { index, label ->
                        val selected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) Color.White else Color.Transparent
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text       = label,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (selected) Gray900 else Gray500,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 확률 테이블
                val grades = if (selectedTab == 0) attendanceGrades else coinGrades

                grades.forEach { grade ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 색상 도트
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(grade.color)
                        )
                        Spacer(Modifier.width(12.dp))
                        // 등급명
                        Text(
                            text       = grade.name,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = grade.color,
                            modifier   = Modifier.weight(1f),
                        )
                        // 확률 바
                        val pctValue = grade.pct.replace("%", "").toFloatOrNull() ?: 0f
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFF0F0F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(pctValue / 100f)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(grade.color)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        // 확률 텍스트
                        Text(
                            text      = grade.pct,
                            style     = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color     = Gray700,
                        )
                    }
                    if (grade != grades.last()) {
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 부가 설명
                val note = if (selectedTab == 0)
                    "출석 가챠는 매일 무료로 한 번 참여할 수 있어요."
                else
                    "코인 가챠는 코인 10개를 소모해 참여해요."
                Text(
                    text  = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray400,
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Cinematic Gacha Reveal
// ══════════════════════════════════════════════════════════════════════════════

private enum class RevealPhase { ENTERING, FLOATING, TAPPING, OPENING, REVEALED }

private data class RevealParticle(
    val angle: Float,
    val speed: Float,
    val size:  Float,
    val color: Color,
)

// 등급별 캡슐 색상
private fun gradeAccentColor(grade: GachaGrade): Color = when (grade) {
    GachaGrade.COMMON    -> Color(0xFF9E9E9E)
    GachaGrade.RARE      -> Color(0xFF1565C0)
    GachaGrade.UNIQUE    -> Color(0xFF6A1B9A)
    GachaGrade.LEGENDARY -> Color(0xFFE65100)
}

private fun gradeLidColor(grade: GachaGrade): Color = when (grade) {
    GachaGrade.COMMON    -> Color(0xFFE0E0E0)
    GachaGrade.RARE      -> Color(0xFF64B5F6)
    GachaGrade.UNIQUE    -> Color(0xFFCE93D8)
    GachaGrade.LEGENDARY -> Color(0xFFFFD54F)
}

private fun gradeParticleCount(grade: GachaGrade): Int = when (grade) {
    GachaGrade.COMMON    -> 10
    GachaGrade.RARE      -> 18
    GachaGrade.UNIQUE    -> 28
    GachaGrade.LEGENDARY -> 50
}

private fun buildRevealParticles(grade: GachaGrade, accent: Color, lid: Color): List<RevealParticle> {
    val count  = gradeParticleCount(grade)
    val colors = listOf(accent, lid, Color.White, accent.copy(alpha = 0.7f))
    return List(count) { i ->
        RevealParticle(
            angle = (i.toFloat() / count) * 2f * PI.toFloat() + ((i * 7919L) % 628L) * 0.01f,
            speed = 110f + ((i * 3571L) % 130L),
            size  = 4.5f + ((i * 2017L) % 9L),
            color = colors[i % colors.size],
        )
    }
}

@Composable
private fun CinematicGachaReveal(
    result:    GachaResult,
    onDismiss: () -> Unit,
) {
    val item        = when (result) {
        is GachaResult.NewItem       -> result.item
        is GachaResult.DuplicateCoin -> result.item
    }
    val isDuplicate = result is GachaResult.DuplicateCoin
    val coinReward  = if (result is GachaResult.DuplicateCoin) result.coins else 0

    val accent    = gradeAccentColor(item.grade)
    val lidColor  = gradeLidColor(item.grade)
    val hasRays   = item.grade == GachaGrade.LEGENDARY
    val particles = remember { buildRevealParticles(item.grade, accent, lidColor) }

    var phase     by remember { mutableStateOf(RevealPhase.ENTERING) }
    val haptic    = LocalHapticFeedback.current
    val scope     = rememberCoroutineScope()

    // ── Enter → Float ─────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        delay(80)
        phase = RevealPhase.FLOATING
    }

    val enterScale by animateFloatAsState(
        targetValue   = if (phase == RevealPhase.ENTERING) 0f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "enter",
    )

    // ── 무한 반복 애니메이션 ──────────────────────────────────────────────────
    val loop = rememberInfiniteTransition(label = "loop")

    val swayX by loop.animateFloat(
        initialValue  = -7f, targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "swayX",
    )
    val swayY by loop.animateFloat(
        initialValue  = 0f, targetValue = -9f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "swayY",
    )
    val glowPulse by loop.animateFloat(
        initialValue  = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow",
    )
    val tapPulse by loop.animateFloat(
        initialValue  = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "tapPulse",
    )
    val rayRotation by loop.animateFloat(
        initialValue  = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing)),
        label = "ray",
    )

    // ── 탭 플래시 ─────────────────────────────────────────────────────────────
    var flashActive by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(
        targetValue      = if (flashActive) 0.88f else 0f,
        animationSpec    = tween(if (flashActive) 50 else 400),
        finishedListener = { v -> if (v > 0f) flashActive = false },
        label            = "flash",
    )

    // ── 뚜껑 열림 (0 → -1 정규화, Canvas 내에서 관리) ────────────────────
    // lidProgress 0=닫혀있음  1=완전히 열림
    val lidProgress by animateFloatAsState(
        targetValue = when (phase) {
            RevealPhase.OPENING, RevealPhase.REVEALED -> 1f
            else -> 0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "lid",
    )

    // ── 아이템 등장 ───────────────────────────────────────────────────────────
    val itemAlpha by animateFloatAsState(
        targetValue   = if (phase == RevealPhase.REVEALED) 1f else 0f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label         = "itemAlpha",
    )
    val itemScale by animateFloatAsState(
        targetValue   = if (phase == RevealPhase.REVEALED) 1f else 0.1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "itemScale",
    )

    // ── 파티클 진행도 ─────────────────────────────────────────────────────────
    var particleProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(phase) {
        if (phase == RevealPhase.OPENING) {
            val start = System.currentTimeMillis()
            while (particleProgress < 1f) {
                particleProgress = ((System.currentTimeMillis() - start).toFloat() / 1400f).coerceIn(0f, 1f)
                delay(16L)
            }
        }
    }

    // ── OPENING → REVEALED ────────────────────────────────────────────────────
    LaunchedEffect(phase) {
        if (phase == RevealPhase.OPENING) {
            delay(800)
            phase = RevealPhase.REVEALED
        }
    }

    val isFloating = phase == RevealPhase.FLOATING || phase == RevealPhase.TAPPING

    Dialog(
        onDismissRequest = { if (phase == RevealPhase.REVEALED) onDismiss() },
        properties       = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {

            // ── 배경 ─────────────────────────────────────────────────────────
            Box(Modifier.fillMaxSize().background(Color(0xFF06060F).copy(alpha = 0.96f)))

            // ── 글로우 링 + 광선 효과 (풀스크린 Canvas – 잘림 없는 완전한 원) ─────
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = if (hasRays && phase != RevealPhase.ENTERING) rayRotation else 0f
                        alpha     = enterScale
                    },
            ) {
                val cx  = size.width  / 2f
                val cy  = size.height / 2f
                val len = maxOf(size.width, size.height) * 1.4f

                // 글로우 링 (항상)
                val ringCount = if (hasRays) 5 else 3
                val baseRadius = minOf(size.width, size.height) * 0.22f
                repeat(ringCount) { i ->
                    val ratio = (i + 1f) / ringCount
                    drawCircle(
                        color  = accent.copy(alpha = 0.10f * glowPulse * (1f - ratio * 0.45f)),
                        radius = baseRadius * (1f + ratio * 1.2f) * glowPulse,
                        center = Offset(cx, cy),
                    )
                }

                // 광선 (Legendary)
                if (hasRays && phase != RevealPhase.ENTERING) {
                    repeat(12) { i ->
                        val a = (i * (PI / 6.0)).toFloat()
                        drawLine(
                            color       = accent.copy(alpha = 0.11f * glowPulse),
                            start       = Offset(cx, cy),
                            end         = Offset(cx + cos(a) * len, cy + sin(a) * len),
                            strokeWidth = 80f,
                        )
                    }
                }
            }

            // ── 등급 뱃지 ─────────────────────────────────────────────────────
            if (phase != RevealPhase.ENTERING) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 72.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                ) {
                    Text(
                        text       = "✦  ${item.grade.displayName}  ✦",
                        color      = lidColor,
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // ── 캡슐 ─────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .offset(y = (-20).dp)
                    .pointerInput(phase) {
                        if (phase == RevealPhase.FLOATING) {
                            detectTapGestures {
                                scope.launch {
                                    phase       = RevealPhase.TAPPING
                                    flashActive = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    delay(150)
                                    phase = RevealPhase.OPENING
                                }
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX       = enterScale
                        scaleY       = enterScale
                        alpha        = enterScale
                        translationX = if (isFloating) swayX * density else 0f
                        translationY = if (isFloating) swayY * density else 0f
                    },
                contentAlignment = Alignment.Center,
            ) {
                // ── 캡슐 Canvas ──────────────────────────────────────────────────
                // 260dp 정사각형, 중심 (cx,cy), 반지름 cr 의 완전한 정원
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width  / 2f
                    val cy = size.height / 2f
                    // 정원을 보장하려면 width==height 이므로 한쪽만 쓰면 됨
                    val cr = size.width * 0.42f

                    // ──────────────────────────────────────────────────
                    // 파티클
                    // ──────────────────────────────────────────────────
                    if (particleProgress > 0f) {
                        particles.forEach { p ->
                            val dist   = p.speed * particleProgress
                            val px     = cx + cos(p.angle) * dist
                            val py     = cy + sin(p.angle) * dist
                            val pAlpha = (1f - particleProgress * 0.85f).coerceIn(0f, 1f)
                            drawCircle(
                                color  = p.color.copy(alpha = pAlpha),
                                radius = p.size * (1f - particleProgress * 0.3f),
                                center = Offset(px, py),
                            )
                        }
                    }

                    // ──────────────────────────────────────────────────
                    // Body (하반구) – cy 고정
                    // ──────────────────────────────────────────────────
                    clipRect(left = cx - cr, top = cy, right = cx + cr, bottom = cy + cr) {
                        // 1) 베이스 색상
                        drawCircle(color = accent, radius = cr, center = Offset(cx, cy))

                        // 2) 측면 어둠 (구체 느낌 핵심: 가장자리를 어둡게)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.45f),
                                ),
                                center = Offset(cx, cy),
                                radius = cr,
                            ),
                            radius = cr,
                            center = Offset(cx, cy),
                        )

                        // 3) 하단 그림자 (광원 위)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.28f),
                                ),
                                center = Offset(cx, cy + cr * 0.55f),
                                radius = cr * 0.85f,
                            ),
                            radius = cr,
                            center = Offset(cx, cy),
                        )

                        // 4) 림 라이트 (뒤에서 오는 빛 – 구체 현실감)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.65f to Color.Transparent,
                                    0.85f to accent.copy(alpha = 0.50f),
                                    1.00f to Color.White.copy(alpha = 0.18f),
                                ),
                                center = Offset(cx, cy),
                                radius = cr,
                            ),
                            radius = cr,
                            center = Offset(cx, cy),
                        )
                    }

                    // ──────────────────────────────────────────────────
                    // Lid (상반구) – lidProgress 0→1 로 위로 이동 + 페이드 아웃
                    // ──────────────────────────────────────────────────
                    val lidCy = cy - lidProgress * (size.height + cr)
                    // 초반엔 불투명 유지, 후반부에 빠르게 사라지는 EaseIn 곡선
                    val lidAlpha = (1f - (lidProgress * 1.4f).coerceIn(0f, 1f).let { it * it }).coerceIn(0f, 1f)

                    if (lidAlpha > 0f) {
                        clipRect(left = cx - cr, top = lidCy - cr, right = cx + cr, bottom = lidCy) {
                            // 1) 베이스 색상
                            drawCircle(color = lidColor.copy(alpha = lidAlpha), radius = cr, center = Offset(cx, lidCy))

                            // 2) 측면 어둠
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.38f * lidAlpha),
                                    ),
                                    center = Offset(cx, lidCy),
                                    radius = cr,
                                ),
                                radius = cr,
                                center = Offset(cx, lidCy),
                            )

                            // 3) 주 하이라이트 – 왼쪽 위 (광원이 좌상단)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.75f * lidAlpha),
                                        Color.White.copy(alpha = 0.25f * lidAlpha),
                                        Color.Transparent,
                                    ),
                                    center = Offset(cx - cr * 0.28f, lidCy - cr * 0.42f),
                                    radius = cr * 0.55f,
                                ),
                                radius = cr,
                                center = Offset(cx, lidCy),
                            )

                            // 4) 유리 굴절 – 하단 내부 밝은 반사
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.18f * lidAlpha),
                                        Color.Transparent,
                                    ),
                                    center = Offset(cx + cr * 0.15f, lidCy - cr * 0.05f),
                                    radius = cr * 0.45f,
                                ),
                                radius = cr,
                                center = Offset(cx, lidCy),
                            )
                        }
                    }

                    // 작은 스페큘러 하이라이트 (광원 반짝이) – 뚜껑과 함께 페이드 아웃
                    val shineCy = lidCy - cr * 0.44f
                    if (shineCy > cy - cr - 20f && lidAlpha > 0f) {
                        // 큰 글로우
                        drawCircle(
                            color  = Color.White.copy(alpha = 0.35f * lidAlpha),
                            radius = cr * 0.10f,
                            center = Offset(cx - cr * 0.28f, shineCy),
                        )
                        // 작은 하이라이트 점
                        drawCircle(
                            color  = Color.White.copy(alpha = 0.90f * lidAlpha),
                            radius = cr * 0.045f,
                            center = Offset(cx - cr * 0.28f, shineCy),
                        )
                    }

                    // ──────────────────────────────────────────────────
                    // 이음선 (seam) – 타원 호로 3D 구형처럼
                    // ──────────────────────────────────────────────────
                    val seamAlpha = (1f - lidProgress * 2.8f).coerceIn(0f, 1f)
                    if (seamAlpha > 0.01f) {
                        // 타원 이음선: 폭=cr*2, 높이=cr*0.22 (구면 투시)
                        val seamW = cr * 2f
                        val seamH = cr * 0.22f

                        // 그림자 호 (살짝 아래)
                        drawOval(
                            color   = Color.Black.copy(alpha = seamAlpha * 0.40f),
                            topLeft = Offset(cx - seamW / 2f, cy - seamH / 2f + 3f),
                            size    = Size(seamW, seamH),
                            style   = Stroke(width = 4f),
                        )
                        // 하이라이트 호
                        drawOval(
                            color   = Color.White.copy(alpha = seamAlpha * 0.85f),
                            topLeft = Offset(cx - seamW / 2f, cy - seamH / 2f),
                            size    = Size(seamW, seamH),
                            style   = Stroke(width = 2.5f),
                        )
                        // 음영 호 (아래쪽 반)
                        drawOval(
                            color   = Color.Black.copy(alpha = seamAlpha * 0.25f),
                            topLeft = Offset(cx - seamW / 2f, cy - seamH / 2f + 2f),
                            size    = Size(seamW, seamH),
                            style   = Stroke(width = 2f),
                        )
                    }
                }

                // ── 아이템 이미지 (OPENING 이후 등장) ────────────────────────
                if (phase == RevealPhase.OPENING || phase == RevealPhase.REVEALED) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-14).dp)
                            .size(76.dp)
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                                alpha  = itemAlpha
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter            = painterResource(id = item.drawableResId),
                            contentDescription = item.name,
                            modifier           = Modifier.size(68.dp),
                        )
                    }
                }
            }

            // ── 탭 힌트 ──────────────────────────────────────────────────────
            if (phase == RevealPhase.FLOATING) {
                Column(
                    modifier            = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 130.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text       = "탭해서 열기",
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = Color.White.copy(alpha = tapPulse),
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = tapPulse * 0.55f))
                    )
                }
            }

            // ── 결과 정보 + 확인 버튼 ─────────────────────────────────────────
            if (phase == RevealPhase.REVEALED) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 32.dp, vertical = 52.dp)
                        .graphicsLayer { alpha = itemAlpha },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text       = item.name,
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                        textAlign  = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (isDuplicate) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFFFFB800).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.MonetizationOn,
                                contentDescription = null,
                                tint               = Color(0xFFFFB800),
                                modifier           = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text       = "중복 아이템  +$coinReward 코인",
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFFFFB800),
                            )
                        }
                    } else {
                        Text(
                            text       = "새 아이템 획득!",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = lidColor,
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick   = onDismiss,
                        modifier  = Modifier.fillMaxWidth().height(54.dp),
                        shape     = RoundedCornerShape(18.dp),
                        colors    = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor   = Color.White,
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                    ) {
                        Text(
                            text       = "확인",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // ── 플래시 오버레이 ───────────────────────────────────────────────
            if (flashAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = flashAlpha))
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
    cooldownText: String?,
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

                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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

                if (cooldownText != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFF3F3))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
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
