package com.example.personalfinance.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.personalfinance.data.ExpenseCategoryClassifier

class CardNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected for test payment notifications.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!CardNotificationSourcePolicy.isSupported(sbn.packageName, packageName)) {
            Log.d(TAG, "Ignoring unsupported notification package=${sbn.packageName}")
            return
        }

        val notification = sbn.notification ?: return
        val content = notification.extractContent()
        val result = runCatching {
            CardNotificationParser.parse(title = content.title, text = content.text)
        }.getOrElse { error ->
            Log.w(TAG, "Failed to parse supported payment notification.", error)
            CardNotificationParseResult(
                amount = null,
                merchantName = "",
                transactionDateTime = null,
                rawTitle = content.title,
                rawText = content.text,
                parseStatus = CardNotificationParseStatus.FAILED
            )
        }
        val notificationType = if (
            sbn.packageName == packageName &&
            result.parseStatus == CardNotificationParseStatus.SUCCESS
        ) {
            PaymentNotificationType.APPROVED
        } else {
            PaymentNotificationClassifier.classify(
                title = content.title,
                text = content.text
            )
        }
        val category = ExpenseCategoryClassifier.classify(
            merchantName = result.merchantName,
            rawText = content.text
        )

        val handlingStatus = if (
            result.parseStatus == CardNotificationParseStatus.SUCCESS &&
            result.amount != null &&
            notificationType == PaymentNotificationType.APPROVED
        ) {
            val processedKey = ProcessedNotificationKey(
                packageName = sbn.packageName,
                postTime = sbn.postTime,
                amount = result.amount,
                merchantName = result.merchantName
            )

            if (markProcessed(processedKey)) {
                val store = com.example.personalfinance.data.UserStatsStore.getInstance(this)
                store.addExpense(result.amount, category)
                Log.i(TAG, "Added expense ${result.amount} to UserStatsStore with category=$category")
                CardNotificationHandlingStatus.APPROVED_RECORDED
            } else {
                Log.i(TAG, "Ignoring duplicate parsed payment notification: $processedKey")
                CardNotificationHandlingStatus.DUPLICATE_IGNORED
            }
        } else {
            when {
                notificationType == PaymentNotificationType.CANCELED -> {
                    Log.i(TAG, "Ignoring canceled payment notification package=${sbn.packageName}")
                    CardNotificationHandlingStatus.CANCELED_IGNORED
                }
                result.parseStatus == CardNotificationParseStatus.FAILED || result.amount == null -> {
                    Log.i(TAG, "Payment notification parsing failed package=${sbn.packageName}")
                    CardNotificationHandlingStatus.PARSE_FAILED
                }
                else -> {
                    Log.i(TAG, "Payment notification needs review package=${sbn.packageName}")
                    CardNotificationHandlingStatus.NEEDS_REVIEW
                }
            }
        }

        CardNotificationDebugStore.update(
            sourcePackage = sbn.packageName,
            title = content.title,
            text = content.text,
            result = result,
            category = category,
            notificationType = notificationType,
            handlingStatus = handlingStatus
        )

        Log.i(
            TAG,
            "Handled payment notification: handlingStatus=$handlingStatus, " +
                "notificationType=$notificationType, parseStatus=${result.parseStatus}, " +
                "amount=${result.amount}, merchant=${result.merchantName}, " +
                "category=$category, transactionDateTime=${result.transactionDateTime}"
        )
    }

    private fun Notification.extractContent(): ExtractedNotificationContent {
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val parts = LinkedHashSet<String>()

        fun addPart(value: CharSequence?) {
            val text = value?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) {
                parts.add(text)
            }
        }

        addPart(extras.getCharSequence(Notification.EXTRA_TITLE))
        addPart(extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
        addPart(extras.getCharSequence(Notification.EXTRA_TEXT))
        addPart(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        addPart(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        addPart(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        addPart(extras.getCharSequence(Notification.EXTRA_INFO_TEXT))
        addPart(tickerText)
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.forEach { line -> addPart(line) }

        return ExtractedNotificationContent(
            title = title,
            text = parts.joinToString(separator = " ")
        )
    }

    private data class ExtractedNotificationContent(
        val title: String,
        val text: String
    )

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
