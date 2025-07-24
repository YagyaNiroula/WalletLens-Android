package com.example.walletlens.util

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

object SmoothScrollingOptimizer {
    
    private const val TAG = "SmoothScrollingOptimizer"
    
    /**
     * Apply performance optimizations to RecyclerView for smooth scrolling
     */
    fun optimizeRecyclerView(
        recyclerView: RecyclerView,
        cacheSize: Int = 20,
        maxRecycledViews: Int = 20,
        enableNestedScrolling: Boolean = false
    ) {
        try {
            // Set fixed size for better performance
            recyclerView.setHasFixedSize(true)
            
            // Increase view cache size
            recyclerView.setItemViewCacheSize(cacheSize)
            
            // Set max recycled views
            recyclerView.recycledViewPool.setMaxRecycledViews(0, maxRecycledViews)
            
            // Configure nested scrolling
            recyclerView.isNestedScrollingEnabled = enableNestedScrolling
            
            // Optimize layout manager if it's LinearLayoutManager
            val layoutManager = recyclerView.layoutManager
            if (layoutManager is LinearLayoutManager) {
                layoutManager.isItemPrefetchEnabled = true
                layoutManager.initialPrefetchItemCount = 4
            }
            
            Log.d(TAG, "RecyclerView optimized with cache size: $cacheSize, max recycled views: $maxRecycledViews")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error optimizing RecyclerView: ${e.message}")
        }
    }
    
    /**
     * Apply light optimizations for small lists
     */
    fun optimizeSmallList(recyclerView: RecyclerView) {
        optimizeRecyclerView(recyclerView, cacheSize = 10, maxRecycledViews = 10)
    }
    
    /**
     * Apply heavy optimizations for large lists
     */
    fun optimizeLargeList(recyclerView: RecyclerView) {
        optimizeRecyclerView(recyclerView, cacheSize = 50, maxRecycledViews = 50)
    }
    
    /**
     * Apply medium optimizations for medium lists
     */
    fun optimizeMediumList(recyclerView: RecyclerView) {
        optimizeRecyclerView(recyclerView, cacheSize = 20, maxRecycledViews = 20)
    }
    
    /**
     * Clear RecyclerView cache to free memory
     */
    fun clearRecyclerViewCache(recyclerView: RecyclerView) {
        try {
            recyclerView.clearOnScrollListeners()
            recyclerView.recycledViewPool.clear()
            System.gc()
            Log.d(TAG, "RecyclerView cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing RecyclerView cache: ${e.message}")
        }
    }
    
    /**
     * Check if RecyclerView is scrolling smoothly
     */
    fun isScrollingSmoothly(recyclerView: RecyclerView): Boolean {
        return try {
            !recyclerView.isComputingLayout && 
            !recyclerView.isLayoutSuppressed() && 
            recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE
        } catch (e: Exception) {
            Log.e(TAG, "Error checking scroll state: ${e.message}")
            false
        }
    }
    
    /**
     * Get RecyclerView performance metrics
     */
    fun getRecyclerViewMetrics(recyclerView: RecyclerView): String {
        return try {
            val adapter = recyclerView.adapter
            val itemCount = adapter?.itemCount ?: 0
            val recycledViewCount = recyclerView.recycledViewPool.getRecycledViewCount(0)
            
            "RecyclerView Metrics:\n" +
            "Item Count: $itemCount\n" +
            "Recycled Views: $recycledViewCount\n" +
            "Scroll State: ${getScrollStateString(recyclerView.scrollState)}"
        } catch (e: Exception) {
            "Error getting metrics: ${e.message}"
        }
    }
    
    /**
     * Monitor scroll performance and apply optimizations if needed
     */
    fun monitorScrollPerformance(recyclerView: RecyclerView, context: Context) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var lastScrollTime = 0L
            private var scrollCount = 0
            
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        val currentTime = System.currentTimeMillis()
                        if (lastScrollTime > 0) {
                            val scrollDuration = currentTime - lastScrollTime
                            scrollCount++
                            
                            // Log performance metrics
                            if (scrollCount % 10 == 0) { // Log every 10 scrolls
                                Log.d(TAG, "Scroll performance: $scrollDuration ms for scroll #$scrollCount")
                            }
                            
                            // Apply optimizations if scrolling is slow
                            if (scrollDuration > 100) { // More than 100ms is considered slow
                                Log.w(TAG, "Slow scrolling detected: ${scrollDuration}ms")
                                optimizeRecyclerView(recyclerView)
                            }
                        }
                        lastScrollTime = 0L
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        lastScrollTime = System.currentTimeMillis()
                    }
                }
            }
        })
    }
    
    /**
     * Apply automatic optimizations based on list size
     */
    fun applyAutomaticOptimizations(recyclerView: RecyclerView) {
        val adapter = recyclerView.adapter
        val itemCount = adapter?.itemCount ?: 0
        
        when {
            itemCount <= 10 -> optimizeSmallList(recyclerView)
            itemCount <= 50 -> optimizeMediumList(recyclerView)
            else -> optimizeLargeList(recyclerView)
        }
        
        Log.d(TAG, "Applied automatic optimizations for list with $itemCount items")
    }
    
    /**
     * Preload views for smoother scrolling
     */
    fun preloadViews(recyclerView: RecyclerView, preloadCount: Int = 5) {
        try {
            val layoutManager = recyclerView.layoutManager
            if (layoutManager is LinearLayoutManager) {
                layoutManager.initialPrefetchItemCount = preloadCount
                layoutManager.isItemPrefetchEnabled = true
            }
            Log.d(TAG, "Preloaded $preloadCount views for smoother scrolling")
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading views: ${e.message}")
        }
    }
    
    private fun getScrollStateString(scrollState: Int): String {
        return when (scrollState) {
            RecyclerView.SCROLL_STATE_IDLE -> "IDLE"
            RecyclerView.SCROLL_STATE_DRAGGING -> "DRAGGING"
            RecyclerView.SCROLL_STATE_SETTLING -> "SETTLING"
            else -> "UNKNOWN"
        }
    }
} 