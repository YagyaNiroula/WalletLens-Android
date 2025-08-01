package com.example.walletlens.data.repository

import androidx.lifecycle.LiveData
import com.example.walletlens.data.dao.BudgetDao
import com.example.walletlens.data.entity.Budget

class BudgetRepository(private val budgetDao: BudgetDao) {
    
    fun getAllActiveBudgets(): LiveData<List<Budget>> {
        return budgetDao.getAllActiveBudgets()
    }
    
    fun getBudgetByCategory(category: String): LiveData<Budget?> {
        return budgetDao.getBudgetByCategory(category)
    }
    
    suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }
    
    suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget)
    }
    
    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }
    
    suspend fun deactivateBudget(id: Long) {
        budgetDao.deactivateBudget(id)
    }
    
    suspend fun getActiveBudgets(): List<Budget> {
        return budgetDao.getActiveBudgets()
    }
} 