package com.example.personalfinance.notification

import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CardNotificationDebugStore {
    private val _latestResult = MutableStateFlow<CardNotificationDebugEntry?>(null)
    val latestResult: StateFlow<CardNotificationDebugEntry?> = _latestResult.asStateFlow()

    private val _recentDiagnostics = MutableStateFlow<List<CardNotificationDiagnosticEntry>>(emptyList())
    val recentDiagnostics: StateFlow<List<CardNotificationDiagnosticEntry>> =
        _recentDiagnostics.asStateFlow()

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

    fun recordDiagnostic(
        diagnosticId: String,
        packageName: String,
        title: String,
        status: CardNotificationDiagnosticStatus,
        reason: String,
        handled: Boolean,
        rawText: String? = null,
        allowRawTextPreview: Boolean = false
    ) {
        val entry = CardNotificationDiagnosticEntry(
            diagnosticId = diagnosticId,
            packageName = packageName,
            title = title,
            receivedAt = LocalDateTime.now(),
            handled = handled,
            status = status,
            reason = reason,
            rawTextPreview = if (allowRawTextPreview) rawText.toPreview() else null
        )

        _recentDiagnostics.value = (
            listOf(entry) + _recentDiagnostics.value.filterNot { it.diagnosticId == diagnosticId }
        ).take(MAX_RECENT_DIAGNOSTICS)
    }

    internal fun resetForTest() {
        _latestResult.value = null
        _recentDiagnostics.value = emptyList()
    }

    private fun String?.toPreview(): String? {
        val value = this?.replace(Regex("""\s+"""), " ")?.trim().orEmpty()
        if (value.isBlank()) return null

        return if (value.length > RAW_TEXT_PREVIEW_MAX_LENGTH) {
            value.take(RAW_TEXT_PREVIEW_MAX_LENGTH) + "..."
        } else {
            value
        }
    }

    private const val MAX_RECENT_DIAGNOSTICS = 20
    private const val RAW_TEXT_PREVIEW_MAX_LENGTH = 160
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

data class CardNotificationDiagnosticEntry(
    val diagnosticId: String,
    val packageName: String,
    val title: String,
    val receivedAt: LocalDateTime,
    val handled: Boolean,
    val status: CardNotificationDiagnosticStatus,
    val reason: String,
    val rawTextPreview: String?
)

enum class CardNotificationDiagnosticStatus {
    RECEIVED,
    IGNORED_PACKAGE,
    PARSE_FAILED,
    NEEDS_REVIEW,
    CANCELED,
    DUPLICATE_IGNORED,
    APPROVED_RECORDED
}
