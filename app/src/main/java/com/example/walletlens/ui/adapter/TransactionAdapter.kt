package com.example.walletlens.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.data.entity.TransactionType
import com.example.walletlens.databinding.ItemTransactionBinding
import com.example.walletlens.R
import java.time.format.DateTimeFormatter

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding, onTransactionClick)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
        private val onTransactionClick: (Transaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun bind(transaction: Transaction) {
            binding.apply {
                tvTransactionDescription.text = transaction.description
                tvTransactionCategory.text = transaction.category
                tvTransactionAmount.text = "$${String.format("%.2f", transaction.amount)}"
                tvTransactionDate.text = transaction.date.format(dateFormatter)

                // Set color based on transaction type
                val colorRes = if (transaction.type == TransactionType.INCOME) {
                    android.R.color.holo_green_dark
                } else {
                    android.R.color.holo_red_dark
                }
                tvTransactionAmount.setTextColor(
                    root.context.getColor(colorRes)
                )

                // Set icon and background based on category and type
                val iconRes: Int
                val backgroundColorRes: Int
                
                when {
                    transaction.type == TransactionType.INCOME -> {
                        when (transaction.category.lowercase()) {
                            "salary" -> {
                                iconRes = R.drawable.ic_salary
                                backgroundColorRes = R.color.income_green
                            }
                            "freelance" -> {
                                iconRes = R.drawable.ic_freelance
                                backgroundColorRes = R.color.income_green
                            }
                            else -> {
                                iconRes = R.drawable.ic_balance
                                backgroundColorRes = R.color.income_green
                            }
                        }
                    }
                    else -> {
                        when (transaction.category.lowercase()) {
                            "food & dining" -> {
                                iconRes = R.drawable.ic_food_dining
                                backgroundColorRes = R.color.expense_red
                            }
                            "transportation" -> {
                                iconRes = R.drawable.ic_transportation
                                backgroundColorRes = R.color.expense_red
                            }
                            else -> {
                                iconRes = R.drawable.ic_transaction
                                backgroundColorRes = R.color.expense_red
                            }
                        }
                    }
                }
                
                ivTransactionIcon.setImageResource(iconRes)
                ivTransactionIcon.background = root.context.getDrawable(backgroundColorRes)

                // Handle click
                root.setOnClickListener {
                    onTransactionClick(transaction)
                }
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
} 