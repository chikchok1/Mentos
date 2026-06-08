package com.example.personalfinance.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.personalfinance.network.ApiClient
import com.example.personalfinance.network.PrivacySettingsRequest
import com.example.personalfinance.network.PrivacySettingsResponse
import com.example.personalfinance.network.SaveTransactionRequest
import com.example.personalfinance.network.UpdateCategoryByClientIdRequest
import com.example.personalfinance.network.UpdateBudgetRequest
import com.example.personalfinance.network.UpdateUserProfileRequest
import com.example.personalfinance.network.UserProfileResponse
import com.example.personalfinance.network.UserStatsResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class UserStats(
    val currentLevel: Int = 1,
    val currentXP: Int = 0,
    val nextLevelXP: Int = 100,
    val monthlyBudget: Long = 1_500_000L,
    val job: String = "beginner",
    val jobReason: String = "이번 달 지출 내역이 없어 모험가로 시작했어요.",
    val jobMonth: String = java.time.YearMonth.now().toString(),
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

data class PrivacySettings(
    val spendingVisibility: String = "PRIVATE",
    val characterVisibility: String = "FRIENDS"
)

data class UserProfile(
    val nickname: String = "",
    val friendCode: String = "",
    val displayName: String = ""
)

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
            ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE          -> "cook"
            ExpenseCategoryClassifier.CATEGORY_LIVING_MART        -> "manager"
            ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE    -> "merchant"
            ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE    -> "artist"
            ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION -> "planner"
            ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL     -> "healer"
            else                                                   -> "beginner"
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

    fun jobReason(
        job: String,
        categorySpending: Map<String, Long>,
        thisMonthSpending: Long
    ): String {
        if (thisMonthSpending == 0L) return "아직 이번 달 지출 내역이 없어요."
        val topEntry = categorySpending.filterValues { it > 0L }.maxByOrNull { it.value }
            ?: return "지출 내역을 분석 중이에요."
        val ratio = (topEntry.value.toFloat() / thisMonthSpending * 100).toInt()
        return when (job) {
            "cook"     -> "이번 달 식비·카페 지출이 전체의 ${ratio}%를 차지해 요리사가 되었어요."
            "manager"  -> "이번 달 생활·마트 지출이 전체의 ${ratio}%를 차지해 생활관리사가 되었어요."
            "merchant" -> "이번 달 쇼핑·온라인 지출이 전체의 ${ratio}%를 차지해 상인이 되었어요."
            "artist"   -> "이번 달 문화·여가 지출이 전체의 ${ratio}%를 차지해 예술가가 되었어요."
            "planner"  -> "이번 달 구독·고정 지출이 전체의 ${ratio}%를 차지해 관리자가 되었어요."
            "healer"   -> "이번 달 건강·의료 지출이 전체의 ${ratio}%를 차지해 힐러가 되었어요."
            else       -> "아직 뚜렷한 지출 패턴이 없어 모험가로 지내고 있어요."
        }
    }

    fun calculateEarnedXP(
        amount: Long,
        thisMonthSpending: Long = 0L,
        monthlyBudget: Long = 1_500_000L,
        category: String = ExpenseCategoryClassifier.CATEGORY_OTHER
    ): Int {
        val baseXP = BASE_EXPENSE_XP
        val amountXP = ((amount.coerceAtLeast(0L) / 10_000L) * 10L)
            .coerceAtMost(100L)
            .toInt()
        val rawXP = ((baseXP + amountXP) * categoryXpWeight(category)).toInt().coerceAtLeast(1)

        val budgetRatio = if (monthlyBudget > 0) {
            thisMonthSpending.toFloat() / monthlyBudget.toFloat()
        } else 1f

        return when {
            budgetRatio > 1.0f -> MINIMUM_XP
            budgetRatio > 0.8f -> (rawXP * 0.5f).toInt().coerceAtLeast(MINIMUM_XP)
            else               -> rawXP
        }
    }

    private fun categoryXpWeight(category: String): Float {
        val normalized = category
            .trim()
            .lowercase()
            .replace(Regex("""[\s_\-/·]"""), "")

        return when {
            category == ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE -> 1.0f
            category == ExpenseCategoryClassifier.CATEGORY_LIVING_MART -> 1.0f
            category == ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE -> 0.8f
            category == ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE -> 0.9f
            category == ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION -> 1.0f
            category == ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL -> 1.2f
            normalized.contains("study") ||
                normalized.contains("education") ||
                normalized.contains("edu") ||
                normalized.contains("교육") ||
                normalized.contains("학습") -> 1.2f
            normalized.contains("health") ||
                normalized.contains("medical") ||
                normalized.contains("병원") ||
                normalized.contains("의료") ||
                normalized.contains("건강") -> 1.2f
            normalized.contains("shopping") ||
                normalized.contains("shop") ||
                normalized.contains("online") ||
                normalized.contains("쇼핑") ||
                normalized.contains("온라인") -> 0.8f
            normalized.contains("entertainment") ||
                normalized.contains("culture") ||
                normalized.contains("leisure") ||
                normalized.contains("문화") ||
                normalized.contains("여가") -> 0.9f
            normalized.contains("cafe") ||
                normalized.contains("coffee") ||
                normalized.contains("카페") ||
                normalized.contains("커피") -> 0.9f
            else -> 1.0f
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

    // 직업 변경 이벤트 — 변경된 직업 한글 이름을 방출
    private val _jobChangedFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val jobChangedFlow: SharedFlow<String> = _jobChangedFlow.asSharedFlow()

    // ── 닉네임 ────────────────────────────────────────────────────────────────
    private val _nicknameFlow = MutableStateFlow(prefs.getString(KEY_NICKNAME, "") ?: "")
    val nicknameFlow: StateFlow<String> = _nicknameFlow.asStateFlow()

    private val _friendCodeFlow = MutableStateFlow(prefs.getString(KEY_FRIEND_CODE, "") ?: "")
    val friendCodeFlow: StateFlow<String> = _friendCodeFlow.asStateFlow()

    private val _displayNameFlow = MutableStateFlow(prefs.getString(KEY_DISPLAY_NAME, "") ?: "")
    val displayNameFlow: StateFlow<String> = _displayNameFlow.asStateFlow()

    private val _privacyFlow = MutableStateFlow(loadPrivacy())
    val privacyFlow: StateFlow<PrivacySettings> = _privacyFlow.asStateFlow()

    fun saveNickname(name: String) {
        prefs.edit().putString(KEY_NICKNAME, name.trim()).apply()
        _nicknameFlow.value = name.trim()
    }

    suspend fun refreshProfile(): Boolean {
        return try {
            val tokenManager = TokenManager(appContext)
            val api = ApiClient.getUserApi(appContext, tokenManager)
            val resp = api.getProfile()
            if (resp.isSuccessful) {
                resp.body()?.let { applyProfile(it) }
                true
            } else {
                Log.w(TAG, "프로필 조회 실패 (HTTP ${resp.code()})")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "프로필 조회 예외: ${e.message}")
            false
        }
    }

    suspend fun updateNicknameWithServer(nickname: String): Boolean {
        return try {
            val tokenManager = TokenManager(appContext)
            val api = ApiClient.getUserApi(appContext, tokenManager)
            val resp = api.updateProfile(UpdateUserProfileRequest(nickname.trim()))
            if (resp.isSuccessful) {
                resp.body()?.let { applyProfile(it) }
                true
            } else {
                Log.w(TAG, "닉네임 저장 실패 (HTTP ${resp.code()})")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "닉네임 저장 예외: ${e.message}")
            false
        }
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

            val totalXP = restored.sumOf { tx ->
                UserStatsCalculator.calculateEarnedXP(
                    amount = tx.amount,
                    category = tx.category
                ).toLong()
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
                monthlyBudget     = _statsFlow.value.monthlyBudget,
                job               = _statsFlow.value.job,
                jobReason         = _statsFlow.value.jobReason,
                jobMonth          = _statsFlow.value.jobMonth,
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

    suspend fun refreshServerStats(): Boolean {
        return try {
            val tokenManager = TokenManager(appContext)
            val api = ApiClient.getUserApi(appContext, tokenManager)
            val resp = api.getStats()
            if (!resp.isSuccessful) {
                Log.w(TAG, "서버 통계 조회 실패 (HTTP ${resp.code()})")
                false
            } else {
                resp.body()?.let { applyServerStats(it) }
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "서버 통계 조회 예외 (로컬 fallback 유지): ${e.message}")
            false
        }
    }

    suspend fun refreshPrivacy(): Boolean {
        return try {
            val tokenManager = TokenManager(appContext)
            val api = ApiClient.getUserApi(appContext, tokenManager)
            val resp = api.getPrivacy()
            if (resp.isSuccessful) {
                resp.body()?.let { applyPrivacy(it) }
                true
            } else {
                Log.w(TAG, "공개 설정 조회 실패 (HTTP ${resp.code()})")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "공개 설정 조회 예외: ${e.message}")
            false
        }
    }

    suspend fun updateSpendingVisibility(visibleToFriends: Boolean): Boolean {
        val nextSpending = if (visibleToFriends) "FRIENDS" else "PRIVATE"
        return updatePrivacy(
            PrivacySettings(
                spendingVisibility = nextSpending,
                characterVisibility = _privacyFlow.value.characterVisibility
            )
        )
    }

    suspend fun updateCharacterVisibility(visibleToFriends: Boolean): Boolean {
        val nextCharacter = if (visibleToFriends) "FRIENDS" else "PRIVATE"
        return updatePrivacy(
            PrivacySettings(
                spendingVisibility = _privacyFlow.value.spendingVisibility,
                characterVisibility = nextCharacter
            )
        )
    }

    private suspend fun updatePrivacy(settings: PrivacySettings): Boolean {
        return try {
            val tokenManager = TokenManager(appContext)
            val api = ApiClient.getUserApi(appContext, tokenManager)
            val resp = api.updatePrivacy(
                PrivacySettingsRequest(
                    spendingVisibility = settings.spendingVisibility,
                    characterVisibility = settings.characterVisibility
                )
            )
            if (resp.isSuccessful) {
                resp.body()?.let { applyPrivacy(it) }
                true
            } else {
                Log.w(TAG, "공개 설정 변경 실패 (HTTP ${resp.code()})")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "공개 설정 변경 예외: ${e.message}")
            false
        }
    }

    suspend fun updateMonthlyBudget(monthlyBudget: Long): Boolean {
        if (monthlyBudget <= 0L) return false
        return try {
            val tokenManager = TokenManager(appContext)
            val api = ApiClient.getUserApi(appContext, tokenManager)
            val resp = api.updateBudget(UpdateBudgetRequest(monthlyBudget))
            if (resp.isSuccessful) {
                resp.body()?.let { applyServerStats(it) }
                true
            } else {
                Log.w(TAG, "서버 예산 수정 실패 (HTTP ${resp.code()})")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "서버 예산 수정 예외: ${e.message}")
            false
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
            monthlyBudget = prefs.getLong(KEY_MONTHLY_BUDGET, 1_500_000L),
            job = prefs.getString(KEY_JOB, "beginner") ?: "beginner",
            jobReason = prefs.getString(
                KEY_JOB_REASON,
                "이번 달 지출 내역이 없어 모험가로 시작했어요."
            ) ?: "이번 달 지출 내역이 없어 모험가로 시작했어요.",
            jobMonth = prefs.getString(KEY_JOB_MONTH, YearMonth.now().toString())
                ?: YearMonth.now().toString(),
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
            category = normalizedCategory,
            thisMonthSpending = newSpending,
            monthlyBudget = current.monthlyBudget
        )

        val newStats = UserStats(
            currentLevel = UserStatsCalculator.calculateLevel(newTotalXP),
            currentXP = newTotalXP,
            nextLevelXP = UserStatsCalculator.nextLevelThreshold(newTotalXP),
            monthlyBudget = current.monthlyBudget,
            job = UserStatsCalculator.determineJob(newCategorySpending),
            jobReason = UserStatsCalculator.jobReason(
                UserStatsCalculator.determineJob(newCategorySpending),
                newCategorySpending,
                newSpending
            ),
            jobMonth = YearMonth.now().toString(),
            thisMonthSpending = newSpending,
            categorySpending = newCategorySpending,
            transactions = newTransactions
        )

        // 직업 변경 감지 — saveStats() 전에 비교
        val oldJob = UserStatsCalculator.determineJob(current.categorySpending)
        val newJob = UserStatsCalculator.determineJob(newCategorySpending)
        if (oldJob != newJob) {
            _jobChangedFlow.tryEmit(UserStatsCalculator.jobTitle(newJob))
            Log.i(TAG, "직업 변경: $oldJob → $newJob")
        }

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

        // 카테고리 수정 시에도 직업 변경 감지
        val oldJob = UserStatsCalculator.determineJob(current.categorySpending)
        val newJob = UserStatsCalculator.determineJob(newCategorySpending)
        if (oldJob != newJob) {
            _jobChangedFlow.tryEmit(UserStatsCalculator.jobTitle(newJob))
            Log.i(TAG, "직업 변경 (카테고리 수정): $oldJob → $newJob")
        }

        val newStats = current.copy(
            job = newJob,
            jobReason = UserStatsCalculator.jobReason(newJob, newCategorySpending, newSpending),
            jobMonth = YearMonth.now().toString(),
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
        _friendCodeFlow.value = ""
        _displayNameFlow.value = ""
        _privacyFlow.value = PrivacySettings()
    }

    private fun loadTransactions(): List<Transaction> =
        TransactionJsonCodec.decode(prefs.getString(KEY_TRANSACTIONS, null).orEmpty())

    private fun saveStats(stats: UserStats, transactions: List<Transaction> = stats.transactions) {
        val editor = prefs.edit()
            .putInt(KEY_LEVEL, stats.currentLevel)
            .putInt(KEY_XP, stats.currentXP)
            .putInt(KEY_TOTAL_XP, stats.currentXP)
            .putInt(KEY_NEXT_XP, stats.nextLevelXP)
            .putLong(KEY_MONTHLY_BUDGET, stats.monthlyBudget)
            .putString(KEY_JOB, stats.job)
            .putString(KEY_JOB_REASON, stats.jobReason)
            .putString(KEY_JOB_MONTH, stats.jobMonth)
            .putLong(KEY_SPENDING, stats.thisMonthSpending)
            .putString(KEY_TRANSACTIONS, TransactionJsonCodec.encode(transactions))

        ExpenseCategoryClassifier.categories.forEach { category ->
            editor.putLong(categorySpendingKey(category), stats.categorySpending[category] ?: 0L)
        }

        editor.apply()

        _statsFlow.value = stats
        _transactionsFlow.value = transactions
    }

    private fun applyServerStats(serverStats: UserStatsResponse) {
        val normalizedCategories = ExpenseCategoryClassifier.categories.associateWith { category ->
            serverStats.categorySpending[category] ?: 0L
        }
        val current = _statsFlow.value
        val totalXp = serverStats.totalXp.coerceAtLeast(0)
        val newStats = current.copy(
            currentLevel = serverStats.level,
            currentXP = totalXp,
            nextLevelXP = UserStatsCalculator.nextLevelThreshold(totalXp),
            monthlyBudget = serverStats.monthlyBudget,
            job = serverStats.job,
            jobReason = serverStats.jobReason,
            jobMonth = serverStats.jobMonth,
            thisMonthSpending = serverStats.thisMonthSpending,
            categorySpending = normalizedCategories
        )

        saveStats(newStats, current.transactions)
        applyProfile(
            UserProfileResponse(
                id = null,
                email = null,
                nickname = serverStats.nickname,
                friendCode = serverStats.friendCode,
                displayName = serverStats.displayName
            )
        )
    }

    private fun applyProfile(response: UserProfileResponse) {
        val nickname = response.nickname?.trim().orEmpty()
        val friendCode = response.friendCode?.trim().orEmpty()
        val displayName = response.displayName?.trim().orEmpty()
            .ifBlank { buildDisplayName(nickname, friendCode) }

        prefs.edit()
            .putString(KEY_NICKNAME, nickname)
            .putString(KEY_FRIEND_CODE, friendCode)
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()

        _nicknameFlow.value = nickname
        _friendCodeFlow.value = friendCode
        _displayNameFlow.value = displayName
    }

    private fun buildDisplayName(nickname: String, friendCode: String): String =
        when {
            nickname.isNotBlank() && friendCode.isNotBlank() -> "$nickname#$friendCode"
            nickname.isNotBlank() -> nickname
            else -> ""
        }

    private fun loadPrivacy(): PrivacySettings =
        PrivacySettings(
            spendingVisibility = prefs.getString(KEY_SPENDING_VISIBILITY, "PRIVATE") ?: "PRIVATE",
            characterVisibility = prefs.getString(KEY_CHARACTER_VISIBILITY, "FRIENDS") ?: "FRIENDS"
        )

    private fun applyPrivacy(response: PrivacySettingsResponse) {
        val settings = PrivacySettings(
            spendingVisibility = response.spendingVisibility,
            characterVisibility = response.characterVisibility
        )
        prefs.edit()
            .putString(KEY_SPENDING_VISIBILITY, settings.spendingVisibility)
            .putString(KEY_CHARACTER_VISIBILITY, settings.characterVisibility)
            .apply()
        _privacyFlow.value = settings
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
        private const val KEY_MONTHLY_BUDGET = "key_monthly_budget"
        private const val KEY_JOB = "key_job"
        private const val KEY_JOB_REASON = "key_job_reason"
        private const val KEY_JOB_MONTH = "key_job_month"
        private const val KEY_SPENDING = "key_spending"
        private const val KEY_CATEGORY_SPENDING_PREFIX = "key_category_spending_"
        private const val KEY_TRANSACTIONS = "key_transactions"
        private const val KEY_NICKNAME = "key_nickname"
        private const val KEY_FRIEND_CODE = "key_friend_code"
        private const val KEY_DISPLAY_NAME = "key_display_name"
        private const val KEY_SPENDING_VISIBILITY = "key_spending_visibility"
        private const val KEY_CHARACTER_VISIBILITY = "key_character_visibility"
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
