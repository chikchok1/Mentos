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
    val categorySpending: Map<String, Long>
)

data class UpdateBudgetRequest(
    val monthlyBudget: Long
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
