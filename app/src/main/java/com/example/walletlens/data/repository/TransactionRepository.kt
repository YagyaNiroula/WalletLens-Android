package com.example.walletlens.data.repository

import androidx.lifecycle.LiveData
import com.example.walletlens.data.dao.TransactionDao
import com.example.walletlens.data.entity.CategoryTotal
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.data.entity.TransactionType
import java.time.LocalDateTime

class TransactionRepository(private val transactionDao: TransactionDao) {
    
    fun getAllTransactions(): LiveData<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }
    
    fun getTransactionsByType(type: TransactionType): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByType(type)
    }
    
    fun getTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }
    
    fun getTotalByType(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): LiveData<Double> {
        return transactionDao.getTotalByType(type, startDate, endDate)
    }
    
    fun getCategoryTotals(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): LiveData<List<CategoryTotal>> {
        return transactionDao.getCategoryTotals(type, startDate, endDate)
    }
    
    fun getTransactionsByCategory(category: String): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(category)
    }
    
    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }
    
    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }
    
    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
    
    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }
    
    // Direct query methods for ViewModel
    suspend fun getAllTransactionsDirect(): List<Transaction> {
        return transactionDao.getAllTransactionsDirect()
    }
    
    suspend fun getTransactionsByDateRangeDirect(startDate: LocalDateTime, endDate: LocalDateTime): List<Transaction> {
        return transactionDao.getTransactionsByDateRangeDirect(startDate, endDate)
    }
    
    suspend fun getCategoryTotalsDirect(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): List<CategoryTotal> {
        return transactionDao.getCategoryTotalsDirect(type, startDate, endDate)
    }
    
    suspend fun getTransactionsByCategoryAndDateRange(category: String, startDate: LocalDateTime, endDate: LocalDateTime): List<Transaction> {
        return transactionDao.getTransactionsByCategoryAndDateRange(category, startDate, endDate)
    }
} 