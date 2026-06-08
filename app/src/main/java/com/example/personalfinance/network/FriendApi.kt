package com.example.personalfinance.network

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class FriendSearchResponse(
    val id: Long?,
    val email: String?,
    val nickname: String?,
    val friendCode: String?,
    val displayName: String?,
    val level: Int?,
    val totalXp: Int?,
    val currentXp: Int?,
    val nextLevelXp: Int?,
    val job: String?,
    val jobReason: String?,
    val jobMonth: String?,
    val alreadyFriend: Boolean?,
    val requestStatus: String?,
    val pendingRequestId: Long?,
    val characterVisible: Boolean?,
    val characterAppearance: CharacterAppearanceResponse?
)

data class FriendResponse(
    val friendId: Long,
    val email: String?,
    val nickname: String?,
    val friendCode: String?,
    val displayName: String?,
    val level: Int?,
    val totalXp: Int?,
    val currentXp: Int?,
    val nextLevelXp: Int?,
    val job: String?,
    val jobReason: String?,
    val jobMonth: String?,
    val characterVisible: Boolean?,
    val ownedItems: List<String>?,
    val representativeItemId: String?,
    val characterAppearance: CharacterAppearanceResponse?,
    val monthlySpendingVisible: Boolean?,
    val monthlySpending: Long?
)

data class FriendComparisonResponse(
    val month: String?,
    val me: ComparisonUserResponse?,
    val friend: ComparisonUserResponse?
)

data class ComparisonUserResponse(
    val id: Long?,
    val email: String?,
    val nickname: String?,
    val friendCode: String?,
    val displayName: String?,
    val level: Int?,
    val totalXp: Int?,
    val currentXp: Int?,
    val nextLevelXp: Int?,
    val job: String?,
    val jobReason: String?,
    val jobMonth: String?,
    val characterVisible: Boolean?,
    val characterAppearance: CharacterAppearanceResponse?,
    val monthlySpendingVisible: Boolean?,
    val spendingPrivacyStatus: String?,
    val monthlySpending: Long?,
    val topCategories: List<CategorySpendingResponse>?,
    val categorySpending: List<CategorySpendingResponse>?
)

data class CategorySpendingResponse(
    val category: String?,
    val amount: Long?,
    val ratio: Int?
)

interface FriendApi {
    @GET("api/friends/search")
    suspend fun search(
        @Query("keyword") keyword: String
    ): Response<List<FriendSearchResponse>>

    @GET("api/friends")
    suspend fun getFriends(): Response<List<FriendResponse>>

    @DELETE("api/friends/{friendId}")
    suspend fun deleteFriend(
        @Path("friendId") friendId: Long
    ): Response<Unit>

    @GET("api/friends/{friendId}/comparison")
    suspend fun getComparison(
        @Path("friendId") friendId: Long
    ): Response<FriendComparisonResponse>
}
