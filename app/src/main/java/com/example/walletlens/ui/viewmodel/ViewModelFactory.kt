package com.example.walletlens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.walletlens.data.repository.BudgetRepository
import com.example.walletlens.data.repository.ReminderRepository
import com.example.walletlens.data.repository.TransactionRepository
import com.example.walletlens.data.cache.DataCache

class ViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val reminderRepository: ReminderRepository,
    private val dataCache: DataCache
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(transactionRepository, budgetRepository, reminderRepository, dataCache) as T
            }
            modelClass.isAssignableFrom(ReminderViewModel::class.java) -> {
                ReminderViewModel(reminderRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 