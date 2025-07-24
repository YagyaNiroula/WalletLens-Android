package com.example.walletlens.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.walletlens.data.dao.BudgetDao
import com.example.walletlens.data.dao.ReminderDao
import com.example.walletlens.data.dao.TransactionDao
import com.example.walletlens.data.entity.Budget
import com.example.walletlens.data.entity.Reminder
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.data.util.Converters

@Database(
    entities = [Transaction::class, Budget::class, Reminder::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WalletLensDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: WalletLensDatabase? = null

        fun getDatabase(context: Context): WalletLensDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WalletLensDatabase::class.java,
                    "wallet_lens_database"
                )
                .fallbackToDestructiveMigration() // Allow destructive migrations for development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 