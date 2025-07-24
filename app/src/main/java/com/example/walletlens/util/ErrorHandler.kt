package com.example.walletlens.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.walletlens.R
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.sql.SQLException

sealed class AppError : Exception() {
    data class DatabaseError(override val message: String, override val cause: Throwable? = null) : AppError()
    data class NetworkError(override val message: String, override val cause: Throwable? = null) : AppError()
    data class ValidationError(override val message: String) : AppError()
    data class CacheError(override val message: String, override val cause: Throwable? = null) : AppError()
    data class UnknownError(override val message: String, override val cause: Throwable? = null) : AppError()
}

object ErrorHandler {
    
    private const val TAG = "WalletLens_Error"
    
    fun handleError(context: Context, error: Throwable, showUserFeedback: Boolean = true) {
        val appError = when (error) {
            is AppError -> error
            is SQLException -> AppError.DatabaseError("Database operation failed", error)
            is IOException -> AppError.NetworkError("Network operation failed", error)
            is CancellationException -> return // Don't handle coroutine cancellations
            else -> AppError.UnknownError("An unexpected error occurred", error)
        }
        
        // Log the error
        logError(appError)
        
        // Show user feedback if requested
        if (showUserFeedback) {
            showUserFriendlyError(context, appError)
        }
    }
    
    fun handleDatabaseError(context: Context, error: Throwable, operation: String) {
        val appError = AppError.DatabaseError("Failed to $operation", error)
        handleError(context, appError)
    }
    
    fun handleValidationError(context: Context, message: String) {
        val appError = AppError.ValidationError(message)
        handleError(context, appError)
    }
    
    fun handleCacheError(context: Context, error: Throwable, operation: String) {
        val appError = AppError.CacheError("Failed to $operation", error)
        handleError(context, appError)
    }
    
    private fun logError(error: AppError) {
        when (error) {
            is AppError.DatabaseError -> {
                Log.e(TAG, "Database Error: ${error.message}", error.cause)
            }
            is AppError.NetworkError -> {
                Log.e(TAG, "Network Error: ${error.message}", error.cause)
            }
            is AppError.ValidationError -> {
                Log.w(TAG, "Validation Error: ${error.message}")
            }
            is AppError.CacheError -> {
                Log.e(TAG, "Cache Error: ${error.message}", error.cause)
            }
            is AppError.UnknownError -> {
                Log.e(TAG, "Unknown Error: ${error.message}", error.cause)
            }
        }
    }
    
    private fun showUserFriendlyError(context: Context, error: AppError) {
        val (title, message, isCritical) = when (error) {
            is AppError.DatabaseError -> {
                Triple(
                    "Database Error",
                    "Unable to access your data. Please try again or restart the app.",
                    true
                )
            }
            is AppError.NetworkError -> {
                Triple(
                    "Connection Error",
                    "Unable to connect to the network. Please check your internet connection.",
                    false
                )
            }
            is AppError.ValidationError -> {
                Triple(
                    "Invalid Input",
                    error.message,
                    false
                )
            }
            is AppError.CacheError -> {
                Triple(
                    "Cache Error",
                    "Unable to load cached data. Loading fresh data instead.",
                    false
                )
            }
            is AppError.UnknownError -> {
                Triple(
                    "Unexpected Error",
                    "Something went wrong. Please try again.",
                    true
                )
            }
        }
        
        if (isCritical) {
            showErrorDialog(context, title, message)
        } else {
            showErrorToast(context, message)
        }
    }
    
    private fun showErrorDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Report Issue") { dialog, _ ->
                reportIssue(context, title, message)
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showErrorToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    private fun reportIssue(context: Context, title: String, message: String) {
        // In a real app, this would send error reports to a service
        Log.i(TAG, "User reported issue: $title - $message")
        Toast.makeText(context, "Issue reported. Thank you!", Toast.LENGTH_SHORT).show()
    }
    
    fun validateAmount(amount: String): Result<Double> {
        return try {
            val parsedAmount = amount.toDouble()
            if (parsedAmount <= 0) {
                Result.failure(IllegalArgumentException("Amount must be greater than 0"))
            } else {
                Result.success(parsedAmount)
            }
        } catch (e: NumberFormatException) {
            Result.failure(IllegalArgumentException("Please enter a valid amount"))
        }
    }
    
    fun validateDescription(description: String): Result<String> {
        return if (description.trim().isEmpty()) {
            Result.failure(IllegalArgumentException("Description cannot be empty"))
        } else if (description.length > 100) {
            Result.failure(IllegalArgumentException("Description is too long (max 100 characters)"))
        } else {
            Result.success(description.trim())
        }
    }
    
    fun validateDate(date: String): Result<String> {
        return if (date.trim().isEmpty()) {
            Result.failure(IllegalArgumentException("Date cannot be empty"))
        } else {
            Result.success(date.trim())
        }
    }
    
    fun validateCategory(category: String): Result<String> {
        return if (category.trim().isEmpty()) {
            Result.failure(IllegalArgumentException("Please select a category"))
        } else {
            Result.success(category.trim())
        }
    }
} 