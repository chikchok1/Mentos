package com.example.personalfinance.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        1 to 0,
        2 to 100,
        3 to 300,
        4 to 700,
        5 to 1200
    )

    fun calculateEarnedXP(amount: Long): Int {
        val amountXP = ((amount.coerceAtLeast(0L) / 10_000L) * 10L)
            .coerceAtMost(100L)
            .toInt()
        return BASE_EXPENSE_XP + amountXP
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
}

class UserStatsStore private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _statsFlow = MutableStateFlow(loadStats())
    val statsFlow: StateFlow<UserStats> = _statsFlow.asStateFlow()

    private val _transactionsFlow = MutableStateFlow(loadTransactions())
    val transactionsFlow: StateFlow<List<Transaction>> = _transactionsFlow.asStateFlow()

    private fun loadStats(): UserStats {
        val totalXP = if (prefs.contains(KEY_TOTAL_XP)) {
            prefs.getInt(KEY_TOTAL_XP, 0)
        } else {
            prefs.getInt(KEY_XP, 0)
        }

        return UserStats(
            currentLevel = UserStatsCalculator.calculateLevel(totalXP),
            currentXP = totalXP,
            nextLevelXP = UserStatsCalculator.nextLevelThreshold(totalXP),
            thisMonthSpending = prefs.getLong(KEY_SPENDING, 0L),
            categorySpending = loadCategorySpending(),
            transactions = loadTransactions()
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
        if (!TransactionPersistencePolicy.shouldPersist(status)) {
            return false
        }

        val current = _statsFlow.value
        val normalizedCategory = if (category in ExpenseCategoryClassifier.categories) {
            category
        } else {
            ExpenseCategoryClassifier.CATEGORY_OTHER
        }
        val occurredAt = transactionDateTime ?: LocalDateTime.now()
        val displayDate = occurredAt.format(transactionDisplayFormatter)
        val normalizedMerchantName = merchantName.ifBlank { "Unknown" }
        val transaction = Transaction(
            store = normalizedMerchantName,
            date = displayDate,
            amount = amount,
            category = normalizedCategory,
            status = status,
            source = source,
            occurredAt = occurredAt.toString(),
            id = transactionId ?: buildTransactionId(
                source = source,
                occurredAt = occurredAt.toString(),
                amount = amount,
                merchantName = normalizedMerchantName,
                category = normalizedCategory
            )
        )
        val currentTransactions = _transactionsFlow.value
        val newTransactions = TransactionHistory.prependIfAbsent(
            current = currentTransactions,
            transaction = transaction
        )

        if (newTransactions === currentTransactions) {
            return false
        }

        val newSpending = current.thisMonthSpending + amount
        val newTotalXP = current.currentXP + UserStatsCalculator.calculateEarnedXP(amount)
        val newCategorySpending = current.categorySpending.toMutableMap().apply {
            this[normalizedCategory] = (this[normalizedCategory] ?: 0L) + amount
        }

        val newStats = UserStats(
            currentLevel = UserStatsCalculator.calculateLevel(newTotalXP),
            currentXP = newTotalXP,
            nextLevelXP = UserStatsCalculator.nextLevelThreshold(newTotalXP),
            thisMonthSpending = newSpending,
            categorySpending = newCategorySpending,
            transactions = newTransactions
        )

        saveStats(newStats, newTransactions)
        return true
    }

    fun reclassifyOtherTransactions(): Int {
        val current = _statsFlow.value
        val currentTransactions = _transactionsFlow.value
        val result = StoredTransactionReclassifier.reclassifyOtherApprovedTransactions(currentTransactions)

        if (result.reclassifiedCount == 0) {
            return 0
        }

        val newStats = current.copy(
            categorySpending = StoredTransactionReclassifier.calculateCategorySpending(result.transactions),
            transactions = result.transactions
        )

        saveStats(newStats, result.transactions)
        return result.reclassifiedCount
    }

    private fun loadCategorySpending(): Map<String, Long> =
        ExpenseCategoryClassifier.categories.associateWith { category ->
            prefs.getLong(categorySpendingKey(category), 0L)
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

    companion object {
        private const val PREFS_NAME = "user_stats_prefs"
        private const val KEY_LEVEL = "key_level"
        private const val KEY_XP = "key_xp"
        private const val KEY_TOTAL_XP = "key_total_xp"
        private const val KEY_NEXT_XP = "key_next_xp"
        private const val KEY_SPENDING = "key_spending"
        private const val KEY_CATEGORY_SPENDING_PREFIX = "key_category_spending_"
        private const val KEY_TRANSACTIONS = "key_transactions"
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

internal object StoredTransactionReclassifier {
    data class Result(
        val transactions: List<Transaction>,
        val reclassifiedCount: Int
    )

    fun reclassifyOtherApprovedTransactions(transactions: List<Transaction>): Result {
        var changedCount = 0
        val reclassified = transactions.map { transaction ->
            if (!transaction.isReclassificationCandidate()) {
                return@map transaction
            }

            val newCategory = ExpenseCategoryClassifier.classify(
                merchantName = transaction.store,
                rawText = ""
            )

            if (newCategory == ExpenseCategoryClassifier.CATEGORY_OTHER) {
                transaction
            } else {
                changedCount += 1
                transaction.copy(category = newCategory)
            }
        }

        return Result(
            transactions = reclassified,
            reclassifiedCount = changedCount
        )
    }

    fun calculateCategorySpending(transactions: List<Transaction>): Map<String, Long> {
        val spending = ExpenseCategoryClassifier.categories.associateWith { 0L }.toMutableMap()
        transactions
            .filter { it.status == TransactionStatus.APPROVED_RECORDED }
            .forEach { transaction ->
                val category = if (transaction.category in ExpenseCategoryClassifier.categories) {
                    transaction.category
                } else {
                    ExpenseCategoryClassifier.CATEGORY_OTHER
                }
                spending[category] = (spending[category] ?: 0L) + transaction.amount
            }
        return spending
    }

    private fun Transaction.isReclassificationCandidate(): Boolean =
        status == TransactionStatus.APPROVED_RECORDED &&
            category == ExpenseCategoryClassifier.CATEGORY_OTHER
}

internal object TransactionHistory {
    private const val MAX_TRANSACTIONS = 200

    fun prependIfAbsent(
        current: List<Transaction>,
        transaction: Transaction
    ): List<Transaction> {
        if (current.any { it.id == transaction.id }) {
            return current
        }

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
