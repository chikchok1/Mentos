package com.mentos.backend.controller

import com.mentos.backend.security.JwtProvider
import com.mentos.backend.service.AiClassificationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/classification")
class ClassificationController(
    private val aiClassificationService: AiClassificationService,
    private val jwtProvider: JwtProvider
) {

    @GetMapping("/categorize")
    fun categorizeMerchant(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestParam merchantName: String
    ): ResponseEntity<Map<String, String>> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(mapOf("error" to "Missing authorization token"))
        }

        val token = authHeader.removePrefix("Bearer ").trim()
        if (!jwtProvider.validateAccessToken(token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Invalid access token"))
        }

        val category = aiClassificationService.classifyMerchant(merchantName)
        return ResponseEntity.ok(mapOf(
            "merchantName" to merchantName,
            "category" to category
        ))
    }

    /**
     * 사용자가 카테고리를 수동으로 수정할 때 해당 가맹점의 캐시도 함께 업데이트합니다.
     * PATCH /api/classification/cache?merchantName=xxx&category=yyy
     */
    @PatchMapping("/cache")
    fun updateCategoryCache(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestParam merchantName: String,
        @RequestParam category: String
    ): ResponseEntity<Map<String, String>> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(mapOf("error" to "Missing authorization token"))
        }
        val token = authHeader.removePrefix("Bearer ").trim()
        if (!jwtProvider.validateAccessToken(token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Invalid access token"))
        }

        aiClassificationService.updateCategoryCache(merchantName, category)
        return ResponseEntity.ok(mapOf(
            "merchantName" to merchantName,
            "updatedCategory" to category
        ))
    }
}
