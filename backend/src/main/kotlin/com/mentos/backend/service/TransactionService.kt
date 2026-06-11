package com.mentos.backend.service

import com.mentos.backend.dto.*
import com.mentos.backend.entity.Transaction
import com.mentos.backend.repository.TransactionRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val userStatsService: UserStatsService
) {

    // ── 저장 ────────────────────────────────────────────────────────────────

    @Transactional
    fun save(userId: Long, req: SaveTransactionRequest): TransactionResponse {
        val clientTransactionId = req.clientTransactionId
        if (clientTransactionId != null &&
            transactionRepository.existsByUserIdAndClientTransactionId(userId, clientTransactionId)
        ) {
            val existing = transactionRepository
                .findByUserIdAndClientTransactionId(userId, clientTransactionId)
                .orElse(null)
            if (existing != null) return existing.toResponse()
        }

        val entity = Transaction(
            userId              = userId,
            amount              = req.amount,
            merchantName        = req.merchantName,
            category            = req.category,
            occurredAt          = LocalDateTime.parse(req.occurredAt),
            source              = req.source,
            clientTransactionId = clientTransactionId
        )
        return try {
            val saved = transactionRepository.save(entity)
            userStatsService.applyTransaction(userId, saved)
            saved.toResponse()
        } catch (e: DataIntegrityViolationException) {
            if (clientTransactionId == null) throw e
            transactionRepository
                .findByUserIdAndClientTransactionId(userId, clientTransactionId)
                .orElseThrow { e }
                .toResponse()
        }
    }

    // ── 조회 ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    fun getAll(userId: Long): List<TransactionResponse> =
        transactionRepository.findByUserIdOrderByOccurredAtDesc(userId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getByMonth(userId: Long, year: Int, month: Int): List<TransactionResponse> {
        val (start, end) = monthRange(year, month)
        return transactionRepository
            .findByUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(userId, start, end)
            .map { it.toResponse() }
    }

    // ── 월별 통계 ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    fun getMonthlyStats(userId: Long, year: Int, month: Int): MonthlyStatsResponse {
        val (start, end) = monthRange(year, month)

        val categoryBreakdown = transactionRepository
            .sumAmountByCategoryInRange(userId, start, end)
            .associate { row ->
                @Suppress("UNCHECKED_CAST")
                (row[0] as String) to (row[1] as Number).toLong()
            }

        val dailyBreakdown = transactionRepository
            .sumAmountByDayInRange(userId, start, end)
            .associate { row ->
                (row[0] as Number).toInt() to (row[1] as Number).toLong()
            }

        val totalAmount = categoryBreakdown.values.sum()

        return MonthlyStatsResponse(
            year              = year,
            month             = month,
            totalAmount       = totalAmount,
            categoryBreakdown = categoryBreakdown,
            dailyBreakdown    = dailyBreakdown
        )
    }

    // ── 동향 (최근 N개월) ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    fun getTrend(userId: Long, months: Int = 6): MonthlyTrendResponse {
        val since = YearMonth.now()
            .minusMonths((months - 1).toLong())
            .atDay(1)
            .atStartOfDay()

        val rows = transactionRepository.sumAmountByMonthSince(userId, since)

        val entries = rows.map { row ->
            val y = (row[0] as Number).toInt()
            val m = (row[1] as Number).toInt()
            val total = (row[2] as Number).toLong()
            MonthlyTrendEntry(
                year        = y,
                month       = m,
                label       = "${m}월",
                totalAmount = total
            )
        }

        return MonthlyTrendResponse(trend = entries)
    }

    // ── 카테고리 수정 (서버 DB id 기준) ──────────────────────────────────────

    @Transactional
    fun updateCategory(userId: Long, transactionId: Long, newCategory: String): TransactionResponse {
        val tx = transactionRepository.findById(transactionId)
            .orElseThrow { NoSuchElementException("거래 내역을 찾을 수 없습니다. id=$transactionId") }

        if (tx.userId != userId) {
            throw IllegalAccessException("해당 거래 내역에 접근 권한이 없습니다.")
        }

        tx.category = newCategory
        val saved = transactionRepository.save(tx)
        userStatsService.recalculate(userId)
        return saved.toResponse()
    }

    // ── 가맹점명·카테고리 통합 수정 (clientTransactionId 기준) ─────────────────

    @Transactional
    fun updateByClientId(
        userId: Long,
        clientTransactionId: String,
        newMerchantName: String?,
        newCategory: String?
    ): TransactionResponse {
        val tx = transactionRepository.findByUserIdAndClientTransactionId(userId, clientTransactionId)
            .orElseThrow { NoSuchElementException("거래 내역을 찾을 수 없습니다. clientId=$clientTransactionId") }

        var changed = false
        if (!newMerchantName.isNullOrBlank()) {
            tx.merchantName = newMerchantName.trim()
            changed = true
        }
        if (!newCategory.isNullOrBlank()) {
            tx.category = newCategory.trim()
            changed = true
        }

        if (!changed) return tx.toResponse()

        val saved = transactionRepository.save(tx)
        // merchantName 만 변경된 경우 XP/레벨에 영향 없도록 카테고리 변경이 있을 때만 재계산
        if (!newCategory.isNullOrBlank()) {
            userStatsService.recalculate(userId)
        }
        return saved.toResponse()
    }

    // ── 카테고리 수정 (clientTransactionId 기준) — 앱 연동용 ──────────────────

    @Transactional
    fun updateCategoryByClientId(
        userId: Long,
        clientTransactionId: String,
        newCategory: String
    ): TransactionResponse {
        val tx = transactionRepository.findByUserIdAndClientTransactionId(userId, clientTransactionId)
            .orElseThrow { NoSuchElementException("거래 내역을 찾을 수 없습니다. clientId=$clientTransactionId") }

        tx.category = newCategory
        val saved = transactionRepository.save(tx)
        userStatsService.recalculate(userId)
        return saved.toResponse()
    }

    @Transactional
    fun updateById(
        userId: Long,
        transactionId: Long,
        newMerchantName: String?,
        newCategory: String?
    ): TransactionResponse {
        val tx = transactionRepository.findById(transactionId)
            .orElseThrow { NoSuchElementException("Transaction not found. id=$transactionId") }

        if (tx.userId != userId) {
            throw IllegalAccessException("No permission to access this transaction.")
        }

        var changed = false
        if (!newMerchantName.isNullOrBlank()) {
            tx.merchantName = newMerchantName.trim()
            changed = true
        }
        if (!newCategory.isNullOrBlank()) {
            tx.category = newCategory.trim()
            changed = true
        }

        if (!changed) return tx.toResponse()

        val saved = transactionRepository.save(tx)
        if (!newCategory.isNullOrBlank()) {
            userStatsService.recalculate(userId)
        }
        return saved.toResponse()
    }

    @Transactional
    fun deleteByClientId(userId: Long, clientTransactionId: String) {
        val tx = transactionRepository.findByUserIdAndClientTransactionId(userId, clientTransactionId)
            .orElseThrow { NoSuchElementException("거래 내역을 찾을 수 없습니다. clientId=$clientTransactionId") }

        transactionRepository.delete(tx)
        userStatsService.recalculate(userId)
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────

    @Transactional
    fun deleteById(userId: Long, transactionId: Long) {
        val tx = transactionRepository.findById(transactionId)
            .orElseThrow { NoSuchElementException("Transaction not found. id=$transactionId") }

        if (tx.userId != userId) {
            throw IllegalAccessException("No permission to access this transaction.")
        }

        transactionRepository.delete(tx)
        userStatsService.recalculate(userId)
    }

    private fun monthRange(year: Int, month: Int): Pair<LocalDateTime, LocalDateTime> {
        val ym    = YearMonth.of(year, month)
        val start = ym.atDay(1).atStartOfDay()
        val end   = ym.plusMonths(1).atDay(1).atStartOfDay()
        return start to end
    }

    private fun Transaction.toResponse() = TransactionResponse(
        id           = id,
        amount       = amount,
        merchantName = merchantName,
        category     = category,
        occurredAt   = occurredAt.toString(),
        source       = source,
        clientTransactionId = clientTransactionId,
        createdAt    = createdAt.toString()
    )
}
