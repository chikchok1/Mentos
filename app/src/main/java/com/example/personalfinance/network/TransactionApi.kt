package com.example.personalfinance.network

import retrofit2.Response
import retrofit2.http.*

// ── 요청 ──────────────────────────────────────────────────────────────────────

data class SaveTransactionRequest(
    val amount: Long,
    val merchantName: String,
    val category: String,
    /** ISO-8601: "2025-05-01T14:30:00" */
    val occurredAt: String,
    /** "NOTIFICATION" | "MANUAL" | "SAMPLE" */
    val source: String,
    /** 앱 자체 생성 고유 ID — 중복 저장 방지용 */
    val clientTransactionId: String? = null
)

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
    val occurredAt: String,
    val source: String,
    val createdAt: String
)

/**
 * GET /api/transactions/stats 응답
 * dailyBreakdown: key = 일(1~31), value = 해당 일 지출 합계
 * categoryBreakdown: key = 카테고리명, value = 해당 카테고리 지출 합계
 */
data class MonthlyStatsResponse(
    val year: Int,
    val month: Int,
    val totalAmount: Long,
    val categoryBreakdown: Map<String, Long>,
    val dailyBreakdown: Map<Int, Long>
)

// ── API 인터페이스 ─────────────────────────────────────────────────────────────

interface TransactionApi {

    /** 거래 저장 (카드 알림 자동 / 수동 입력 모두) */
    @POST("api/transactions")
    suspend fun save(
        @Body req: SaveTransactionRequest
    ): Response<TransactionResponse>

    /**
     * 카테고리 수정 — clientTransactionId 기준
     * 앱에서 서버 DB id를 알 수 없으므로 이 방식 사용
     */
    @PATCH("api/transactions/by-client/category")
    suspend fun updateCategoryByClientId(
        @Body req: UpdateCategoryByClientIdRequest
    ): Response<TransactionResponse>

    /**
     * 월별 통계 (카테고리별 합계 + 일자별 합계)
     * GET /api/transactions/stats?year=2026&month=5
     */
    @GET("api/transactions/stats")
    suspend fun getStats(
        @Query("year")  year: Int,
        @Query("month") month: Int
    ): Response<MonthlyStatsResponse>
}
