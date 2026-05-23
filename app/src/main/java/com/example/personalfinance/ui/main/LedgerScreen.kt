package com.example.personalfinance.ui.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.YearMonth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.example.personalfinance.data.*
import com.example.personalfinance.ui.theme.*

// ── 카테고리 이름 매핑 (ExpenseCategoryClassifier → 화면 표시용) ───────────────────

private fun classifierCategoryToDisplayName(category: String): String = when (category) {
    ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE          -> "식비/카페"
    ExpenseCategoryClassifier.CATEGORY_LIVING_MART        -> "생활/마트"
    ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE    -> "쇼핑/온라인"
    ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE    -> "문화/여가"
    ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION -> "고정비/구독"
    ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL     -> "건강/의료"
    else                                                   -> "기타"
}

private fun categoryColorFor(category: String): Color = when (category) {
    ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE          -> CategoryFood
    ExpenseCategoryClassifier.CATEGORY_LIVING_MART        -> CategoryShopping
    ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE    -> CategoryGame
    ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE    -> CategoryCulture
    ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION -> CategoryBeauty
    ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL     -> Color(0xFF34D399)
    else                                                   -> CategoryOther
}

private fun categoryEmojiForClassifier(category: String): String = when (category) {
    ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE          -> "🍽️"
    ExpenseCategoryClassifier.CATEGORY_LIVING_MART        -> "🛒"
    ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE    -> "🛍️"
    ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE    -> "🎬"
    ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION -> "📱"
    ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL     -> "💊"
    else                                                   -> "📦"
}

// ── 실제 거래 데이터를 월별로 집계해 TrendsTab에 넘기는 함수 ──────────────────────

private fun buildMonthlyData(transactions: List<Transaction>): List<MonthlyData> {
    val monthNames = listOf("", "1월", "2월", "3월", "4월", "5월", "6월",
                             "7월", "8월", "9월", "10월", "11월", "12월")
    val now = YearMonth.now()
    // 최근 6개월 집계
    val built = (5 downTo 0).map { offset ->
        val ym = now.minusMonths(offset.toLong())
        val total = transactions
            .filter { tx ->
                runCatching {
                    YearMonth.from(java.time.LocalDateTime.parse(tx.occurredAt).toLocalDate()) == ym
                }.getOrDefault(false)
            }
            .sumOf { it.amount }
            .toInt()
        MonthlyData(
            month  = if (ym.year == now.year) monthNames[ym.monthValue] else "${ym.monthValue}월",
            amount = total
        )
    }.filter { it.amount > 0 }

    // 데이터가 없으면 현재 월만 0으로 표시
    return built.ifEmpty {
        listOf(MonthlyData(monthNames[now.monthValue], 0))
    }
}

// ── transactions 기반 CategoryData 빌드 ──────────────────────────────────────

private fun buildCategoriesFromStats(
    categorySpending: Map<String, Long>,
    transactions: List<Transaction>
): List<CategoryData> {
    val total = categorySpending.values.sum().coerceAtLeast(1L)
    val transactionCountsByCategory = transactions.groupingBy { it.category }.eachCount()
    return categorySpending
        .filter { it.value > 0L }
        .map { (category, amount) ->
            CategoryData(
                name       = category,
                value      = amount.toInt(),
                count      = transactionCountsByCategory[category] ?: 0,
                percentage = ((amount.toFloat() / total) * 100).toInt(),
                color      = categoryColorFor(category)
            )
        }
        .sortedByDescending { it.value }
}

// ── LedgerScreen ──────────────────────────────────────────────────────────────

