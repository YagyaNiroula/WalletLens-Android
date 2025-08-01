package com.example.walletlens.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.walletlens.data.WalletLensDatabase
import com.example.walletlens.data.entity.Budget
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.data.entity.TransactionType
import com.example.walletlens.data.repository.BudgetRepository
import com.example.walletlens.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

class BudgetWarningWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = WalletLensDatabase.getDatabase(context)
            val budgetRepository = BudgetRepository(database.budgetDao())
            val transactionRepository = TransactionRepository(database.transactionDao())
            
            val now = LocalDateTime.now()
            val activeBudgets = budgetRepository.getActiveBudgets()
            
            for (budget in activeBudgets) {
                // Calculate current spending for this budget category
                val currentSpending = calculateCurrentSpending(
                    transactionRepository,
                    budget,
                    now
                )
                
                val percentageUsed = (currentSpending / budget.amount) * 100
                
                // Send notification based on spending thresholds
                if (shouldSendBudgetWarning(percentageUsed, budget.id)) {
                    val notificationHelper = NotificationHelper(context)
                    notificationHelper.showBudgetWarning(budget, currentSpending, percentageUsed)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private suspend fun calculateCurrentSpending(
        transactionRepository: TransactionRepository,
        budget: Budget,
        now: LocalDateTime
    ): Double {
        val startDate = when (budget.period.lowercase()) {
            "weekly" -> now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0)
            "monthly" -> now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
            "yearly" -> now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0)
            else -> budget.startDate
        }
        
        val endDate = when (budget.period.lowercase()) {
            "weekly" -> startDate.plusWeeks(1).minusSeconds(1)
            "monthly" -> startDate.plusMonths(1).minusSeconds(1)
            "yearly" -> startDate.plusYears(1).minusSeconds(1)
            else -> budget.endDate
        }
        
        val transactions = transactionRepository.getTransactionsByCategoryAndDateRange(
            budget.category,
            startDate,
            endDate
        )
        
        return transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }
    
    private fun shouldSendBudgetWarning(percentageUsed: Double, budgetId: Long): Boolean {
        // Check if we should send a warning based on spending percentage
        return when {
            percentageUsed >= 100 -> true  // Budget exceeded
            percentageUsed >= 90 -> true   // Almost exceeded
            percentageUsed >= 80 -> true   // Warning threshold
            else -> false
        }
    }
} 