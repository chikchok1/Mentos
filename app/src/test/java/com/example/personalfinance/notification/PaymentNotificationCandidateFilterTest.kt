package com.example.personalfinance.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentNotificationCandidateFilterTest {
    @Test
    fun isCandidate_acceptsTossPaymentNotification() {
        assertTrue(
            PaymentNotificationCandidateFilter.isCandidate(
                title = "100원 결제",
                rawText = """
                    100원 결제
                    토스뱅크 체크카드 | 네이버파이낸셜
                    잔액75,701원 토스뱅크
                """.trimIndent()
            )
        )
    }

    @Test
    fun isCandidate_rejectsTossPaymentQuizNotificationWithoutAmount() {
        assertFalse(
            PaymentNotificationCandidateFilter.isCandidate(
                title = "결제퀴즈 도착",
                rawText = "결제퀴즈 도착 양진원님 정답을 맞힐 수 있어요."
            )
        )
    }

    @Test
    fun isCandidate_rejectsBlankRawText() {
        assertFalse(
            PaymentNotificationCandidateFilter.isCandidate(
                title = "",
                rawText = ""
            )
        )
    }
}
