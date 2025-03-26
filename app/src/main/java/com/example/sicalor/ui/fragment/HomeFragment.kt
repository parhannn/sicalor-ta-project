package com.example.sicalor.ui.fragment

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sicalor.R
import com.example.sicalor.adapter.SchedulePlanAdapter
import com.example.sicalor.databinding.FragmentHomeBinding
import com.example.sicalor.ui.data.MealData
import com.example.sicalor.ui.data.MealPlanData
import com.example.sicalor.ui.data.UserData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var userId: String
    private lateinit var adapter: SchedulePlanAdapter
    private lateinit var recyclerView: RecyclerView
    private var _binding: FragmentHomeBinding? = null
    private var calorieTarget: Double = 0.0
    private var calorieConsumedToday: Double = 0.0
    private var isGained: Boolean = false
    private val binding get() = _binding!!
    private var dateMealToday: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
        Date()
    ).toString()
    private var allPlanList: List<MealPlanData> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authUser()
        setupUI()
    }

    private fun setupUI() {
        getUserData()
        setupRecyclerView()
        loadMealPlan(dateMealToday)
    }

    private fun loadMealPlan(date: String) {
        database = Firebase.database.reference.child("MealPlanData")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (calorieTarget == 0.0) {
                        Log.d("DEBUG", "Menunggu perhitungan kebutuhan kalori harian...")
                        return
                    }

                    if (snapshot.exists()) {
                        val mealPlanDataList = mutableListOf<MealPlanData>()
                        val mealDataList = mutableListOf<MealData>()
                        var totalCalories = 0.0

                        for (userSnapshot in snapshot.children) {
                            for (mealPlanDataSnapshot in userSnapshot.children) {
                                val mealPlanData =
                                    mealPlanDataSnapshot.getValue(MealPlanData::class.java)
                                if (mealPlanData != null && mealPlanData.userId == userId && mealPlanData.date == date) {
                                    mealPlanDataList.add(mealPlanData)
                                    mealDataList.add(mealPlanData.mealData)
                                    totalCalories += mealPlanData.mealData.calories.toDouble()
                                }
                            }
                        }

                        calorieConsumedToday = totalCalories
                        binding.tvCalorieConsumed.text = String.format("%.2f", totalCalories)

                        allPlanList = mealPlanDataList
                        adapter.updateData(mealPlanDataList, mealDataList)

                        if (calorieConsumedToday >= calorieTarget && !isGained) {
                            isGained = true
                            showCalorieNotification()
                        }
                    } else {
                        Log.d("DEBUG", "No data available")
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseError", "Error: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error: ${error.message}")
            }
        })
    }


    private fun getUserData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val userData = userSnapshot.getValue(UserData::class.java)

                    if (userData != null) {
                        val activityLevel: Double = when (userData.activity) {
                            "Very Low" -> 1.2
                            "Low" -> 1.35
                            "Medium" -> 1.5
                            "High" -> 1.75
                            "Very High" -> 1.9
                            else -> 0.0
                        }
                        val dailyCalorie = userData.bmr.toDouble() * activityLevel
                        val formatDailyCalorie = String.format("%.2f", dailyCalorie)

                        binding.tvName.text = if (userData.name.isNotEmpty()) "${userData.name}!" else "!"
                        binding.tvDailyCalorie.text = "${formatDailyCalorie} kcal"
                        calorieTarget = formatDailyCalorie.toDouble()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = SchedulePlanAdapter(requireContext(), mutableListOf(), mutableListOf())
        recyclerView = binding.rvMealPlanToday
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun authUser() {
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser!!.uid
        database = Firebase.database.reference.child("UserData")
            .child(userId)
    }

    private fun showCalorieNotification() {
        val title = getString(R.string.notification_title)
        val message = getString(R.string.notification_message)

        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = CHANNEL_NAME
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.app_logo_notification)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSubText(getString(R.string.notification_subtext))

        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "channel_01"
        private const val CHANNEL_NAME = "sicalor channel"
    }
}