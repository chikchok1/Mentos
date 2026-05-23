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

    // 가결제(선결제) 패턴 — 승인으로 오판되지 않도록 먼저 제외
    // 카카오T: "가결제", "제목_가", "제목_O" (옵로드스타일)
    // 다양한 택시 앱 가결제 패턴 포함
    private val preAuthKeywords = listOf(
        "가결제",
        "_가",      // 카카오T 제목 접미사
        "_o",      // 카카오T 영어 소문자 (대소문자 무관)
        "pre-auth",
        "preauth"
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

        // 가결제 패턴 매칭 시 NEEDS_REVIEW로 처리 — 기록하지 않음
        if (preAuthKeywords.any { keyword -> normalized.contains(normalize(keyword)) }) {
            return PaymentNotificationType.NEEDS_REVIEW
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
