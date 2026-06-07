package com.example.personalfinance.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

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

interface UserApi {
    @GET("api/users/me/stats")
    suspend fun getStats(): Response<UserStatsResponse>

    @PATCH("api/users/me/budget")
    suspend fun updateBudget(
        @Body req: UpdateBudgetRequest
    ): Response<UserStatsResponse>

    @POST("api/users/me/stats/recalculate")
    suspend fun recalculateStats(): Response<UserStatsResponse>
}
