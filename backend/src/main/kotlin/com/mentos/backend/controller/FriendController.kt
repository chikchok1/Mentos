package com.mentos.backend.controller

import com.mentos.backend.security.JwtProvider
import com.mentos.backend.service.FriendService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/friends")
class FriendController(
    private val friendService: FriendService,
    private val jwtProvider: JwtProvider
) {
    @GetMapping("/search")
    fun search(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestParam keyword: String
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        return ResponseEntity.ok(friendService.search(userId, keyword))
    }

    @GetMapping
    fun friends(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        return ResponseEntity.ok(friendService.getFriends(userId))
    }

    @DeleteMapping("/{friendId}")
    fun delete(
        @RequestHeader("Authorization") authHeader: String?,
        @PathVariable friendId: Long
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        return try {
            friendService.deleteFriend(userId, friendId)
            ResponseEntity.noContent().build()
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("error" to (e.message ?: "친구 관계를 찾을 수 없습니다.")))
        }
    }

    @GetMapping("/{friendId}/comparison")
    fun comparison(
        @RequestHeader("Authorization") authHeader: String?,
        @PathVariable friendId: Long
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))
        return try {
            ResponseEntity.ok(friendService.compare(userId, friendId))
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(403).body(mapOf("error" to (e.message ?: "접근 권한이 없습니다.")))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("error" to (e.message ?: "사용자를 찾을 수 없습니다.")))
        }
    }

    private fun resolveUserId(authHeader: String?): Long? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
        val token = authHeader.removePrefix("Bearer ")
        if (!jwtProvider.validateAccessToken(token)) return null
        return jwtProvider.getUserIdFromToken(token)
    }
}
