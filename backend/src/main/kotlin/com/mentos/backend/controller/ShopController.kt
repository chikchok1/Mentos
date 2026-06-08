package com.mentos.backend.controller

import com.mentos.backend.dto.PurchaseItemRequest
import com.mentos.backend.security.JwtProvider
import com.mentos.backend.service.ShopService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/shop")
class ShopController(
    private val shopService: ShopService,
    private val jwtProvider: JwtProvider
) {
    /**
     * 아이템 구매
     * POST /api/shop/purchase
     * - 200: 구매 성공, ShopStateResponse 반환
     * - 402: 코인 부족
     * - 409: 이미 보유 중
     */
    @PostMapping("/purchase")
    fun purchaseItem(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestBody req: PurchaseItemRequest
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return try {
            val result = shopService.purchaseItem(userId, req.itemId, req.price)
            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            // 이미 보유 중
            ResponseEntity.status(409).body(mapOf("error" to (e.message ?: "이미 보유 중인 아이템입니다.")))
        } catch (e: IllegalArgumentException) {
            // 코인 부족 or 유효하지 않은 요청
            val status = if (e.message?.contains("코인이 부족") == true) 402 else 400
            ResponseEntity.status(status).body(mapOf("error" to (e.message ?: "구매 처리 중 오류가 발생했습니다.")))
        }
    }

    /**
     * 보유 아이템 및 코인 잔액 조회
     * GET /api/shop/state
     */
    @GetMapping("/state")
    fun getShopState(
        @RequestHeader("Authorization") authHeader: String?
    ): ResponseEntity<Any> {
        val userId = resolveUserId(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "유효하지 않은 토큰입니다."))

        return ResponseEntity.ok(shopService.getShopState(userId))
    }

    private fun resolveUserId(authHeader: String?): Long? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
        val token = authHeader.removePrefix("Bearer ")
        if (!jwtProvider.validateAccessToken(token)) return null
        return jwtProvider.getUserIdFromToken(token)
    }
}
