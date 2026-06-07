package com.mentos.backend.dto

data class FriendSearchResponse(
    val id: Long,
    val email: String?,
    val nickname: String?,
    val level: Int,
    val totalXp: Int,
    val currentXp: Int,
    val nextLevelXp: Int,
    val job: String,
    val jobReason: String,
    val jobMonth: String,
    val alreadyFriend: Boolean,
    val requestStatus: String,
    val pendingRequestId: Long?
)

data class FriendRequestCreateRequest(
    val receiverId: Long
)

data class FriendRequestResponse(
    val id: Long,
    val requesterId: Long,
    val requesterEmail: String?,
    val requesterNickname: String?,
    val receiverId: Long,
    val receiverEmail: String?,
    val receiverNickname: String?,
    val status: String,
    val createdAt: String,
    val respondedAt: String?
)

data class FriendResponse(
    val friendId: Long,
    val email: String?,
    val nickname: String?,
    val level: Int?,
    val totalXp: Int?,
    val currentXp: Int?,
    val nextLevelXp: Int?,
    val job: String?,
    val jobReason: String?,
    val jobMonth: String?,
    val characterVisible: Boolean,
    val ownedItems: List<String>,
    val representativeItemId: String?,
    val monthlySpendingVisible: Boolean,
    val monthlySpending: Long?
)

data class FriendComparisonResponse(
    val month: String,
    val me: ComparisonUserResponse,
    val friend: ComparisonUserResponse
)

data class ComparisonUserResponse(
    val id: Long,
    val email: String?,
    val nickname: String?,
    val level: Int?,
    val totalXp: Int?,
    val currentXp: Int?,
    val nextLevelXp: Int?,
    val job: String?,
    val jobReason: String?,
    val jobMonth: String?,
    val characterVisible: Boolean,
    val monthlySpendingVisible: Boolean,
    val spendingPrivacyStatus: String,
    val monthlySpending: Long?,
    val topCategories: List<CategorySpendingResponse>,
    val categorySpending: List<CategorySpendingResponse>
)

data class CategorySpendingResponse(
    val category: String,
    val amount: Long,
    val ratio: Int
)

data class PrivacySettingsRequest(
    val spendingVisibility: String,
    val characterVisibility: String
)

data class PrivacySettingsResponse(
    val spendingVisibility: String,
    val characterVisibility: String
)
