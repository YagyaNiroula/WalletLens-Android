package com.example.walletlens.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.data.entity.TransactionType
import com.example.walletlens.databinding.ItemTransactionBinding
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
                tvTransactionTime.text = transaction.date.format(timeFormatter)

                // Set color based on transaction type
                val colorRes = if (transaction.type == TransactionType.INCOME) {
                    android.R.color.holo_green_dark
                } else {
                    android.R.color.holo_red_dark
                }
                tvTransactionAmount.setTextColor(
                    root.context.getColor(colorRes)
                )

                // Set icon based on category (simplified for performance)
                val iconRes = when (transaction.category.lowercase()) {
                    "food & dining" -> android.R.drawable.ic_menu_edit
                    "transportation" -> android.R.drawable.ic_menu_directions
                    "shopping" -> android.R.drawable.ic_menu_gallery
                    "entertainment" -> android.R.drawable.ic_menu_view
                    "utilities" -> android.R.drawable.ic_menu_manage
                    else -> android.R.drawable.ic_menu_info_details
                }
                ivTransactionIcon.setImageResource(iconRes)

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