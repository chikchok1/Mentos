package com.example.personalfinance.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.personalfinance.R

object SamplePaymentNotification {
    const val SAMPLE_TITLE = "Mentos Test Card"

    private val merchants = listOf("스타벅스", "맥도날드", "이마트", "올리브영", "다이소", "쿠팡", "배달의민족")

    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context): Boolean {
        if (!canPostNotifications(context)) return false

        ensureChannel(context)

        // Generate random amount between 1,000 and 50,000
        val randomAmount = (1..50).random() * 1000
        val randomMerchant = merchants.random()
        val formattedAmount = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(randomAmount)
        val dynamicText = "05/13 14:22 ${formattedAmount}원 $randomMerchant"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(SAMPLE_TITLE)
            .setContentText(dynamicText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(dynamicText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(SAMPLE_NOTIFICATION_ID, notification)
        return true
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.sample_payment_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.sample_payment_notification_channel_description)
        }

        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private const val CHANNEL_ID = "sample_payment_notifications"
    private const val SAMPLE_NOTIFICATION_ID = 20260513
}
