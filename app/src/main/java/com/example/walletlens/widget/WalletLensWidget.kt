package com.example.walletlens.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.walletlens.MainActivity
import com.example.walletlens.R
import com.example.walletlens.data.WalletLensDatabase
import com.example.walletlens.data.entity.TransactionType
import com.example.walletlens.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WalletLensWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        android.util.Log.d("WalletLensWidget", "onUpdate called with ${appWidgetIds.size} widget IDs")
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            android.util.Log.d("WalletLensWidget", "Manual update received")
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (appWidgetIds != null) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }
    
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_wallet_lens)
        
        // Set up click intent to open the app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Set up quick-add button click intent
        val quickAddIntent = Intent(context, MainActivity::class.java).apply {
            action = "QUICK_ADD_TRANSACTION"
            putExtra("widget_action", "quick_add")
        }
        val quickAddPendingIntent = PendingIntent.getActivity(
            context,
            1,
            quickAddIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_quick_add, quickAddPendingIntent)
        
        // Update widget data
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = WalletLensDatabase.getDatabase(context)
                val transactionRepository = TransactionRepository(database.transactionDao())
                
                val today = LocalDateTime.now()
                val startOfDay = today.withHour(0).withMinute(0).withSecond(0)
                val endOfDay = today.withHour(23).withMinute(59).withSecond(59)
                
                android.util.Log.d("WalletLensWidget", "Fetching transactions for today: ${startOfDay} to ${endOfDay}")
                
                val todayTransactions = transactionRepository.getTransactionsByDateRangeDirect(
                    startOfDay, endOfDay
                )
                
                android.util.Log.d("WalletLensWidget", "Found ${todayTransactions.size} transactions today")
                todayTransactions.forEach { transaction ->
                    android.util.Log.d("WalletLensWidget", "Transaction: ${transaction.type} - ${transaction.description} - $${transaction.amount}")
                }
                
                val todaySpending = todayTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                
                val dateText = today.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                
                android.util.Log.d("WalletLensWidget", "Today's spending: $${String.format("%.2f", todaySpending)}")
                
                // Update UI on main thread
                CoroutineScope(Dispatchers.Main).launch {
                    views.setTextViewText(R.id.widget_today_spending, "$${String.format("%.2f", todaySpending)}")
                    views.setTextViewText(R.id.widget_date, dateText)
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    android.util.Log.d("WalletLensWidget", "Widget updated successfully")
                }
            } catch (e: Exception) {
                android.util.Log.e("WalletLensWidget", "Error updating widget: ${e.message}", e)
                // Handle error gracefully
                CoroutineScope(Dispatchers.Main).launch {
                    views.setTextViewText(R.id.widget_today_spending, "$0.00")
                    views.setTextViewText(R.id.widget_date, "Error")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Widget enabled
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Widget disabled
    }
} 