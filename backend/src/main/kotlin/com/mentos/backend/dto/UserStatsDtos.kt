package com.mentos.backend.dto

data class UserStatsResponse(
    val totalXp: Int,
    val level: Int,
    val currentXp: Int,
    val nextLevelXp: Int,
    val monthlyBudget: Long,
    val job: String,
    val jobTitle: String,
    val jobReason: String,
    val jobMonth: String,
    val thisMonthSpending: Long,
    val categorySpending: Map<String, Long>,
    val nickname: String? = null,
    val friendCode: String? = null,
    val displayName: String? = null
)

data class UpdateBudgetRequest(
    val monthlyBudget: Long
)

data class UserProfileResponse(
    val id: Long,
    val email: String?,
    val nickname: String?,
    val friendCode: String?,
    val displayName: String
)

data class UpdateUserProfileRequest(
    val nickname: String?
)

/** 상점 아이템 구매 요청 DTO */
data class PurchaseItemRequest(
    val itemId: String,   // "grade/categoryFolder/filename" 형식
    val price: Int
)

/** 상점 상태 응답 DTO */
data class ShopStateResponse(
    val coins: Int,
    val ownedItems: List<String>
)
