package com.example.walletlens.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.walletlens.data.entity.Budget

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY category")
    fun getAllActiveBudgets(): LiveData<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE category = :category AND isActive = 1")
    fun getBudgetByCategory(category: String): LiveData<Budget?>
    
    @Insert
    suspend fun insertBudget(budget: Budget)
    
    @Update
    suspend fun updateBudget(budget: Budget)
    
    @Delete
    suspend fun deleteBudget(budget: Budget)
    
    @Query("UPDATE budgets SET isActive = 0 WHERE id = :id")
    suspend fun deactivateBudget(id: Long)
    
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY category")
    suspend fun getActiveBudgets(): List<Budget>
} 