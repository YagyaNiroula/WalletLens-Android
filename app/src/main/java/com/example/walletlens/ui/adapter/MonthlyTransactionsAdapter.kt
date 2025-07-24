package com.example.walletlens.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.databinding.ItemMonthHeaderBinding
import com.example.walletlens.databinding.ItemTransactionBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.YearMonth

class MonthlyTransactionsAdapter(
    private val onTransactionClick: (Transaction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var monthlyData: List<MonthlySection> = emptyList()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TRANSACTION = 1
    }

    fun updateTransactions(transactions: List<Transaction>) {
        monthlyData = groupTransactionsByMonth(transactions)
        notifyDataSetChanged()
    }

    private fun groupTransactionsByMonth(transactions: List<Transaction>): List<MonthlySection> {
        val grouped = transactions.groupBy { transaction ->
            YearMonth.from(transaction.date)
        }.toSortedMap(compareByDescending { it })

        return grouped.map { (yearMonth, monthTransactions) ->
            MonthlySection(
                yearMonth = yearMonth,
                transactions = monthTransactions.sortedByDescending { it.date }
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        var currentPosition = 0
        for (section in monthlyData) {
            if (currentPosition == position) {
                return VIEW_TYPE_HEADER
            }
            currentPosition += 1 + section.transactions.size
            if (position < currentPosition) {
                return VIEW_TYPE_TRANSACTION
            }
        }
        return VIEW_TYPE_TRANSACTION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemMonthHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                MonthHeaderViewHolder(binding)
            }
            VIEW_TYPE_TRANSACTION -> {
                val binding = ItemTransactionBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                TransactionViewHolder(binding, onTransactionClick)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MonthHeaderViewHolder -> {
                val section = getSectionForPosition(position)
                holder.bind(section)
            }
            is TransactionViewHolder -> {
                val transaction = getTransactionForPosition(position)
                holder.bind(transaction)
            }
        }
    }

    override fun getItemCount(): Int {
        return monthlyData.sumOf { 1 + it.transactions.size }
    }

    private fun getSectionForPosition(position: Int): MonthlySection {
        var currentPosition = 0
        for (section in monthlyData) {
            if (currentPosition == position) {
                return section
            }
            currentPosition += 1 + section.transactions.size
        }
        throw IllegalArgumentException("Position $position not found")
    }

    private fun getTransactionForPosition(position: Int): Transaction {
        var currentPosition = 0
        for (section in monthlyData) {
            currentPosition += 1 // Skip header
            if (position < currentPosition + section.transactions.size) {
                return section.transactions[position - currentPosition]
            }
            currentPosition += section.transactions.size
        }
        throw IllegalArgumentException("Position $position not found")
    }

    class MonthHeaderViewHolder(
        private val binding: ItemMonthHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: MonthlySection) {
            val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
            binding.tvMonthHeader.text = section.yearMonth.format(monthFormatter)
            
            val totalIncome = section.transactions
                .filter { it.type == com.example.walletlens.data.entity.TransactionType.INCOME }
                .sumOf { it.amount }
            val totalExpense = section.transactions
                .filter { it.type == com.example.walletlens.data.entity.TransactionType.EXPENSE }
                .sumOf { it.amount }
            
            binding.tvMonthIncome.text = "Income: $${String.format("%.2f", totalIncome)}"
            binding.tvMonthExpense.text = "Expense: $${String.format("%.2f", totalExpense)}"
            binding.tvTransactionCount.text = "${section.transactions.size} transactions"
        }
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
        private val onTransactionClick: (Transaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                tvTransactionDescription.text = transaction.description
                tvTransactionCategory.text = transaction.category
                tvTransactionAmount.text = "$${String.format("%.2f", transaction.amount)}"
                
                val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
                tvTransactionDate.text = transaction.date.format(dateFormatter)
                tvTransactionTime.text = transaction.date.format(timeFormatter)
                
                // Set color based on transaction type
                val color = if (transaction.type == com.example.walletlens.data.entity.TransactionType.INCOME) {
                    android.graphics.Color.parseColor("#4CAF50") // Green for income
                } else {
                    android.graphics.Color.parseColor("#F44336") // Red for expense
                }
                tvTransactionAmount.setTextColor(color)
                
                root.setOnClickListener {
                    onTransactionClick(transaction)
                }
            }
        }
    }

    data class MonthlySection(
        val yearMonth: YearMonth,
        val transactions: List<Transaction>
    )
} 