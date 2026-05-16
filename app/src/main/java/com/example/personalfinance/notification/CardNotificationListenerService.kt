package com.example.personalfinance.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class CardNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected for test payment notifications.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != packageName) {
            Log.d(TAG, "Ignoring non-test notification.")
            return
        }

        val notification = sbn.notification ?: return
        val title = notification.extractTitle()
        val text = notification.extractText()
        val result = CardNotificationParser.parse(title = title, text = text)

        CardNotificationDebugStore.update(
            sourcePackage = sbn.packageName,
            title = title,
            text = text,
            result = result
        )
        
        if (result.parseStatus == CardNotificationParseStatus.SUCCESS && result.amount != null) {
            val store = com.example.personalfinance.data.UserStatsStore.getInstance(this)
            store.addExpense(result.amount)
            Log.i(TAG, "Added expense ${result.amount} to UserStatsStore")
        }

        Log.i(
            TAG,
            "Parsed test notification: status=${result.parseStatus}, " +
                "amount=${result.amount}, merchant=${result.merchantName}, " +
                "transactionDateTime=${result.transactionDateTime}, title=$title, text=$text"
        )
    }

    private fun Notification.extractTitle(): String =
        extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()

    private fun Notification.extractText(): String {
        val directText = extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)

        if (directText != null) return directText.toString()

        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        return lines?.joinToString(separator = " ") { it.toString() }.orEmpty()
    }

    private companion object {
        const val TAG = "CardNotificationListener"
    }
}
