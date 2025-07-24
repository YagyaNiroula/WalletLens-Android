package com.example.walletlens.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.walletlens.R
import com.example.walletlens.data.WalletLensDatabase
import com.example.walletlens.data.entity.Reminder
import java.time.LocalDateTime

class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val CHANNEL_ID = "wallet_lens_channel"
        const val CHANNEL_NAME = "WalletLens Notifications"
        const val REMINDER_ID = "reminder_id"
        const val REMINDER_TITLE = "reminder_title"
        const val REMINDER_DESCRIPTION = "reminder_description"
        const val REMINDER_AMOUNT = "reminder_amount"
    }

    override fun doWork(): Result {
        val reminderId = inputData.getLong(REMINDER_ID, -1)
        val title = inputData.getString(REMINDER_TITLE) ?: "Payment Reminder"
        val description = inputData.getString(REMINDER_DESCRIPTION) ?: "You have a payment due"
        val amount = inputData.getDouble(REMINDER_AMOUNT, 0.0)

        createNotificationChannel()
        showNotification(reminderId.toInt(), title, description, amount)

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "WalletLens payment reminders and budget alerts"
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(id: Int, title: String, description: String, amount: Double) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$description - Amount: $${String.format("%.2f", amount)}")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(id, notification)
    }
} 