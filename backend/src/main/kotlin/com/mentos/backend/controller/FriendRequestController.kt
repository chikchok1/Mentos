package com.mentos.backend.controller

import com.mentos.backend.dto.FriendRequestCreateRequest
import com.mentos.backend.security.JwtProvider
import com.mentos.backend.service.FriendRequestService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/friend-requests")
class FriendRequestController(
    private val friendRequestService: FriendRequestService,
    private val jwtProvider: JwtProvider
) {
    @PostMapping
    fun create(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestBody req: FriendRequestCreateRequest
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        return try {
            ResponseEntity.ok(friendRequestService.create(userId, req))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "요청을 처리할 수 없습니다.")))
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "요청을 처리할 수 없습니다.")))
        }
    }

    @GetMapping("/received")
    fun received(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        return ResponseEntity.ok(friendRequestService.received(userId))
    }

    @GetMapping("/sent")
    fun sent(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        return ResponseEntity.ok(friendRequestService.sent(userId))
    }

    @PostMapping("/{requestId}/accept")
    fun accept(
        @RequestHeader("Authorization") authHeader: String?,
        @PathVariable requestId: Long
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        return try {
            ResponseEntity.ok(friendRequestService.accept(userId, requestId))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(403).body(mapOf("error" to (e.message ?: "요청 권한이 없습니다.")))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("error" to (e.message ?: "요청을 찾을 수 없습니다.")))
        }
    }

    @PostMapping("/{requestId}/reject")
    fun reject(
        @RequestHeader("Authorization") authHeader: String?,
        @PathVariable requestId: Long
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        return try {
            ResponseEntity.ok(friendRequestService.reject(userId, requestId))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(403).body(mapOf("error" to (e.message ?: "요청 권한이 없습니다.")))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("error" to (e.message ?: "요청을 찾을 수 없습니다.")))
        }
    }

    private fun resolveUserId(authHeader: String?): Long? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
        val token = authHeader.removePrefix("Bearer ")
        if (!jwtProvider.validateAccessToken(token)) return null
        return jwtProvider.getUserIdFromToken(token)
    }
}
