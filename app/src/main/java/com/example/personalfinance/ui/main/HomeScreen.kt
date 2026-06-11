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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun HomeScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { com.example.personalfinance.data.UserStatsStore.getInstance(context) }
    val userStats by store.statsFlow.collectAsState()
    val nickname  by store.nicknameFlow.collectAsState()

    // 서버 통계를 받아오기 전까지 로컬 캐시 레벨을 보여주다가
    // refreshServerStats() 완료 후 서버 기준 레벨로 자동 업데이트됨
    // (statsFlow가 applyServerStats에서 갱신되므로 별도 상태 불필요)
    LaunchedEffect(Unit) {
        store.refreshServerStats()
    }

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
    val currentJob  = userStats.job
    val jobTitle    = UserStatsCalculator.jobTitle(currentJob)
    val levelTitle  = UserStatsCalculator.levelTitle(currentLevel)
    val jobReason   = userStats.jobReason
    val safeJobReason = jobReason.trim().takeIf { it.isNotBlank() }
        ?: "이번 달 소비 패턴이 반영되었어요."
    val levelGrades = UserStatsCalculator.levelGradeGuides()
    val nextGrade   = UserStatsCalculator.nextGrade(currentLevel)

    var showJobDialog by remember { mutableStateOf(false) }

    val appearanceStore = remember { CharacterAppearanceStore.getInstance(context) }
    val characterAppearance by appearanceStore.appearanceFlow.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val systemBarsInsets = WindowInsets.systemBars
    val bottomInset = with(androidx.compose.ui.platform.LocalDensity.current) {
        systemBarsInsets.getBottom(this).toDp()
    }
    val navBarHeight = 88.dp + bottomInset

    // ── 성장 정보 다이얼로그 ──────────────────────────────────────────────────
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    // ── 스크롤 가능한 본문 ──────────────────────────────────
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 제목
                        Text(
                            text = "나의 성장 정보",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray900,
                            letterSpacing = 0.sp,
                        )

                        Spacer(Modifier.height(18.dp))

                        // ── 1. 현재 성장 상태 ──────────────────────────────
                        JobInfoSection(title = "현재 성장 상태") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = jobEmoji, fontSize = 26.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Lv.$currentLevel $levelTitle $jobTitle",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Gray900,
                                    letterSpacing = 0.sp,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (nextGrade != null) {
                                    Text(
                                        text = "다음 등급: Lv.${nextGrade.minLevel} ${nextGrade.title}",
                                        fontSize = 13.sp,
                                        color = Blue500,
                                        fontWeight = FontWeight.Medium,
                                    )
                                } else {
                                    Text(
                                        text = "★ 최고 등급에 도달했어요!",
                                        fontSize = 13.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── 2. 레벨 등급표 ─────────────────────────────────
                        JobInfoSection(title = "레벨 등급표") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                levelGrades.forEach { grade ->
                                    val isCurrent = levelTitle == grade.title
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isCurrent) Blue500.copy(alpha = 0.10f)
                                                else Color.Transparent
                                            )
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isCurrent) {
                                                Text(
                                                    text = "▶ ",
                                                    fontSize = 10.sp,
                                                    color = Blue500,
                                                )
                                            } else {
                                                Spacer(Modifier.width(14.dp))
                                            }
                                            Text(
                                                text = "Lv.${grade.minLevel} 이상",
                                                fontSize = 13.sp,
                                                color = if (isCurrent) Blue500 else Gray500,
                                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                            )
                                        }
                                        Text(
                                            text = grade.title,
                                            fontSize = 13.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) Blue500 else Gray700,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── 3. 직업 변경 기준 ──────────────────────────────
                        JobInfoSection(title = "직업 변경 기준") {
                            Text(
                                text = safeJobReason,
                                fontSize = 13.sp,
                                color = Gray700,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "직업은 이번 달 소비 카테고리 비중을 기준으로 자동 부여되며, 소비 패턴이 달라지면 같은 달 안에서도 변경될 수 있어요.",
                                fontSize = 12.sp,
                                color = Gray500,
                                lineHeight = 19.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── 확인 버튼 ─────────────────────────────────────────
                    Button(
                        onClick    = { showJobDialog = false },
                        modifier   = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape      = RoundedCornerShape(14.dp),
                        colors     = ButtonDefaults.buttonColors(containerColor = Blue500),
                        elevation  = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
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

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(bottom = navBarHeight)
        ) {

            // ── Header ────────────────────────────────────────────────────
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

            // ── Character section ─────────────────────────────────────────
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

                    // 직업 배지 — 단순 텍스트, 터치 시 이유 팝업
                    Text(
                        text       = "Lv.$currentLevel $levelTitle $jobTitle",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = Gray500,
                        modifier   = Modifier.clickable { showJobDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── XP 바 + 획득 pill ──────────────────────────────────
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                    ) {
                        Column {
                            LinearProgressIndicator(
                                progress   = { xpProgress },
                                modifier   = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color      = Blue500,
                                trackColor = Blue50
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    text  = "$xpInLevel / $xpToNext XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray400
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Spending card ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = slideInVertically(tween(500, 200)) { it / 3 } + fadeIn(tween(500, 200))
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFAFAFA))
                        .border(0.5.dp, Gray200, RoundedCornerShape(20.dp))
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
                                color = Gray500
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
                            fontWeight = FontWeight.Medium,
                            modifier   = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        Text(
                            text     = "월 예산 ₩${String.format("%,d", userStats.monthlyBudget)}",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = Gray400,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        HorizontalDivider(color = Gray200, modifier = Modifier.padding(bottom = 12.dp))

                        // 지난달 대비
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(bottom = 8.dp)
                        ) {
                            val isDown = (lastMonthChange ?: 0) <= 0
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isDown) GreenSuccess else RedDanger,
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(10.dp))
                            if (lastMonthChange == null) {
                                Text(
                                    "지난달 데이터 없음",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gray500
                                )
                            } else {
                                Text(
                                    "지난달 대비 ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gray600
                                )
                                Text(
                                    "${if (lastMonthChange >= 0) "+" else ""}${lastMonthChange}%",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = if (isDown) GreenSuccess else RedDanger
                                )
                            }
                        }

                        // 최다 카테고리
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Blue500, CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "최다 카테고리 ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray600
                            )
                            Text(
                                topCategory,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        UserStatsFeedbackHost(
            store    = store,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = navBarHeight + 16.dp)
        )

        // ── Bottom Navigation ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(4.dp)
                .background(Color.White)
                .padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 16.dp + bottomInset)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigate(Screen.Ledger.route) }) {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = "가계부", tint = Gray400)
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(8.dp, CircleShape)
                        .background(Blue500, CircleShape)
                        .clip(CircleShape)
                        .clickable { navController.navigate(Screen.NewRecord.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "추가", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { navController.navigate(Screen.Menu.route) }) {
                    Icon(Icons.Rounded.Menu, contentDescription = "메뉴", tint = Gray400)
                }
            }
        }
    }
}

@Composable
private fun JobInfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF8FAFF))
            .border(0.5.dp, Blue50, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Blue500,
            letterSpacing = 0.sp
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}
