package com.example.personalfinance.ui.screens

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

@Composable
fun LedgerScreen(navController: NavController) {
    var selectedTab      by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var budgetEnabled    by remember { mutableStateOf(true) }

    val categories   = SampleData.categories
    val transactions = SampleData.transactions
    val monthlyData  = SampleData.monthly

    val totalSpending         = categories.sumOf { it.value }
    val monthlyBudget         = 1_500_000
    val budgetUsedPercentage  = totalSpending.toFloat() / monthlyBudget * 100f
    val remainingBudget       = monthlyBudget - totalSpending

    val filteredTransactions = if (selectedCategory != null)
        transactions.filter { it.category == selectedCategory }
    else transactions

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.Close, null, tint = Gray600)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { }) {
                    Icon(Icons.Rounded.KeyboardArrowLeft, null, tint = Gray600)
                }
                Text(
                    text      = "2026년 4월",
                    style     = MaterialTheme.typography.titleMedium,
                    modifier  = Modifier.width(120.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { }) {
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
                0 -> LedgerTab(categories, filteredTransactions, selectedCategory) { cat ->
                    selectedCategory = if (selectedCategory == cat) null else cat
                }
                1 -> TrendsTab(monthlyData)
                2 -> BudgetTab(
                    budgetEnabled, { budgetEnabled = it },
                    monthlyBudget, budgetUsedPercentage, remainingBudget, categories
                )
            }
        }
    }
}

// ── Tab 1: 가계부 ──────────────────────────────────────────────────────────────

@Composable
private fun LedgerTab(
    categories: List<CategoryData>,
    transactions: List<Transaction>,
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit
) {
    val total = categories.sumOf { it.value }

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
                Text("이번 달 총 지출", style = MaterialTheme.typography.bodySmall, color = Gray500)
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
        modifier             = Modifier.padding(horizontal = 24.dp),
        verticalArrangement  = Arrangement.spacedBy(10.dp)
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
        style    = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color    = Gray600,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
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
                    Text("${cat.count}건", style = MaterialTheme.typography.bodySmall, color = Gray500)
                }
            }
            Text(formatWon(cat.value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }

    // Transaction list
    Spacer(Modifier.height(12.dp))
    Text(
        if (selectedCategory != null) "${selectedCategory} 내역" else "전체 내역",
        style      = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color      = Gray600,
        modifier   = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
    transactions.forEach { tx ->
        val catColor = SampleData.categories.find { it.name == tx.category }?.color ?: Gray400
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier.size(40.dp).background(catColor.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(categoryEmoji(tx.category), fontSize = 18.sp) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(tx.store, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(tx.date,  style = MaterialTheme.typography.bodySmall,  color = Gray500)
                }
            }
            Text(formatWon(tx.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Tab 2: 동향 변화 ───────────────────────────────────────────────────────────

@Composable
private fun TrendsTab(monthlyData: List<com.example.personalfinance.data.MonthlyData>) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "2026년 월별 지출",
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color      = Gray600,
            modifier   = Modifier.padding(bottom = 16.dp)
        )

        // Bar chart drawn with Canvas
        val maxAmount = monthlyData.maxOf { it.amount }.toFloat()
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
                Text("₩1,192,000", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                Text("4월은 평균보다 5% 높아요", style = MaterialTheme.typography.bodySmall, color = Gray500, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

// ── Tab 3: 예산 관리 ───────────────────────────────────────────────────────────

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
        // Budget tracker card
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
                            checkedThumbColor  = Color.White,
                            checkedTrackColor  = Blue500
                        )
                    )
                }

                if (budgetEnabled) {
                    Spacer(Modifier.height(16.dp))
                    Text("월 예산", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text(
                        formatWon(monthlyBudget),
                        style     = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier  = Modifier.padding(top = 4.dp)
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
                        progress   = { budgetUsedPercentage / 100f },
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

        Spacer(Modifier.height(24.dp))

        // Per-category budgets
        Text(
            "카테고리별 예산",
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color      = Gray600,
            modifier   = Modifier.padding(bottom = 12.dp)
        )
        categories.take(4).forEach { cat ->
            val catBudget   = monthlyBudget * (cat.percentage / 100f)
            val catProgress = (cat.value / catBudget).coerceIn(0f, 1f)

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
