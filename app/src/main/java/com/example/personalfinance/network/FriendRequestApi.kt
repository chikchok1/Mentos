package com.example.personalfinance.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class FriendRequestCreateRequest(
    val receiverId: Long
)

data class FriendRequestResponse(
    val id: Long?,
    val requesterId: Long?,
    val requesterEmail: String?,
    val requesterNickname: String?,
    val receiverId: Long?,
    val receiverEmail: String?,
    val receiverNickname: String?,
    val requesterCharacterVisible: Boolean?,
    val requesterCharacterAppearance: CharacterAppearanceResponse?,
    val receiverCharacterVisible: Boolean?,
    val receiverCharacterAppearance: CharacterAppearanceResponse?,
    val status: String?,
    val createdAt: String?,
    val respondedAt: String?
)

interface FriendRequestApi {
    @POST("api/friend-requests")
    suspend fun create(
        @Body req: FriendRequestCreateRequest
    ): Response<FriendRequestResponse>

    @GET("api/friend-requests/received")
    suspend fun received(): Response<List<FriendRequestResponse>>

    @GET("api/friend-requests/sent")
    suspend fun sent(): Response<List<FriendRequestResponse>>

    @POST("api/friend-requests/{requestId}/accept")
    suspend fun accept(
        @Path("requestId") requestId: Long
    ): Response<FriendRequestResponse>

    @POST("api/friend-requests/{requestId}/reject")
    suspend fun reject(
        @Path("requestId") requestId: Long
    ): Response<FriendRequestResponse>
}
