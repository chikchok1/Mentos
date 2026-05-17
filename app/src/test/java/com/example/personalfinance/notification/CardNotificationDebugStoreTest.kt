package com.example.personalfinance.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
