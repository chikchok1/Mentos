package com.mentos.backend.dto

import java.time.LocalDateTime

// ── 요청 ──────────────────────────────────────────────────────────────────────

/** POST /api/transactions */
data class SaveTransactionRequest(
    val amount: Long,
    val merchantName: String,
    val category: String,
    val occurredAt: LocalDateTime,
    val source: String = "MANUAL",
    val clientTransactionId: String? = null
)

/** PATCH /api/transactions/{id}/category */
data class UpdateCategoryRequest(
    val category: String
)

/** PATCH /api/transactions/by-client/category */
data class UpdateCategoryByClientIdRequest(
    val clientTransactionId: String,
    val category: String
)

// ── 응답 ──────────────────────────────────────────────────────────────────────

data class TransactionResponse(
    val id: Long,
    val amount: Long,
    val merchantName: String,
    val category: String,
    val occurredAt: LocalDateTime,
    val source: String,
    val createdAt: LocalDateTime
)

/** GET /api/transactions/stats */
data class MonthlyStatsResponse(
    val year: Int,
    val month: Int,
    val totalAmount: Long,
    val categoryBreakdown: Map<String, Long>,
    val dailyBreakdown: Map<Int, Long>
)

/** GET /api/transactions/stats/trend */
data class MonthlyTrendResponse(
    val trend: List<MonthlyTrendEntry>
)

data class MonthlyTrendEntry(
    val year: Int,
    val month: Int,
    val label: String,
    val totalAmount: Long
)
