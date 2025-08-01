package com.example.walletlens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walletlens.data.WalletLensDatabase
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.data.entity.TransactionType
import com.example.walletlens.data.repository.BudgetRepository
import com.example.walletlens.data.repository.TransactionRepository
import com.example.walletlens.data.repository.ReminderRepository
import com.example.walletlens.data.cache.DataCache
import com.example.walletlens.util.ErrorHandler
import com.example.walletlens.util.PerformanceOptimizer
import com.example.walletlens.util.SmoothScrollingOptimizer
import com.example.walletlens.util.SmoothAnimationOptimizer

import com.example.walletlens.ui.TransactionsActivity
import com.example.walletlens.ui.receipt.ReceiptScannerActivity
import com.example.walletlens.databinding.ActivityMainBinding
import com.example.walletlens.ui.adapter.TransactionAdapter
import com.example.walletlens.ui.adapter.BillRemindersAdapter
import com.example.walletlens.ui.viewmodel.MainViewModel
import com.example.walletlens.ui.viewmodel.ViewModelFactory
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.TextView
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import kotlin.math.min
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.content.res.ColorStateList
import android.widget.LinearLayout
import com.example.walletlens.notification.NotificationScheduler
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var billRemindersAdapter: BillRemindersAdapter
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }
    

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d("MainActivity", "onCreate started")
        
        try {
            // Set the month in the pie chart card
            val pieChartMonth = findViewById<TextView>(R.id.tvPieChartMonth)
            pieChartMonth.text = "Month: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM, yyyy"))}"
            
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
            
            Log.d("MainActivity", "Setting up ViewModel")
            setupViewModel()
            
            Log.d("MainActivity", "Setting up basic UI")
            setupBasicUI()
            
            Log.d("MainActivity", "Setting up RecyclerViews")
            setupRecyclerViews()
            
            Log.d("MainActivity", "Setting up click listeners")
            setupClickListeners()
            
            Log.d("MainActivity", "Setting up data observers")
            observeData()
            
            // Request notification permission
            requestNotificationPermission()
            
            // Update widget on app start
            updateWidget()
            
            Log.d("MainActivity", "onCreate completed successfully")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale dialog
                    AlertDialog.Builder(this)
                        .setTitle("Notification Permission")
                        .setMessage("This app needs notification permission to send you payment reminders and budget warnings.")
                        .setPositiveButton("Grant Permission") { _, _ ->
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    private fun updateWidget() {
        try {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(this, com.example.walletlens.widget.WalletLensWidget::class.java)
            )
            
            if (appWidgetIds.isNotEmpty()) {
                Log.d("MainActivity", "Updating ${appWidgetIds.size} widgets")
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_today_spending)
                
                // Force update all widgets
                val intent = Intent(this, com.example.walletlens.widget.WalletLensWidget::class.java)
                intent.action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                sendBroadcast(intent)
            } else {
                Log.d("MainActivity", "No widgets found to update")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating widget: ${e.message}", e)
        }
    }
    
    private fun showQuickAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val amountEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextAmount)
        val descriptionEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextDescription)
        val categorySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.categorySpinner)
        val transactionTypeToggle = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.transactionTypeToggle)
        
        // Setup categories
        val categories = listOf(
            "Food & Dining", "Transportation", "Shopping", "Entertainment", 
            "Healthcare", "Utilities", "Housing", "Education", "Other"
        )
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categorySpinner.setAdapter(categoryAdapter)
        categorySpinner.setText(categories[0], false)
        
        // Set default type to expense
        transactionTypeToggle.check(R.id.btnExpense)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Quick Add Transaction")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
                val description = descriptionEditText.text.toString()
                val category = categorySpinner.text.toString()
                val type = if (transactionTypeToggle.checkedButtonId == R.id.btnIncome) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }
                
                if (amount > 0 && description.isNotBlank()) {
                    val transaction = Transaction(
                        amount = amount,
                        description = description,
                        category = category,
                        type = type,
                        date = LocalDateTime.now()
                    )
                    
                    viewModel.addTransaction(transaction)
                    updateWidget()
                    
                    val message = if (type == TransactionType.INCOME) "Income added!" else "Expense added!"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please enter amount and description", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Handle widget quick-add action
        handleWidgetAction()
        
        // Check if there's receipt data from camera
        val sharedPrefs = getSharedPreferences("receipt_data", MODE_PRIVATE)
        val receiptPath = sharedPrefs.getString("last_receipt_path", null)
        val receiptDate = sharedPrefs.getString("last_receipt_date", null)
        
        if (receiptPath != null) {
            // Clear the data so it doesn't show again
            sharedPrefs.edit().clear().apply()
            // Show the receipt dialog
            showSimpleReceiptDialog(receiptPath, receiptDate)
        }
        
        // Only optimize performance if viewModel is initialized
        if (::viewModel.isInitialized) {
            viewModel.viewModelScope.launch {
                PerformanceOptimizer.optimizePerformance(this@MainActivity)
            }
        }
    }
    
    private fun handleWidgetAction() {
        val action = intent?.action
        val widgetAction = intent?.getStringExtra("widget_action")
        
        if (action == "QUICK_ADD_TRANSACTION" || widgetAction == "quick_add") {
            Log.d("MainActivity", "Widget quick-add action received")
            // Clear the intent to prevent multiple triggers
            intent?.replaceExtras(android.os.Bundle())
            // Show quick-add dialog
            showQuickAddDialog()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Clean up memory when app goes to background
        PerformanceOptimizer.forceMemoryCleanup()
    }
    
    private fun setupViewModel() {
        try {
            Log.d("MainActivity", "Setting up database...")
            val database = WalletLensDatabase.getDatabase(this)
            
            Log.d("MainActivity", "Setting up repositories...")
            val transactionRepository = TransactionRepository(database.transactionDao())
            val budgetRepository = BudgetRepository(database.budgetDao())
            val reminderRepository = ReminderRepository(database.reminderDao())
            
            Log.d("MainActivity", "Setting up data cache...")
            val dataCache = DataCache(this)
            
            Log.d("MainActivity", "Creating ViewModel factory...")
            val factory = ViewModelFactory(transactionRepository, budgetRepository, reminderRepository, dataCache)
            
            Log.d("MainActivity", "Creating ViewModel...")
            viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
            
            Log.d("MainActivity", "ViewModel setup completed successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up ViewModel: ${e.message}", e)
            Toast.makeText(this, "Error setting up app: ${e.message}", Toast.LENGTH_LONG).show()
            // Try to continue with a basic setup
            try {
                val database = WalletLensDatabase.getDatabase(this)
                val transactionRepository = TransactionRepository(database.transactionDao())
                val budgetRepository = BudgetRepository(database.budgetDao())
                val reminderRepository = ReminderRepository(database.reminderDao())
                val dataCache = DataCache(this)
                val factory = ViewModelFactory(transactionRepository, budgetRepository, reminderRepository, dataCache)
                viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
            } catch (e2: Exception) {
                Log.e("MainActivity", "Critical error setting up ViewModel: ${e2.message}", e2)
                finish() // Close the app if we can't set up the ViewModel
            }
        }
    }
    
    private fun setupBasicUI() {
        try {
            // Set basic text values
            binding.tvIncomeAmount.text = "$0"
            binding.tvExpenseAmount.text = "$0"
            binding.tvBalanceAmount.text = "$0"
            binding.tvRecentTransactionsTitle.text = "Recent Transactions (0)"
            
            Log.d("MainActivity", "Basic UI setup completed")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up basic UI: ${e.message}", e)
        }
    }
    
    private fun setupRecyclerViews() {
        try {
            Log.d("MainActivity", "Setting up RecyclerViews...")
            
            // Setup transactions RecyclerView
            transactionAdapter = TransactionAdapter { transaction ->
                // Handle transaction click
                Toast.makeText(this, "Transaction: ${transaction.description}", Toast.LENGTH_SHORT).show()
            }
            
            binding.recentTransactionsRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = transactionAdapter
            }
            
            // Setup bill reminders RecyclerView
            billRemindersAdapter = BillRemindersAdapter(
                onReminderClick = { reminder ->
                    // Handle reminder click
                    Toast.makeText(this, "Reminder: ${reminder.title}", Toast.LENGTH_SHORT).show()
                },
                onReminderComplete = { reminderId ->
                    // Handle reminder completion
                    viewModel.markReminderAsCompleted(reminderId)
                    Toast.makeText(this, "Reminder marked as completed", Toast.LENGTH_SHORT).show()
                }
            )
            
            binding.rvBillReminders.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = billRemindersAdapter
            }
            
            Log.d("MainActivity", "RecyclerViews setup completed successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up RecyclerViews: ${e.message}", e)
        }
    }
    
    // Helper to format amounts without trailing .00
    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            "${amount.toInt()}"
        } else {
            String.format("%.2f", amount)
        }
    }

    private fun observeData() {
        try {
            Log.d("MainActivity", "Setting up data observers...")
            
            viewModel.transactions.observe(this) { transactions ->
                try {
                    Log.d("MainActivity", "Transactions updated: ${transactions.size} transactions")
                    Log.d("MainActivity", "Transactions: ${transactions.map { "${it.type}: ${it.description} - $${it.amount}" }}")
                    
                    // Update the transaction count
                    updateTransactionCount(transactions.size)
                    
                    // Update RecyclerView with actual transactions
                    transactionAdapter.submitList(transactions.take(4)) // Show only recent 4 transactions
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating transactions: ${e.message}")
                }
            }
            
            viewModel.totalIncome.observe(this) { income ->
                try {
                    binding.tvIncomeAmount.text = "$${formatAmount(income)}"
                    Log.d("MainActivity", "Income updated: $${formatAmount(income)}")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating income: ${e.message}")
                }
            }
            
            viewModel.totalExpense.observe(this) { expense ->
                try {
                    binding.tvExpenseAmount.text = "$${formatAmount(expense)}"
                    Log.d("MainActivity", "Expense updated: $${formatAmount(expense)}")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating expense: ${e.message}")
                }
            }
            
            viewModel.balance.observe(this) { balance ->
                try {
                    val formattedBalance = if (balance < 0) {
                        "-$${formatAmount(abs(balance))}"
                    } else {
                        "$${formatAmount(balance)}"
                    }
                    binding.tvBalanceAmount.text = formattedBalance
                    
                    // Keep text color white (default)
                    binding.tvBalanceAmount.setTextColor(getColor(android.R.color.white))
                    
                    Log.d("MainActivity", "Balance updated: $formattedBalance")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating balance: ${e.message}")
                }
            }
            
            viewModel.categoryTotals.observe(this) { categoryTotals ->
                try {
                    Log.d("MainActivity", "Category totals updated: ${categoryTotals.size} categories")
                    Log.d("MainActivity", "Category totals: ${categoryTotals.map { "${it.category}: $${it.total}" }}")
                    
                    // Convert CategoryTotal list to Map for pie chart
                    val categoryMap = categoryTotals.associate { it.category to it.total }
                    updatePieChart(categoryMap)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating category totals: ${e.message}")
                }
            }
            
            viewModel.upcomingReminders.observe(this) { reminders ->
                try {
                    Log.d("MainActivity", "Reminders updated: ${reminders.size} reminders")
                    Log.d("MainActivity", "Reminders: ${reminders.map { "${it.title} - ${it.dueDate}" }}")
                    billRemindersAdapter.updateReminders(reminders)
                    updateRemindersVisibility(reminders.isEmpty())
                    
                    // Debug: Check if adapter is properly updated
                    Log.d("MainActivity", "Adapter item count: ${billRemindersAdapter.itemCount}")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating reminders: ${e.message}")
                }
            }
            
            Log.d("MainActivity", "Data observers setup completed successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up data observers: ${e.message}", e)
        }
    }
    
    private fun updateTransactionCount(count: Int) {
        // Just show "Recent Transactions" without the count
        binding.tvRecentTransactionsTitle.text = "Recent Transactions"
    }
    
    private fun updateRemindersVisibility(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyRemindersLayout.visibility = View.VISIBLE
            binding.rvBillReminders.visibility = View.GONE
        } else {
            binding.emptyRemindersLayout.visibility = View.GONE
            binding.rvBillReminders.visibility = View.VISIBLE
        }
    }
    
    private fun updatePieChart(categoryTotals: Map<String, Double>) {
        // Run chart calculations on background thread
        Thread {
            try {
                // Group small categories as 'Other' if more than 6 categories
                val sorted = categoryTotals.entries.sortedByDescending { it.value }
                val total = categoryTotals.values.sum()
                val maxCategories = 6
                val grouped = if (sorted.size > maxCategories) {
                    val main = sorted.take(maxCategories - 1)
                    val otherSum = sorted.drop(maxCategories - 1).sumOf { it.value }
                    (main + mapOf("Other" to otherSum).entries.first()).associate { it.key to it.value }
                } else {
                    categoryTotals
                }

                val entries = grouped.map { (category, amount) ->
                    PieEntry(amount.toFloat(), category)
                }

                val customColors = listOf(
                    ContextCompat.getColor(this, R.color.primary_color),
                    ContextCompat.getColor(this, R.color.expense_red),
                    ContextCompat.getColor(this, R.color.income_green),
                    ContextCompat.getColor(this, R.color.balance_blue),
                    ContextCompat.getColor(this, R.color.warning_orange),
                    ContextCompat.getColor(this, R.color.primary_dark),
                    ContextCompat.getColor(this, R.color.accent_color),
                    ContextCompat.getColor(this, R.color.primary_light)
                )

                val dataSet = PieDataSet(entries, "Expenses by Category")
                dataSet.colors = customColors
                dataSet.valueTextSize = 16f
                dataSet.valueTextColor = ContextCompat.getColor(this, R.color.black)
                dataSet.valueLinePart1Length = 0.7f
                dataSet.valueLinePart2Length = 0.5f
                dataSet.valueLineColor = ContextCompat.getColor(this, R.color.secondary_text)
                dataSet.valueLineWidth = 2.5f
                dataSet.yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
                dataSet.xValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
                dataSet.sliceSpace = 4f
                dataSet.selectionShift = 10f
                dataSet.setDrawValues(true)
                dataSet.setDrawIcons(false)
                dataSet.setAutomaticallyDisableSliceSpacing(false)

                val pieData = PieData(dataSet)
                pieData.setValueFormatter(PercentFormatter(binding.pieChart))

                // Update the widget UI on the main thread
                runOnUiThread {
                    binding.pieChart.apply {
                        data = pieData
                        description.isEnabled = false
                        legend.isEnabled = true
                        legend.textSize = 12f
                        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                        setUsePercentValues(true)
                        setEntryLabelTextSize(12f)
                        setEntryLabelColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                        
                        // Faster animation for smoother experience
                        animateY(500)
                        invalidate()
                    }
                }
            } catch (e: Exception) {
                // Handle any errors silently to prevent crashes
                Log.e("MainActivity", "Error updating pie chart: ${e.message}")
            }
        }.start()
    }
    
    private fun animateCard(card: View) {
        SmoothAnimationOptimizer.animateCard(card)
    }
    
    private fun showAddTransactionDialog(defaultType: TransactionType = TransactionType.EXPENSE) {
        try {
            Log.d("MainActivity", "showAddTransactionDialog called with default type: $defaultType")
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
            Log.d("MainActivity", "Dialog layout inflated successfully")
            
            val amountEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextAmount)
            val descriptionEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextDescription)
            val categorySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.categorySpinner)
            val transactionTypeToggle = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.transactionTypeToggle)
            val dateEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextDate)
            
            Log.d("MainActivity", "Dialog views found successfully")
            
            // Setup categories for both income and expense
            val expenseCategories = listOf("Food & Dining", "Transportation", "Shopping", "Entertainment", "Utilities", "Healthcare", "Education", "Insurance", "Other")
            val incomeCategories = listOf("Salary", "Freelance", "Investment", "Business", "Gift", "Refund", "Other Income")
            
            val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, 
                if (defaultType == TransactionType.INCOME) incomeCategories else expenseCategories)
            categorySpinner.setAdapter(categoryAdapter)
            categorySpinner.setText(if (defaultType == TransactionType.INCOME) incomeCategories[0] else expenseCategories[0], false)
            
            // Show the transaction type toggle and set default
            transactionTypeToggle.visibility = View.VISIBLE
            val defaultButtonId = if (defaultType == TransactionType.INCOME) R.id.btnIncome else R.id.btnExpense
            transactionTypeToggle.check(defaultButtonId)
            
            // Setup date picker
            dateEditText.setOnClickListener {
                val calendar = java.util.Calendar.getInstance()
                val year = calendar.get(java.util.Calendar.YEAR)
                val month = calendar.get(java.util.Calendar.MONTH)
                val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

                val datePickerDialog = android.app.DatePickerDialog(
                    this,
                    { _, selectedYear, selectedMonth, selectedDay ->
                        val selectedDate = LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, 0, 0)
                        dateEditText.setText(selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                    },
                    year, month, day
                )
                datePickerDialog.show()
            }
            
            // Set default date to today
            dateEditText.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
            
            // Handle transaction type toggle
            transactionTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
                Log.d("MainActivity", "Transaction type toggle - CheckedId: $checkedId, IsChecked: $isChecked")
                if (isChecked) {
                    val categories = if (checkedId == R.id.btnIncome) {
                        Log.d("MainActivity", "Switching to income categories")
                        incomeCategories
                    } else {
                        Log.d("MainActivity", "Switching to expense categories")
                        expenseCategories
                    }
                    
                    val newAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
                    categorySpinner.setAdapter(newAdapter)
                    categorySpinner.setText(categories[0], false)
                }
            }
            
            val dialog = AlertDialog.Builder(this)
                .setTitle("Add Transaction")
                .setView(dialogView)
                            .setPositiveButton("Add") { _, _ ->
                val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
                val description = descriptionEditText.text.toString()
                val category = categorySpinner.text.toString()
                val checkedButtonId = transactionTypeToggle.checkedButtonId
                val type = if (checkedButtonId == R.id.btnIncome) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }
                
                Log.d("MainActivity", "Add transaction - Amount: $amount, Description: $description, Category: $category, Type: $type")
                
                if (amount > 0 && category.isNotEmpty()) {
                    val transaction = Transaction(
                        amount = amount,
                        description = description,
                        category = category,
                        type = type,
                        date = LocalDateTime.now()
                    )
                    Log.d("MainActivity", "Creating transaction: $transaction")
                    viewModel.addTransaction(transaction)
                    
                    // Schedule immediate budget check after adding transaction
                    NotificationScheduler.scheduleImmediateBudgetCheck(this)
                    
                    // Update widget after adding transaction
                    updateWidget()
                    
                    val message = if (type == TransactionType.INCOME) "Income added!" else "Expense added!"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("MainActivity", "Validation failed - Amount: $amount, Category: '$category'")
                    Toast.makeText(this, "Please enter a valid amount and select a category", Toast.LENGTH_SHORT).show()
                }
            }
                .setNegativeButton("Cancel", null)
                .create()
            
            Log.d("MainActivity", "Showing add transaction dialog")
            dialog.show()
            Log.d("MainActivity", "Add transaction dialog shown successfully")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing add transaction dialog: ${e.message}", e)
            Toast.makeText(this, "Error showing dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkBudgetWarning(amount: Double, category: String, warningText: TextView) {
        // This is a placeholder for budget checking
        // In a real app, you would check against actual budget limits
        val budgetLimit = 500.0 // Example budget limit
        if (amount > budgetLimit) {
            warningText.visibility = View.VISIBLE
        } else {
            warningText.visibility = View.GONE
        }
    }

    private fun showCategorySummary(category: String, amount: Double) {
        // Get current month's total for this category
        val currentMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
        val endOfMonth = LocalDateTime.now().withDayOfMonth(LocalDateTime.now().month.length(LocalDateTime.now().toLocalDate().isLeapYear))
            .withHour(23).withMinute(59).withSecond(59)
        
        // TODO: Implement category total lookup
        Toast.makeText(this, "Category total feature coming soon!", Toast.LENGTH_LONG).show()
    }
    
    private fun showAddReminderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.etReminderTitle)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.etReminderDescription)
        val amountEditText = dialogView.findViewById<EditText>(R.id.etReminderAmount)
        val categorySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)
        val dueDateEditText = dialogView.findViewById<EditText>(R.id.etReminderDueDate)

        // Setup categories
        val categories = listOf(
            "Utilities", "Rent", "Insurance", "Phone", "Internet", 
            "Groceries", "Transportation", "Healthcare", "Entertainment", "Other"
        )
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categorySpinner.setAdapter(categoryAdapter)
        categorySpinner.setText(categories[0], false)

        // Setup date picker
        dueDateEditText.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, 0, 0)
                    dueDateEditText.setText(selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        // Set default date (7 days from now)
        val defaultDate = LocalDateTime.now().plusDays(7)
        dueDateEditText.setText(defaultDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set up button click listeners
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveReminder).setOnClickListener {
            val title = titleEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val category = categorySpinner.text.toString()
            val dateText = dueDateEditText.text.toString()
            
            if (title.isNotBlank() && amount > 0) {
                val dueDate = try {
                    // Parse the date string like "Dec 15, 2024" to LocalDateTime
                    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                    val localDate = java.time.LocalDate.parse(dateText, formatter)
                    LocalDateTime.of(localDate, java.time.LocalTime.of(0, 0))
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing date: $dateText", e)
                    LocalDateTime.now().plusDays(7)
                }
                
                val reminder = com.example.walletlens.data.entity.Reminder(
                    title = title,
                    description = description.ifBlank { "Bill payment reminder" },
                    amount = amount,
                    dueDate = dueDate,
                    category = category
                )
                
                // Save reminder to database
                Log.d("MainActivity", "Adding reminder: $reminder")
                viewModel.addReminder(reminder)
                
                // Schedule notification for the reminder
                NotificationScheduler.scheduleReminderNotification(this, reminder)
                
                // Force refresh the reminders list
                viewModel.loadUpcomingReminders()
                
                Toast.makeText(this, "Bill reminder added successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    
    private fun showBalanceDetailsDialog() {
        val currentIncome = viewModel.totalIncome.value ?: 0.0
        val currentExpense = viewModel.totalExpense.value ?: 0.0
        val currentBalance = viewModel.balance.value ?: 0.0
        
        val message = """
            📊 **Financial Summary**
            
            💰 **Total Income**: $${String.format("%.2f", currentIncome)}
            💸 **Total Expenses**: $${String.format("%.2f", currentExpense)}
            💳 **Current Balance**: $${String.format("%.2f", currentBalance)}
            
            📈 **Net Savings**: $${String.format("%.2f", currentBalance)}
            
            ${if (currentBalance > 0) "✅ You're in a good financial position!" else "⚠️ Consider reducing expenses or increasing income."}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Balance Details")
            .setMessage(message)
            .setPositiveButton("Add Income") { _, _ ->
                showAddTransactionDialog(TransactionType.INCOME)
            }
            .setNegativeButton("Add Expense") { _, _ ->
                showAddTransactionDialog(TransactionType.EXPENSE)
            }
            .setNeutralButton("Close", null)
            .show()
    }
    
    private fun showSimpleReceiptDialog(imagePath: String, date: String?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        
        // Set default values for receipt
        val amountEditText = dialogView.findViewById<EditText>(R.id.editTextAmount)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val categorySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.categorySpinner)
        val dateEditText = dialogView.findViewById<EditText>(R.id.editTextDate)
        
        // Set default values
        descriptionEditText.setText("Receipt")
        dateEditText.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
        
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("Add Receipt Transaction")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
                val description = descriptionEditText.text.toString()
                val category = categorySpinner.text.toString()
                
                if (amount > 0 && category.isNotEmpty()) {
                    val transaction = Transaction(
                        amount = amount,
                        description = description,
                        category = category,
                        type = TransactionType.EXPENSE,
                        date = LocalDateTime.now(),
                        imagePath = imagePath
                    )
                    viewModel.addTransaction(transaction)
                    Toast.makeText(this, "Receipt transaction added successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please enter a valid amount and select a category", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }
    
    private fun setupClickListeners() {
        try {
            Log.d("MainActivity", "Setting up click listeners...")
            
            binding.btnAddReminder.setOnClickListener {
                Log.d("MainActivity", "Add reminder button clicked")
                showAddReminderDialog()
            }
            
            // Debug: Add a long press to refresh reminders
            binding.btnAddReminder.setOnLongClickListener {
                Log.d("MainActivity", "Long press on add reminder button - refreshing reminders")
                viewModel.refreshData()
                viewModel.debugCheckDatabase()
                
                // Test notification
                val notificationHelper = com.example.walletlens.notification.NotificationHelper(this)
                notificationHelper.showGeneralNotification(
                    "Test Notification",
                    "This is a test notification to verify the system is working!"
                )
                
                // Test: Add a reminder for today
                val testReminder = com.example.walletlens.data.entity.Reminder(
                    title = "Test Bill",
                    description = "Test reminder for debugging",
                    amount = 50.0,
                    dueDate = LocalDateTime.now().plusDays(1),
                    category = "Test"
                )
                viewModel.addReminder(testReminder)
                
                // Test: Update widget
                updateWidget()
                
                // Test: Add a transaction to see if widget updates
                val testTransaction = com.example.walletlens.data.entity.Transaction(
                    amount = 25.50,
                    description = "Test transaction for widget",
                    category = "Test",
                    type = com.example.walletlens.data.entity.TransactionType.EXPENSE,
                    date = LocalDateTime.now()
                )
                viewModel.addTransaction(testTransaction)
                
                true
            }
            
            // Add click listeners for summary cards
            binding.cardIncome.setOnClickListener {
                Log.d("MainActivity", "Income card clicked")
                // Add visual feedback
                binding.cardIncome.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    binding.cardIncome.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
                showAddTransactionDialog(TransactionType.INCOME)
            }
            
            binding.cardExpense.setOnClickListener {
                Log.d("MainActivity", "Expense card clicked")
                // Add visual feedback
                binding.cardExpense.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    binding.cardExpense.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
                showAddTransactionDialog(TransactionType.EXPENSE)
            }
            
            binding.cardBalance.setOnClickListener {
                Log.d("MainActivity", "Balance card clicked")
                // Add visual feedback
                binding.cardBalance.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    binding.cardBalance.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
                showBalanceDetailsDialog()
            }
            
            Log.d("MainActivity", "Setting up bottom navigation listener...")
            binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
                Log.d("MainActivity", "Bottom navigation clicked: ${menuItem.itemId}")
                when (menuItem.itemId) {
                    R.id.navigation_home -> {
                        Log.d("MainActivity", "Home navigation selected")
                        // Already on home, do nothing
                        true
                    }
                                          R.id.navigation_add_transaction -> {
                          Log.d("MainActivity", "Add transaction navigation selected, showing dialog")
                          showAddTransactionDialog(TransactionType.EXPENSE)
                          true
                      }
                    R.id.navigation_camera -> {
                        Log.d("MainActivity", "Camera navigation selected, launching ReceiptScannerActivity")
                        val intent = Intent(this, ReceiptScannerActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.navigation_receipt -> {
                        Log.d("MainActivity", "Receipt navigation selected, launching TransactionsActivity")
                        val intent = Intent(this, TransactionsActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> {
                        Log.d("MainActivity", "Unknown navigation item: ${menuItem.itemId}")
                        false
                    }
                }
            }
            
            Log.d("MainActivity", "Click listeners setup completed successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up click listeners: ${e.message}", e)
        }
    }

}