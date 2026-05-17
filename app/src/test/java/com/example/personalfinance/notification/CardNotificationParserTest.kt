package com.example.personalfinance.notification

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardNotificationParserTest {
    private val referenceDate = LocalDate.of(2026, 5, 13)

    @Test
    fun parse_extractsAmountMerchantAndRawNotificationFields() {
        val samples = listOf(
            Sample(
                title = "KB국민카드",
                text = "05/13 14:22 스타벅스 12,300원 승인",
                amount = 12300L,
                merchantName = "스타벅스",
                transactionDateTime = LocalDateTime.of(2026, 5, 13, 14, 22)
            ),
            Sample(
                title = "카카오페이",
                text = "[카카오페이] 8,500원 결제 CU편의점",
                amount = 8500L,
                merchantName = "CU편의점"
            ),
            Sample(
                title = "신한카드",
                text = "체크승인 23,000원 맥도날드",
                amount = 23000L,
                merchantName = "맥도날드"
            ),
            Sample(
                title = "현대카드",
                text = "일시불 승인 45,600원 배달의민족",
                amount = 45600L,
                merchantName = "배달의민족"
            ),
            Sample(
                title = "토스페이",
                text = "토스페이 9,900원 결제 네이버플러스",
                amount = 9900L,
                merchantName = "네이버플러스"
            )
        )

        samples.forEach { sample ->
            val result = CardNotificationParser.parse(
                title = sample.title,
                text = sample.text,
                referenceDate = referenceDate
            )

            assertEquals(sample.amount, result.amount)
            assertEquals(sample.merchantName, result.merchantName)
            assertTrue(result.merchantName.isNotBlank())
            assertEquals(sample.title, result.rawTitle)
            assertEquals(sample.text, result.rawText)
            assertEquals(CardNotificationParseStatus.SUCCESS, result.parseStatus)
            assertEquals(sample.transactionDateTime, result.transactionDateTime)
        }
    }

    @Test
    fun parse_returnsFailedWhenAmountIsMissing() {
        val result = CardNotificationParser.parse(
            title = "KB국민카드",
            text = "05/13 14:22 스타벅스 승인",
            referenceDate = referenceDate
        )

        assertEquals(null, result.amount)
        assertEquals("", result.merchantName)
        assertEquals("KB국민카드", result.rawTitle)
        assertEquals("05/13 14:22 스타벅스 승인", result.rawText)
        assertEquals(CardNotificationParseStatus.FAILED, result.parseStatus)
        assertEquals(null, result.transactionDateTime)
    }

    @Test
    fun parseStatusSeparatesSuccessAndFailure() {
        val success = CardNotificationParser.parse(
            title = "신한카드",
            text = "체크승인 23,000원 맥도날드",
            referenceDate = referenceDate
        )
        val failure = CardNotificationParser.parse(
            title = "신한카드",
            text = "체크승인 맥도날드",
            referenceDate = referenceDate
        )

        assertEquals(CardNotificationParseStatus.SUCCESS, success.parseStatus)
        assertNotNull(success.amount)
        assertEquals(CardNotificationParseStatus.FAILED, failure.parseStatus)
        assertEquals(null, failure.amount)
    }

    @Test
    fun parse_extractsMerchantAfterPipeAndIgnoresTossBankBalance() {
        val result = CardNotificationParser.parse(
            title = "토스뱅크",
            text = """
                5,000원 결제
                토스뱅크 체크카드 | 네이버파이낸셜
                잔액 75,801원
            """.trimIndent(),
            referenceDate = referenceDate
        )

        assertEquals(5_000L, result.amount)
        assertEquals("네이버파이낸셜", result.merchantName)
        assertEquals(CardNotificationParseStatus.SUCCESS, result.parseStatus)
        assertFalse(result.merchantName.contains("잔액"))
        assertFalse(result.merchantName.contains("75,801"))
        assertFalse(result.merchantName.contains("토스뱅크"))
        assertFalse(result.merchantName.contains("체크카드"))
    }

    @Test
    fun parse_ignoresBalanceAmountWhenBalanceAppearsBeforePaymentAmount() {
        val result = CardNotificationParser.parse(
            title = "토스뱅크",
            text = """
                잔액 75,801원
                5,000원 결제
                토스뱅크 체크카드 | 네이버파이낸셜
            """.trimIndent(),
            referenceDate = referenceDate
        )

        assertEquals(5_000L, result.amount)
        assertFalse(result.amount == 75_801L)
        assertEquals("네이버파이낸셜", result.merchantName)
        assertEquals(CardNotificationParseStatus.SUCCESS, result.parseStatus)
    }

    private data class Sample(
        val title: String,
        val text: String,
        val amount: Long,
        val merchantName: String,
        val transactionDateTime: LocalDateTime? = null
    )
}
