package com.example.personalfinance.ui.main

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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.personalfinance.data.*
import com.example.personalfinance.ui.theme.*
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.launch

// ── 소비 성향 키워드 헬퍼 ─────────────────────────────────────────────────────

private fun spendingTendencyLabel(topCategory: String): String = when (topCategory) {
    ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE          -> "식비 중심형"
    ExpenseCategoryClassifier.CATEGORY_LIVING_MART        -> "생활 밀착형"
    ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE    -> "쇼핑 애호형"
    ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE    -> "문화 탐구형"
    ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION -> "구독 관리형"
    ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL     -> "건강 투자형"
    else                                                   -> "균형 소비형"
}

private fun spendingTendencyDesc(topCategory: String, ratio: Int): String = when (topCategory) {
    ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE          -> "이번 달 지출의 ${ratio}%를 식비·카페에 사용했어요."
    ExpenseCategoryClassifier.CATEGORY_LIVING_MART        -> "이번 달 지출의 ${ratio}%를 생활·마트에 사용했어요."
    ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE    -> "이번 달 지출의 ${ratio}%를 쇼핑·온라인에 사용했어요."
    ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE    -> "이번 달 지출의 ${ratio}%를 문화·여가에 사용했어요."
    ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION -> "이번 달 지출의 ${ratio}%를 구독·고정비에 사용했어요."
    ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL     -> "이번 달 지출의 ${ratio}%를 건강·의료에 사용했어요."
    else                                                   -> "이번 달 지출이 여러 카테고리에 고르게 분포되어 있어요."
}

private fun categoryColorForProfile(category: String): Color = when (category) {
    ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE          -> CategoryFood
    ExpenseCategoryClassifier.CATEGORY_LIVING_MART        -> CategoryShopping
    ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE    -> CategoryGame
    ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE    -> CategoryCulture
    ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION -> CategoryBeauty
    ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL     -> Color(0xFF34D399)
    else                                                   -> CategoryOther
}

private fun categoryEmojiForProfile(category: String): String = when (category) {
    ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE          -> "🍽️"
    ExpenseCategoryClassifier.CATEGORY_LIVING_MART        -> "🛒"
    ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE    -> "🛍️"
    ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE    -> "🎬"
    ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION -> "📱"
    ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL     -> "💊"
    else                                                   -> "📦"
}

private fun formatWonProfile(amount: Long): String = "₩${String.format("%,d", amount)}"
private fun formatWonProfile(amount: Int): String  = "₩${String.format("%,d", amount)}"

// ── 주차별 지출 집계 ──────────────────────────────────────────────────────────

private data class WeeklySpending(val label: String, val amount: Long)

private fun buildWeeklyData(transactions: List<Transaction>, month: YearMonth): List<WeeklySpending> {
    val weeks = listOf("1주" to (1..7), "2주" to (8..14), "3주" to (15..21), "4주" to (22..month.lengthOfMonth()))
    return weeks.map { (label, range) ->
        val total = transactions.filter { tx ->
            runCatching {
                val day = LocalDateTime.parse(tx.occurredAt).dayOfMonth
                day in range
            }.getOrDefault(false)
        }.sumOf { it.amount }
        WeeklySpending(label, total)
    }
}

