package com.mentos.backend.controller

import com.mentos.backend.dto.PrivacySettingsRequest
import com.mentos.backend.dto.UpdateCharacterRequest
import com.mentos.backend.dto.UpdateBudgetRequest
import com.mentos.backend.security.JwtProvider
import com.mentos.backend.service.CharacterService
import com.mentos.backend.service.UserStatsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/me")
class UserController(
    private val userStatsService: UserStatsService,
    private val characterService: CharacterService,
    private val jwtProvider: JwtProvider
) {
    @GetMapping("/stats")
    fun getStats(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return ResponseEntity.ok(userStatsService.getStats(userId))
    }

    @PatchMapping("/budget")
    fun updateBudget(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestBody req: UpdateBudgetRequest
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return try {
            ResponseEntity.ok(userStatsService.updateBudget(userId, req.monthlyBudget))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "예산 수정 중 오류가 발생했습니다.")))
        }
    }

    @PostMapping("/stats/recalculate")
    fun recalculateStats(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return ResponseEntity.ok(userStatsService.recalculate(userId))
    }

    @GetMapping("/privacy")
    fun getPrivacy(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return ResponseEntity.ok(userStatsService.getPrivacy(userId))
    }

    @PatchMapping("/privacy")
    fun updatePrivacy(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestBody req: PrivacySettingsRequest
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return try {
            ResponseEntity.ok(
                userStatsService.updatePrivacy(
                    userId = userId,
                    spendingVisibility = req.spendingVisibility,
                    characterVisibility = req.characterVisibility
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "공개 설정 변경 중 오류가 발생했습니다.")))
        }
    }

    @GetMapping("/character")
    fun getCharacter(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "?좏슚?섏? ?딆? ?좏겙?낅땲??"))

        return ResponseEntity.ok(characterService.getCharacter(userId))
    }

    @PatchMapping("/character")
    fun updateCharacter(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestBody req: UpdateCharacterRequest
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "?좏슚?섏? ?딆? ?좏겙?낅땲??"))

        return try {
            ResponseEntity.ok(characterService.updateCharacter(userId, req))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "캐릭터 저장에 실패했습니다.")))
        }
    }

    private fun resolveUserId(authHeader: String?): Long? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
        val token = authHeader.removePrefix("Bearer ")
        if (!jwtProvider.validateAccessToken(token)) return null
        return jwtProvider.getUserIdFromToken(token)
    }
}
