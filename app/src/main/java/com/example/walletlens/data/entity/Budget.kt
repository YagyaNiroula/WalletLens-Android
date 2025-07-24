package com.example.walletlens.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val amount: Double,
    val description: String = "",
    val period: String, // monthly, weekly, yearly
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val isActive: Boolean = true,
    val isGoal: Boolean = false
) 