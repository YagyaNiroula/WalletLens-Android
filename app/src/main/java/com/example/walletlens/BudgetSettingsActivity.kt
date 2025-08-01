package com.example.walletlens

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.walletlens.data.WalletLensDatabase
import com.example.walletlens.data.repository.BudgetRepository
import com.example.walletlens.data.repository.ReminderRepository
import com.example.walletlens.data.repository.TransactionRepository
import com.example.walletlens.data.cache.DataCache
import com.example.walletlens.ui.viewmodel.MainViewModel
import com.example.walletlens.ui.viewmodel.ViewModelFactory
import com.example.walletlens.databinding.ActivityBudgetSettingsBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import android.widget.ImageView
import kotlin.math.abs

class BudgetSettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBudgetSettingsBinding
    private lateinit var viewModel: MainViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewModel()
        setupUI()
        setupClickListeners()
        observeData()
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
    
    private fun setupUI() {
        // Set up toolbar with back button
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Budget Settings"
        }
    }
    
    private fun setupClickListeners() {
        binding.btnSaveBudget.setOnClickListener {
            saveNewBudget()
        }
        
        // Bill Reminders Toggle
        binding.ivBillRemindersToggle.setOnClickListener {
            toggleBillReminders()
        }
        
        // Budget Warnings Toggle
        binding.ivBudgetWarningsToggle.setOnClickListener {
            toggleBudgetWarnings()
        }
    }
    
    private fun observeData() {
        // Observe budget data
        viewModel.monthlyBudget.observe(this) { budget ->
            binding.tvCurrentBudget.text = "$${String.format("%.2f", budget)}"
        }
        
        viewModel.budgetSpent.observe(this) { spent ->
            binding.tvBudgetSpent.text = "Spent: $${String.format("%.2f", spent)}"
        }
        
        viewModel.budgetRemaining.observe(this) { remaining ->
            val remainingText = if (remaining < 0) {
                "Overspent: $${String.format("%.2f", abs(remaining))}"
            } else {
                "Remaining: $${String.format("%.2f", remaining)}"
            }
            binding.tvBudgetRemaining.text = remainingText
            
            // Set color based on remaining amount
            val color = if (remaining < 0) {
                ContextCompat.getColor(this, R.color.expense_red)
            } else {
                ContextCompat.getColor(this, R.color.income_green)
            }
            binding.tvBudgetRemaining.setTextColor(color)
        }
    }
    
    private fun saveNewBudget() {
        val budgetText = binding.etNewBudget.text.toString()
        if (budgetText.isBlank()) {
            Toast.makeText(this, "Please enter a budget amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        val newBudget = budgetText.toDoubleOrNull()
        if (newBudget == null || newBudget <= 0) {
            Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.updateMonthlyBudget(newBudget)
        binding.etNewBudget.text?.clear()
        Toast.makeText(this, "Budget updated to $${String.format("%.2f", newBudget)}", Toast.LENGTH_SHORT).show()
    }
    
    private fun toggleBillReminders() {
        // Toggle bill reminders notification setting
        val isEnabled = binding.ivBillRemindersToggle.tag as? Boolean ?: true
        val newState = !isEnabled
        
        binding.ivBillRemindersToggle.tag = newState
        binding.ivBillRemindersToggle.setColorFilter(
            if (newState) ContextCompat.getColor(this, R.color.income_green)
            else ContextCompat.getColor(this, R.color.secondary_text)
        )
        
        // Save setting to SharedPreferences
        getSharedPreferences("notification_settings", MODE_PRIVATE)
            .edit()
            .putBoolean("bill_reminders_enabled", newState)
            .apply()
        
        Toast.makeText(
            this,
            if (newState) "Bill reminders enabled" else "Bill reminders disabled",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun toggleBudgetWarnings() {
        // Toggle budget warnings notification setting
        val isEnabled = binding.ivBudgetWarningsToggle.tag as? Boolean ?: true
        val newState = !isEnabled
        
        binding.ivBudgetWarningsToggle.tag = newState
        binding.ivBudgetWarningsToggle.setColorFilter(
            if (newState) ContextCompat.getColor(this, R.color.income_green)
            else ContextCompat.getColor(this, R.color.secondary_text)
        )
        
        // Save setting to SharedPreferences
        getSharedPreferences("notification_settings", MODE_PRIVATE)
            .edit()
            .putBoolean("budget_warnings_enabled", newState)
            .apply()
        
        Toast.makeText(
            this,
            if (newState) "Budget warnings enabled" else "Budget warnings disabled",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 