// ── ProfileScreen ─────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    navController: NavController
) {
    val context   = LocalContext.current
    val store     = remember { UserStatsStore.getInstance(context) }
    val userStats by store.statsFlow.collectAsState()
    val nickname  by store.nicknameFlow.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        store.refreshServerStats()
    }

    val currentMonth = YearMonth.now()
    val transactions by store.transactionsFlow.collectAsState()
    val thisMonthTxs = transactions.filter { tx ->
        runCatching {
            YearMonth.from(LocalDateTime.parse(tx.occurredAt).toLocalDate()) == currentMonth
        }.getOrDefault(false)
    }

    val categorySpending = userStats.categorySpending
    val totalSpending    = userStats.thisMonthSpending
    val topCategory      = userStats.topCategory
    val currentJob       = userStats.job
    val jobTitle         = UserStatsCalculator.jobTitle(currentJob)
    val jobReason        = userStats.jobReason
    val tendencyLabel    = spendingTendencyLabel(topCategory)
    val topRatio = if (totalSpending > 0L) {
        ((categorySpending[topCategory] ?: 0L).toFloat() / totalSpending * 100).toInt()
    } else 0
    val tendencyDesc = spendingTendencyDesc(topCategory, topRatio)

    // 파이차트용 카테고리 목록
    val chartCategories = categorySpending
        .filter { it.value > 0L }
        .map { (cat, amount) ->
            Triple(cat, amount, categoryColorForProfile(cat))
        }
        .sortedByDescending { it.second }
    val chartTotal = chartCategories.sumOf { it.second }.coerceAtLeast(1L)

    // 주차별 데이터
    val weeklyData = buildWeeklyData(thisMonthTxs, currentMonth)
    val weeklyMax  = weeklyData.maxOfOrNull { it.amount }?.toFloat()?.coerceAtLeast(1f) ?: 1f

    // 닉네임 수정 다이얼로그
    var showEditDialog by remember { mutableStateOf(false) }
    var editingName    by remember { mutableStateOf("") }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf("") }
    var budgetSaveError by remember { mutableStateOf<String?>(null) }

    // 직업 이유 팝업
    var showJobDialog by remember { mutableStateOf(false) }

    val jobEmoji = when (currentJob) {
        "cook"     -> "🍳"
        "manager"  -> "🏠"
        "merchant" -> "🛍️"
        "artist"   -> "🎨"
        "planner"  -> "📋"
        "healer"   -> "💊"
        else       -> "⚔️"
    }

    // ── 직업 이유 다이얼로그 ──────────────────────────────────────────────────
    if (showJobDialog) {
        Dialog(
            onDismissRequest = { showJobDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
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
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Brush.linearGradient(listOf(Blue50, Purple50)), CircleShape)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(listOf(Blue300.copy(alpha = 0.6f), Purple400.copy(alpha = 0.4f))),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text(jobEmoji, fontSize = 32.sp) }

                    Spacer(Modifier.height(20.dp))
                    Text(jobTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gray900)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Brush.horizontalGradient(listOf(Blue500, Purple500)), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("이번 달 직업", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = Gray200)
                    Spacer(Modifier.height(20.dp))
                    Text(jobReason, fontSize = 14.sp, color = Gray600, lineHeight = 22.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = { showJobDialog = false },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(Blue500, Purple500)), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("확인", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
                    }
                }
            }
        }
    }

    // ── 닉네임 수정 다이얼로그 ────────────────────────────────────────────────
    if (showEditDialog) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("닉네임 변경", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value         = editingName,
                    onValueChange = { if (it.length <= 12) editingName = it },
                    placeholder   = { Text("닉네임을 입력하세요 (최대 12자)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = Blue500)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingName.isNotBlank()) store.saveNickname(editingName)
                    showEditDialog = false
                }) { Text("저장", color = Blue500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("취소", color = Gray500) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("월 예산 수정", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editingBudget,
                        onValueChange = { value ->
                            editingBudget = value.filter { it.isDigit() }.take(12)
                            budgetSaveError = null
                        },
                        placeholder = { Text("월 예산을 입력하세요") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Blue500)
                    )
                    budgetSaveError?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = RedDanger,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val budget = editingBudget.toLongOrNull()
                    if (budget == null || budget <= 0L) {
                        budgetSaveError = "0보다 큰 숫자를 입력하세요."
                        return@TextButton
                    }
                    scope.launch {
                        val saved = store.updateMonthlyBudget(budget)
                        if (saved) {
                            showBudgetDialog = false
                        } else {
                            budgetSaveError = "서버 저장에 실패했습니다."
                        }
                    }
                }) { Text("저장", color = Blue500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("취소", color = Gray500) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── 화면 본문 ─────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {

        // ── 헤더 ─────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Gray600)
            }
            Text("내 프로필", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(48.dp))
        }
        HorizontalDivider(color = Gray100)

        // ── 프로필 카드 ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Blue50, Purple50)))
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("👤", fontSize = 28.sp) }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = if (nickname.isBlank()) "사용자님" else "${nickname}님",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Lv.${userStats.currentLevel} · ${UserStatsCalculator.levelTitle(userStats.currentLevel)}",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = Gray600,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(onClick = {
                    editingName    = nickname
                    showEditDialog = true
                }) {
                    Icon(Icons.Rounded.Edit, null, tint = Blue500, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── 소비 성향 + 직업 뱃지 ────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                "이번 달 소비 성향",
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = Gray500,
                modifier   = Modifier.padding(bottom = 10.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // 소비 성향 키워드
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(categoryColorForProfile(topCategory).copy(alpha = 0.10f))
                        .border(1.dp, categoryColorForProfile(topCategory).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(categoryEmojiForProfile(topCategory), fontSize = 24.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            tendencyLabel,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color      = categoryColorForProfile(topCategory)
                        )
                        Text(
                            tendencyDesc,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = Gray500,
                            modifier = Modifier.padding(top = 4.dp),
                            lineHeight = 18.sp
                        )
                    }
                }

                // 직업 뱃지 — 터치 시 이유 팝업
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Blue50)
                        .border(1.dp, Blue300.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { showJobDialog = true }
                        .padding(16.dp)
                ) {
                    Column {
                        Text(jobEmoji, fontSize = 24.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            jobTitle,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color      = Blue500
                        )
                        Text(
                            "탭해서 획득 이유 보기",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = Gray400,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(Blue500.copy(alpha = 0.12f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "이번 달 직업",
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Blue500
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Gray50)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "월 예산",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray500
                    )
                    Text(
                        formatWonProfile(userStats.monthlyBudget),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(onClick = {
                    editingBudget = userStats.monthlyBudget.toString()
                    budgetSaveError = null
                    showBudgetDialog = true
                }) {
                    Icon(Icons.Rounded.Edit, null, tint = Blue500, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 카테고리별 지출 파이차트 ─────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                "카테고리별 지출",
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = Gray500,
                modifier   = Modifier.padding(bottom = 12.dp)
            )

            if (chartCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Gray50)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("이번 달 지출 내역이 없어요", style = MaterialTheme.typography.bodyMedium, color = Gray400)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Gray50)
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 파이차트
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(180.dp)) {
                                    var startAngle = -90f
                                    chartCategories.forEach { (_, amount, color) ->
                                        val sweep = amount.toFloat() / chartTotal * 360f
                                        drawArc(
                                            color      = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep - 1.5f,
                                            useCenter  = false,
                                            style      = Stroke(width = 54f, cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "총 지출",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray500
                                    )
                                    Text(
                                        formatWonProfile(totalSpending),
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // 범례
                        chartCategories.forEach { (cat, amount, color) ->
                            val pct = (amount.toFloat() / chartTotal * 100).toInt()
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(categoryEmojiForProfile(cat), fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(cat, style = MaterialTheme.typography.bodySmall, color = Gray700)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${pct}%",
                                        style      = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = color
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        formatWonProfile(amount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray500
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 주차별 지출 흐름 ──────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                "주차별 지출 흐름",
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = Gray500,
                modifier   = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Gray50)
                    .padding(24.dp)
            ) {
                if (weeklyData.all { it.amount == 0L }) {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("이번 달 지출 내역이 없어요", style = MaterialTheme.typography.bodyMedium, color = Gray400)
                    }
                } else {
                    Column {
                        // 바 차트
                        Row(
                            modifier              = Modifier.fillMaxWidth().height(140.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment     = Alignment.Bottom
                        ) {
                            weeklyData.forEach { week ->
                                val fraction = (week.amount / weeklyMax).coerceIn(0f, 1f)
                                val barColor = if (week.amount > 0L) Blue500 else Gray200
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier            = Modifier.weight(1f)
                                ) {
                                    if (week.amount > 0L) {
                                        Text(
                                            formatWonProfile(week.amount),
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = Gray500,
                                            fontSize = 8.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.55f)
                                            .height(((fraction * 100).coerceAtLeast(if (week.amount > 0L) 16f else 4f)).dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(barColor)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Gray200)
                        Spacer(Modifier.height(8.dp))

                        // 주차 레이블
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            weeklyData.forEach { week ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier            = Modifier.weight(1f)
                                ) {
                                    Text(
                                        week.label,
                                        style      = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = if (week.amount > 0L) Blue500 else Gray400
                                    )
                                }
                            }
                        }

                        // 최다 지출 주차 하이라이트
                        val maxWeek = weeklyData.maxByOrNull { it.amount }
                        if ((maxWeek?.amount ?: 0L) > 0L) {
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Blue50)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "💡 ${maxWeek!!.label}에 가장 많이 지출했어요 (${formatWonProfile(maxWeek.amount)})",
                                    style     = MaterialTheme.typography.bodySmall,
                                    color     = Blue500,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
