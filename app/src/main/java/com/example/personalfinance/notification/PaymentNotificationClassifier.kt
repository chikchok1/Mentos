package com.example.personalfinance.notification

import java.util.Locale

object PaymentNotificationClassifier {
    private val cancelKeywords = listOf(
        "취소",
        "승인취소",
        "결제취소",
        "환불",
        "매출취소"
    )

    private val approvalKeywords = listOf(
        "승인",
        "결제",
        "사용",
        "체크카드",
        "신용카드"
    )

    fun classify(title: String, text: String): PaymentNotificationType {
        val normalized = normalize("$title $text")

        if (cancelKeywords.any { keyword -> normalized.contains(normalize(keyword)) }) {
            return PaymentNotificationType.CANCELED
        }

        if (approvalKeywords.any { keyword -> normalized.contains(normalize(keyword)) }) {
            return PaymentNotificationType.APPROVED
        }

        return PaymentNotificationType.NEEDS_REVIEW
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT).filterNot { it.isWhitespace() }
}

enum class PaymentNotificationType {
    APPROVED,
    CANCELED,
    NEEDS_REVIEW
}

enum class CardNotificationHandlingStatus {
    APPROVED_RECORDED,
    CANCELED_IGNORED,
    PARSE_FAILED,
    NEEDS_REVIEW,
    DUPLICATE_IGNORED
}
