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
