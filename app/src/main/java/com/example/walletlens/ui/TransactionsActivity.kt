package com.example.walletlens.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walletlens.data.WalletLensDatabase
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.data.repository.BudgetRepository
import com.example.walletlens.data.repository.ReminderRepository
import com.example.walletlens.data.repository.TransactionRepository
import com.example.walletlens.data.cache.DataCache
import com.example.walletlens.databinding.ActivityTransactionsBinding
import com.example.walletlens.ui.adapter.TransactionAdapter
import com.example.walletlens.ui.viewmodel.MainViewModel
import com.example.walletlens.ui.viewmodel.ViewModelFactory
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.example.walletlens.util.SmoothScrollingOptimizer

class TransactionsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTransactionsBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var transactionAdapter: TransactionAdapter
    private var selectedYearMonth: YearMonth = YearMonth.now()
    private var allTransactions: List<Transaction> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewModel()
        setupRecyclerView()
        setupMonthNavigation()
        observeData()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when activity resumes to show latest transactions
        Log.d("TransactionsActivity", "onResume - refreshing data...")
        viewModel.refreshData()
    }
    
    override fun onStart() {
        super.onStart()
        // Force load all transactions when activity starts
        Log.d("TransactionsActivity", "onStart - forcing data load...")
        viewModel.loadAllTransactionsData()
    }
    
    private fun setupViewModel() {
        val database = WalletLensDatabase.getDatabase(this)
        val transactionRepository = TransactionRepository(database.transactionDao())
        val budgetRepository = BudgetRepository(database.budgetDao())
        val reminderRepository = ReminderRepository(database.reminderDao())
        val dataCache = DataCache(this)
        
        val factory = ViewModelFactory(transactionRepository, budgetRepository, reminderRepository, dataCache)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter { transaction ->
            // Handle transaction click - could open edit dialog
        }
        
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(this@TransactionsActivity)
            adapter = transactionAdapter
            
            // Use enhanced SmoothScrollingOptimizer for large lists
            SmoothScrollingOptimizer.applyAutomaticOptimizations(this)
            SmoothScrollingOptimizer.preloadViews(this, 5)
            SmoothScrollingOptimizer.monitorScrollPerformance(this, this@TransactionsActivity)
        }
    }
    
    private fun setupMonthNavigation() {
        // Set current month
        updateMonthDisplay()
        
        // Previous month button
        binding.btnPreviousMonth.setOnClickListener {
            selectedYearMonth = selectedYearMonth.minusMonths(1)
            updateMonthDisplay()
            filterTransactionsByMonth()
        }
        
        // Next month button
        binding.btnNextMonth.setOnClickListener {
            selectedYearMonth = selectedYearMonth.plusMonths(1)
            updateMonthDisplay()
            filterTransactionsByMonth()
        }
    }
    
    private fun updateMonthDisplay() {
        val monthText = selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        binding.tvCurrentMonth.text = monthText
    }
    
    private fun observeData() {
        Log.d("TransactionsActivity", "Setting up data observer...")
        viewModel.allTransactions.observe(this) { transactions ->
            Log.d("TransactionsActivity", "Received ${transactions.size} transactions from ViewModel")
            allTransactions = transactions
            Log.d("TransactionsActivity", "All transactions: ${transactions.map { "${it.description}: $${it.amount} (${it.date})" }}")
            filterTransactionsByMonth()
        }
    }
    
    private fun filterTransactionsByMonth() {
        Log.d("TransactionsActivity", "Filtering transactions for month: ${selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}")
        Log.d("TransactionsActivity", "Total transactions available: ${allTransactions.size}")
        
        val filteredTransactions = allTransactions.filter { transaction ->
            val transactionYearMonth = YearMonth.from(transaction.date)
            val matches = transactionYearMonth == selectedYearMonth
            Log.d("TransactionsActivity", "Transaction ${transaction.description} (${transaction.date}) - YearMonth: $transactionYearMonth, Selected: $selectedYearMonth, Matches: $matches")
            if (!matches) {
                Log.d("TransactionsActivity", "  -> Transaction date: ${transaction.date}")
                Log.d("TransactionsActivity", "  -> Transaction YearMonth: $transactionYearMonth")
                Log.d("TransactionsActivity", "  -> Selected YearMonth: $selectedYearMonth")
                Log.d("TransactionsActivity", "  -> Year comparison: ${transactionYearMonth.year == selectedYearMonth.year}")
                Log.d("TransactionsActivity", "  -> Month comparison: ${transactionYearMonth.month == selectedYearMonth.month}")
            }
            matches
        }.sortedByDescending { it.date }
        
        // If no transactions found for selected month, show empty state
        if (filteredTransactions.isEmpty()) {
            Log.d("TransactionsActivity", "No transactions found for selected month: ${selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}")
            if (allTransactions.isNotEmpty()) {
                Log.d("TransactionsActivity", "Available transaction dates: ${allTransactions.map { it.date }}")
            }
        }
        
        Log.d("TransactionsActivity", "Filtered ${filteredTransactions.size} transactions for ${selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}")
        
        transactionAdapter.submitList(filteredTransactions)
        updateTransactionCount(filteredTransactions.size)
        updateEmptyState(filteredTransactions.isEmpty())
        updateMonthHeader()
    }
    
    private fun updateTransactionCount(count: Int) {
        // Transaction count is now handled in the month navigation
        // No separate count display needed
    }
    
    private fun updateMonthHeader() {
        // Month header is now handled in the navigation bar
        // No separate header needed
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerViewTransactions.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerViewTransactions.visibility = View.VISIBLE
        }
    }
} 