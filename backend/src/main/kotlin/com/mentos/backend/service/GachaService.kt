package com.mentos.backend.service

import com.mentos.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GachaService(
    private val userRepository: UserRepository
) {
    /**
     * 서버 기동 시 또는 수동 호출로, 구형 GachaItemPool(leather/iron/golden/diamond) 형식의
     * 아이템을 모든 플레이어의 인벤토리에서 제거한다.
     */
    @Transactional
    fun cleanLegacyGachaItems(): Map<String, Any> {
        val allUsers = userRepository.findAll()
        var removedCount = 0
        allUsers.forEach { user ->
            val before = user.ownedItems.size
            user.ownedItems.removeIf { GachaItemPool.isLegacyItem(it) }
            removedCount += (before - user.ownedItems.size)
        }
        userRepository.saveAll(allUsers)
        return mapOf(
            "success"      to true,
            "message"      to "구형 가챠 아이템이 정리되었습니다.",
            "removedCount" to removedCount
        )
    }

    @Transactional
    fun performAttendanceGacha(userId: Long): Map<String, Any> {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        val today = LocalDate.now()

        if (user.lastAttendanceDate == today) {
            throw IllegalStateException("오늘은 이미 출석 가챠를 진행했습니다.")
        }

        // 구형 아이템을 제외한 보유 목록으로 중복 판정
        val validOwnedItems = user.ownedItems.filter { !GachaItemPool.isLegacyItem(it) }.toSet()

        // 가챠 굴리기
        val result = GachaEngine.roll(validOwnedItems)

        var isDuplicate = false
        var coinReward = 0
        var itemId = ""
        var itemName = ""

        when (result) {
            is GachaResult.NewItem -> {
                user.ownedItems.removeIf { GachaItemPool.isLegacyItem(it) } // 혹시 남은 구형 제거
                user.ownedItems.add(result.item.id)
                itemId = result.item.id
                itemName = result.item.name
            }
            is GachaResult.DuplicateCoin -> {
                isDuplicate = true
                coinReward = result.coins
                user.coins += coinReward
                itemId = result.item.id
                itemName = result.item.name
            }
        }

        user.lastAttendanceDate = today
        userRepository.save(user)

        return mapOf(
            "success"     to true,
            "message"     to "출석 가챠가 완료되었습니다.",
            "itemId"      to itemId,
            "itemName"    to itemName,
            "isDuplicate" to isDuplicate,
            "coinReward"  to coinReward,
            "totalCoins"  to user.coins
        )
    }

    fun getAttendanceStatus(userId: Long): Map<String, Any> {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        val today = LocalDate.now()

        return mapOf(
            "usedToday" to (user.lastAttendanceDate == today)
        )
    }

    @Transactional
    fun resetAttendance(): Map<String, Any> {
        val allUsers = userRepository.findAll()
        allUsers.forEach { it.lastAttendanceDate = null }
        userRepository.saveAll(allUsers)
        return mapOf(
            "success"     to true,
            "message"     to "출석체크가 초기화되었습니다.",
            "resetCount"  to allUsers.size
        )
    }

    fun getUserGachaState(userId: Long): Map<String, Any> {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        // 구형 아이템을 제외한 목록만 반환
        val validOwnedItems = user.ownedItems.filter { !GachaItemPool.isLegacyItem(it) }
        return mapOf(
            "coins"      to user.coins,
            "ownedItems" to validOwnedItems
        )
    }

    @Transactional
    fun performCoinGacha(userId: Long): Map<String, Any> {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

        if (user.coins < 10) {
            throw IllegalStateException("코인이 부족합니다. (보유: ${user.coins}, 필요: 10)")
        }

        // 코인 차감
        user.coins -= 10

        // 구형 아이템을 제외한 보유 목록으로 중복 판정
        val validOwnedItems = user.ownedItems.filter { !GachaItemPool.isLegacyItem(it) }.toSet()

        // 가챠 굴리기
        val result = GachaEngine.roll(validOwnedItems)

        var isDuplicate = false
        var coinReward = 0
        var itemId = ""
        var itemName = ""

        when (result) {
            is GachaResult.NewItem -> {
                user.ownedItems.removeIf { GachaItemPool.isLegacyItem(it) } // 혹시 남은 구형 제거
                user.ownedItems.add(result.item.id)
                itemId = result.item.id
                itemName = result.item.name
            }
            is GachaResult.DuplicateCoin -> {
                isDuplicate = true
                coinReward = result.coins
                user.coins += coinReward
                itemId = result.item.id
                itemName = result.item.name
            }
        }

        userRepository.save(user)

        return mapOf(
            "success"     to true,
            "message"     to "코인 가챠가 완료되었습니다.",
            "itemId"      to itemId,
            "itemName"    to itemName,
            "isDuplicate" to isDuplicate,
            "coinReward"  to coinReward,
            "totalCoins"  to user.coins
        )
    }

    @Transactional
    fun addCoinsForTest(userId: Long, amount: Int): Map<String, Any> {
        require(amount > 0) { "지급 코인은 1 이상이어야 합니다." }
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        user.coins += amount
        userRepository.save(user)
        return mapOf(
            "success"    to true,
            "addedCoins" to amount,
            "totalCoins" to user.coins
        )
    }
}
