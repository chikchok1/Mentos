package com.mentos.backend.controller

import com.mentos.backend.dto.*
import com.mentos.backend.security.JwtProvider
import com.mentos.backend.service.TransactionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/transactions")
class TransactionController(
    private val transactionService: TransactionService,
    private val jwtProvider: JwtProvider
) {

    // ── 인증 헬퍼 ────────────────────────────────────────────────────────────

    private fun resolveUserId(authHeader: String?): Long? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
        val token = authHeader.removePrefix("Bearer ")
        if (!jwtProvider.validateAccessToken(token)) return null
        return jwtProvider.getUserIdFromToken(token)
    }

    // ── 저장 ─────────────────────────────────────────────────────────────────

    /** POST /api/transactions */
    @PostMapping
    fun save(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestBody req: SaveTransactionRequest
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return try {
            ResponseEntity.ok(transactionService.save(userId, req))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "저장 중 오류가 발생했습니다.")))
        }
    }

    // ── 전체 목록 조회 ────────────────────────────────────────────────────────

    /** GET /api/transactions/all */
    @GetMapping("/all")
    fun getAll(
        @RequestHeader("Authorization") authHeader: String?
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return try {
            ResponseEntity.ok(transactionService.getAll(userId))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "조회 중 오류가 발생했습니다.")))
        }
    }

    // ── 월별 목록 조회 ────────────────────────────────────────────────────────

    /** GET /api/transactions?year=2025&month=5 */
    @GetMapping
    fun getByMonth(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        val now = LocalDate.now()
        return try {
            ResponseEntity.ok(transactionService.getByMonth(userId, year ?: now.year, month ?: now.monthValue))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "조회 중 오류가 발생했습니다.")))
        }
    }

    // ── 카테고리 수정 (DB id 기준) ─────────────────────────────────────────────

    /** PATCH /api/transactions/{id}/category */
    @PatchMapping("/{id}/category")
    fun updateCategory(
        @RequestHeader("Authorization") authHeader: String?,
        @PathVariable id: Long,
        @RequestBody req: UpdateCategoryRequest
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return try {
            ResponseEntity.ok(transactionService.updateCategory(userId, id, req.category))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("error" to (e.message ?: "거래 내역을 찾을 수 없습니다.")))
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(403).body(mapOf("error" to (e.message ?: "접근 권한이 없습니다.")))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "수정 중 오류가 발생했습니다.")))
        }
    }

    // ── 카테고리 수정 (clientTransactionId 기준) — 앱 연동 전용 ──────────────────

    /**
     * PATCH /api/transactions/by-client/category
     * Body: { clientTransactionId: String, category: String }
     *
     * 앱에서는 서버 DB id를 알 수 없으므로 clientTransactionId로 찾아서 수정.
     */
    @PatchMapping("/by-client/category")
    fun updateCategoryByClientId(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestBody req: UpdateCategoryByClientIdRequest
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return try {
            ResponseEntity.ok(
                transactionService.updateCategoryByClientId(userId, req.clientTransactionId, req.category)
            )
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("error" to (e.message ?: "거래 내역을 찾을 수 없습니다.")))
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(403).body(mapOf("error" to (e.message ?: "접근 권한이 없습니다.")))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "수정 중 오류가 발생했습니다.")))
        }
    }

    // ── 월별 통계 ─────────────────────────────────────────────────────────────

    /** GET /api/transactions/stats?year=2025&month=5 */
    @GetMapping("/stats")
    fun getMonthlyStats(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        val now = LocalDate.now()
        return try {
            ResponseEntity.ok(transactionService.getMonthlyStats(userId, year ?: now.year, month ?: now.monthValue))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "통계 조회 중 오류가 발생했습니다.")))
        }
    }

    // ── 동향 (최근 N개월) ─────────────────────────────────────────────────────

    /** GET /api/transactions/stats/trend?months=6 */
    @GetMapping("/stats/trend")
    fun getTrend(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestParam(required = false, defaultValue = "6") months: Int
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return try {
            ResponseEntity.ok(transactionService.getTrend(userId, months.coerceIn(1, 12)))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "동향 조회 중 오류가 발생했습니다.")))
        }
    }
}
