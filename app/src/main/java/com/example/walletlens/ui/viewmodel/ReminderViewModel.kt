package com.example.walletlens.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walletlens.data.entity.Reminder
import com.example.walletlens.data.repository.ReminderRepository
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ReminderViewModel(private val reminderRepository: ReminderRepository) : ViewModel() {
    
    private val _reminders = MutableLiveData<List<Reminder>>()
    val reminders: LiveData<List<Reminder>> = _reminders
    
    private val _upcomingReminders = MutableLiveData<List<Reminder>>()
    val upcomingReminders: LiveData<List<Reminder>> = _upcomingReminders
    
    init {
        loadActiveReminders()
        loadUpcomingReminders()
    }
    
    private fun loadActiveReminders() {
        viewModelScope.launch {
            reminderRepository.getAllActiveReminders().observeForever { reminders ->
                _reminders.value = reminders
            }
        }
    }
    
    private fun loadUpcomingReminders() {
        val now = LocalDateTime.now()
        val nextWeek = now.plusDays(7)
        
        viewModelScope.launch {
            reminderRepository.getRemindersByDateRange(now, nextWeek).observeForever { reminders ->
                _upcomingReminders.value = reminders
            }
        }
    }
    
    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.insertReminder(reminder)
        }
    }
    
    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.updateReminder(reminder)
        }
    }
    
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(reminder)
        }
    }
    
    fun markReminderAsCompleted(id: Long) {
        viewModelScope.launch {
            reminderRepository.markReminderAsCompleted(id)
        }
    }
    
    fun refreshReminders() {
        loadActiveReminders()
        loadUpcomingReminders()
    }
} 