package com.example.sicalor.ui.landing

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.sicalor.R
import com.example.sicalor.databinding.ActivityOnboardingBinding
import com.example.sicalor.ui.auth.LoginActivity

@Suppress("DEPRECATION")
class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        supportActionBar?.hide()

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasOnboarded = sharedPreferences.getBoolean("has_onboarded", false)
        val window: Window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        if (hasOnboarded) {
            updateUI()
            return
        }

        val buttonStart = binding.button

        buttonStart.text = "GET STARTED"
        buttonStart.setOnClickListener {
            sharedPreferences.edit().putBoolean("has_onboarded", true).apply()
            updateUI()
        }
    }

    private fun updateUI() {
        val fromNotification = intent.getBooleanExtra("from_notification", false)
        val mainIntent = Intent(this@OnboardingActivity, LoginActivity::class.java).apply {
            putExtra("from_notification", fromNotification)
        }
        startActivity(mainIntent)
        finish()
    }
}