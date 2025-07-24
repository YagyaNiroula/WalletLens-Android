package com.example.walletlens.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object ImageLoader {
    
    private const val CACHE_DIR = "image_cache"
    private const val MAX_CACHE_SIZE = 50 * 1024 * 1024 // 50MB
    
    fun loadImageWithCache(context: Context, imageView: ImageView, imagePath: String?) {
        if (imagePath.isNullOrEmpty()) {
            return
        }
        
        Glide.with(context)
            .load(imagePath)
            .apply(RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .centerCrop()
                .override(300, 300)) // Limit size for memory optimization
            .into(imageView)
    }
    
    fun loadCircularImage(context: Context, imageView: ImageView, imagePath: String?) {
        if (imagePath.isNullOrEmpty()) {
            return
        }
        
        Glide.with(context)
            .asBitmap()
            .load(imagePath)
            .apply(RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .centerCrop()
                .override(200, 200))
            .into(object : BitmapImageViewTarget(imageView) {
                override fun setResource(resource: Bitmap?) {
                    resource?.let {
                        val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(
                            context.resources, it
                        )
                        circularBitmapDrawable.isCircular = true
                        imageView.setImageDrawable(circularBitmapDrawable)
                    }
                }
            })
    }
    
    fun clearImageCache(context: Context) {
        Glide.get(context).clearMemory()
        Thread {
            Glide.get(context).clearDiskCache()
        }.start()
    }
    
    fun getCacheSize(context: Context): Long {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        return if (cacheDir.exists()) {
            calculateDirSize(cacheDir)
        } else {
            0L
        }
    }
    
    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles()
        files?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirSize(file)
            } else {
                file.length()
            }
        }
        return size
    }
    
    fun cleanupOldCache(context: Context) {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        if (cacheDir.exists() && getCacheSize(context) > MAX_CACHE_SIZE) {
            val files = cacheDir.listFiles()?.sortedBy { it.lastModified() }
            files?.forEach { file ->
                if (getCacheSize(context) > MAX_CACHE_SIZE / 2) {
                    file.delete()
                }
            }
        }
    }
    
    fun saveImageToCache(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val file = File(cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    fun loadImageFromCache(context: Context, fileName: String): Bitmap? {
        return try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            val file = File(cacheDir, fileName)
            if (file.exists()) {
                val inputStream = FileInputStream(file)
                BitmapFactory.decodeStream(inputStream)
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
} 