package com.example.walletlens.data.repository

import androidx.lifecycle.LiveData
import com.example.walletlens.data.dao.ReminderDao
import com.example.walletlens.data.entity.Reminder
import java.time.LocalDateTime

class ReminderRepository(private val reminderDao: ReminderDao) {
    
    fun getAllActiveReminders(): LiveData<List<Reminder>> {
        return reminderDao.getAllActiveReminders()
    }
    
    fun getRemindersByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): LiveData<List<Reminder>> {
        return reminderDao.getRemindersByDateRange(startDate, endDate)
    }
    
    fun getRemindersByCategory(category: String): LiveData<List<Reminder>> {
        return reminderDao.getRemindersByCategory(category)
    }
    
    suspend fun insertReminder(reminder: Reminder) {
        reminderDao.insertReminder(reminder)
    }
    
    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
    }
    
    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.deleteReminder(reminder)
    }
    
    suspend fun markReminderAsCompleted(id: Long) {
        reminderDao.markReminderAsCompleted(id)
    }
    
    // Direct data access methods for ViewModel
    suspend fun getActiveRemindersDirect(): List<Reminder> {
        return reminderDao.getAllActiveRemindersDirect()
    }
    
    suspend fun getRemindersByDateRangeDirect(startDate: LocalDateTime, endDate: LocalDateTime): List<Reminder> {
        return reminderDao.getRemindersByDateRangeDirect(startDate, endDate)
    }
} 