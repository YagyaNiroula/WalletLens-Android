package com.example.walletlens

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.walletlens.notification.NotificationScheduler

class WalletLensApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager
        val config = Configuration.Builder()
            .build()
        WorkManager.initialize(this, config)
        
        // Initialize notification scheduling
        NotificationScheduler.scheduleBudgetWarningCheck(this)
    }
} 