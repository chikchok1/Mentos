package com.mentos.backend.dto

import java.time.LocalDateTime

// ── 요청 ──────────────────────────────────────────────────────────────────────

/** POST /api/transactions */
data class SaveTransactionRequest(
    val amount: Long,
    val merchantName: String,
    val category: String,
    /** ISO-8601 문자열: "2026-06-05T14:30:00" — 앱에서 String으로 전송 */
    val occurredAt: String,
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

/**
 * PATCH /api/transactions/by-client
 * 가맹점명 + 카테고리를 함께 또는 개별로 수정
 * null 이면 해당 필드는 변경하지 않음
 */
data class UpdateTransactionByClientIdRequest(
    val clientTransactionId: String,
    val merchantName: String? = null,
    val category: String? = null
)

// ── 응답 ──────────────────────────────────────────────────────────────────────

data class TransactionResponse(
    val id: Long,
    val amount: Long,
    val merchantName: String,
    val category: String,
    /** ISO-8601 문자열로 직렬화됨 — Jackson 설정으로 보장 */
    val occurredAt: String,
    val source: String,
    val createdAt: String
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
