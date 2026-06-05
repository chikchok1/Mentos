package com.example.personalfinance.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.personalfinance.network.ApiClient
import com.example.personalfinance.network.SaveTransactionRequest
import com.example.personalfinance.network.UpdateCategoryByClientIdRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class UserStats(
    val currentLevel: Int = 1,
    val currentXP: Int = 0,
    val nextLevelXP: Int = 100,
    val thisMonthSpending: Long = 0L,
    val categorySpending: Map<String, Long> = ExpenseCategoryClassifier.categories.associateWith { 0L },
    val transactions: List<Transaction> = emptyList()
) {
    val topCategory: String
        get() = categorySpending
            .filterValues { it > 0L }
            .maxByOrNull { it.value }
            ?.key
            ?: ExpenseCategoryClassifier.CATEGORY_OTHER
}

object UserStatsCalculator {
    private val levelThresholds = listOf(
        1  to 0,
        2  to 50,
        3  to 120,
        4  to 220,
        5  to 350,
        6  to 500,
        7  to 670,
        8  to 860,
        9  to 1_070,
        10 to 1_300,
        12 to 1_600,
        14 to 1_950,
        16 to 2_350,
        18 to 2_800,
        20 to 3_300,
        23 to 3_900,
        26 to 4_600,
        28 to 5_100,
        30 to 5_700,
        35 to 6_800,
        40 to 8_100,
        45 to 9_600,
        50 to 11_000
    )

    fun levelTitle(level: Int): String = when {
        level >= 50 -> "마스터"
        level >= 30 -> "명인"
        level >= 20 -> "전문가"
        level >= 10 -> "숙련 모험가"
        level >= 5  -> "견습 모험가"
        else        -> "초보 모험가"
    }

    fun determineJob(categorySpending: Map<String, Long>): String {
        val top = categorySpending.filterValues { it > 0L }.maxByOrNull { it.value }?.key
            ?: return "beginner"
        return when (top) {
            ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE         -> "cook"
            ExpenseCategoryClassifier.CATEGORY_LIVING_MART       -> "manager"
            ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE   -> "merchant"
            ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE   -> "artist"
            ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION -> "planner"
            ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL    -> "healer"
            else                                                  -> "beginner"
        }
    }

    fun jobTitle(job: String): String = when (job) {
        "cook"     -> "요리사"
        "manager"  -> "생활관리사"
        "merchant" -> "상인"
        "artist"   -> "예술가"
        "planner"  -> "관리자"
        "healer"   -> "힐러"
        else       -> "모험가"
    }

    fun calculateEarnedXP(
        amount: Long,
        thisMonthSpending: Long = 0L,
        monthlyBudget: Long = 1_500_000L
    ): Int {
        val baseXP = BASE_EXPENSE_XP
        val amountXP = ((amount.coerceAtLeast(0L) / 10_000L) * 10L)
            .coerceAtMost(100L)
            .toInt()
        val rawXP = baseXP + amountXP

        val budgetRatio = if (monthlyBudget > 0) {
            thisMonthSpending.toFloat() / monthlyBudget.toFloat()
        } else 1f

        return when {
            budgetRatio > 1.0f -> MINIMUM_XP
            budgetRatio > 0.8f -> (rawXP * 0.5f).toInt().coerceAtLeast(MINIMUM_XP)
            else               -> rawXP
        }
    }

    fun levelProgress(totalXP: Int): Float {
        val normalizedXP = totalXP.coerceAtLeast(0)
        val currentLevelThreshold = levelThresholds.last { (_, xp) -> normalizedXP >= xp }.second
        val nextLevelThreshold = levelThresholds
            .firstOrNull { (_, xp) -> normalizedXP < xp }
            ?.second
            ?: return 1f
        val range = (nextLevelThreshold - currentLevelThreshold).toFloat()
        return if (range <= 0f) 1f
        else ((normalizedXP - currentLevelThreshold).toFloat() / range).coerceIn(0f, 1f)
    }

