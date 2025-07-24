package com.example.walletlens.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.data.entity.Budget
import com.example.walletlens.data.entity.Reminder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DataCache(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "wallet_lens_cache", Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    companion object {
        private const val KEY_TRANSACTIONS = "cached_transactions"
        private const val KEY_BUDGETS = "cached_budgets"
        private const val KEY_REMINDERS = "cached_reminders"
        private const val KEY_LAST_SYNC = "last_sync_time"
        private const val KEY_CACHE_VERSION = "cache_version"
        private const val CACHE_VERSION = 1
    }
    
    // Cache transactions
    fun cacheTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit()
            .putString(KEY_TRANSACTIONS, json)
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .putInt(KEY_CACHE_VERSION, CACHE_VERSION)
            .apply()
    }
    
    fun getCachedTransactions(): List<Transaction> {
        val json = sharedPreferences.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<Transaction>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Cache budgets
    fun cacheBudgets(budgets: List<Budget>) {
        val json = gson.toJson(budgets)
        sharedPreferences.edit()
            .putString(KEY_BUDGETS, json)
            .apply()
    }
    
    fun getCachedBudgets(): List<Budget> {
        val json = sharedPreferences.getString(KEY_BUDGETS, null) ?: return emptyList()
        val type = object : TypeToken<List<Budget>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Cache reminders
    fun cacheReminders(reminders: List<Reminder>) {
        val json = gson.toJson(reminders)
        sharedPreferences.edit()
            .putString(KEY_REMINDERS, json)
            .apply()
    }
    
    fun getCachedReminders(): List<Reminder> {
        val json = sharedPreferences.getString(KEY_REMINDERS, null) ?: return emptyList()
        val type = object : TypeToken<List<Reminder>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Cache management
    fun isCacheValid(): Boolean {
        val lastSync = sharedPreferences.getLong(KEY_LAST_SYNC, 0)
        val cacheAge = System.currentTimeMillis() - lastSync
        val maxAge = 24 * 60 * 60 * 1000 // 24 hours
        return cacheAge < maxAge
    }
    
    fun clearCache() {
        sharedPreferences.edit().clear().apply()
    }
    
    fun getLastSyncTime(): LocalDateTime? {
        val timestamp = sharedPreferences.getLong(KEY_LAST_SYNC, 0)
        return if (timestamp > 0) {
            LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC)
        } else null
    }
    
    fun getFormattedLastSyncTime(): String {
        val lastSync = getLastSyncTime()
        return if (lastSync != null) {
            lastSync.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
        } else "Never"
    }
} 