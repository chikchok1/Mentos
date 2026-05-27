package com.mentos.backend.service

import com.mentos.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GachaService(
    private val userRepository: UserRepository
) {
    @Transactional
    fun performAttendanceGacha(userId: Long): Map<String, Any> {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        val today = LocalDate.now()
        
        if (user.lastAttendanceDate == today) {
            throw IllegalStateException("오늘은 이미 출석 가챠를 진행했습니다.")
        }
        
        // 가챠 굴리기
        val result = GachaEngine.roll(user.ownedItems)
        
        var isDuplicate = false
        var coinReward = 0
        var itemId = ""
        
        when (result) {
            is GachaResult.NewItem -> {
                user.ownedItems.add(result.item.id)
                itemId = result.item.id
            }
            is GachaResult.DuplicateCoin -> {
                isDuplicate = true
                coinReward = result.coins
                user.coins += coinReward
                itemId = result.item.id
            }
        }
        
        user.lastAttendanceDate = today
        userRepository.save(user)
        
        return mapOf(
            "success" to true,
            "message" to "출석 가챠가 완료되었습니다.",
            "itemId" to itemId,
            "isDuplicate" to isDuplicate,
            "coinReward" to coinReward,
            "totalCoins" to user.coins
        )
    }
    
    fun getAttendanceStatus(userId: Long): Map<String, Any> {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        val today = LocalDate.now()
        
        return mapOf(
            "usedToday" to (user.lastAttendanceDate == today)
        )
    }

    fun getUserGachaState(userId: Long): Map<String, Any> {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        return mapOf(
            "coins" to user.coins,
            "ownedItems" to user.ownedItems.toList()
        )
    }
}
