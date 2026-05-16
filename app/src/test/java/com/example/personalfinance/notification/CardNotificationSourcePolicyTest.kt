package com.example.personalfinance.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardNotificationSourcePolicyTest {
    @Test
    fun isSupported_allowsOwnAppPackageForSampleNotifications() {
        assertTrue(
            CardNotificationSourcePolicy.isSupported(
                packageName = "com.example.personalfinance",
                appPackageName = "com.example.personalfinance"
            )
        )
    }

    @Test
    fun isSupported_allowsKnownPaymentPackage() {
        assertTrue(
            CardNotificationSourcePolicy.isSupported(
                packageName = "viva.republica.toss",
                appPackageName = "com.example.personalfinance"
            )
        )
    }

    @Test
    fun isSupported_rejectsUnknownPackage() {
        assertFalse(
            CardNotificationSourcePolicy.isSupported(
                packageName = "com.example.unknown",
                appPackageName = "com.example.personalfinance"
            )
        )
    }
}
