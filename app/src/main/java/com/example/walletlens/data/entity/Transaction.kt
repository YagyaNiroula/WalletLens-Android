package com.example.walletlens.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.LocalDateTime

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["date", "type"]),
        Index(value = ["type", "category"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val category: String,
    val type: TransactionType,
    val date: LocalDateTime,
    val imagePath: String? = null,

    val isRecurring: Boolean = false,
    val recurringInterval: String? = null
)

enum class TransactionType {
    INCOME, EXPENSE
} 