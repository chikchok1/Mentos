package com.mentos.backend.controller

import com.mentos.backend.security.JwtProvider
import com.mentos.backend.service.CoinRewardService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 코인 보상 관련 API 엔드포인트.
 *
 * POST /api/coins/attendance  — 출석 체크 + 코인 지급
 * GET  /api/coins/attendance/status — 오늘 출석 여부 + 현재 코인
 */
@RestController
@RequestMapping("/api/coins")
class CoinRewardController(
    private val coinRewardService: CoinRewardService,
    private val jwtProvider: JwtProvider,
) {
    /**
     * 출석 체크 + 코인 지급.
     * - 오늘 이미 출석한 경우 alreadyChecked=true, 코인 미지급
     * - 월 예산 성공 보상이 가능한 경우 함께 지급
     */
    @PostMapping("/attendance")
    fun checkAttendance(
        @RequestHeader("Authorization") authHeader: String?
    ): ResponseEntity<Map<String, Any>> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return try {
            val result = coinRewardService.checkAttendance(userId)
            ResponseEntity.ok(
                mapOf(
                    "success"          to true,
                    "alreadyChecked"   to result.alreadyChecked,
                    "attendanceCoins"  to result.attendanceCoins,
                    "budgetRewarded"   to result.budgetRewarded,
                    "budgetRewardCoins" to result.budgetRewardCoins,
                    "totalCoins"       to result.totalCoins,
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "출석 처리 중 오류가 발생했습니다.")))
        }
    }

    /**
     * 오늘 출석 여부 + 현재 보유 코인 조회 (코인 미지급).
     */
    @GetMapping("/attendance/status")
    fun getAttendanceStatus(
        @RequestHeader("Authorization") authHeader: String?
    ): ResponseEntity<Map<String, Any>> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return try {
            val status = coinRewardService.getAttendanceStatus(userId)
            ResponseEntity.ok(
                mapOf(
                    "checkedToday" to status.checkedToday,
                    "totalCoins"   to status.totalCoins,
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "조회 중 오류가 발생했습니다.")))
        }
    }

    private fun resolveUserId(authHeader: String?): Long? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
        val token = authHeader.removePrefix("Bearer ")
        if (!jwtProvider.validateAccessToken(token)) return null
        return jwtProvider.getUserIdFromToken(token)
    }
}
