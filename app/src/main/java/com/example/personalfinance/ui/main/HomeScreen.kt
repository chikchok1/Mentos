package com.example.personalfinance.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personalfinance.data.UserStatsCalculator
import com.example.personalfinance.navigation.Screen
import com.example.personalfinance.ui.components.PixelCharacter
import com.example.personalfinance.ui.theme.*
import java.time.YearMonth
import java.time.LocalDateTime

@Composable
fun HomeScreen(navController: NavController) {
    // ── State ────────────────────────────────────────────────────────────────
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { com.example.personalfinance.data.UserStatsStore.getInstance(context) }
    val userStats by store.statsFlow.collectAsState()

    val currentLevel      = userStats.currentLevel
    val currentXP         = userStats.currentXP
    val xpProgress        = UserStatsCalculator.levelProgress(currentXP)
    val (xpInLevel, xpToNext) = UserStatsCalculator.levelProgressXP(currentXP)
    val thisMonthSpending = userStats.thisMonthSpending
    val storedTransactions by store.transactionsFlow.collectAsState()
    // [FIX #4] 지난달 대비 변화율을 실제 거래 데이터에서 계산
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
        if (lastSum == 0L) null   // 지난달 데이터 없으면 null
        else ((thisSum - lastSum).toFloat() / lastSum * 100).toInt()
    }
    val topCategory       = userStats.topCategory
    val currentJob        = UserStatsCalculator.determineJob(userStats.categorySpending)
    val jobTitle          = UserStatsCalculator.jobTitle(currentJob)
    val levelTitle        = UserStatsCalculator.levelTitle(currentLevel)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // 시스템 바 inset
    val systemBarsInsets = WindowInsets.systemBars
    val bottomInset = with(androidx.compose.ui.platform.LocalDensity.current) {
        systemBarsInsets.getBottom(this).toDp()
    }
    // 바텀 네비 바 전체 높이 = 콘텐츠(16+56+16) + 시스템 inset
    val navBarHeight = 88.dp + bottomInset

    // ── Layout ───────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // Scrollable content
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
                    text     = "안녕하세요 👋",
                    style    = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Character section (fade + scale in)
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
                    PixelCharacter(level = currentLevel, job = currentJob)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Level badge + 직업명
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(Blue50, Purple50)),
                                RoundedCornerShape(50)
                            )
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

                    // XP progress bar
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

            // Spending card (slide up + fade in) — 탭하면 가계부로 이동
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
                                imageVector        = Icons.Rounded.KeyboardArrowRight,
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

                        // Insight row 1 — monthly change
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            val isDown = (lastMonthChange ?: 0) <= 0
                            Box(
                                modifier         = Modifier.size(36.dp).background(
                                    if (isDown) Color(0xFFDCFCE7) else Color(0xFFFFE4E6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isDown) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
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

                        // Insight row 2 — top category
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

        // ── Bottom Navigation ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(12.dp)
                .background(Color.White)
                // 콘텐츠 패딩은 위쪽만, 하단은 시스템 inset만큼 추가
                .padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 16.dp + bottomInset)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Ledger
                IconButton(onClick = { navController.navigate(Screen.Ledger.route) }) {
                    Icon(Icons.Rounded.MenuBook, contentDescription = "가계부", tint = Gray600)
                }

                // FAB — add expense
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

                // Menu
                IconButton(onClick = { navController.navigate(Screen.Menu.route) }) {
                    Icon(Icons.Rounded.Menu, contentDescription = "메뉴", tint = Gray600)
                }
            }
        }
    }
}
