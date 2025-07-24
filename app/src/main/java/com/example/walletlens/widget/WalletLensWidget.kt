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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class WalletLensWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_wallet_lens)
        
        // Set up click listener to open the app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Load today's spending data
        CoroutineScope(Dispatchers.IO).launch {
            val database = WalletLensDatabase.getDatabase(context)
            val transactionDao = database.transactionDao()
            
            val today = LocalDateTime.now()
            val startOfDay = today.withHour(0).withMinute(0).withSecond(0)
            val endOfDay = today.withHour(23).withMinute(59).withSecond(59)
            
            val todayExpenses = transactionDao.getTotalByType(
                TransactionType.EXPENSE, startOfDay, endOfDay
            )
            
            // Update the widget UI on the main thread
            context.mainExecutor.execute {
                val totalExpense = todayExpenses.value ?: 0.0
                views.setTextViewText(R.id.widget_today_spending, "$${String.format("%.2f", totalExpense)}")
                views.setTextViewText(R.id.widget_date, today.toLocalDate().toString())
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
} 