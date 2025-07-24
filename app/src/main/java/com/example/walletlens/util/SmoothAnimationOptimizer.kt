package com.example.walletlens.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart

object SmoothAnimationOptimizer {
    
    private const val TAG = "SmoothAnimationOptimizer"
    
    /**
     * Apply smooth fade in animation with optimized performance
     */
    fun fadeIn(view: View, duration: Long = 300L, delay: Long = 0L) {
        try {
            view.alpha = 0f
            view.visibility = View.VISIBLE
            
            view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setStartDelay(delay)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.alpha = 1f
                    }
                })
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Error in fadeIn animation: ${e.message}")
            view.alpha = 1f
            view.visibility = View.VISIBLE
        }
    }
    
    /**
     * Apply smooth fade out animation with optimized performance
     */
    fun fadeOut(view: View, duration: Long = 300L, onEnd: (() -> Unit)? = null) {
        try {
            view.animate()
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = View.GONE
                        onEnd?.invoke()
                    }
                })
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Error in fadeOut animation: ${e.message}")
            view.visibility = View.GONE
            onEnd?.invoke()
        }
    }
    
    /**
     * Apply smooth slide up animation
     */
    fun slideUp(view: View, duration: Long = 300L, delay: Long = 0L) {
        try {
            view.translationY = view.height.toFloat()
            view.visibility = View.VISIBLE
            
            view.animate()
                .translationY(0f)
                .setDuration(duration)
                .setStartDelay(delay)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Error in slideUp animation: ${e.message}")
            view.visibility = View.VISIBLE
        }
    }
    
    /**
     * Apply smooth slide down animation
     */
    fun slideDown(view: View, duration: Long = 300L, onEnd: (() -> Unit)? = null) {
        try {
            view.animate()
                .translationY(view.height.toFloat())
                .setDuration(duration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = View.GONE
                        onEnd?.invoke()
                    }
                })
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Error in slideDown animation: ${e.message}")
            view.visibility = View.GONE
            onEnd?.invoke()
        }
    }
    
    /**
     * Apply smooth scale animation
     */
    fun scaleIn(view: View, duration: Long = 300L, delay: Long = 0L) {
        try {
            view.scaleX = 0f
            view.scaleY = 0f
            view.visibility = View.VISIBLE
            
            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(duration)
                .setStartDelay(delay)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Error in scaleIn animation: ${e.message}")
            view.visibility = View.VISIBLE
        }
    }
    
    /**
     * Apply smooth scale out animation
     */
    fun scaleOut(view: View, duration: Long = 300L, onEnd: (() -> Unit)? = null) {
        try {
            view.animate()
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(duration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = View.GONE
                        onEnd?.invoke()
                    }
                })
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Error in scaleOut animation: ${e.message}")
            view.visibility = View.GONE
            onEnd?.invoke()
        }
    }
    
    /**
     * Apply staggered animations to multiple views
     */
    fun staggerAnimations(views: List<View>, animationType: String = "fadeIn", staggerDelay: Long = 100L) {
        views.forEachIndexed { index, view ->
            val delay = index * staggerDelay
            when (animationType) {
                "fadeIn" -> fadeIn(view, delay = delay)
                "slideUp" -> slideUp(view, delay = delay)
                "scaleIn" -> scaleIn(view, delay = delay)
                else -> fadeIn(view, delay = delay)
            }
        }
    }
    
    /**
     * Cancel all animations on a view
     */
    fun cancelAnimations(view: View) {
        try {
            view.animate().cancel()
            view.clearAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling animations: ${e.message}")
        }
    }
    
    /**
     * Check if view is currently animating
     */
    fun isAnimating(view: View): Boolean {
        return try {
            view.animation != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Apply optimized card animation
     */
    fun animateCard(view: View, duration: Long = 200L) {
        try {
            view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(duration / 2)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(duration / 2)
                        .start()
                }
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Error in card animation: ${e.message}")
        }
    }
} 