package com.example.sicalor.ui.landing

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.sicalor.databinding.ActivitySplashBinding
import com.example.sicalor.utils.InitApp

@Suppress("DEPRECATION")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (InitApp().isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        supportActionBar?.hide()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            goToMainActivity()
        }, 2000)
    }

    private fun goToMainActivity() {
        val fromNotification = intent.getBooleanExtra("from_notification", false)
        val mainIntent = Intent(this@SplashActivity, OnboardingActivity::class.java).apply {
            putExtra("from_notification", fromNotification)
        }
        startActivity(mainIntent)
        finish()
    }
}