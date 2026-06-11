package com.example.personalfinance.ui.main

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.example.personalfinance.data.*
import com.example.personalfinance.network.ApiClient
import com.example.personalfinance.network.MonthlyStatsResponse
import com.example.personalfinance.ui.theme.*
import kotlinx.coroutines.launch

// ── 일별 지출 데이터 모델 ──────────────────────────────────────────────────────

private data class DailySpendingData(
    val day: Int,
    val amount: Long,
    val topCategory: String
)

// ── 헬퍼 함수들 ───────────────────────────────────────────────────────────────

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

private val merchantTechnicalSuffixPattern = Regex(
    pattern = """[_\s]+(?:나이스|KICC)\s*(?:\(\s*\))?\s*$""",
    option = RegexOption.IGNORE_CASE
)
private val merchantBankPrefixPattern = Regex(
    pattern = """^(?:완료\s+)?(?:NH농협은행|농협은행)\s+(.+)$""",
    option = RegexOption.IGNORE_CASE
)
private val merchantLeadingSeparatorPattern = Regex("""^[\s"ㆍ·•･・‧∙\-_]+""")

private fun displayMerchantName(rawName: String): String {
    val normalized = rawName.trim().replace(Regex("""\s+"""), " ")
    val withoutBankPrefix = merchantBankPrefixPattern
        .matchEntire(normalized)
        ?.groupValues
        ?.getOrNull(1)
        ?: normalized
    val cleaned = withoutBankPrefix
        .trim()
        .replace(merchantTechnicalSuffixPattern, "")
        .replace(Regex("""\s*\(\s*\)\s*$"""), "")
        .replace(merchantLeadingSeparatorPattern, "")
        .trim()
    val parts = cleaned.split(Regex("""\s+""")).filter { it.isNotBlank() }
    val deduped = if (parts.size > 1 && parts.distinct().size == 1) parts.first() else cleaned

    return deduped.ifBlank { rawName.trim() }
}

private val detailDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm")

private val transactionZone: ZoneId = ZoneId.of("Asia/Seoul")
private val transactionDisplayFallbackFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM/dd HH:mm")

private fun transactionLocalDateTime(value: String): LocalDateTime? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null

    return runCatching {
        OffsetDateTime.parse(trimmed).atZoneSameInstant(transactionZone).toLocalDateTime()
    }.getOrNull()
        ?: runCatching {
            ZonedDateTime.parse(trimmed).withZoneSameInstant(transactionZone).toLocalDateTime()
        }.getOrNull()
        ?: runCatching {
            Instant.parse(trimmed).atZone(transactionZone).toLocalDateTime()
        }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(trimmed)
        }.getOrNull()
        ?: runCatching {
            LocalDate.parse(trimmed).atStartOfDay()
        }.getOrNull()
}

private fun transactionDateFallback(value: String): LocalDate? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null

    return runCatching {
        LocalDate.parse(trimmed)
    }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(trimmed).toLocalDate()
        }.getOrNull()
        ?: runCatching {
            val monthDay = MonthDay.from(transactionDisplayFallbackFormatter.parse(trimmed))
            monthDay.atYear(LocalDate.now(transactionZone).year)
        }.getOrNull()
}

private fun transactionLocalDate(tx: Transaction): LocalDate? =
    transactionLocalDateTime(tx.occurredAt)?.toLocalDate()
        ?: transactionDateFallback(tx.date)

private fun transactionLocalDateTime(tx: Transaction): LocalDateTime? =
    transactionLocalDateTime(tx.occurredAt)
        ?: transactionDateFallback(tx.date)?.atStartOfDay()

private fun formatTransactionDateTime(value: String, fallback: String): String =
    transactionLocalDateTime(value)?.format(detailDateTimeFormatter) ?: fallback

/**
 * Builds daily spending from local transactions so chart bars and the selected day list
 * always use the same transaction date rule.
 */
private fun mergeDailyData(
    localTransactions: List<Transaction>,
    month: YearMonth
): List<DailySpendingData> {
    val daysInMonth = month.lengthOfMonth()

    // 로컬 거래에서 일별 카테고리 집계 (topCategory 결정용)
    val localByDay = localTransactions
        .mapNotNull { tx ->
            val txDate = transactionLocalDate(tx)
            if (txDate != null && YearMonth.from(txDate) == month) txDate.dayOfMonth to tx else null
        }
        .groupBy { it.first }
        .mapValues { (_, pairs) -> pairs.map { it.second } }

    return (1..daysInMonth).map { day ->
        val amount = localByDay[day]?.sumOf { it.amount } ?: 0L
        val topCat = localByDay[day]
            ?.groupBy { it.category }
            ?.maxByOrNull { (_, v) -> v.sumOf { it.amount } }
            ?.key ?: ExpenseCategoryClassifier.CATEGORY_OTHER
        DailySpendingData(day = day, amount = amount, topCategory = topCat)
    }
}

