package com.example.walletlens.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val amount: Double,
    val dueDate: LocalDateTime,
    val isCompleted: Boolean = false,
    val category: String,
    val isRecurring: Boolean = false,
    val recurringInterval: String? = null
) 