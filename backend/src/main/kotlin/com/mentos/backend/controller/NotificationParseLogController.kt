package com.mentos.backend.controller

import com.mentos.backend.dto.SaveNotificationParseLogRequest
import com.mentos.backend.security.JwtProvider
import com.mentos.backend.service.NotificationParseLogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notification-parse-logs")
class NotificationParseLogController(
    private val notificationParseLogService: NotificationParseLogService,
    private val jwtProvider: JwtProvider
) {
    @PostMapping
    fun save(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestBody req: SaveNotificationParseLogRequest
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return try {
            ResponseEntity.ok(notificationParseLogService.save(userId, req))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "알림 로그 저장 중 오류가 발생했습니다.")))
        }
    }

    private fun resolveUserId(authHeader: String?): Long? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
        val token = authHeader.removePrefix("Bearer ")
        if (!jwtProvider.validateAccessToken(token)) return null
        return jwtProvider.getUserIdFromToken(token)
    }
}