/** 월별 집계 (최근 6개월, 실거래 기반) */
private fun buildMonthlyData(transactions: List<Transaction>): List<MonthlyData> {
    val monthNames = listOf("", "1월", "2월", "3월", "4월", "5월", "6월",
                             "7월", "8월", "9월", "10월", "11월", "12월")
    val now = YearMonth.now()
    val built = (5 downTo 0).map { offset ->
        val ym = now.minusMonths(offset.toLong())
        val total = transactions.filter { tx ->
            transactionLocalDate(tx)?.let { YearMonth.from(it) == ym } == true
        }.sumOf { it.amount }.toInt()
        MonthlyData(
            month  = if (ym.year == now.year) monthNames[ym.monthValue] else "${ym.year % 100}/" + monthNames[ym.monthValue],
            amount = total
        )
    }.filter { it.amount > 0 }
    return built.ifEmpty { listOf(MonthlyData(monthNames[now.monthValue], 0)) }
}

/** transactions 기반 CategoryData 빌드 */
private fun buildCategoriesFromStats(
    categorySpending: Map<String, Long>,
    transactions: List<Transaction>
): List<CategoryData> {
    val total = categorySpending.values.sum().coerceAtLeast(1L)
    val countByCategory = transactions.groupingBy { it.category }.eachCount()
    return categorySpending
        .filter { it.value > 0L }
        .map { (category, amount) ->
            CategoryData(
                name       = category,
                value      = amount.toInt(),
                count      = countByCategory[category] ?: 0,
                percentage = ((amount.toFloat() / total) * 100).toInt(),
                color      = categoryColorFor(category)
            )
        }
        .sortedByDescending { it.value }
}

