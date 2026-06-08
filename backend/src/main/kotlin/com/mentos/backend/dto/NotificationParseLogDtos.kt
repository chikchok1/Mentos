package com.mentos.backend.dto

data class SaveNotificationParseLogRequest(
    val diagnosticId: String,
    val packageName: String,
    val title: String? = null,
    val rawText: String? = null,
    val status: String,
    val failureReason: String? = null,
    val parsedAmount: Long? = null,
    val parsedMerchant: String? = null,
    val parsedOccurredAt: String? = null,
    val clientTransactionId: String? = null,
    val receivedAt: String? = null,
    val createdAt: String? = null
)

data class NotificationParseLogResponse(
    val id: Long,
    val status: String,
    val receivedAt: String,
    val createdAt: String
)
