package com.mentos.backend.controller

import com.mentos.backend.service.GachaService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.mentos.backend.security.JwtProvider

@RestController
@RequestMapping("/api/gacha")
class GachaController(
    private val gachaService: GachaService,
    private val jwtProvider: JwtProvider
) {
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
}