// ── LedgerScreen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store   = remember { UserStatsStore.getInstance(context) }
    val storedTransactions by store.transactionsFlow.collectAsState()
    val userStats by store.statsFlow.collectAsState()

    var selectedTab      by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var budgetEnabled    by remember { mutableStateOf(true) }
    var currentMonth     by remember { mutableStateOf(YearMonth.now()) }

    var showCategoryEditSheet      by remember { mutableStateOf(false) }
    var selectedTransactionForEdit by remember { mutableStateOf<Transaction?>(null) }
    var showTransactionDetailSheet by remember { mutableStateOf(false) }
    var selectedTransactionDetailId by remember { mutableStateOf<String?>(null) }
    var reopenDetailAfterCategoryEdit by remember { mutableStateOf(false) }
    var showBudgetDialog           by remember { mutableStateOf(false) }
    var editingBudget              by remember { mutableStateOf("") }
    var budgetSaveError            by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()

    // ── 서버 stats 상태 ───────────────────────────────────────────────────────
    var serverStats      by remember { mutableStateOf<MonthlyStatsResponse?>(null) }
    var statsLoading     by remember { mutableStateOf(false) }
    var statsRefreshNonce by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        store.refreshServerStats()
    }

    // 월이 바뀔 때마다 서버에서 dailyBreakdown 조회
    // statsLoading은 헤더 스피너용 — UI는 로컬 데이터를 즉시 표시하고 서버 응답으로 업데이트
    LaunchedEffect(currentMonth, statsRefreshNonce) {
        statsLoading = true
        serverStats  = null
        try {
            val tokenManager = com.example.personalfinance.data.TokenManager(context)
            val api = ApiClient.getTransactionApi(context, tokenManager)
            val resp = kotlinx.coroutines.withTimeoutOrNull(5_000L) {
                api.getStats(currentMonth.year, currentMonth.monthValue)
            }
            if (resp?.isSuccessful == true) {
                serverStats = resp.body()
            }
        } catch (e: Exception) {
            // 오프라인이거나 서버 오류 → 로컬 데이터로 폴백
        } finally {
            statsLoading = false
        }
    }

    // 선택된 월 거래만 필터
    val transactions = storedTransactions.filter { tx ->
        transactionLocalDate(tx)?.let { YearMonth.from(it) == currentMonth } == true
    }
    val selectedTransactionForDetail = selectedTransactionDetailId?.let { selectedId ->
        storedTransactions.firstOrNull { it.id == selectedId }
            ?: transactions.firstOrNull { it.id == selectedId }
    }

    // 카테고리별 지출: 서버 categoryBreakdown 우선, 없으면 로컬 집계
    val categorySpendingForMonth: Map<String, Long> =
        serverStats?.categoryBreakdown?.ifEmpty { null }
            ?: transactions.groupBy { it.category }.mapValues { (_, txs) -> txs.sumOf { it.amount } }

    val categories    = buildCategoriesFromStats(categorySpendingForMonth, transactions)
    val totalSpending = (serverStats?.totalAmount ?: transactions.sumOf { it.amount }).toInt()

    // 일별 데이터: 로컬 거래 날짜 기준으로 목록과 동일하게 집계
    val dailyData = mergeDailyData(
        localTransactions = transactions,
        month             = currentMonth
    )

    // 월별 동향 (전체 거래 기반)
    val monthlyData = buildMonthlyData(storedTransactions)

    val monthlyBudget        = userStats.monthlyBudget
    val budgetUsedPercentage = if (monthlyBudget > 0L) {
        totalSpending.toFloat() / monthlyBudget.toFloat() * 100f
    } else {
        0f
    }
    val remainingBudget      = monthlyBudget - totalSpending.toLong()

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {

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
                    }) { Icon(Icons.Rounded.KeyboardArrowLeft, null, tint = Gray600) }
                    Text(
                        text      = "${currentMonth.year}년 ${currentMonth.monthValue}월",
                        style     = MaterialTheme.typography.titleMedium,
                        modifier  = Modifier.width(120.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = {
                        currentMonth = currentMonth.plusMonths(1)
                        selectedCategory = null
                    }) { Icon(Icons.Rounded.KeyboardArrowRight, null, tint = Gray600) }
                }
                // 로딩 스피너 (서버 조회 중일 때)
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    if (statsLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Blue400)
                    }
                }
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
                        categories         = categories,
                        transactions       = transactions,
                        selectedCategory   = selectedCategory,
                        totalSpending      = totalSpending,
                        currentMonth       = currentMonth,
                        dailyData          = dailyData,
                        statsLoading       = statsLoading,
                        onCategoryClick    = { cat -> selectedCategory = if (selectedCategory == cat) null else cat },
                        onTransactionClick = { tx ->
                            selectedTransactionDetailId = tx.id
                            showTransactionDetailSheet = true
                        }
                    )
                    1 -> TrendsTab(
                        monthlyData  = monthlyData,
                        dailyData    = dailyData,
                        currentMonth = currentMonth,
                        statsLoading = statsLoading
                    )
                    2 -> BudgetTab(
                        budgetEnabled, { budgetEnabled = it },
                        monthlyBudget, budgetUsedPercentage, remainingBudget, categories,
                        onEditBudget = {
                            editingBudget = monthlyBudget.toString()
                            budgetSaveError = null
                            showBudgetDialog = true
                        }
                    )
                }
            }
        }

        UserStatsFeedbackHost(
            store = store,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
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

    if (showTransactionDetailSheet && selectedTransactionForDetail != null) {
        val detailTx = selectedTransactionForDetail
        val catColor = categoryColorFor(detailTx.category)
        val detailMerchantName = displayMerchantName(detailTx.store)
        ModalBottomSheet(
            onDismissRequest = {
                showTransactionDetailSheet = false
                selectedTransactionDetailId = null
            },
            sheetState = detailSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // ── 헤더: 금액 + 가게명 + 카테고리 뱃지 ──────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        formatWon(detailTx.amount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gray900
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        detailMerchantName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray500
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = catColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${categoryEmojiForClassifier(detailTx.category)}  ${detailTx.category}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = Gray100)

                // ── 상세 행들 ────────────────────────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    TransactionDetailRow(
                        icon  = Icons.Rounded.ShoppingCart,
                        label = "결제처",
                        value = detailMerchantName
                    )
                    if (detailMerchantName != detailTx.store) {
                        TransactionDetailRow(
                            icon  = Icons.Rounded.Info,
                            label = "원본 가맹점명",
                            value = detailTx.store,
                            valueColor = Gray500
                        )
                    }
                    TransactionDetailRow(
                        icon  = Icons.Rounded.DateRange,
                        label = "결제 일시",
                        value = formatTransactionDateTime(detailTx.occurredAt, detailTx.date)
                    )


                }

                HorizontalDivider(color = Gray100, modifier = Modifier.padding(top = 4.dp))

                // ── 버튼 ────────────────────────────────────────────────────
                var showDeleteConfirm by remember { mutableStateOf(false) }
                var isDeleting by remember(detailTx.id) { mutableStateOf(false) }

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            selectedTransactionForEdit = detailTx
                            reopenDetailAfterCategoryEdit = true
                            scope.launch { detailSheetState.hide() }.invokeOnCompletion {
                                showTransactionDetailSheet = false
                                showCategoryEditSheet = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue500),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
                    ) {
                        Text("수정하기", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedDanger.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("삭제하기", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    TextButton(
                        onClick = {
                            showTransactionDetailSheet = false
                            selectedTransactionDetailId = null
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("닫기", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Gray400)
                    }
                }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = {
                            if (!isDeleting) showDeleteConfirm = false
                        },
                        title = { Text("소비 내역을 삭제할까요?", fontWeight = FontWeight.SemiBold) },
                        text = {
                            Text(
                                "'${displayMerchantName(detailTx.store)}' ${formatWon(detailTx.amount)} 내역을 삭제합니다.\n삭제한 내역은 되돌릴 수 없어요.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray600
                            )
                        },
                        confirmButton = {
                            TextButton(
                                enabled = !isDeleting,
                                onClick = {
                                    scope.launch {
                                        isDeleting = true
                                        val deleted = store.deleteTransaction(detailTx.id)
                                        isDeleting = false

                                        if (deleted) {
                                            showDeleteConfirm = false
                                            serverStats = null
                                            statsRefreshNonce += 1
                                            detailSheetState.hide()
                                            showTransactionDetailSheet = false
                                            selectedTransactionDetailId = null
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "삭제에 실패했습니다. 잠시 후 다시 시도해 주세요.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    if (isDeleting) "삭제 중..." else "삭제",
                                    color = RedDanger,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                enabled = !isDeleting,
                                onClick = { showDeleteConfirm = false }
                            ) {
                                Text("취소", color = Gray500)
                            }
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }

    // ── Category / MerchantName Edit Bottom Sheet ──────────────────────────────
    if (showCategoryEditSheet && selectedTransactionForEdit != null) {
        val editTx = selectedTransactionForEdit!!
        var editingMerchantName by remember(editTx.id) {
            mutableStateOf(displayMerchantName(editTx.store))
        }
        var selectedCategoryInSheet by remember(editTx.id) {
            mutableStateOf(editTx.category)
        }
        var isSaving by remember(editTx.id) { mutableStateOf(false) }
        var saveError by remember(editTx.id) { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = {
                showCategoryEditSheet = false
                reopenDetailAfterCategoryEdit = false
            },
            sheetState     = sheetState,
            containerColor = Color.White,
            shape          = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "가맹점명 / 카테고리 수정",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Gray600
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "가맹점명을 수정하고 카테고리를 선택하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500
                )
                Spacer(Modifier.height(20.dp))

                // ── 가맹점명 입력 ─────────────────────────────────────────────
                OutlinedTextField(
                    value         = editingMerchantName,
                    onValueChange = { editingMerchantName = it; saveError = null },
                    label         = { Text("가맹점명") },
                    placeholder   = { Text("예: AliExpress") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(14.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue500,
                        focusedLabelColor  = Blue500
                    )
                )
                Spacer(Modifier.height(20.dp))

                // ── 카테고리 선택 그리드 ──────────────────────────────────────
                ExpenseCategoryClassifier.categories.chunked(3).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { cat ->
                            val isSelected = cat == selectedCategoryInSheet
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Blue50 else Gray50)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Blue400 else Gray100,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedCategoryInSheet = cat }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(categoryEmojiForClassifier(cat), fontSize = 24.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        cat,
                                        style      = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color      = if (isSelected) Blue500 else Gray600
                                    )
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                // ── 오류 메시지 ───────────────────────────────────────────────
                saveError?.let { msg ->
                    Text(
                        msg,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = RedDanger,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // ── 저장 버튼 ─────────────────────────────────────────────────
                val merchantChanged = editingMerchantName.trim() != displayMerchantName(editTx.store)
                val categoryChanged = selectedCategoryInSheet != editTx.category
                val hasAnyChange    = (merchantChanged || categoryChanged) && !isSaving

                Button(
                    onClick = {
                        if (!hasAnyChange) return@Button
                        isSaving  = true
                        saveError = null
                        val newMerchant = editingMerchantName.trim().takeIf { merchantChanged }
                        val newCategory = selectedCategoryInSheet.takeIf { categoryChanged }
                        val ok = store.updateTransactionFields(editTx.id, newMerchant, newCategory)
                        isSaving = false
                        if (ok) {
                            selectedTransactionDetailId = editTx.id
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showCategoryEditSheet = false
                                    if (reopenDetailAfterCategoryEdit) {
                                        reopenDetailAfterCategoryEdit = false
                                        showTransactionDetailSheet = true
                                    }
                                }
                            }
                        } else {
                            saveError = "수정에 실패했습니다. 다시 시도해주세요."
                        }
                    },
                    modifier  = Modifier.fillMaxWidth().height(52.dp),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor         = if (hasAnyChange) Blue500 else Gray200,
                        disabledContainerColor = Gray200
                    ),
                    enabled   = hasAnyChange,
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color       = Color.White
                        )
                    } else {
                        Text("저장", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = Gray700
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Gray400,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
        value.ifBlank { "-" },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = valueColor,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(1f)
        )
    }
    HorizontalDivider(color = Gray100)
}

// ── 일별 지출 바차트 (공통 컴포넌트) ─────────────────────────────────────────────

@Composable
private fun DailySpendingChart(
    dailyData: List<DailySpendingData>,
    currentMonth: YearMonth,
    statsLoading: Boolean = false,
    selectedDay: Int? = null,
    onDaySelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isThisMonth = currentMonth.year == today.year && currentMonth.monthValue == today.monthValue
    val hasSpending = dailyData.any { it.amount > 0L }
    val chartH = 136.dp
    val barWidth = 30.dp
    val barGap = 6.dp
    val minSpendingBarH = 32.dp
    val emptyBarH = 4.dp

    if (statsLoading && !hasSpending) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(chartH + 18.dp)
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(barGap),
            verticalAlignment = Alignment.Bottom
        ) {
            val skeletonHeights = listOf(0.42f, 0.72f, 0.34f, 0.88f, 0.56f, 0.66f, 0.26f)
            repeat(8) { index ->
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .fillMaxHeight(skeletonHeights[index % skeletonHeights.size])
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(Gray100)
                )
            }
        }
        return
    }

    val selectedData = selectedDay?.let { day -> dailyData.firstOrNull { it.day == day } }
    val maxAmount = dailyData.maxOfOrNull { it.amount }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val listState = rememberLazyListState()
    val initialScrollIndex = when {
        isThisMonth -> today.dayOfMonth - 1
        selectedDay != null -> selectedDay - 1
        else -> dailyData.indexOfLast { it.amount > 0L }
    }

    LaunchedEffect(currentMonth, dailyData.size, isThisMonth, selectedDay) {
        if (dailyData.isNotEmpty()) {
            val target = (initialScrollIndex.coerceAtLeast(0) - 3).coerceAtLeast(0)
            listState.scrollToItem(target)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        androidx.compose.animation.AnimatedVisibility(
            visible = selectedData != null,
            enter = androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 140)
            ) + androidx.compose.animation.expandVertically(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 140)
            ),
            exit = androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 100)
            ) + androidx.compose.animation.shrinkVertically(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 100)
            )
        ) {
            selectedData?.let { data ->
                val categoryColor = categoryColorFor(data.topCategory)
                val summaryColor = if (data.amount > 0L) categoryColor else Gray400
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (data.amount > 0L) summaryColor.copy(alpha = 0.10f) else Gray50,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(summaryColor, CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${currentMonth.monthValue}월 ${data.day}일",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Gray700
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (data.amount > 0L) data.topCategory else "지출 없음",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (data.amount > 0L) Gray500 else Gray400,
                                maxLines = 1
                            )
                        }
                        Text(
                            if (data.amount > 0L) formatWon(data.amount) else "지출 없음",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = summaryColor
                        )
                    }
                }
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(chartH + 22.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(barGap),
            verticalAlignment = Alignment.Bottom
        ) {
            items(dailyData.size) { index ->
                val data = dailyData[index]
                val isToday = isThisMonth && data.day == today.dayOfMonth
                val isSelected = selectedDay == data.day
                val categoryColor = categoryColorFor(data.topCategory)
                val rawFraction = if (data.amount > 0L) (data.amount / maxAmount).coerceIn(0f, 1f) else 0f
                val visualFraction = if (data.amount > 0L) {
                    (0.25f + 0.75f * kotlin.math.sqrt(rawFraction.toDouble()).toFloat()).coerceIn(0.25f, 1f)
                } else {
                    0f
                }
                val barH = when {
                    data.amount > 0L -> (chartH.value * visualFraction).coerceAtLeast(minSpendingBarH.value).dp
                    else -> emptyBarH
                }
                val selectedColor = if (data.amount > 0L) categoryColor else Gray200
                val barColor = when {
                    isSelected && data.amount > 0L -> selectedColor
                    isSelected -> Gray100
                    data.amount > 0L -> categoryColor.copy(alpha = 0.48f)
                    else -> Gray100
                }
                val label = when {
                    isToday -> "오늘"
                    isSelected -> "${data.day}일"
                    data.day == 1 || data.day % 5 == 0 -> "${data.day}일"
                    else -> ""
                }
                val labelColor = when {
                    isSelected && data.amount > 0L -> selectedColor
                    isSelected -> Gray500
                    isToday -> Blue500
                    else -> Gray400
                }

                // ── 클릭 영역을 Column 전체로 확장 ──────────────────────────
                Column(
                    modifier = Modifier
                        .width(barWidth)
                        .height(chartH + 22.dp)
                        .clickable { onDaySelected(data.day) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartH),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (isToday) {
                            val todayDotColor = if (data.amount > 0L) Blue500 else Gray200
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .size(if (isSelected) 7.dp else 5.dp)
                                    .background(todayDotColor, CircleShape)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(barH)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(barColor)
                                .border(
                                    width = if (isSelected && data.amount > 0L) 1.5.dp else 0.dp,
                                    color = if (isSelected && data.amount > 0L) selectedColor else Color.Transparent,
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                )
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        modifier = Modifier.height(13.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                        color = labelColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 6.dp, start = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isThisMonth) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(Blue500, CircleShape))
                    Spacer(Modifier.width(5.dp))
                    Text("오늘", style = MaterialTheme.typography.labelSmall, color = Gray500)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 8.dp, height = 10.dp).background(Gray200, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(5.dp))
                Text("탭하면 상세 보기", style = MaterialTheme.typography.labelSmall, color = Gray400)
            }
        }
    }
}