@Composable
fun LedgerScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store   = remember { UserStatsStore.getInstance(context) }
    val stats   by store.statsFlow.collectAsState()
    val storedTransactions by store.transactionsFlow.collectAsState()

    var selectedTab      by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var budgetEnabled    by remember { mutableStateOf(true) }
    var currentMonth     by remember { mutableStateOf(YearMonth.now()) }

    // ── currentMonth 기준으로 거래 필터링 ────────────────────────────────────
    val transactions = storedTransactions.filter {
        val txMonth = runCatching {
            YearMonth.from(java.time.LocalDateTime.parse(it.occurredAt).toLocalDate())
        }.getOrNull()
        txMonth == null || txMonth == currentMonth
    }

    // [FIX #2] stats.categorySpending(이번 달 고정) 대신
    //          필터된 transactions에서 직접 카테고리/합계 집계
    val categorySpendingForMonth = transactions
        .groupBy { it.category }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    val categories    = buildCategoriesFromStats(categorySpendingForMonth, transactions)
    val totalSpending = transactions.sumOf { it.amount }.toInt()

    // [FIX #5] SampleData.monthly 대신 실제 거래 데이터로 월별 집계
    val monthlyData = buildMonthlyData(storedTransactions)

    val monthlyBudget        = 1_500_000
    val budgetUsedPercentage = totalSpending.toFloat() / monthlyBudget * 100f
    val remainingBudget      = monthlyBudget - totalSpending

    val filteredTransactions = if (selectedCategory != null)
        transactions.filter { it.category == selectedCategory }
    else transactions

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

        // ── Header ───────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.Close, null, tint = Gray600)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    currentMonth = currentMonth.minusMonths(1)
                    selectedCategory = null
                }) {
                    Icon(Icons.Rounded.KeyboardArrowLeft, null, tint = Gray600)
                }
                Text(
                    text      = "${currentMonth.year}년 ${currentMonth.monthValue}월",
                    style     = MaterialTheme.typography.titleMedium,
                    modifier  = Modifier.width(120.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = {
                    currentMonth = currentMonth.plusMonths(1)
                    selectedCategory = null
                }) {
                    Icon(Icons.Rounded.KeyboardArrowRight, null, tint = Gray600)
                }
            }
            Spacer(modifier = Modifier.width(48.dp))
        }

        // ── Tab Row ───────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = Color.White,
            contentColor     = Blue500,
            divider          = { HorizontalDivider(color = Gray100) }
        ) {
            listOf("가계부", "동향 변화", "예산 관리").forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick  = { selectedTab = i },
                    text = {
                        Text(
                            title,
                            color      = if (selectedTab == i) Blue500 else Gray400,
                            fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // ── Tab Content ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            when (selectedTab) {
                0 -> LedgerTab(
                    categories       = categories,
                    transactions     = filteredTransactions,
                    selectedCategory = selectedCategory,
                    totalSpending    = totalSpending,
                    currentMonth     = currentMonth,
                    onCategoryClick  = { cat ->
                        selectedCategory = if (selectedCategory == cat) null else cat
                    }
                )
                1 -> TrendsTab(monthlyData)
                2 -> BudgetTab(
                    budgetEnabled, { budgetEnabled = it },
                    monthlyBudget, budgetUsedPercentage, remainingBudget, categories
                )
            }
        }
    }
}

// ── Tab 1: 가계부 ─────────────────────────────────────────────────────────────

