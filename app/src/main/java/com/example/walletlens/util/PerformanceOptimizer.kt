package com.example.walletlens.util

import android.content.Context
import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PerformanceOptimizer {
    
    private const val TAG = "PerformanceOptimizer"
    private const val MAX_CACHE_SIZE_MB = 50L
    private const val MAX_IMAGE_CACHE_SIZE_MB = 20L
    
    /**
     * Clean up old cache files to free memory
     */
    suspend fun cleanupCache(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = context.cacheDir
                val files = cacheDir.listFiles() ?: return@withContext
                
                var totalSize = 0L
                val sortedFiles = files.sortedBy { it.lastModified() }
                
                // Calculate total cache size
                for (file in sortedFiles) {
                    totalSize += file.length()
                }
                
                // Remove old files if cache is too large
                if (totalSize > MAX_CACHE_SIZE_MB * 1024 * 1024) {
                    val filesToDelete = sortedFiles.take(sortedFiles.size / 2)
                    for (file in filesToDelete) {
                        if (file.delete()) {
                            Log.d(TAG, "Deleted old cache file: ${file.name}")
                        }
                    }
                }
                
                Log.d(TAG, "Cache cleanup completed. Total size: ${totalSize / (1024 * 1024)}MB")
            } catch (e: Exception) {
                Log.e(TAG, "Error during cache cleanup: ${e.message}")
            }
        }
    }
    
    /**
     * Clean up image cache specifically
     */
    suspend fun cleanupImageCache(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val imageCacheDir = File(context.cacheDir, "images")
                if (!imageCacheDir.exists()) return@withContext
                
                val files = imageCacheDir.listFiles() ?: return@withContext
                var totalSize = 0L
                
                for (file in files) {
                    totalSize += file.length()
                }
                
                // Remove old images if cache is too large
                if (totalSize > MAX_IMAGE_CACHE_SIZE_MB * 1024 * 1024) {
                    val sortedFiles = files.sortedBy { it.lastModified() }
                    val filesToDelete = sortedFiles.take(sortedFiles.size / 2)
                    
                    for (file in filesToDelete) {
                        if (file.delete()) {
                            Log.d(TAG, "Deleted old image cache: ${file.name}")
                        }
                    }
                }
                
                Log.d(TAG, "Image cache cleanup completed. Total size: ${totalSize / (1024 * 1024)}MB")
            } catch (e: Exception) {
                Log.e(TAG, "Error during image cache cleanup: ${e.message}")
            }
        }
    }
    
    /**
     * Get current memory usage information
     */
    fun getMemoryInfo(context: Context): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - usedMemory
        
        return "Memory: ${usedMemory / (1024 * 1024)}MB used, " +
               "${availableMemory / (1024 * 1024)}MB available, " +
               "${maxMemory / (1024 * 1024)}MB max"
    }
    
    /**
     * Force garbage collection and memory cleanup
     */
    fun forceMemoryCleanup() {
        try {
            System.gc()
            Log.d(TAG, "Forced memory cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during memory cleanup: ${e.message}")
        }
    }
    
    /**
     * Check if device is running low on memory
     */
    fun isLowMemory(context: Context): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble()) * 100
        
        return memoryUsagePercent > 80.0
    }
    
    /**
     * Optimize app performance based on current conditions
     */
    suspend fun optimizePerformance(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                // Clean up caches
                cleanupCache(context)
                cleanupImageCache(context)
                
                // Force garbage collection if memory is low
                if (isLowMemory(context)) {
                    forceMemoryCleanup()
                    Log.d(TAG, "Low memory detected, performed aggressive cleanup")
                }
                
                Log.d(TAG, "Performance optimization completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during performance optimization: ${e.message}")
            }
        }
    }
} 