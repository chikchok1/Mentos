package com.example.personalfinance.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.personalfinance.data.CharacterAppearanceStore
import com.example.personalfinance.data.UserStatsCalculator
import com.example.personalfinance.navigation.Screen
import com.example.personalfinance.ui.components.CharacterLayerPreview
import com.example.personalfinance.ui.theme.*
import java.time.YearMonth
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun HomeScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { com.example.personalfinance.data.UserStatsStore.getInstance(context) }
    val userStats by store.statsFlow.collectAsState()
    val nickname  by store.nicknameFlow.collectAsState()

    val currentLevel      = userStats.currentLevel
    val currentXP         = userStats.currentXP
    val xpProgress        = UserStatsCalculator.levelProgress(currentXP)
    val (xpInLevel, xpToNext) = UserStatsCalculator.levelProgressXP(currentXP)
    val thisMonthSpending = userStats.thisMonthSpending
    val storedTransactions by store.transactionsFlow.collectAsState()
    val lastMonthChange: Int? = remember(storedTransactions) {
        val now = YearMonth.now()
        val lastMonth = now.minusMonths(1)
        fun sumForMonth(ym: YearMonth) = storedTransactions
            .filter { tx ->
                runCatching {
                    YearMonth.from(LocalDateTime.parse(tx.occurredAt).toLocalDate()) == ym
                }.getOrDefault(false)
            }
            .sumOf { it.amount }
        val thisSum  = sumForMonth(now)
        val lastSum  = sumForMonth(lastMonth)
        if (lastSum == 0L) null
        else ((thisSum - lastSum).toFloat() / lastSum * 100).toInt()
    }
    val topCategory = userStats.topCategory
    val currentJob  = UserStatsCalculator.determineJob(userStats.categorySpending)
    val jobTitle    = UserStatsCalculator.jobTitle(currentJob)
    val levelTitle  = UserStatsCalculator.levelTitle(currentLevel)
    val jobReason   = UserStatsCalculator.jobReason(currentJob, userStats.categorySpending, thisMonthSpending)

    // 직업 이유 팝업
    var showJobDialog by remember { mutableStateOf(false) }

    // 직업 변경 토스트
    var jobChangedMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        store.jobChangedFlow.collect { newJobTitle ->
            jobChangedMessage = "${newJobTitle}이(가) 되었어요!"
        }
    }

    val appearanceStore = remember { CharacterAppearanceStore.getInstance(context) }
    val characterAppearance by appearanceStore.appearanceFlow.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val systemBarsInsets = WindowInsets.systemBars
    val bottomInset = with(androidx.compose.ui.platform.LocalDensity.current) {
        systemBarsInsets.getBottom(this).toDp()
    }
    val navBarHeight = 88.dp + bottomInset

    // ── 직업 이유 다이얼로그 ──────────────────────────────────────────────────
    if (showJobDialog) {
        val jobEmoji = when (currentJob) {
            "cook"     -> "🍳"
            "manager"  -> "🏠"
            "merchant" -> "🛍️"
            "artist"   -> "🎨"
            "planner"  -> "📋"
            "healer"   -> "💊"
            else       -> "⚔️"
        }

        Dialog(
            onDismissRequest = { showJobDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    // ── 이모지 아이콘 원형 배지 ──────────────────────────
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                Brush.linearGradient(listOf(Blue50, Purple50)),
                                CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(Blue300.copy(alpha = 0.6f), Purple400.copy(alpha = 0.4f))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text     = jobEmoji,
                            fontSize = 32.sp
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── 직업명 ────────────────────────────────────────────
                    Text(
                        text       = jobTitle,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Gray900,
                        letterSpacing = (-0.3).sp,
                    )

                    Spacer(Modifier.height(4.dp))

                    // ── 서브 태그 ─────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(Blue500, Purple500)),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text      = "이번 달 직업",
                            fontSize  = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color     = Color.White,
                            letterSpacing = 0.3.sp,
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── 구분선 ────────────────────────────────────────────
                    HorizontalDivider(color = Gray200)

                    Spacer(Modifier.height(20.dp))

                    // ── 이유 텍스트 ───────────────────────────────────────
                    Text(
                        text       = jobReason,
                        fontSize   = 14.sp,
                        color      = Gray600,
                        lineHeight = 22.sp,
                        textAlign  = TextAlign.Center,
                    )

                    Spacer(Modifier.height(28.dp))

                    // ── 확인 버튼 ─────────────────────────────────────────
                    Button(
                        onClick = { showJobDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(Blue500, Purple500)),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text       = "확인",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White, Gray50)))
                .verticalScroll(rememberScrollState())
                .padding(bottom = navBarHeight)
        ) {

            // Header
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                Text(
                    text  = java.time.LocalDate.now().let { "${it.year}년 ${it.monthValue}월 ${it.dayOfMonth}일" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500
                )
                Text(
                    text     = if (nickname.isBlank()) "안녕하세요 👋" else "${nickname}님, 안녕하세요 👋",
                    style    = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Character section
            AnimatedVisibility(
                visible = visible,
                enter   = scaleIn(initialScale = 0.8f, animationSpec = tween(500)) + fadeIn(tween(500))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    CharacterLayerPreview(
                        layerState = characterAppearance,
                        size       = 220.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 직업 뱃지 — 터치 시 이유 팝업 표시
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(Blue50, Purple50)),
                                RoundedCornerShape(50)
                            )
                            .clickable { showJobDialog = true }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text       = "Lv.$currentLevel $levelTitle · $jobTitle",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = Blue500
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress   = { xpProgress },
                        modifier   = Modifier
                            .width(160.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color      = Blue500,
                        trackColor = Gray100
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text  = "$xpInLevel / $xpToNext XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray400
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spending card
            AnimatedVisibility(
                visible = visible,
                enter   = slideInVertically(tween(500, 200)) { it / 3 } + fadeIn(tween(500, 200))
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Blue50, Purple50)))
                        .clickable { navController.navigate(Screen.Ledger.route) }
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text  = "이번 달 지출",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray600
                            )
                            Icon(
                                imageVector        = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint               = Gray400,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text       = "₩${String.format("%,d", thisMonthSpending)}",
                            style      = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(top = 8.dp, bottom = 20.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            val isDown = (lastMonthChange ?: 0) <= 0
                            Box(
                                modifier         = Modifier.size(36.dp).background(
                                    if (isDown) Color(0xFFDCFCE7) else Color(0xFFFFE4E6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isDown) Icons.AutoMirrored.Rounded.TrendingDown else Icons.AutoMirrored.Rounded.TrendingUp,
                                    null,
                                    tint     = if (isDown) GreenSuccess else RedDanger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            if (lastMonthChange == null) {
                                Text("지난달 데이터 없음", style = MaterialTheme.typography.bodyMedium, color = Gray500)
                            } else {
                                Text("지난달 대비 ", style = MaterialTheme.typography.bodyMedium, color = Gray700)
                                Text(
                                    "${if (lastMonthChange >= 0) "+" else ""}${lastMonthChange}%",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = if (isDown) GreenSuccess else RedDanger
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier         = Modifier.size(36.dp).background(Color(0xFFFFF7ED), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Restaurant, null, tint = OrangeWarning, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("가장 많이 사용한 카테고리: ", style = MaterialTheme.typography.bodyMedium, color = Gray700)
                            Text(topCategory, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 직업 변경 토스트
        jobChangedMessage?.let { msg ->
            LaunchedEffect(msg) {
                delay(2500)
                jobChangedMessage = null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
                    .background(Color(0xFF1E1E2E), RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text       = msg,
                    color      = Color.White,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Bottom Navigation
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(12.dp)
                .background(Color.White)
                .padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 16.dp + bottomInset)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigate(Screen.Ledger.route) }) {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = "가계부", tint = Gray600)
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(12.dp, CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Blue500, Purple500)),
                            CircleShape
                        )
                        .clip(CircleShape)
                        .clickable { navController.navigate(Screen.NewRecord.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "추가", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { navController.navigate(Screen.Menu.route) }) {
                    Icon(Icons.Rounded.Menu, contentDescription = "메뉴", tint = Gray600)
                }
            }
        }
    }
}