@Composable
private fun LedgerTab(
    categories: List<CategoryData>,
    transactions: List<Transaction>,
    selectedCategory: String?,
    totalSpending: Int,
    currentMonth: YearMonth,
    onCategoryClick: (String) -> Unit
) {
    // 데이터가 없을 때 안내 화면
    if (categories.isEmpty()) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📊", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "아직 지출 내역이 없어요",
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = Gray500,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "카드 결제 알림을 받으면\n자동으로 기록돼요",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = Gray400,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val total = totalSpending.toLong().coerceAtLeast(1L)
    val isCurrentMonth = currentMonth == YearMonth.now()
    val chartLabel = if (isCurrentMonth) "이번 달 총 지출" else "${currentMonth.monthValue}월 총 지출"

    // Donut chart
    Box(
        modifier         = Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(220.dp)) {
                var startAngle = -90f
                categories.forEach { cat ->
                    val sweep = cat.value / total.toFloat() * 360f
                    val alpha = if (selectedCategory == null || selectedCategory == cat.name) 1f else 0.25f
                    drawArc(
                        color      = cat.color.copy(alpha = alpha),
                        startAngle = startAngle,
                        sweepAngle = sweep - 1.5f,
                        useCenter  = false,
                        style      = Stroke(width = 64f, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(chartLabel, style = MaterialTheme.typography.bodySmall, color = Gray500)
                Text(
                    "₩${String.format("%,d", total)}",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Category legend grid (3 columns)
    val rows = categories.chunked(3)
    Column(
        modifier            = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { cat ->
                    val isActive = selectedCategory == cat.name
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) Gray100 else Gray50)
                            .clickable { onCategoryClick(cat.name) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier         = Modifier.size(38.dp).background(cat.color.copy(0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text(categoryEmoji(cat.name), fontSize = 16.sp) }
                            Spacer(Modifier.height(6.dp))
                            Text(cat.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text("${cat.percentage}%", style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }

    // Category breakdown list
    Spacer(Modifier.height(20.dp))
    Text(
        "카테고리별 내역",
        style      = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color      = Gray600,
        modifier   = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
    categories.forEach { cat ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Gray50)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier.size(48.dp).background(cat.color.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(categoryEmoji(cat.name), fontSize = 22.sp) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (cat.count > 0) {
                        Text("${cat.count}건", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    }
                }
            }
            Text(formatWon(cat.value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }

    // Transaction list
    if (transactions.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(
            if (selectedCategory != null) "${selectedCategory} 내역" else "전체 내역",
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color      = Gray600,
            modifier   = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        transactions.forEach { tx ->
            val catColor = categoryColorFor(tx.category)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Gray100, RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier         = Modifier.size(40.dp).background(catColor.copy(0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text(categoryEmojiForClassifier(tx.category), fontSize = 18.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tx.store,
                            style    = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            "${tx.category} · ${tx.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }
                Text(
                    formatWon(tx.amount),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

// ── Tab 2: 동향 변화 ─────────────────────────────────────────────────────────
// [FIX #5] SampleData.monthly 제거 → 실제 집계 데이터(buildMonthlyData) 사용
// [FIX #5] 하드코딩된 "2026년 월별 지출", 평균값, 텍스트 → 실제 계산값으로 교체

@Composable
private fun TrendsTab(monthlyData: List<MonthlyData>) {
    val now        = YearMonth.now()
    val headerText = "${now.year}년 최근 월별 지출"
    val avgAmount  = if (monthlyData.isEmpty()) 0
                     else monthlyData.sumOf { it.amount } / monthlyData.size
    val lastEntry  = monthlyData.lastOrNull()
    val avgDiffText = when {
        lastEntry == null || avgAmount == 0 -> "데이터가 부족해요"
        else -> {
            val diff = ((lastEntry.amount - avgAmount).toFloat() / avgAmount * 100).toInt()
            if (diff >= 0) "${lastEntry.month}은 평균보다 ${diff}% 높아요"
            else           "${lastEntry.month}은 평균보다 ${-diff}% 낮아요"
        }
    }

    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            headerText,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color      = Gray600,
            modifier   = Modifier.padding(bottom = 16.dp)
        )

        if (monthlyData.all { it.amount == 0 }) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("지출 내역이 없어요", style = MaterialTheme.typography.bodyMedium, color = Gray400)
            }
            return
        }

        val maxAmount = monthlyData.maxOf { it.amount }.toFloat().coerceAtLeast(1f)
        Row(
            modifier              = Modifier.fillMaxWidth().height(200.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.Bottom
        ) {
            monthlyData.forEach { data ->
                val fraction = data.amount / maxAmount
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height((fraction * 160).dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(Blue400)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(data.month, style = MaterialTheme.typography.bodySmall, color = Gray400)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Blue50)
                .padding(16.dp)
        ) {
            Column {
                Text("월 평균 지출", style = MaterialTheme.typography.bodyMedium, color = Gray600)
                Text(
                    formatWon(avgAmount),
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(top = 4.dp)
                )
                Text(
                    avgDiffText,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = Gray500,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ── Tab 3: 예산 관리 ──────────────────────────────────────────────────────────

@Composable
private fun BudgetTab(
    budgetEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    monthlyBudget: Int,
    budgetUsedPercentage: Float,
    remainingBudget: Int,
    categories: List<CategoryData>
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Purple50, Blue50)))
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("예산 추적", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Gray600)
                    Switch(
                        checked         = budgetEnabled,
                        onCheckedChange = onToggle,
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Blue500
                        )
                    )
                }

                if (budgetEnabled) {
                    Spacer(Modifier.height(16.dp))
                    Text("월 예산", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text(
                        formatWon(monthlyBudget),
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(top = 4.dp)
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("사용 현황", style = MaterialTheme.typography.bodySmall, color = Gray600)
                        Text("${budgetUsedPercentage.toInt()}% 사용됨", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress   = { (budgetUsedPercentage / 100f).coerceIn(0f, 1f) },
                        modifier   = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                        color      = Blue500,
                        trackColor = Gray200
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Gray200)

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("남은 예산", style = MaterialTheme.typography.bodySmall, color = Gray600)
                        Text(
                            formatWon(remainingBudget),
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color      = if (remainingBudget > 0) GreenSuccess else RedDanger
                        )
                    }

                    if (budgetUsedPercentage > 80) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFEDD5))
                                .padding(12.dp)
                        ) {
                            Text(
                                "💡 예산의 ${budgetUsedPercentage.toInt()}%를 사용했어요. 남은 기간 주의하세요!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9A3412)
                            )
                        }
                    }
                }
            }
        }

        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                "카테고리별 예산",
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = Gray600,
                modifier   = Modifier.padding(bottom = 12.dp)
            )
            categories.take(4).forEach { cat ->
                val catBudget   = monthlyBudget * (cat.percentage / 100f)
                val catProgress = if (catBudget > 0) (cat.value / catBudget).coerceIn(0f, 1f) else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Gray50)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier         = Modifier.size(32.dp).background(cat.color.copy(0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text(categoryEmoji(cat.name), fontSize = 14.sp) }
                            Spacer(Modifier.width(8.dp))
                            Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            "${formatWon(cat.value)} / ${formatWon(catBudget.toInt())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress   = { catProgress },
                        modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color      = cat.color,
                        trackColor = Gray200
                    )
                }
            }
        }
    }
}
