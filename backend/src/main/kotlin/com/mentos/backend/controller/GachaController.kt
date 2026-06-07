package com.mentos.backend.controller

import com.mentos.backend.service.GachaService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.mentos.backend.security.JwtProvider

@RestController
@RequestMapping("/api/gacha")
class GachaController(
    private val gachaService: GachaService,
    private val jwtProvider: JwtProvider
) {
    private val log = LoggerFactory.getLogger(GachaController::class.java)

    /**
     * 서버 기동 시 구형 가챠 아이템(leather/iron/golden/diamond)을
     * 모든 플레이어 인벤토리에서 자동으로 제거한다.
     */
    @PostConstruct
    fun cleanLegacyItemsOnStartup() {
        try {
            val result = gachaService.cleanLegacyGachaItems()
            log.info("[Gacha] 레거시 아이템 정리 완료: 제거된 항목 수 = ${result["removedCount"]}")
        } catch (e: Exception) {
            log.warn("[Gacha] 레거시 아이템 정리 실패: ${e.message}")
        }
    }

    /**
     * 수동으로 레거시 아이템을 정리하는 관리자 엔드포인트.
     * 인증 토큰 없이 호출 가능 (내부 운영용).
     */
    @PostMapping("/admin/clean-legacy")
    fun cleanLegacyItems(): ResponseEntity<Map<String, Any>> {
        return try {
            val result = gachaService.cleanLegacyGachaItems()
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(mapOf("error" to (e.message ?: "정리 중 오류가 발생했습니다.")))
        }
    }
    @PostMapping("/attendance")
    fun performAttendanceGacha(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Map<String, Any>> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(mapOf("error" to "인증 토큰이 없습니다."))
        }
        val token = authHeader.replace("Bearer ", "")
        if (!jwtProvider.validateAccessToken(token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        }
        val userId = jwtProvider.getUserIdFromToken(token)
        
        return try {
            val result = gachaService.performAttendanceGacha(userId)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "가챠 처리 중 오류가 발생했습니다.")))
        }
    }
    
    @PostMapping("/attendance/reset")
    fun resetAttendance(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Map<String, Any>> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(mapOf("error" to "인증 토큰이 없습니다."))
        }
        val token = authHeader.replace("Bearer ", "")
        if (!jwtProvider.validateAccessToken(token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        }
        return try {
            val result = gachaService.resetAttendance()
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "초기화 중 오류가 발생했습니다.")))
        }
    }

    @GetMapping("/attendance/status")
    fun getAttendanceStatus(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Map<String, Any>> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(mapOf("error" to "인증 토큰이 없습니다."))
        }
        val token = authHeader.replace("Bearer ", "")
        if (!jwtProvider.validateAccessToken(token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        }
        val userId = jwtProvider.getUserIdFromToken(token)
        val status = gachaService.getAttendanceStatus(userId)
        return ResponseEntity.ok(status)
    }
    @GetMapping("/user-state")
    fun getUserGachaState(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Map<String, Any>> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(mapOf("error" to "인증 토큰이 없습니다."))
        }
        val token = authHeader.replace("Bearer ", "")
        if (!jwtProvider.validateAccessToken(token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        }
        val userId = jwtProvider.getUserIdFromToken(token)
        val state = gachaService.getUserGachaState(userId)
        return ResponseEntity.ok(state)
    }

    @PostMapping("/coin")
    fun performCoinGacha(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Map<String, Any>> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(mapOf("error" to "인증 토큰이 없습니다."))
        }
        val token = authHeader.replace("Bearer ", "")
        if (!jwtProvider.validateAccessToken(token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        }
        val userId = jwtProvider.getUserIdFromToken(token)

        return try {
            val result = gachaService.performCoinGacha(userId)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "코인 가챠 처리 중 오류가 발생했습니다.")))
        }
    }

    @PostMapping("/test/add-coins")
    fun addCoinsForTest(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestParam(defaultValue = "100") amount: Int
    ): ResponseEntity<Map<String, Any>> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(mapOf("error" to "인증 토큰이 없습니다."))
        }
        val token = authHeader.replace("Bearer ", "")
        if (!jwtProvider.validateAccessToken(token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        }
        val userId = jwtProvider.getUserIdFromToken(token)
        return try {
            val result = gachaService.addCoinsForTest(userId, amount)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "코인 지급 중 오류가 발생했습니다.")))
        }
    }
}
