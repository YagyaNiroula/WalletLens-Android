package com.example.walletlens.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walletlens.data.entity.Reminder
import com.example.walletlens.databinding.ItemBillReminderBinding
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class BillRemindersAdapter(
    private val onReminderClick: (Reminder) -> Unit,
    private val onReminderComplete: (Long) -> Unit
) : RecyclerView.Adapter<BillRemindersAdapter.ReminderViewHolder>() {

    private var reminders: List<Reminder> = emptyList()
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    fun updateReminders(newReminders: List<Reminder>) {
        reminders = newReminders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemBillReminderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(reminders[position])
    }

    override fun getItemCount(): Int = reminders.size

    inner class ReminderViewHolder(
        private val binding: ItemBillReminderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reminder: Reminder) {
            binding.apply {
                tvReminderTitle.text = reminder.title
                tvReminderDescription.text = reminder.description
                tvReminderAmount.text = "$${String.format("%.2f", reminder.amount)}"
                tvReminderDueDate.text = "Due: ${reminder.dueDate.format(dateFormatter)}"
                tvReminderCategory.text = reminder.category

                // Set click listeners
                root.setOnClickListener {
                    onReminderClick(reminder)
                }

                btnComplete.setOnClickListener {
                    onReminderComplete(reminder.id)
                }

                // Set urgency color based on days until due
                val daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(),
                    reminder.dueDate.toLocalDate()
                )

                when {
                    daysUntilDue < 0 -> {
                        // Overdue
                        root.setCardBackgroundColor(
                            root.context.getColor(android.R.color.holo_red_light)
                        )
                        tvReminderDueDate.text = "OVERDUE"
                    }
                    daysUntilDue == 0L -> {
                        // Due today
                        root.setCardBackgroundColor(
                            root.context.getColor(android.R.color.holo_orange_light)
                        )
                        tvReminderDueDate.text = "Due Today"
                    }
                    daysUntilDue <= 3 -> {
                        // Due soon
                        root.setCardBackgroundColor(
                            root.context.getColor(android.R.color.holo_orange_light)
                        )
                    }
                    else -> {
                        // Not urgent
                        root.setCardBackgroundColor(
                            root.context.getColor(android.R.color.white)
                        )
                    }
                }
            }
        }
    }
} 