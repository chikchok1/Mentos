package com.mentos.backend.service

import com.mentos.backend.dto.NotificationParseLogResponse
import com.mentos.backend.dto.SaveNotificationParseLogRequest
import com.mentos.backend.entity.NotificationParseLog
import com.mentos.backend.repository.NotificationParseLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationParseLogService(
    private val notificationParseLogRepository: NotificationParseLogRepository
) {
    @Transactional
    fun save(userId: Long, req: SaveNotificationParseLogRequest): NotificationParseLogResponse {
        require(req.status in allowedStatuses) { "지원하지 않는 알림 로그 상태입니다. status=${req.status}" }
        val now = LocalDateTime.now()

        val entity = NotificationParseLog(
            userId = userId,
            diagnosticId = req.diagnosticId.take(512),
            packageName = req.packageName.take(128),
            title = req.title?.take(512),
            rawText = req.rawText,
            status = req.status,
            failureReason = req.failureReason?.take(512),
            parsedAmount = req.parsedAmount,
            parsedMerchant = req.parsedMerchant?.take(255),
            parsedOccurredAt = req.parsedOccurredAt.parseDateTimeOrNull(),
            clientTransactionId = req.clientTransactionId?.take(512),
            receivedAt = req.receivedAt.parseDateTimeOrDefault(now),
            createdAt = req.createdAt.parseDateTimeOrDefault(now)
        )

        val saved = notificationParseLogRepository.save(entity)
        return NotificationParseLogResponse(
            id = saved.id,
            status = saved.status,
            receivedAt = saved.receivedAt.toString(),
            createdAt = saved.createdAt.toString()
        )
    }

    private fun String?.parseDateTimeOrDefault(defaultValue: LocalDateTime): LocalDateTime =
        this?.let { value -> runCatching { LocalDateTime.parse(value) }.getOrNull() }
            ?: defaultValue

    private fun String?.parseDateTimeOrNull(): LocalDateTime? =
        this?.let { value -> runCatching { LocalDateTime.parse(value) }.getOrNull() }

    private val allowedStatuses = setOf(
        "RECEIVED",
        "PARSE_SUCCESS",
        "PARSE_FAILED",
        "DUPLICATED",
        "IGNORED_NOT_PAYMENT",
        "UNSUPPORTED_FORMAT"
    )
}
