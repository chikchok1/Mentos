package com.example.personalfinance.notification

import com.example.personalfinance.data.ExpenseCategoryClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentNotificationAnalyzerTest {
    @Test
    fun analyze_approvesStarbucksPayment() {
        val analysis = PaymentNotificationAnalyzer.analyze(
            sourcePackage = "com.example.personalfinance",
            title = "신한카드",
            text = "스타벅스 5,000원 승인"
        )

        assertEquals(PaymentNotificationAnalysisStatus.APPROVED, analysis.status)
        assertEquals(5_000L, analysis.parseResult.amount)
        assertEquals("스타벅스", analysis.parseResult.merchantName)
        assertEquals(ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE, analysis.category)
        assertEquals(10, analysis.earnedXP)
        assertTrue(analysis.isRecordable)
    }

    @Test
    fun analyze_approvesConvenienceStoreUsage() {
        val analysis = PaymentNotificationAnalyzer.analyze(
            sourcePackage = "com.example.personalfinance",
            title = "카드 알림",
            text = "CU 3,200원 사용"
        )

        assertEquals(PaymentNotificationAnalysisStatus.APPROVED, analysis.status)
        assertEquals(3_200L, analysis.parseResult.amount)
        assertEquals("CU", analysis.parseResult.merchantName)
        assertEquals(ExpenseCategoryClassifier.CATEGORY_LIVING_MART, analysis.category)
    }

    @Test
    fun analyze_approvesOnlineShoppingPayment() {
        val analysis = PaymentNotificationAnalyzer.analyze(
            sourcePackage = "com.example.personalfinance",
            title = "카드 알림",
            text = "쿠팡 25,000원 결제"
        )

        assertEquals(PaymentNotificationAnalysisStatus.APPROVED, analysis.status)
        assertEquals(25_000L, analysis.parseResult.amount)
        assertEquals("쿠팡", analysis.parseResult.merchantName)
        assertEquals(ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE, analysis.category)
    }

    @Test
    fun analyze_approvesTossBankNaverFinancialPayment() {
        val analysis = PaymentNotificationAnalyzer.analyze(
            sourcePackage = "viva.republica.toss",
            title = "토스뱅크",
            text = """
                5,000원 결제
                토스뱅크 체크카드 | 네이버파이낸셜
                잔액 75,801원
            """.trimIndent()
        )

        assertEquals(PaymentNotificationAnalysisStatus.APPROVED, analysis.status)
        assertEquals(5_000L, analysis.parseResult.amount)
        assertEquals("네이버파이낸셜", analysis.parseResult.merchantName)
        assertEquals(ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE, analysis.category)
        assertTrue(analysis.isRecordable)
    }

    @Test
    fun analyze_classifiesNetflixSubscriptionByCurrentKeywordPriority() {
        val analysis = PaymentNotificationAnalyzer.analyze(
            sourcePackage = "com.example.personalfinance",
            title = "카드 알림",
            text = "넷플릭스 17,000원 정기결제"
        )

        assertEquals(PaymentNotificationAnalysisStatus.APPROVED, analysis.status)
        assertEquals(17_000L, analysis.parseResult.amount)
        assertEquals("넷플릭스", analysis.parseResult.merchantName)
        assertEquals(ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE, analysis.category)
    }

    @Test
    fun analyze_marksCanceledPaymentAsNotRecordable() {
        val analysis = PaymentNotificationAnalyzer.analyze(
            sourcePackage = "com.example.personalfinance",
            title = "신한카드",
            text = "스타벅스 5,000원 승인취소"
        )

        assertEquals(PaymentNotificationAnalysisStatus.CANCELED, analysis.status)
        assertEquals(5_000L, analysis.parseResult.amount)
        assertFalse(analysis.isRecordable)
    }

    @Test
    fun analyze_marksUnknownNotificationAsParseFailed() {
        val analysis = PaymentNotificationAnalyzer.analyze(
            sourcePackage = "com.example.personalfinance",
            title = "카드 알림",
            text = "알 수 없는 알림입니다"
        )

        assertEquals(PaymentNotificationAnalysisStatus.PARSE_FAILED, analysis.status)
        assertEquals(null, analysis.parseResult.amount)
        assertFalse(analysis.isRecordable)
    }
}
