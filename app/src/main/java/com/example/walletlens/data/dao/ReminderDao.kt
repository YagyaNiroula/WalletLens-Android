package com.example.walletlens.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.walletlens.data.entity.Reminder
import java.time.LocalDateTime

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getAllActiveReminders(): LiveData<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE dueDate BETWEEN :startDate AND :endDate AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getRemindersByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): LiveData<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE category = :category AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getRemindersByCategory(category: String): LiveData<List<Reminder>>
    
    @Insert
    suspend fun insertReminder(reminder: Reminder)
    
    @Update
    suspend fun updateReminder(reminder: Reminder)
    
    @Delete
    suspend fun deleteReminder(reminder: Reminder)
    
    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markReminderAsCompleted(id: Long)
    
    // Direct query methods for ViewModel
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY dueDate ASC")
    suspend fun getAllActiveRemindersDirect(): List<Reminder>
    
    @Query("SELECT * FROM reminders WHERE dueDate BETWEEN :startDate AND :endDate AND isCompleted = 0 ORDER BY dueDate ASC")
    suspend fun getRemindersByDateRangeDirect(startDate: LocalDateTime, endDate: LocalDateTime): List<Reminder>
    
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?
} 