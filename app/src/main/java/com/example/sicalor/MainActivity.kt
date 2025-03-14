package com.example.sicalor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.sicalor.databinding.ActivityMainBinding
import com.example.sicalor.scan.CameraActivity
import com.example.sicalor.utils.InitApp
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNavView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (InitApp().isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        bottomNavView = binding.navView

        binding.floatingButton.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        val navController = Navigation.findNavController(this, R.id.navhost)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_food,
                R.id.navigation_schedule,
                R.id.navigation_profile
            )
        )
        supportActionBar?.hide()
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavView.setupWithNavController(navController)
    }
}