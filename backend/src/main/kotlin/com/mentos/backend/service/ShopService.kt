package com.mentos.backend.service

import com.mentos.backend.dto.ShopStateResponse
import com.mentos.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ShopService(
    private val userRepository: UserRepository
) {
    /**
     * 아이템 구매 처리.
     * @param userId  구매 요청 사용자 ID
     * @param itemId  아이템 식별자 ("grade/categoryFolder/filename" 형식)
     * @param price   구매 가격 (코인)
     * @throws IllegalStateException 이미 보유 중인 아이템인 경우
     * @throws IllegalArgumentException 코인 부족 또는 유효하지 않은 가격인 경우
     */
    @Transactional
    fun purchaseItem(userId: Long, itemId: String, price: Int): ShopStateResponse {
        require(price >= 0) { "가격이 유효하지 않습니다." }

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

        // 1) 중복 보유 검증
        if (user.ownedItems.contains(itemId)) {
            throw IllegalStateException("이미 보유 중인 아이템입니다.")
        }

        // 2) 코인 잔액 검증
        if (user.coins < price) {
            throw IllegalArgumentException("코인이 부족합니다. (보유: ${user.coins}, 필요: $price)")
        }

        // 3) 코인 차감 및 아이템 추가
        user.coins -= price
        user.ownedItems.add(itemId)

        userRepository.save(user)

        return ShopStateResponse(
            coins = user.coins,
            ownedItems = user.ownedItems.toList()
        )
    }

    /**
     * 사용자의 현재 상점 상태(코인 잔액 + 보유 아이템 목록) 반환.
     */
    @Transactional(readOnly = true)
    fun getShopState(userId: Long): ShopStateResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

        return ShopStateResponse(
            coins = user.coins,
            ownedItems = user.ownedItems.toList()
        )
    }
}
