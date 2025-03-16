package com.example.sicalor.ui.landing

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.sicalor.R
import com.example.sicalor.adapter.OnboardingAdapter
import com.example.sicalor.databinding.ActivityOnboardingBinding
import com.example.sicalor.ui.auth.LoginActivity

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val delayMillis = 3000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        supportActionBar?.hide()

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasOnboarded = sharedPreferences.getBoolean("has_onboarded", false)

        if (hasOnboarded) {
            updateUI()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        val viewPager = binding.viewPager
        val indicatorLayout = binding.indicatorLayout
        val buttonStart = binding.buttonStart
        val loginButton = binding.loginButton

        val adapter = OnboardingAdapter(this)
        viewPager.adapter = adapter

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            var currentIndex = 0

            override fun run() {
                if (currentIndex == adapter.itemCount) {
                    currentIndex = 0
                }
                viewPager.setCurrentItem(currentIndex, true)
                currentIndex++
                handler.postDelayed(this, delayMillis)
            }
        }

        handler.postDelayed(runnable, delayMillis)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)


                for (i in 0 until indicatorLayout.childCount) {
                    val indicator = indicatorLayout.getChildAt(i)
                    if (i == position) {
                        indicator.setBackgroundColor(resources.getColor(R.color.colorPrimary))  // Active color
                    } else {
                        indicator.setBackgroundColor(resources.getColor(R.color.colorGrayLight))  // Inactive color
                    }
                }
            }
        })

        buttonStart.text = "GET STARTED"
        buttonStart.setOnClickListener {
            handler.removeCallbacks(runnable)
            sharedPreferences.edit().putBoolean("has_onboarded", true).apply()
            updateUI()
        }
        loginButton.setOnClickListener {
            handler.removeCallbacks(runnable)
            sharedPreferences.edit().putBoolean("has_onboarded", true).apply()
            updateUI()
        }
    }

    private fun updateUI() {
        startActivity(Intent(this@OnboardingActivity, LoginActivity::class.java))
        finish()
    }
}