// ── Tab 0: 가계부 ─────────────────────────────────────────────────────────────

@Composable
private fun LedgerTab(
    categories: List<CategoryData>,
    transactions: List<Transaction>,
    selectedCategory: String?,
    totalSpending: Int,
    currentMonth: YearMonth,
    dailyData: List<DailySpendingData>,
    statsLoading: Boolean,
    onCategoryClick: (String) -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    val today = LocalDate.now()
    val isThisMonth = currentMonth.year == today.year && currentMonth.monthValue == today.monthValue
    var selectedDay by remember(currentMonth) {
        mutableStateOf<Int?>(null)
    }
    var isAllTransactionsExpanded by remember(currentMonth, selectedCategory, selectedDay) {
        mutableStateOf(false)
    }
    val timeFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("HH:mm") }
    val listDateTimeFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("M/d HH:mm") }
    val selectedDate = selectedDay?.let { currentMonth.atDay(it) }
    val selectedDayTransactions = selectedDate?.let { date ->
        transactions
            .filter { tx ->
                transactionLocalDate(tx) == date &&
                    (selectedCategory == null || tx.category == selectedCategory)
            }
            .sortedByDescending { tx -> transactionLocalDateTime(tx) ?: LocalDateTime.MIN }
    }.orEmpty()
    val categoryFilteredTransactions = transactions
        .filter { tx ->
            (selectedCategory == null || tx.category == selectedCategory) &&
                (selectedDate == null || transactionLocalDate(tx) == selectedDate)
        }
        .sortedByDescending { tx -> transactionLocalDateTime(tx) ?: LocalDateTime.MIN }
    val visibleTransactions = if (isAllTransactionsExpanded) {
        categoryFilteredTransactions
    } else {
        categoryFilteredTransactions.take(5)
    }

    if (categories.isNotEmpty()) {
        val total = totalSpending.toLong().coerceAtLeast(1L)
        val chartLabel = if (currentMonth == YearMonth.now()) "이번 달 총 지출"
                         else "${currentMonth.monthValue}월 총 지출"

        Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(200.dp)) {
                    var startAngle = -90f
                    categories.forEach { cat ->
                        val sweep = cat.value / total.toFloat() * 360f
                        val alpha = if (selectedCategory == null || selectedCategory == cat.name) 1f else 0.25f
                        drawArc(
                            color = cat.color.copy(alpha = alpha),
                            startAngle = startAngle,
                            sweepAngle = sweep - 1.5f,
                            useCenter = false,
                            style = Stroke(width = 60f, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(chartLabel, style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text(
                        formatWon(total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Gray50)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "일별 지출",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Gray600
            )
            val activeDays = dailyData.count { it.amount > 0L }
            if (activeDays > 0 && !statsLoading) {
                Text("${activeDays}일 지출", style = MaterialTheme.typography.labelSmall, color = Gray400)
            }
        }
        Spacer(Modifier.height(6.dp))
        DailySpendingChart(
            dailyData = dailyData,
            currentMonth = currentMonth,
            statsLoading = statsLoading,
            selectedDay = selectedDay,
            onDaySelected = { day -> selectedDay = if (selectedDay == day) null else day }
        )
    }

    selectedDay?.let { day ->
        Spacer(Modifier.height(12.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "선택한 날짜: ${currentMonth.atDay(day)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray600
                )
                TextButton(onClick = { selectedDay = null }) {
                    Text("전체 보기", color = Blue500, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                "${currentMonth.monthValue}월 ${day}일 지출 내역",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Gray600,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (selectedDayTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Gray50)
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "해당 날짜의 지출 내역이 없어요",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray400,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                selectedDayTransactions.forEach { tx ->
                    val catColor = categoryColorFor(tx.category)
                    val timeText = transactionLocalDateTime(tx)?.format(timeFormatter) ?: tx.date
                    val merchantName = displayMerchantName(tx.store)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Gray100, RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable { onTransactionClick(tx) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(36.dp).background(catColor.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text(categoryEmojiForClassifier(tx.category), fontSize = 16.sp) }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    tx.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = catColor,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    merchantName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Gray700,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(timeText, style = MaterialTheme.typography.labelSmall, color = Gray400)
                            }
                        }
                        Text(
                            formatWon(tx.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gray700,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }
            }
        }
    }

    if (categories.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        Text(
            "이번 달 카테고리 비율",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Gray600,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        val ratioRows = categories.take(6).chunked(3)
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ratioRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { cat ->
                        val isActive = selectedCategory == cat.name
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) cat.color.copy(alpha = 0.12f) else Gray50)
                                .border(
                                    width = if (isActive) 1.5.dp else 0.dp,
                                    color = if (isActive) cat.color else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onCategoryClick(cat.name) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier.size(38.dp).background(cat.color.copy(alpha = 0.20f), CircleShape),
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

        Spacer(Modifier.height(24.dp))
        Text(
            "카테고리별 지출 금액",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Gray600,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        categories.forEach { cat ->
            val isActive = selectedCategory == cat.name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isActive) cat.color.copy(alpha = 0.10f) else Gray50)
                    .border(
                        width = if (isActive) 1.5.dp else 0.dp,
                        color = if (isActive) cat.color else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onCategoryClick(cat.name) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).background(cat.color.copy(alpha = 0.20f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text(categoryEmoji(cat.name), fontSize = 22.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        if (cat.count > 0) Text("${cat.count}건", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    }
                }
                Text(formatWon(cat.value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (categoryFilteredTransactions.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                selectedDate != null && selectedCategory != null -> "${selectedDate} ${selectedCategory} 내역"
                selectedDate != null -> "${selectedDate} 내역"
                selectedCategory != null -> "${selectedCategory} 내역"
                else -> "전체 내역"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Gray600,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        visibleTransactions.forEach { tx ->
            val catColor = categoryColorFor(tx.category)
            val merchantName = displayMerchantName(tx.store)
            val dateTimeText = transactionLocalDateTime(tx)?.format(listDateTimeFormatter) ?: tx.date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Gray100, RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable { onTransactionClick(tx) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.size(40.dp).background(catColor.copy(alpha = 0.20f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text(categoryEmojiForClassifier(tx.category), fontSize = 18.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            merchantName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text("${tx.category} · $dateTimeText", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    }
                }
                Text(
                    formatWon(tx.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        if (categoryFilteredTransactions.size > 5) {
            OutlinedButton(
                onClick = { isAllTransactionsExpanded = !isAllTransactionsExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue500)
            ) {
                Text(
                    if (isAllTransactionsExpanded) "간단히 보기" else "전체 내역 보기",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Tab 1: 동향 변화 ──────────────────────────────────────────────────────────

@Composable
private fun TrendsTab(
    monthlyData: List<MonthlyData>,
    dailyData: List<DailySpendingData>,
    currentMonth: YearMonth,
    statsLoading: Boolean
) {
    val now        = YearMonth.now()
    val avgAmount  = if (monthlyData.isEmpty()) 0L
                     else monthlyData.sumOf { it.amount.toLong() } / monthlyData.size
    val lastEntry  = monthlyData.lastOrNull()
    val avgDiffText = when {
        lastEntry == null || avgAmount == 0L -> "데이터가 부족해요"
        else -> {
            val diff = ((lastEntry.amount - avgAmount).toFloat() / avgAmount * 100).toInt()
            if (diff >= 0) "${lastEntry.month}은 평균보다 ${diff}% 높아요"
            else           "${lastEntry.month}은 평균보다 ${-diff}% 낮아요"
        }
    }

    Column(modifier = Modifier.padding(24.dp)) {

        // ── 월별 바차트 ──────────────────────────────────────────────────────
        Text(
            "${now.year}년 최근 월별 지출",
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color      = Gray600,
            modifier   = Modifier.padding(bottom = 16.dp)
        )

        if (monthlyData.all { it.amount == 0 }) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("지출 내역이 없어요", style = MaterialTheme.typography.bodyMedium, color = Gray400)
            }
        } else {
            val maxAmount = monthlyData.maxOf { it.amount }.toFloat().coerceAtLeast(1f)
            Row(
                modifier              = Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.Bottom
            ) {
                monthlyData.forEach { data ->
                    val fraction = data.amount / maxAmount
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (fraction > 0f) {
                            Text(
                                text     = formatWon(data.amount.toInt()),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = Gray500,
                                fontSize = 8.sp
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height((fraction * 140).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (data.month == "${now.monthValue}월") Blue500 else Blue300
                                )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(data.month, style = MaterialTheme.typography.bodySmall, color = Gray400)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
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
                    Text(avgDiffText,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = Gray500,
                        modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        // ── 일별 지출 차트 ────────────────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Gray100)
        Spacer(Modifier.height(20.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "${currentMonth.monthValue}월 일별 지출",
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = Gray600
            )
            val totalThisMonth = dailyData.sumOf { it.amount }
            if (totalThisMonth > 0L && !statsLoading) {
                Text(
                    "합계 ${formatWon(totalThisMonth)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray400
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Gray50)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            DailySpendingChart(
                dailyData    = dailyData,
                currentMonth = currentMonth,
                statsLoading = statsLoading
            )
        }

        // ── 지출 많은 날 TOP 3 ────────────────────────────────────────────────
        val top3 = dailyData.filter { it.amount > 0L }.sortedByDescending { it.amount }.take(3)
        if (top3.isNotEmpty() && !statsLoading) {
            Spacer(Modifier.height(20.dp))
            Text(
                "이번 달 지출 많은 날",
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = Gray600,
                modifier   = Modifier.padding(bottom = 10.dp)
            )
            top3.forEachIndexed { index, data ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Gray50)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    when (index) {
                                        0 -> Color(0xFFFFD700)
                                        1 -> Color(0xFFC0C0C0)
                                        else -> Color(0xFFCD7F32)
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "${currentMonth.monthValue}월 ${data.day}일",
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                data.topCategory,
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                        }
                    }
                    Text(
                        formatWon(data.amount),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = categoryColorFor(data.topCategory)
                    )
                }
            }
        }
    }
}

// ── Tab 2: 예산 관리 ──────────────────────────────────────────────────────────

@Composable
private fun BudgetTab(
    budgetEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    monthlyBudget: Long,
    budgetUsedPercentage: Float,
    remainingBudget: Long,
    categories: List<CategoryData>,
    onEditBudget: () -> Unit
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
                    Text("예산 추적",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Gray600)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEditBudget) {
                            Icon(Icons.Rounded.Edit, null, tint = Blue500, modifier = Modifier.size(20.dp))
                        }
                        Switch(
                            checked         = budgetEnabled,
                            onCheckedChange = onToggle,
                            colors          = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Blue500)
                        )
                    }
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
                        Text("${budgetUsedPercentage.toInt()}% 사용됨",
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
            Text("카테고리별 예산",
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = Gray600,
                modifier   = Modifier.padding(bottom = 12.dp))
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
                        Text("${formatWon(cat.value)} / ${formatWon(catBudget.toInt())}",
                            style = MaterialTheme.typography.bodySmall, color = Gray500)
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
