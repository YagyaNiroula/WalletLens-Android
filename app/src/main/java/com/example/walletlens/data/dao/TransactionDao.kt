package com.example.walletlens.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.data.entity.TransactionType
import com.example.walletlens.data.entity.CategoryTotal
import java.time.LocalDateTime

@Dao
interface TransactionDao {
    
    // Pagination queries
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getTransactionsPaged(): PagingSource<Int, Transaction>
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByTypePaged(type: TransactionType): PagingSource<Int, Transaction>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRangePaged(startDate: LocalDateTime, endDate: LocalDateTime): PagingSource<Int, Transaction>
    
    // Existing queries for backward compatibility
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): LiveData<List<Transaction>>
    
    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC")
    fun getCategoryTotals(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): LiveData<List<CategoryTotal>>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    fun getTotalByType(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): LiveData<Double>
    
    @Insert
    suspend fun insertTransaction(transaction: Transaction)
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
    
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
    
    @Query("SELECT COUNT(*) FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTransactionCountByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Int
    
    // Direct query methods for ViewModel
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsDirect(): List<Transaction>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByDateRangeDirect(startDate: LocalDateTime, endDate: LocalDateTime): List<Transaction>
    
    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC")
    suspend fun getCategoryTotalsDirect(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): List<CategoryTotal>
} 