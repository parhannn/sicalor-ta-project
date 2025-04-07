package com.example.sicalor.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.sicalor.R
import com.example.sicalor.databinding.ActivityMainBinding
import com.example.sicalor.ui.auth.LoginActivity
import com.example.sicalor.ui.scan.CameraActivity
import com.example.sicalor.ui.user.FormActivity
import com.example.sicalor.utils.InitApp
import com.example.sicalor.utils.MonthlyReminderReceiver
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNavView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var userId: String
    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Camera permission granted.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, CameraActivity::class.java))
            } else {
                Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    var isGained = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkExactAlarmPermission(this)) {
            setMonthlyReminder(this)
        } else {
            requestExactAlarmPermission(this)
        }

        setMonthlyReminder(this)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (InitApp().isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        auth = Firebase.auth
        userId = auth.currentUser!!.uid
        database = Firebase.database.reference.child("UserData")
            .child(userId)
        val firebaseUser = auth.currentUser

        checkUserData()

        if (firebaseUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        bottomNavView = binding.navView

        binding.floatingButton.setOnClickListener {
            checkCameraPermission()
        }

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
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

        val fromNotification = intent.getBooleanExtra("from_notification", false)

        if (fromNotification) {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, FormActivity::class.java))
            }, 500) // atau setelah load selesai
        }
    }

    private fun checkUserData() {
        database.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists() && !isFinishing && !isDestroyed) {
                startActivity(Intent(this@MainActivity, FormActivity::class.java))
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                signOut()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startActivity(Intent(this, CameraActivity::class.java))
            }

            else -> {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun signOut() {
        lifecycleScope.launch {
            val credentialManager = CredentialManager.create(this@MainActivity)
            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun setMonthlyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MonthlyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun checkExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1234
    }
}