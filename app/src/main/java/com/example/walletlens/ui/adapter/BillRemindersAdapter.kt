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
    private val onReminderEdit: (Reminder) -> Unit,
    private val onReminderMarkPaid: (Reminder) -> Unit
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
                tvReminderAmount.text = "$${String.format("%.2f", reminder.amount)}"
                tvReminderDueDate.text = reminder.dueDate.format(dateFormatter)

                // Set the visual state of the paid button
                btnMarkPaid.isActivated = reminder.isCompleted

                // Set click listeners
                root.setOnClickListener {
                    onReminderClick(reminder)
                }

                btnEdit.setOnClickListener {
                    onReminderEdit(reminder)
                }
                
                btnMarkPaid.setOnClickListener {
                    onReminderMarkPaid(reminder)
                }
            }
        }
    }
} 