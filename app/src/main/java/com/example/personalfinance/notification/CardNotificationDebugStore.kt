package com.example.personalfinance.notification

import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CardNotificationDebugStore {
    private val _latestResult = MutableStateFlow<CardNotificationDebugEntry?>(null)
    val latestResult: StateFlow<CardNotificationDebugEntry?> = _latestResult.asStateFlow()

    fun update(
        sourcePackage: String,
        title: String,
        text: String,
        result: CardNotificationParseResult,
        category: String,
        notificationType: PaymentNotificationType,
        handlingStatus: CardNotificationHandlingStatus
    ) {
        _latestResult.value = CardNotificationDebugEntry(
            sourcePackage = sourcePackage,
            title = title,
            text = text,
            result = result,
            category = category,
            notificationType = notificationType,
            handlingStatus = handlingStatus,
            receivedAt = LocalDateTime.now()
        )
    }
}

data class CardNotificationDebugEntry(
    val sourcePackage: String,
    val title: String,
    val text: String,
    val result: CardNotificationParseResult,
    val category: String,
    val notificationType: PaymentNotificationType,
    val handlingStatus: CardNotificationHandlingStatus,
    val receivedAt: LocalDateTime
)
