package com.example.personalfinance.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentNotificationClassifierTest {
    @Test
    fun classify_returnsCanceledWhenCancelKeywordExists() {
        assertEquals(
            PaymentNotificationType.CANCELED,
            PaymentNotificationClassifier.classify(
                title = "카드 승인취소",
                text = "05/13 14:22 스타벅스 12,300원"
            )
        )
    }

    @Test
    fun classify_cancelKeywordTakesPriorityOverApprovalKeyword() {
        assertEquals(
            PaymentNotificationType.CANCELED,
            PaymentNotificationClassifier.classify(
                title = "결제취소",
                text = "신용카드 결제 12,300원 취소"
            )
        )
    }

    @Test
    fun classify_returnsApprovedWhenApprovalKeywordExists() {
        assertEquals(
            PaymentNotificationType.APPROVED,
            PaymentNotificationClassifier.classify(
                title = "카드 승인",
                text = "05/13 14:22 스타벅스 12,300원"
            )
        )
    }

    @Test
    fun classify_returnsNeedsReviewWhenTypeIsUnclear() {
        assertEquals(
            PaymentNotificationType.NEEDS_REVIEW,
            PaymentNotificationClassifier.classify(
                title = "카드 알림",
                text = "05/13 14:22 스타벅스 12,300원"
            )
        )
    }
}
