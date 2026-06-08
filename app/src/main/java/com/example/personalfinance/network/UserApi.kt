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
    val categorySpending: Map<String, Long>,
    val nickname: String?,
    val friendCode: String?,
    val displayName: String?
)

data class UpdateBudgetRequest(
    val monthlyBudget: Long
)

data class PrivacySettingsRequest(
    val spendingVisibility: String,
    val characterVisibility: String
)

data class PrivacySettingsResponse(
    val spendingVisibility: String,
    val characterVisibility: String
)

data class UserProfileResponse(
    val id: Long?,
    val email: String?,
    val nickname: String?,
    val friendCode: String?,
    val displayName: String?
)

data class UpdateUserProfileRequest(
    val nickname: String?
)

data class EquippedItemDto(
    val slot: String?,
    val itemId: String?,
    val layerOrder: Int?
)

data class CharacterAppearanceResponse(
    val baseCharacter: String?,
    val equippedItems: List<EquippedItemDto>?
)

data class UpdateCharacterRequest(
    val equippedItems: List<EquippedItemDto>
)

interface UserApi {
    @GET("api/users/me/stats")
    suspend fun getStats(): Response<UserStatsResponse>

    @GET("api/users/me/profile")
    suspend fun getProfile(): Response<UserProfileResponse>

    @PATCH("api/users/me/profile")
    suspend fun updateProfile(
        @Body req: UpdateUserProfileRequest
    ): Response<UserProfileResponse>

    @PATCH("api/users/me/budget")
    suspend fun updateBudget(
        @Body req: UpdateBudgetRequest
    ): Response<UserStatsResponse>

    @POST("api/users/me/stats/recalculate")
    suspend fun recalculateStats(): Response<UserStatsResponse>

    @GET("api/users/me/privacy")
    suspend fun getPrivacy(): Response<PrivacySettingsResponse>

    @PATCH("api/users/me/privacy")
    suspend fun updatePrivacy(
        @Body req: PrivacySettingsRequest
    ): Response<PrivacySettingsResponse>

    @GET("api/users/me/character")
    suspend fun getCharacter(): Response<CharacterAppearanceResponse>

    @PATCH("api/users/me/character")
    suspend fun updateCharacter(
        @Body req: UpdateCharacterRequest
    ): Response<CharacterAppearanceResponse>
}
