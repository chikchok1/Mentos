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

data class MonthlyStatsResponse(
    val year: Int,
    val month: Int,
    val totalAmount: Long,
    val categoryBreakdown: Map<String, Long>,
    val dailyBreakdown: Map<Int, Long>
)

// ── API 인터페이스 ─────────────────────────────────────────────────────────────

interface TransactionApi {

    /** 거래 저장 */
    @POST("api/transactions")
    suspend fun save(
        @Body req: SaveTransactionRequest
    ): Response<TransactionResponse>

    /** 전체 거래 목록 조회 (로그인 복원용) */
    @GET("api/transactions/all")
    suspend fun getAll(): Response<List<TransactionResponse>>

    /**
     * 월별 거래 목록 조회
     * GET /api/transactions?year=2026&month=6
     */
    @GET("api/transactions")
    suspend fun getByMonth(
        @Query("year")  year: Int,
        @Query("month") month: Int
    ): Response<List<TransactionResponse>>

    /** 카테고리 수정 — clientTransactionId 기준 */
    @PATCH("api/transactions/by-client/category")
    suspend fun updateCategoryByClientId(
        @Body req: UpdateCategoryByClientIdRequest
    ): Response<TransactionResponse>

    /** 월별 통계 */
    @GET("api/transactions/stats")
    suspend fun getStats(
        @Query("year")  year: Int,
        @Query("month") month: Int
    ): Response<MonthlyStatsResponse>
}
