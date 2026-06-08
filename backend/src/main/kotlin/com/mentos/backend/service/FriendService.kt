package com.mentos.backend.service

import com.mentos.backend.dto.CategorySpendingResponse
import com.mentos.backend.dto.ComparisonUserResponse
import com.mentos.backend.dto.FriendComparisonResponse
import com.mentos.backend.dto.FriendResponse
import com.mentos.backend.dto.FriendSearchResponse
import com.mentos.backend.entity.FriendRequestStatus
import com.mentos.backend.entity.User
import com.mentos.backend.entity.VisibilityScope
import com.mentos.backend.repository.FriendRepository
import com.mentos.backend.repository.FriendRequestRepository
import com.mentos.backend.repository.TransactionRepository
import com.mentos.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class FriendService(
    private val userRepository: UserRepository,
    private val friendRepository: FriendRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val transactionRepository: TransactionRepository,
    private val characterService: CharacterService
) {
    @Transactional(readOnly = true)
    fun search(currentUserId: Long, keyword: String): List<FriendSearchResponse> {
        val normalized = keyword.trim()
        if (normalized.isBlank()) return emptyList()

        return userRepository.searchFriends(normalized, currentUserId).map { user ->
            val alreadyFriend = friendRepository.existsByUserIdAndFriendId(currentUserId, user.id)
            val pending = friendRequestRepository
                .findPendingBetween(currentUserId, user.id, FriendRequestStatus.PENDING)
                .firstOrNull()
            val requestStatus = when {
                alreadyFriend -> "FRIEND"
                pending == null -> "NONE"
                pending.requesterId == currentUserId -> "PENDING_SENT"
                else -> "PENDING_RECEIVED"
            }
            val characterVisible = alreadyFriend && user.characterVisibility == VisibilityScope.FRIENDS

            FriendSearchResponse(
                id = user.id,
                email = user.email,
                nickname = null,
                level = if (characterVisible) user.level else null,
                totalXp = if (characterVisible) user.totalXp else null,
                currentXp = if (characterVisible) user.currentXp else null,
                nextLevelXp = if (characterVisible) user.nextLevelXp else null,
                job = if (characterVisible) user.job else null,
                jobReason = if (characterVisible) user.jobReason else null,
                jobMonth = if (characterVisible) user.jobMonth else null,
                alreadyFriend = alreadyFriend,
                requestStatus = requestStatus,
                pendingRequestId = pending?.id,
                characterVisible = characterVisible,
                characterAppearance = if (characterVisible) characterService.appearanceFor(user.id) else null
            )
        }
    }

    @Transactional(readOnly = true)
    fun getFriends(userId: Long): List<FriendResponse> {
        val friendIds = friendRepository.findByUserIdOrderByCreatedAtDesc(userId).map { it.friendId }
        if (friendIds.isEmpty()) return emptyList()
        val friendsById = userRepository.findAllById(friendIds).associateBy { it.id }

        return friendIds.mapNotNull { friendId ->
            friendsById[friendId]?.let { user -> user.toFriendResponse(includeSpending = true) }
        }
    }

    @Transactional
    fun deleteFriend(userId: Long, friendId: Long) {
        if (!friendRepository.existsByUserIdAndFriendId(userId, friendId)) {
            throw NoSuchElementException("친구 관계가 아닙니다.")
        }
        friendRepository.deleteByUserIdAndFriendId(userId, friendId)
        friendRepository.deleteByUserIdAndFriendId(friendId, userId)
    }

    @Transactional(readOnly = true)
    fun compare(userId: Long, friendId: Long): FriendComparisonResponse {
        if (!friendRepository.existsByUserIdAndFriendId(userId, friendId)) {
            throw IllegalAccessException("친구만 비교할 수 있습니다.")
        }
        val me = findUser(userId)
        val friend = findUser(friendId)
        val month = YearMonth.now()

        return FriendComparisonResponse(
            month = month.toString(),
            me = me.toComparisonResponse(
                characterVisible = true,
                spendingVisible = true,
                spendingPrivacyStatus = "VISIBLE",
                month = month
            ),
            friend = friend.toComparisonResponse(
                characterVisible = friend.characterVisibility == VisibilityScope.FRIENDS,
                spendingVisible = friend.spendingVisibility == VisibilityScope.FRIENDS,
                spendingPrivacyStatus = if (friend.spendingVisibility == VisibilityScope.FRIENDS) "VISIBLE" else "PRIVATE",
                month = month
            )
        )
    }

    private fun findUser(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("사용자를 찾을 수 없습니다.") }

    private fun User.toFriendResponse(includeSpending: Boolean): FriendResponse {
        val characterVisible = characterVisibility == VisibilityScope.FRIENDS
        val spendingVisible = includeSpending && spendingVisibility == VisibilityScope.FRIENDS
        val monthlySpending = if (spendingVisible) monthlyStats(id, YearMonth.now()).total else null
        val sortedItems = ownedItems.sorted()

        return FriendResponse(
            friendId = id,
            email = email,
            nickname = null,
            level = if (characterVisible) level else null,
            totalXp = if (characterVisible) totalXp else null,
            currentXp = if (characterVisible) currentXp else null,
            nextLevelXp = if (characterVisible) nextLevelXp else null,
            job = if (characterVisible) job else null,
            jobReason = if (characterVisible) jobReason else null,
            jobMonth = if (characterVisible) jobMonth else null,
            characterVisible = characterVisible,
            ownedItems = if (characterVisible) sortedItems else emptyList(),
            representativeItemId = if (characterVisible) sortedItems.firstOrNull() else null,
            characterAppearance = if (characterVisible) characterService.appearanceFor(id) else null,
            monthlySpendingVisible = spendingVisible,
            monthlySpending = monthlySpending
        )
    }

    private fun User.toComparisonResponse(
        characterVisible: Boolean,
        spendingVisible: Boolean,
        spendingPrivacyStatus: String,
        month: YearMonth
    ): ComparisonUserResponse {
        val stats = if (spendingVisible) monthlyStats(id, month) else MonthlySpendingStats.empty()
        return ComparisonUserResponse(
            id = id,
            email = email,
            nickname = null,
            level = if (characterVisible) level else null,
            totalXp = if (characterVisible) totalXp else null,
            currentXp = if (characterVisible) currentXp else null,
            nextLevelXp = if (characterVisible) nextLevelXp else null,
            job = if (characterVisible) job else null,
            jobReason = if (characterVisible) jobReason else null,
            jobMonth = if (characterVisible) jobMonth else null,
            characterVisible = characterVisible,
            characterAppearance = if (characterVisible) characterService.appearanceFor(id) else null,
            monthlySpendingVisible = spendingVisible,
            spendingPrivacyStatus = spendingPrivacyStatus,
            monthlySpending = if (spendingVisible) stats.total else null,
            topCategories = if (spendingVisible) stats.categories.take(3) else emptyList(),
            categorySpending = if (spendingVisible) stats.categories else emptyList()
        )
    }

    private fun monthlyStats(userId: Long, month: YearMonth): MonthlySpendingStats {
        val start = month.atDay(1).atStartOfDay()
        val end = month.plusMonths(1).atDay(1).atStartOfDay()
        val categoryAmounts = transactionRepository
            .sumAmountByCategoryInRange(userId, start, end)
            .associate { row -> (row[0] as String) to (row[1] as Number).toLong() }
        val total = categoryAmounts.values.sum()
        val categories = categoryAmounts
            .map { (category, amount) ->
                CategorySpendingResponse(
                    category = category,
                    amount = amount,
                    ratio = if (total > 0L) ((amount.toDouble() / total.toDouble()) * 100).toInt() else 0
                )
            }
            .sortedByDescending { it.amount }
        return MonthlySpendingStats(total, categories)
    }

    private data class MonthlySpendingStats(
        val total: Long,
        val categories: List<CategorySpendingResponse>
    ) {
        companion object {
            fun empty() = MonthlySpendingStats(0L, emptyList())
        }
    }
}
