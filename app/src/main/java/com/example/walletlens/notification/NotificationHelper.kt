package com.example.walletlens.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.walletlens.MainActivity
import com.example.walletlens.R
import com.example.walletlens.data.entity.Reminder
import com.example.walletlens.data.entity.Budget

class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID_REMINDERS = "payment_reminders"
        const val CHANNEL_ID_BUDGET_WARNINGS = "budget_warnings"
        const val CHANNEL_ID_GENERAL = "general_notifications"
        
        const val NOTIFICATION_ID_REMINDER = 1001
        const val NOTIFICATION_ID_BUDGET_WARNING = 1002
        const val NOTIFICATION_ID_GENERAL = 1003
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Payment Reminders Channel
            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Payment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming bill payments and reminders"
                enableLights(true)
                enableVibration(true)
            }
            
            // Budget Warnings Channel
            val budgetChannel = NotificationChannel(
                CHANNEL_ID_BUDGET_WARNINGS,
                "Budget Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for budget overspending and warnings"
                enableLights(true)
                enableVibration(false)
            }
            
            // General Notifications Channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "General app notifications"
                enableLights(false)
                enableVibration(false)
            }
            
            notificationManager.createNotificationChannels(listOf(remindersChannel, budgetChannel, generalChannel))
        }
    }
    
    fun showPaymentReminder(reminder: Reminder) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Payment Reminder")
            .setContentText("${reminder.title}: $${String.format("%.2f", reminder.amount)}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${reminder.title}: $${String.format("%.2f", reminder.amount)}\n${reminder.description}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        NotificationManagerCompat.from(context).notify(
            (NOTIFICATION_ID_REMINDER + reminder.id).toInt(),
            notification
        )
    }
    
    fun showBudgetWarning(budget: Budget, currentSpending: Double, percentageUsed: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            budget.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val warningMessage = when {
            percentageUsed >= 100 -> "Budget exceeded!"
            percentageUsed >= 90 -> "Budget almost exceeded!"
            percentageUsed >= 80 -> "Budget warning!"
            else -> "Budget update"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET_WARNINGS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(warningMessage)
            .setContentText("${budget.category}: $${String.format("%.2f", currentSpending)} / $${String.format("%.2f", budget.amount)}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${budget.category}: $${String.format("%.2f", currentSpending)} / $${String.format("%.2f", budget.amount)}\n${String.format("%.1f", percentageUsed)}% of budget used"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        NotificationManagerCompat.from(context).notify(
            (NOTIFICATION_ID_BUDGET_WARNING + budget.id).toInt(),
            notification
        )
    }
    
    fun showGeneralNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_GENERAL, notification)
    }
} 