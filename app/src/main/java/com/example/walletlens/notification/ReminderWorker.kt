package com.example.walletlens.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.walletlens.data.WalletLensDatabase
import com.example.walletlens.data.entity.Reminder
import com.example.walletlens.data.repository.ReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val reminderId = inputData.getLong("reminder_id", -1)
            if (reminderId == -1L) {
                return@withContext Result.failure()
            }
            
            val database = WalletLensDatabase.getDatabase(context)
            val reminderRepository = ReminderRepository(database.reminderDao())
            
            // Get the reminder
            val reminder = reminderRepository.getReminderById(reminderId)
            if (reminder == null || reminder.isCompleted) {
                return@withContext Result.success()
            }
            
            // Check if it's time to show the notification
            val now = LocalDateTime.now()
            if (reminder.dueDate.isAfter(now)) {
                // Not yet due, reschedule for later
                return@withContext Result.retry()
            }
            
            // Show notification
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showPaymentReminder(reminder)
            
            // If it's a recurring reminder, schedule the next one
            if (reminder.isRecurring && reminder.recurringInterval != null) {
                scheduleNextRecurringReminder(reminder)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private suspend fun scheduleNextRecurringReminder(reminder: Reminder) {
        val nextDueDate = when (reminder.recurringInterval?.lowercase()) {
            "daily" -> reminder.dueDate.plusDays(1)
            "weekly" -> reminder.dueDate.plusWeeks(1)
            "monthly" -> reminder.dueDate.plusMonths(1)
            "yearly" -> reminder.dueDate.plusYears(1)
            else -> return
        }
        
        // Update the reminder with the next due date
        val updatedReminder = reminder.copy(dueDate = nextDueDate)
        val database = WalletLensDatabase.getDatabase(context)
        val reminderRepository = ReminderRepository(database.reminderDao())
        reminderRepository.updateReminder(updatedReminder)
        
        // Schedule the next notification
        NotificationScheduler.scheduleReminderNotification(context, updatedReminder)
    }
} 