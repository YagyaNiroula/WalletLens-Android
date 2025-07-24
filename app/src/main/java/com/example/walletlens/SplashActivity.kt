package com.example.walletlens

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.walletlens.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySplashBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Start animations
        startAnimations()
        
        // Set up the "Let's Go" button
        binding.btnLetsGo.setOnClickListener {
            navigateToMain()
        }
        
        // Auto-navigate after 3 seconds for testing
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToMain()
        }, 3000)
    }
    
    private fun startAnimations() {
        // Fade in the title and subtitle
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.tvAppTitle.startAnimation(fadeIn)
        binding.tvAppSubtitle.startAnimation(fadeIn)
        
        // Slide up the button with delay
        Handler(Looper.getMainLooper()).postDelayed({
            val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            binding.btnLetsGo.startAnimation(slideUp)
        }, 1000)
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
} 