package com.example.walletlens.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walletlens.data.entity.CategoryTotal
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.data.entity.TransactionType
import com.example.walletlens.data.entity.Budget
import com.example.walletlens.data.entity.Reminder
import com.example.walletlens.data.repository.TransactionRepository
import com.example.walletlens.data.repository.BudgetRepository
import com.example.walletlens.data.repository.ReminderRepository
import com.example.walletlens.data.cache.DataCache
import com.example.walletlens.util.ErrorHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.async
import android.util.Log

class MainViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val reminderRepository: ReminderRepository,
    private val dataCache: DataCache
) : ViewModel() {
    
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions
    
    private val _allTransactions = MutableLiveData<List<Transaction>>()
    val allTransactions: LiveData<List<Transaction>> = _allTransactions
    
    private val _totalIncome = MutableLiveData<Double>(0.0)
    val totalIncome: LiveData<Double> = _totalIncome
    
    private val _totalExpense = MutableLiveData<Double>(0.0)
    val totalExpense: LiveData<Double> = _totalExpense
    
    private val _balance = MutableLiveData<Double>(0.0)
    val balance: LiveData<Double> = _balance
    
    private val _categoryTotals = MutableLiveData<List<CategoryTotal>>()
    val categoryTotals: LiveData<List<CategoryTotal>> = _categoryTotals
    
    private val _budgets = MutableLiveData<List<com.example.walletlens.data.entity.Budget>>()
    val budgets: LiveData<List<com.example.walletlens.data.entity.Budget>> = _budgets
    
    // Bill Reminders
    private val _upcomingReminders = MutableLiveData<List<Reminder>>()
    val upcomingReminders: LiveData<List<Reminder>> = _upcomingReminders
    
    // Previous month data for comparisons
    private val _previousMonthIncome = MutableLiveData<Double>(0.0)
    val previousMonthIncome: LiveData<Double> = _previousMonthIncome
    
    private val _previousMonthExpense = MutableLiveData<Double>(0.0)
    val previousMonthExpense: LiveData<Double> = _previousMonthExpense
    
    private val _incomeChange = MutableLiveData<Double>(0.0)
    val incomeChange: LiveData<Double> = _incomeChange
    
    private val _expenseChange = MutableLiveData<Double>(0.0)
    val expenseChange: LiveData<Double> = _expenseChange
    
    // Performance optimization flags
    private var isDataLoaded = false
    
    init {
        loadData()
    }
    
    private fun loadData() {
        if (!isDataLoaded) {
            loadCurrentMonthData()
            loadBudgets()
            loadUpcomingReminders()
            isDataLoaded = true
        }
    }
    
    private fun loadCurrentMonthData() {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "Loading current month data...")
                
                val currentDate = LocalDateTime.now()
                val startOfMonth = currentDate.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfMonth = currentDate.withDayOfMonth(currentDate.month.length(currentDate.toLocalDate().isLeapYear))
                    .withHour(23).withMinute(59).withSecond(59)
                
                Log.d("MainViewModel", "Date range: $startOfMonth to $endOfMonth")
                
                // Get transactions for current month
                val transactions = transactionRepository.getTransactionsByDateRangeDirect(startOfMonth, endOfMonth)
                Log.d("MainViewModel", "Found ${transactions.size} transactions for current month")
                
                withContext(Dispatchers.Main) {
                    _transactions.value = transactions
                }
                
                // Get category totals directly from database
                val categoryTotals = transactionRepository.getCategoryTotalsDirect(TransactionType.EXPENSE, startOfMonth, endOfMonth)
                
                Log.d("MainViewModel", "Category totals: ${categoryTotals.size} categories")
                
                withContext(Dispatchers.Main) {
                    _categoryTotals.value = categoryTotals
                }
                
                // Calculate totals
                calculateTotals(transactions)
                
                Log.d("MainViewModel", "Current month data loaded successfully")
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading current month data: ${e.message}", e)
                // Set empty data on error
                withContext(Dispatchers.Main) {
                    _transactions.value = emptyList()
                    _categoryTotals.value = emptyList()
                    calculateTotals(emptyList())
                }
            }
        }
    }
    
    fun loadAllTransactionsData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transactions = transactionRepository.getAllTransactionsDirect()
                withContext(Dispatchers.Main) {
                    _allTransactions.value = transactions
                    Log.d("MainViewModel", "Loaded ${transactions.size} total transactions")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading all transactions: ${e.message}")
            }
        }
    }
    
    private fun loadPreviousMonthData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = LocalDateTime.now()
                val previousMonth = now.minusMonths(1)
                val startOfPreviousMonth = previousMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfPreviousMonth = previousMonth.withDayOfMonth(previousMonth.month.length(previousMonth.toLocalDate().isLeapYear))
                    .withHour(23).withMinute(59).withSecond(59)
                
                val transactions = transactionRepository.getTransactionsByDateRangeDirect(startOfPreviousMonth, endOfPreviousMonth)
                val previousIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val previousExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                
                withContext(Dispatchers.Main) {
                    _previousMonthIncome.value = previousIncome
                    _previousMonthExpense.value = previousExpense
                    calculateChanges()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading previous month data: ${e.message}")
            }
        }
    }
    
    private fun loadUpcomingReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = LocalDateTime.now()
                val endOfMonth = now.withDayOfMonth(now.month.length(now.toLocalDate().isLeapYear))
                    .withHour(23).withMinute(59).withSecond(59)
                
                val reminders = reminderRepository.getRemindersByDateRangeDirect(now, endOfMonth)
                withContext(Dispatchers.Main) {
                    _upcomingReminders.value = reminders
                    Log.d("MainViewModel", "Loaded ${reminders.size} reminders")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading reminders: ${e.message}")
            }
        }
    }
    
    private fun calculateTotals(transactions: List<Transaction>) {
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        
        _totalIncome.value = income
        _totalExpense.value = expense
        _balance.value = income - expense
        
        // Calculate changes after totals are updated
        calculateChanges()
    }
    
    private fun calculateChanges() {
        val currentIncome = _totalIncome.value ?: 0.0
        val currentExpense = _totalExpense.value ?: 0.0
        val previousIncome = _previousMonthIncome.value ?: 0.0
        val previousExpense = _previousMonthExpense.value ?: 0.0
        
        // Calculate percentage changes
        val incomeChangePercent = if (previousIncome > 0) {
            ((currentIncome - previousIncome) / previousIncome) * 100
        } else {
            0.0
        }
        
        val expenseChangePercent = if (previousExpense > 0) {
            ((currentExpense - previousExpense) / previousExpense) * 100
        } else {
            0.0
        }
        
        _incomeChange.value = incomeChangePercent
        _expenseChange.value = expenseChangePercent
    }
    
    private fun loadBudgets() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val budgets = budgetRepository.getAllActiveBudgets().value ?: emptyList()
                withContext(Dispatchers.Main) {
                    _budgets.value = budgets
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading budgets: ${e.message}")
            }
        }
    }
    
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "Adding transaction: $transaction")
                
                transactionRepository.insertTransaction(transaction)
                
                Log.d("MainViewModel", "Transaction added successfully, refreshing data...")
                
                // Refresh current month data to show the new transaction
                loadCurrentMonthData()
                
                withContext(Dispatchers.Main) {
                    Log.d("MainViewModel", "Transaction added and data refreshed successfully")
                }
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error adding transaction: ${e.message}", e)
            }
        }
    }
    
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionRepository.deleteTransaction(transaction)
                // Refresh current month data
                loadCurrentMonthData()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting transaction: ${e.message}")
            }
        }
    }
    
    fun refreshData() {
        Log.d("MainViewModel", "Force refreshing all data...")
        isDataLoaded = false
        loadData()
    }
    
    // Debug method to check database contents
    fun debugCheckDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allReminders = reminderRepository.getActiveRemindersDirect()
                Log.d("MainViewModel", "=== DATABASE DEBUG ===")
                Log.d("MainViewModel", "Total reminders in database: ${allReminders.size}")
                allReminders.forEachIndexed { index, reminder ->
                    Log.d("MainViewModel", "Reminder $index: ${reminder.title} - ${reminder.dueDate} - Completed: ${reminder.isCompleted}")
                }
                Log.d("MainViewModel", "=== END DEBUG ===")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error checking database: ${e.message}", e)
            }
        }
    }
    
    fun forceRefreshCurrentMonthData() {
        Log.d("MainViewModel", "Force refreshing current month data...")
        loadCurrentMonthData()
    }
    
    fun loadTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate).value ?: emptyList()
                val categoryTotals = transactionRepository.getCategoryTotals(TransactionType.EXPENSE, startDate, endDate).value ?: emptyList()
                
                withContext(Dispatchers.Main) {
                    _transactions.value = transactions
                    _categoryTotals.value = categoryTotals
                    calculateTotals(transactions)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading transactions by date range: ${e.message}")
            }
        }
    }
    
    // Reminder methods
    fun addReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainViewModel", "Adding reminder: $reminder")
                reminderRepository.insertReminder(reminder)
                
                Log.d("MainViewModel", "Reminder inserted, now refreshing list...")
                
                // Test: Check if reminder was actually saved
                val allReminders = reminderRepository.getActiveRemindersDirect()
                Log.d("MainViewModel", "All reminders in database: ${allReminders.size}")
                allReminders.forEach { 
                    Log.d("MainViewModel", "Reminder in DB: ${it.title} - ${it.dueDate}")
                }
                
                // Refresh reminders list
                loadUpcomingReminders()
                
                withContext(Dispatchers.Main) {
                    Log.d("MainViewModel", "Reminder added successfully, list refreshed")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error adding reminder: ${e.message}", e)
            }
        }
    }
    
    fun markReminderAsCompleted(reminderId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainViewModel", "Marking reminder as completed: $reminderId")
                reminderRepository.markReminderAsCompleted(reminderId)
                
                // Refresh reminders list
                loadUpcomingReminders()
                
                withContext(Dispatchers.Main) {
                    Log.d("MainViewModel", "Reminder marked as completed successfully")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error marking reminder as completed: ${e.message}", e)
            }
        }
    }
    
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainViewModel", "Deleting reminder: $reminder")
                reminderRepository.deleteReminder(reminder)
                
                // Refresh reminders list
                loadUpcomingReminders()
                
                withContext(Dispatchers.Main) {
                    Log.d("MainViewModel", "Reminder deleted successfully")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting reminder: ${e.message}", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up any resources if needed
    }
} 