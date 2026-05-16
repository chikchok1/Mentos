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
            val processedKey = ProcessedNotificationKey(
                packageName = sbn.packageName,
                postTime = sbn.postTime,
                amount = result.amount,
                merchantName = result.merchantName
            )

            if (markProcessed(processedKey)) {
                val store = com.example.personalfinance.data.UserStatsStore.getInstance(this)
                store.addExpense(result.amount)
                Log.i(TAG, "Added expense ${result.amount} to UserStatsStore")
            } else {
                Log.i(TAG, "Ignoring duplicate parsed payment notification: $processedKey")
            }
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

    private data class ProcessedNotificationKey(
        val packageName: String,
        val postTime: Long,
        val amount: Long,
        val merchantName: String
    )

    private companion object {
        const val TAG = "CardNotificationListener"
        const val MAX_PROCESSED_NOTIFICATION_KEYS = 100

        val processedNotificationKeys = LinkedHashSet<ProcessedNotificationKey>()

        fun markProcessed(key: ProcessedNotificationKey): Boolean =
            synchronized(processedNotificationKeys) {
                if (!processedNotificationKeys.add(key)) {
                    return@synchronized false
                }

                if (processedNotificationKeys.size > MAX_PROCESSED_NOTIFICATION_KEYS) {
                    val iterator = processedNotificationKeys.iterator()
                    if (iterator.hasNext()) {
                        iterator.next()
                        iterator.remove()
                    }
                }

                true
            }
    }
}
