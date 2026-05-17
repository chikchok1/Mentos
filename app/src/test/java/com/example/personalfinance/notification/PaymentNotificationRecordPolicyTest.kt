package com.example.personalfinance.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentNotificationRecordPolicyTest {
    @Test
    fun shouldRecord_allowsOnlySuccessfulApprovedPaymentWithAmount() {
        assertTrue(
            PaymentNotificationRecordPolicy.shouldRecord(
                result = parseResult(
                    amount = 5_000L,
                    parseStatus = CardNotificationParseStatus.SUCCESS
                ),
                notificationType = PaymentNotificationType.APPROVED
            )
        )
    }

    @Test
    fun shouldRecord_rejectsCanceledNeedsReviewAndParseFailedPayments() {
        assertFalse(
            PaymentNotificationRecordPolicy.shouldRecord(
                result = parseResult(
                    amount = 5_000L,
                    parseStatus = CardNotificationParseStatus.SUCCESS
                ),
                notificationType = PaymentNotificationType.CANCELED
            )
        )
        assertFalse(
            PaymentNotificationRecordPolicy.shouldRecord(
                result = parseResult(
                    amount = 5_000L,
                    parseStatus = CardNotificationParseStatus.SUCCESS
                ),
                notificationType = PaymentNotificationType.NEEDS_REVIEW
            )
        )
        assertFalse(
            PaymentNotificationRecordPolicy.shouldRecord(
                result = parseResult(
                    amount = null,
                    parseStatus = CardNotificationParseStatus.FAILED
                ),
                notificationType = PaymentNotificationType.APPROVED
            )
        )
    }

    private fun parseResult(
        amount: Long?,
        parseStatus: CardNotificationParseStatus
    ): CardNotificationParseResult =
        CardNotificationParseResult(
            amount = amount,
            merchantName = "스타벅스",
            transactionDateTime = null,
            rawTitle = "신한카드",
            rawText = "스타벅스 5,000원 승인",
            parseStatus = parseStatus
        )
}