    fun levelProgressXP(totalXP: Int): Pair<Int, Int> {
        val normalizedXP = totalXP.coerceAtLeast(0)
        val currentLevelThreshold = levelThresholds.last { (_, xp) -> normalizedXP >= xp }.second
        val nextLevelThreshold = levelThresholds
            .firstOrNull { (_, xp) -> normalizedXP < xp }
            ?.second
            ?: return (levelThresholds.last().second to levelThresholds.last().second)
        return (normalizedXP - currentLevelThreshold) to (nextLevelThreshold - currentLevelThreshold)
    }

    fun calculateLevel(totalXP: Int): Int {
        val normalizedXP = totalXP.coerceAtLeast(0)
        return levelThresholds.last { (_, threshold) -> normalizedXP >= threshold }.first
    }

    fun nextLevelThreshold(totalXP: Int): Int {
        val normalizedXP = totalXP.coerceAtLeast(0)
        return levelThresholds
            .firstOrNull { (_, threshold) -> normalizedXP < threshold }
            ?.second
            ?: levelThresholds.last().second
    }

    private const val BASE_EXPENSE_XP = 10
    private const val MINIMUM_XP = 5
}

class UserStatsStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _statsFlow = MutableStateFlow(loadStats())
    val statsFlow: StateFlow<UserStats> = _statsFlow.asStateFlow()

    private val _transactionsFlow = MutableStateFlow(loadTransactions())
    val transactionsFlow: StateFlow<List<Transaction>> = _transactionsFlow.asStateFlow()

    // ── 닉네임 ────────────────────────────────────────────────────────────────
    private val _nicknameFlow = MutableStateFlow(prefs.getString(KEY_NICKNAME, "") ?: "")
    val nicknameFlow: StateFlow<String> = _nicknameFlow.asStateFlow()

    fun saveNickname(name: String) {
        prefs.edit().putString(KEY_NICKNAME, name.trim()).apply()
        _nicknameFlow.value = name.trim()
    }

    // ── 로그인 후 서버 데이터 복원 ────────────────────────────────────────────
    /**
     * 로그인 직후 호출 (suspend). 완료될 때까지 기다린 뒤 화면 전환해야 함.
     * 서버에서 전체 거래 내역을 받아 로컬 SharedPreferences와 StateFlow를 갱신한다.
     */
    suspend fun restoreFromServer() {
        try {
            val tokenManager = TokenManager(appContext)
            val api = ApiClient.getTransactionApi(appContext, tokenManager)

            val resp = api.getAll()
            if (!resp.isSuccessful) {
                Log.w(TAG, "서버 복원 실패 (HTTP ${resp.code()})")
                return
            }

            val serverTxs = resp.body().orEmpty()
            if (serverTxs.isEmpty()) {
                Log.i(TAG, "서버 복원: 데이터 없음")
                return
            }

            // TransactionResponse → 로컬 Transaction 변환
            val restored = serverTxs.map { r ->
                val displayDate = runCatching {
                    LocalDateTime.parse(r.occurredAt)
                        .format(transactionDisplayFormatter)
                }.getOrDefault(r.occurredAt)

                Transaction(
                    store      = r.merchantName,
                    date       = displayDate,
                    amount     = r.amount,
                    category   = r.category,
                    status     = TransactionStatus.APPROVED_RECORDED,
                    source     = r.source,
                    occurredAt = r.occurredAt,
                    id         = r.occurredAt + "|" + r.amount + "|" + r.merchantName
                )
            }.sortedByDescending { it.occurredAt }

            // XP 재계산: 전체 거래 합산
            val totalXP = restored.sumOf { tx ->
                UserStatsCalculator.calculateEarnedXP(tx.amount).toLong()
            }.toInt()

            val now = YearMonth.now()
            val thisMonthTxs = restored.filter { tx ->
                runCatching {
                    YearMonth.from(LocalDateTime.parse(tx.occurredAt).toLocalDate()) == now
                }.getOrDefault(false)
            }
            val spending = thisMonthTxs.sumOf { it.amount }
            val categorySpending = ExpenseCategoryClassifier.categories.associateWith { cat ->
                thisMonthTxs.filter { it.category == cat }.sumOf { it.amount }
            }

            val newStats = UserStats(
                currentLevel      = UserStatsCalculator.calculateLevel(totalXP),
                currentXP         = totalXP,
                nextLevelXP       = UserStatsCalculator.nextLevelThreshold(totalXP),
                thisMonthSpending = spending,
                categorySpending  = categorySpending,
                transactions      = restored
            )

            saveStats(newStats, restored)
            Log.i(TAG, "서버 복원 완료: ${restored.size}건")

        } catch (e: Exception) {
            Log.w(TAG, "서버 복원 실패 (오프라인?): ${e.message}")
        }
    }

    private fun loadStats(): UserStats {
        val totalXP = if (prefs.contains(KEY_TOTAL_XP)) {
            prefs.getInt(KEY_TOTAL_XP, 0)
        } else {
            prefs.getInt(KEY_XP, 0)
        }

        val transactions = loadTransactions()
        val thisMonth = YearMonth.now()
        val thisMonthTransactions = transactions.filter { tx ->
            runCatching {
                YearMonth.from(LocalDateTime.parse(tx.occurredAt).toLocalDate())
            }.getOrNull() == thisMonth
        }
        val thisMonthSpending = thisMonthTransactions.sumOf { it.amount }
        val thisMonthCategorySpending = ExpenseCategoryClassifier.categories
            .associateWith { cat ->
                thisMonthTransactions.filter { it.category == cat }.sumOf { it.amount }
            }

        return UserStats(
            currentLevel = UserStatsCalculator.calculateLevel(totalXP),
            currentXP = totalXP,
            nextLevelXP = UserStatsCalculator.nextLevelThreshold(totalXP),
            thisMonthSpending = thisMonthSpending,
            categorySpending = thisMonthCategorySpending,
            transactions = transactions
        )
    }

    fun addExpense(
        amount: Long,
        category: String = ExpenseCategoryClassifier.CATEGORY_OTHER,
        merchantName: String = "",
        transactionDateTime: LocalDateTime? = null,
        status: String = TransactionStatus.APPROVED_RECORDED,
        source: String = TransactionSource.NOTIFICATION,
        transactionId: String? = null
    ): Boolean {
        if (!TransactionPersistencePolicy.shouldPersist(status)) return false

        val current = _statsFlow.value
        val normalizedCategory = if (category in ExpenseCategoryClassifier.categories) {
            category
        } else {
            ExpenseCategoryClassifier.CATEGORY_OTHER
        }
        val occurredAt = transactionDateTime ?: LocalDateTime.now()
        val displayDate = occurredAt.format(transactionDisplayFormatter)
        val normalizedMerchantName = merchantName.ifBlank { "Unknown" }
        val clientId = transactionId ?: buildTransactionId(
            source = source,
            occurredAt = occurredAt.toString(),
            amount = amount,
            merchantName = normalizedMerchantName,
            category = normalizedCategory
        )
        val transaction = Transaction(
            store = normalizedMerchantName,
            date = displayDate,
            amount = amount,
            category = normalizedCategory,
            status = status,
            source = source,
            occurredAt = occurredAt.toString(),
            id = clientId
        )
        val currentTransactions = _transactionsFlow.value
        val newTransactions = TransactionHistory.prependIfAbsent(
            current = currentTransactions,
            transaction = transaction
        )

        if (newTransactions === currentTransactions) return false

        val thisMonth = YearMonth.now()
        val thisMonthTxs = newTransactions.filter { tx ->
            runCatching {
                YearMonth.from(LocalDateTime.parse(tx.occurredAt).toLocalDate())
            }.getOrNull() == thisMonth
        }
        val newSpending = thisMonthTxs.sumOf { it.amount }
        val newCategorySpending = ExpenseCategoryClassifier.categories
            .associateWith { cat ->
                thisMonthTxs.filter { it.category == cat }.sumOf { it.amount }
            }

        val newTotalXP = current.currentXP + UserStatsCalculator.calculateEarnedXP(
            amount = amount,
            thisMonthSpending = newSpending,
            monthlyBudget = 1_500_000L
        )

        val newStats = UserStats(
            currentLevel = UserStatsCalculator.calculateLevel(newTotalXP),
            currentXP = newTotalXP,
            nextLevelXP = UserStatsCalculator.nextLevelThreshold(newTotalXP),
            thisMonthSpending = newSpending,
            categorySpending = newCategorySpending,
            transactions = newTransactions
        )

        saveStats(newStats, newTransactions)

        syncScope.launch {
            try {
                val tokenManager = TokenManager(appContext)
                val api = ApiClient.getTransactionApi(appContext, tokenManager)
                val req = SaveTransactionRequest(
                    amount              = amount,
                    merchantName        = normalizedMerchantName,
                    category            = normalizedCategory,
                    occurredAt          = occurredAt.toString(),
                    source              = source,
                    clientTransactionId = clientId
                )
                val resp = api.save(req)
                if (resp.isSuccessful) {
                    Log.d(TAG, "서버 동기화 완료: clientId=$clientId")
                } else {
                    Log.w(TAG, "서버 동기화 실패 (HTTP ${resp.code()}): clientId=$clientId")
                }
            } catch (e: Exception) {
                Log.w(TAG, "서버 동기화 예외 (오프라인?): ${e.message}")
            }
        }

        return true
    }

    fun updateTransactionCategory(transactionId: String, newCategory: String): Boolean {
        val normalizedCategory = if (newCategory in ExpenseCategoryClassifier.categories) {
            newCategory
        } else {
            ExpenseCategoryClassifier.CATEGORY_OTHER
        }

        val currentTransactions = _transactionsFlow.value
        val index = currentTransactions.indexOfFirst { it.id == transactionId }
        if (index == -1) return false

        val oldTx = currentTransactions[index]
        if (oldTx.category == normalizedCategory) return false

        val newTx = oldTx.copy(category = normalizedCategory)
        val newTransactions = currentTransactions.toMutableList()
        newTransactions[index] = newTx

        val current = _statsFlow.value
        val thisMonth = YearMonth.now()
        val thisMonthTxs = newTransactions.filter { tx ->
            runCatching {
                YearMonth.from(LocalDateTime.parse(tx.occurredAt).toLocalDate())
            }.getOrNull() == thisMonth
        }
        val newSpending = thisMonthTxs.sumOf { it.amount }
        val newCategorySpending = ExpenseCategoryClassifier.categories
            .associateWith { cat ->
                thisMonthTxs.filter { it.category == cat }.sumOf { it.amount }
            }

        val newStats = current.copy(
            thisMonthSpending = newSpending,
            categorySpending = newCategorySpending,
            transactions = newTransactions
        )

        saveStats(newStats, newTransactions)

        syncScope.launch {
            syncCategoryUpdateWithRetry(transactionId, normalizedCategory)
        }

        return true
    }

    fun clearForLogout() {
        prefs.edit().clear().apply()
        _transactionsFlow.value = emptyList()
        _statsFlow.value = UserStats()
        _nicknameFlow.value = ""
    }

    private fun loadTransactions(): List<Transaction> =
        TransactionJsonCodec.decode(prefs.getString(KEY_TRANSACTIONS, null).orEmpty())

    private fun saveStats(stats: UserStats, transactions: List<Transaction> = stats.transactions) {
        val editor = prefs.edit()
            .putInt(KEY_LEVEL, stats.currentLevel)
            .putInt(KEY_XP, stats.currentXP)
            .putInt(KEY_TOTAL_XP, stats.currentXP)
            .putInt(KEY_NEXT_XP, stats.nextLevelXP)
            .putLong(KEY_SPENDING, stats.thisMonthSpending)
            .putString(KEY_TRANSACTIONS, TransactionJsonCodec.encode(transactions))

        ExpenseCategoryClassifier.categories.forEach { category ->
            editor.putLong(categorySpendingKey(category), stats.categorySpending[category] ?: 0L)
        }

        editor.apply()

        _statsFlow.value = stats
        _transactionsFlow.value = transactions
    }

    private fun categorySpendingKey(category: String): String =
        "$KEY_CATEGORY_SPENDING_PREFIX$category"

    private fun buildTransactionId(
        source: String,
        occurredAt: String,
        amount: Long,
        merchantName: String,
        category: String
    ): String = "$source|$occurredAt|$amount|$merchantName|$category"

    private suspend fun syncCategoryUpdateWithRetry(transactionId: String, category: String) {
        val tokenManager = TokenManager(appContext)
        val api = ApiClient.getTransactionApi(appContext, tokenManager)
        val req = UpdateCategoryByClientIdRequest(
            clientTransactionId = transactionId,
            category = category
        )
        val retryDelaysMs = longArrayOf(1_000L, 3_000L, 5_000L)

        repeat(retryDelaysMs.size) { attempt ->
            try {
                val resp = api.updateCategoryByClientId(req)
                if (resp.isSuccessful) {
                    Log.d(TAG, "카테고리 서버 동기화 완료: clientId=$transactionId")
                    return
                }
                Log.w(
                    TAG,
                    "카테고리 서버 동기화 실패 attempt=${attempt + 1} " +
                        "(HTTP ${resp.code()}): clientId=$transactionId"
                )
            } catch (e: Exception) {
                Log.w(TAG, "카테고리 서버 동기화 예외 attempt=${attempt + 1}: ${e.message}")
            }

            if (attempt < retryDelaysMs.lastIndex) {
                delay(retryDelaysMs[attempt])
            }
        }

        Log.w(TAG, "카테고리 서버 동기화 재시도 보류 필요: clientId=$transactionId")
    }

    companion object {
        private const val TAG = "UserStatsStore"
        private const val PREFS_NAME = "user_stats_prefs"
        private const val KEY_LEVEL = "key_level"
        private const val KEY_XP = "key_xp"
        private const val KEY_TOTAL_XP = "key_total_xp"
        private const val KEY_NEXT_XP = "key_next_xp"
        private const val KEY_SPENDING = "key_spending"
        private const val KEY_CATEGORY_SPENDING_PREFIX = "key_category_spending_"
        private const val KEY_TRANSACTIONS = "key_transactions"
        private const val KEY_NICKNAME = "key_nickname"
        private val transactionDisplayFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")

        @Volatile
        private var instance: UserStatsStore? = null

        fun getInstance(context: Context): UserStatsStore {
            return instance ?: synchronized(this) {
                instance ?: UserStatsStore(context.applicationContext).also { instance = it }
            }
        }
    }
}

internal object TransactionPersistencePolicy {
    fun shouldPersist(status: String): Boolean =
        status == TransactionStatus.APPROVED_RECORDED
}

internal object TransactionHistory {
    private const val MAX_TRANSACTIONS = 200

    fun prependIfAbsent(
        current: List<Transaction>,
        transaction: Transaction
    ): List<Transaction> {
        if (current.any { it.id == transaction.id }) return current
        return (listOf(transaction) + current).take(MAX_TRANSACTIONS)
    }
}

internal object TransactionJsonCodec {
    private val gson = Gson()
    private val transactionListType = object : TypeToken<List<Transaction>>() {}.type

    fun encode(transactions: List<Transaction>): String =
        gson.toJson(transactions)

    fun decode(rawJson: String): List<Transaction> =
        if (rawJson.isBlank()) {
            emptyList()
        } else {
            runCatching {
                gson.fromJson<List<Transaction>>(rawJson, transactionListType).orEmpty()
            }.getOrDefault(emptyList())
        }
}
