package com.example.personalfinance.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SaveNotificationParseLogRequest(
    val diagnosticId: String,
    val packageName: String,
    val title: String? = null,
    val rawText: String? = null,
    val status: String,
    val failureReason: String? = null,
    val amount: Long? = null,
    val merchantName: String? = null,
    val occurredAt: String? = null,
    val clientTransactionId: String? = null
)

data class NotificationParseLogResponse(
    val id: Long,
    val status: String,
    val createdAt: String
)

interface NotificationParseLogApi {
    @POST("api/notification-parse-logs")
    suspend fun save(
        @Body req: SaveNotificationParseLogRequest
    ): Response<NotificationParseLogResponse>
}
