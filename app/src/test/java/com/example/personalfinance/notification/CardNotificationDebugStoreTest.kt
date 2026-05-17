package com.example.personalfinance.notification

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CardNotificationDebugStoreTest {
    @Before
    fun setUp() {
        CardNotificationDebugStore.resetForTest()
    }

    @Test
    fun recordDiagnostic_omitsRawPreviewForUnsupportedPackage() {
        CardNotificationDebugStore.recordDiagnostic(
            diagnosticId = "unsupported-1",
            packageName = "com.example.unknown",
            title = "일반 알림",
            status = CardNotificationDiagnosticStatus.IGNORED_PACKAGE,
            reason = "허용되지 않은 패키지",
            handled = true,
            rawText = "민감할 수 있는 본문 전체",
            allowRawTextPreview = false
        )

        val entry = CardNotificationDebugStore.recentDiagnostics.value.single()

        assertEquals("com.example.unknown", entry.packageName)
        assertEquals(CardNotificationDiagnosticStatus.IGNORED_PACKAGE, entry.status)
        assertEquals("허용되지 않은 패키지", entry.reason)
        assertNull(entry.rawTextPreview)
    }

    @Test
    fun recordDiagnostic_recordsParseFailureAndDuplicateStatuses() {
        CardNotificationDebugStore.recordDiagnostic(
            diagnosticId = "payment-1",
            packageName = "viva.republica.toss",
            title = "토스뱅크",
            status = CardNotificationDiagnosticStatus.PARSE_FAILED,
            reason = "금액 파싱 실패",
            handled = true,
            rawText = "토스뱅크 결제 알림",
            allowRawTextPreview = true
        )
        CardNotificationDebugStore.recordDiagnostic(
            diagnosticId = "payment-2",
            packageName = "viva.republica.toss",
            title = "토스뱅크",
            status = CardNotificationDiagnosticStatus.DUPLICATE_IGNORED,
            reason = "중복 알림",
            handled = true,
            rawText = "5,000원 결제 토스뱅크 체크카드",
            allowRawTextPreview = true
        )

        val entries = CardNotificationDebugStore.recentDiagnostics.value

        assertTrue(entries.any { it.status == CardNotificationDiagnosticStatus.PARSE_FAILED })
        assertTrue(entries.any { it.status == CardNotificationDiagnosticStatus.DUPLICATE_IGNORED })
    }

    @Test
    fun recordDiagnostic_nonPaymentDoesNotOverwriteLatestParseResult() {
        val latestResult = CardNotificationParseResult(
            amount = 100L,
            merchantName = "네이버파이낸셜",
            transactionDateTime = null,
            rawTitle = "100원 결제",
            rawText = "100원 결제 토스뱅크 체크카드 | 네이버파이낸셜",
            parseStatus = CardNotificationParseStatus.SUCCESS
        )
        CardNotificationDebugStore.update(
            sourcePackage = "viva.republica.toss",
            title = "100원 결제",
            text = "100원 결제 토스뱅크 체크카드 | 네이버파이낸셜",
            result = latestResult,
            category = "쇼핑/온라인",
            notificationType = PaymentNotificationType.APPROVED,
            handlingStatus = CardNotificationHandlingStatus.APPROVED_RECORDED
        )
        val before = CardNotificationDebugStore.latestResult.value

        CardNotificationDebugStore.recordDiagnostic(
            diagnosticId = "non-payment-1",
            packageName = "viva.republica.toss",
            title = "결제퀴즈 도착",
            status = CardNotificationDiagnosticStatus.IGNORED_NON_PAYMENT,
            reason = "결제 알림 아님",
            handled = true,
            rawText = "결제퀴즈 도착 양진원님 정답을 맞힐 수 있어요.",
            allowRawTextPreview = true
        )

        assertSame(before, CardNotificationDebugStore.latestResult.value)
        assertEquals(
            CardNotificationDiagnosticStatus.IGNORED_NON_PAYMENT,
            CardNotificationDebugStore.recentDiagnostics.value.first().status
        )
    }

    @Test
    fun visibleEntries_excludesIgnoredStatusesByDefault() {
        val entries = listOf(
            diagnostic(CardNotificationDiagnosticStatus.IGNORED_PACKAGE),
            diagnostic(CardNotificationDiagnosticStatus.IGNORED_NON_PAYMENT),
            diagnostic(CardNotificationDiagnosticStatus.APPROVED_RECORDED)
        )

        val visible = CardNotificationDiagnosticFilter.visibleEntries(
            entries = entries,
            includeIgnored = false
        )

        assertEquals(
            listOf(CardNotificationDiagnosticStatus.APPROVED_RECORDED),
            visible.map { it.status }
        )
    }

    @Test
    fun visibleEntries_includesIgnoredStatusesWhenEnabled() {
        val entries = listOf(
            diagnostic(CardNotificationDiagnosticStatus.IGNORED_PACKAGE),
            diagnostic(CardNotificationDiagnosticStatus.IGNORED_NON_PAYMENT),
            diagnostic(CardNotificationDiagnosticStatus.APPROVED_RECORDED)
        )

        val visible = CardNotificationDiagnosticFilter.visibleEntries(
            entries = entries,
            includeIgnored = true
        )

        assertEquals(entries.map { it.status }, visible.map { it.status })
    }

    private fun diagnostic(status: CardNotificationDiagnosticStatus): CardNotificationDiagnosticEntry =
        CardNotificationDiagnosticEntry(
            diagnosticId = status.name,
            packageName = "viva.republica.toss",
            title = status.name,
            receivedAt = LocalDateTime.of(2026, 5, 17, 12, 0),
            handled = true,
            status = status,
            reason = status.name,
            rawTextPreview = null
        )
}
