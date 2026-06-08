package com.mentos.backend.service

import com.mentos.backend.dto.PrivacySettingsResponse
import com.mentos.backend.dto.UserStatsResponse
import com.mentos.backend.entity.Transaction
import com.mentos.backend.entity.User
import com.mentos.backend.entity.VisibilityScope
import com.mentos.backend.repository.TransactionRepository
import com.mentos.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class UserStatsService(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository
) {
    @Transactional(readOnly = true)
    fun getStats(userId: Long): UserStatsResponse {
        val month = currentMonth()
        val user = findUser(userId)
        return user.toStatsResponse(categorySpending(userId, month), month)
    }

    @Transactional
    fun updateBudget(userId: Long, monthlyBudget: Long): UserStatsResponse {
        require(monthlyBudget > 0L) { "월 예산은 0원보다 커야 합니다." }
        val month = currentMonth()
        val user = findUser(userId)
        user.monthlyBudget = monthlyBudget
        val categorySpending = categorySpending(userId, month)
        refreshCurrentMonthJob(user, categorySpending, month)
        return userRepository.save(user).toStatsResponse(categorySpending, month)
    }

    @Transactional(readOnly = true)
    fun getPrivacy(userId: Long): PrivacySettingsResponse {
        val user = findUser(userId)
        return user.toPrivacyResponse()
    }

    @Transactional
    fun updatePrivacy(
        userId: Long,
        spendingVisibility: String,
        characterVisibility: String
    ): PrivacySettingsResponse {
        val user = findUser(userId)
        user.spendingVisibility = parseVisibility(spendingVisibility, allowPrivate = true)
        user.characterVisibility = parseVisibility(characterVisibility, allowPrivate = true)
        return userRepository.save(user).toPrivacyResponse()
    }

    @Transactional
    fun recalculate(userId: Long): UserStatsResponse {
        val user = findUser(userId)
        val transactions = transactionRepository.findByUserIdOrderByOccurredAtAsc(userId)

        var totalXp = 0
        val monthlySpending = mutableMapOf<YearMonth, Long>()

        transactions.forEach { tx ->
            val ym = YearMonth.from(tx.occurredAt)
            val spendingAfterTransaction = (monthlySpending[ym] ?: 0L) + tx.amount
            monthlySpending[ym] = spendingAfterTransaction
            totalXp += calculateEarnedXp(tx.amount, spendingAfterTransaction, user.monthlyBudget)
        }

        applyLevelState(user, totalXp)
        val month = currentMonth()
        val categorySpending = categorySpending(userId, month)
        refreshCurrentMonthJob(user, categorySpending, month)
        return userRepository.save(user).toStatsResponse(categorySpending, month)
    }

    @Transactional
    fun applyTransaction(userId: Long, transaction: Transaction) {
        val user = findUser(userId)
        val transactionMonth = YearMonth.from(transaction.occurredAt)
        val monthlySpending = categorySpending(userId, transactionMonth).values.sum()
        val earnedXp = calculateEarnedXp(transaction.amount, monthlySpending, user.monthlyBudget)

        applyLevelState(user, user.totalXp + earnedXp)

        val month = currentMonth()
        refreshCurrentMonthJob(user, categorySpending(userId, month), month)
        userRepository.save(user)
    }

    private fun findUser(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

    private fun parseVisibility(value: String, allowPrivate: Boolean): VisibilityScope {
        val normalized = value.trim().uppercase()
        val parsed = runCatching { VisibilityScope.valueOf(normalized) }
            .getOrElse { throw IllegalArgumentException("지원하지 않는 공개 범위입니다: $value") }
        if (!allowPrivate && parsed == VisibilityScope.PRIVATE) {
            throw IllegalArgumentException("캐릭터 공개 범위는 FRIENDS만 지원합니다.")
        }
        return parsed
    }

    private fun User.toPrivacyResponse(): PrivacySettingsResponse =
        PrivacySettingsResponse(
            spendingVisibility = spendingVisibility.name,
            characterVisibility = characterVisibility.name
        )

    private fun calculateEarnedXp(
        amount: Long,
        thisMonthSpending: Long,
        monthlyBudget: Long
    ): Int {
        val baseXp = 10
        val amountXp = ((amount.coerceAtLeast(0L) / 10_000L) * 10L)
            .coerceAtMost(100L)
            .toInt()
        val rawXp = baseXp + amountXp
        val budgetRatio = if (monthlyBudget > 0L) {
            thisMonthSpending.toDouble() / monthlyBudget.toDouble()
        } else {
            1.0
        }

        return when {
            budgetRatio > 1.0 -> 5
            budgetRatio > 0.8 -> (rawXp * 0.5).toInt().coerceAtLeast(5)
            else -> rawXp
        }
    }

    private fun applyLevelState(user: User, totalXp: Int) {
        val normalizedXp = totalXp.coerceAtLeast(0)
        val currentThreshold = levelThresholds.last { (_, threshold) -> normalizedXp >= threshold }
        val nextThreshold = levelThresholds.firstOrNull { (_, threshold) -> normalizedXp < threshold }

        user.totalXp = normalizedXp
        user.level = currentThreshold.first
        user.currentXp = normalizedXp - currentThreshold.second
        user.nextLevelXp = nextThreshold
            ?.let { (_, threshold) -> threshold - currentThreshold.second }
            ?: currentThreshold.second
    }

    private fun refreshCurrentMonthJob(
        user: User,
        categorySpending: Map<String, Long>,
        month: YearMonth
    ) {
        val totalSpending = categorySpending.values.sum()
        val job = determineJob(categorySpending)
        user.job = job
        user.jobReason = jobReason(job, categorySpending, totalSpending)
        user.jobMonth = month.toString()
    }

    private fun categorySpending(userId: Long, month: YearMonth): Map<String, Long> {
        val start = month.atDay(1).atStartOfDay()
        val end = month.plusMonths(1).atDay(1).atStartOfDay()
        return transactionRepository
            .sumAmountByCategoryInRange(userId, start, end)
            .associate { row -> (row[0] as String) to (row[1] as Number).toLong() }
    }

    private fun currentMonth(): YearMonth = YearMonth.now()

    private fun determineJob(categorySpending: Map<String, Long>): String {
        val topCategory = categorySpending.filterValues { it > 0L }.maxByOrNull { it.value }?.key
            ?: return "beginner"
        return when (topCategory) {
            "식비/카페" -> "cook"
            "생활/마트" -> "manager"
            "쇼핑/온라인" -> "merchant"
            "문화/여가" -> "artist"
            "고정비/구독" -> "planner"
            "건강/의료" -> "healer"
            else -> "beginner"
        }
    }

    private fun jobTitle(job: String): String = when (job) {
        "cook" -> "요리사"
        "manager" -> "생활관리사"
        "merchant" -> "상인"
        "artist" -> "예술가"
        "planner" -> "관리자"
        "healer" -> "힐러"
        else -> "모험가"
    }

    private fun jobReason(
        job: String,
        categorySpending: Map<String, Long>,
        thisMonthSpending: Long
    ): String {
        if (thisMonthSpending <= 0L) return "이번 달 지출 내역이 없어 모험가로 시작했어요."
        val topEntry = categorySpending.filterValues { it > 0L }.maxByOrNull { it.value }
            ?: return "지출 내역을 분석하는 중이에요."
        val ratio = (topEntry.value.toDouble() / thisMonthSpending.toDouble() * 100).toInt()
        return when (job) {
            "cook" -> "이번 달 식비/카페 지출이 전체의 ${ratio}%를 차지해 요리사가 되었어요."
            "manager" -> "이번 달 생활/마트 지출이 전체의 ${ratio}%를 차지해 생활관리사가 되었어요."
            "merchant" -> "이번 달 쇼핑/온라인 지출이 전체의 ${ratio}%를 차지해 상인이 되었어요."
            "artist" -> "이번 달 문화/여가 지출이 전체의 ${ratio}%를 차지해 예술가가 되었어요."
            "planner" -> "이번 달 고정비/구독 지출이 전체의 ${ratio}%를 차지해 관리자가 되었어요."
            "healer" -> "이번 달 건강/의료 지출이 전체의 ${ratio}%를 차지해 힐러가 되었어요."
            else -> "이번 달 지출이 여러 카테고리에 분포되어 모험가로 지내고 있어요."
        }
    }

    private fun User.toStatsResponse(
        categorySpending: Map<String, Long>,
        month: YearMonth
    ): UserStatsResponse = UserStatsResponse(
        totalXp = totalXp,
        level = level,
        currentXp = currentXp,
        nextLevelXp = nextLevelXp,
        monthlyBudget = monthlyBudget,
        job = job,
        jobTitle = jobTitle(job),
        jobReason = jobReason,
        jobMonth = jobMonth.ifBlank { month.toString() },
        thisMonthSpending = categorySpending.values.sum(),
        categorySpending = categorySpending
    )

    private val levelThresholds = listOf(
        1 to 0,
        2 to 50,
        3 to 120,
        4 to 220,
        5 to 350,
        6 to 500,
        7 to 670,
        8 to 860,
        9 to 1_070,
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
}
