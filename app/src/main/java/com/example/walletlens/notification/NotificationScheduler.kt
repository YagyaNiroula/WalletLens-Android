package com.example.walletlens.notification

import android.content.Context
import androidx.work.*
import com.example.walletlens.data.entity.Reminder
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    
    fun scheduleReminderNotification(context: Context, reminder: Reminder) {
        val now = LocalDateTime.now()
        val delay = Duration.between(now, reminder.dueDate)
        
        if (delay.isNegative) {
            // Reminder is already due, show notification immediately
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showPaymentReminder(reminder)
            return
        }
        
        val inputData = Data.Builder()
            .putLong("reminder_id", reminder.id)
            .build()
        
        val reminderWork = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .addTag("reminder_${reminder.id}")
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "reminder_${reminder.id}",
                ExistingWorkPolicy.REPLACE,
                reminderWork
            )
    }
    
    fun scheduleBudgetWarningCheck(context: Context) {
        // Schedule daily budget warning checks
        val budgetWork = PeriodicWorkRequestBuilder<BudgetWarningWorker>(
            1, TimeUnit.DAYS
        )
            .addTag("budget_warning_check")
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "budget_warning_check",
                ExistingPeriodicWorkPolicy.KEEP,
                budgetWork
            )
    }
    
    fun cancelReminderNotification(context: Context, reminderId: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("reminder_$reminderId")
    }
    
    fun cancelAllReminderNotifications(context: Context) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("reminder")
    }
    
    fun cancelBudgetWarningCheck(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("budget_warning_check")
    }
    
    fun scheduleImmediateBudgetCheck(context: Context) {
        val budgetWork = OneTimeWorkRequestBuilder<BudgetWarningWorker>()
            .addTag("immediate_budget_check")
            .build()
        
        WorkManager.getInstance(context)
            .enqueue(budgetWork)
    }
} 