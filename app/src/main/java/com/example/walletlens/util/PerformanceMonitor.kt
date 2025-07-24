package com.example.walletlens.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object PerformanceMonitor {
    
    private const val TAG = "PerformanceMonitor"
    private val operationTimes = ConcurrentHashMap<String, Long>()
    private val operationCounts = ConcurrentHashMap<String, Int>()
    
    /**
     * Start timing an operation
     */
    fun startOperation(operationName: String) {
        operationTimes[operationName] = System.currentTimeMillis()
    }
    
    /**
     * End timing an operation and log the result
     */
    fun endOperation(operationName: String) {
        val startTime = operationTimes[operationName] ?: return
        val duration = System.currentTimeMillis() - startTime
        
        operationCounts[operationName] = (operationCounts[operationName] ?: 0) + 1
        
        if (duration > 100) { // Log slow operations
            Log.w(TAG, "Slow operation detected: $operationName took ${duration}ms")
        }
        
        operationTimes.remove(operationName)
    }
    
    /**
     * Get performance statistics
     */
    suspend fun getPerformanceStats(context: Context): String {
        return withContext(Dispatchers.IO) {
            val memoryInfo = PerformanceOptimizer.getMemoryInfo(context)
            val isLowMemory = PerformanceOptimizer.isLowMemory(context)
            
            val stats = StringBuilder()
            stats.append("=== Performance Statistics ===\n")
            stats.append("Memory: $memoryInfo\n")
            stats.append("Low Memory: $isLowMemory\n")
            stats.append("Operation Counts: ${operationCounts.toMap()}\n")
            
            stats.toString()
        }
    }
    
    /**
     * Clear performance data
     */
    fun clearStats() {
        operationTimes.clear()
        operationCounts.clear()
        Log.d(TAG, "Performance statistics cleared")
    }
    
    /**
     * Monitor database operation performance
     */
    suspend fun <T> monitorDatabaseOperation(operationName: String, operation: suspend () -> T): T {
        startOperation(operationName)
        return try {
            operation()
        } finally {
            endOperation(operationName)
        }
    }
    
    /**
     * Monitor UI operation performance
     */
    fun <T> monitorUIOperation(operationName: String, operation: () -> T): T {
        startOperation(operationName)
        return try {
            operation()
        } finally {
            endOperation(operationName)
        }
    }
} 