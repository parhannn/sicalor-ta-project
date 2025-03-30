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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sicalor.R
import com.example.sicalor.adapter.TodayPlanAdapter
import com.example.sicalor.databinding.FragmentHomeBinding
import com.example.sicalor.ui.MainActivity
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var userId: String
    private lateinit var adapter: TodayPlanAdapter
    private lateinit var recyclerView: RecyclerView
    private var _binding: FragmentHomeBinding? = null
    private var calorieTarget: Double = 0.0
    private var calorieConsumedToday: Double = 0.0
    private val binding get() = _binding!!
    private var selectedPlanType: String = "Breakfast"
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
        lifecycleScope.launch {
            getUserData()
            setupSpinner()
            delay(500)
            loadAllMealPlan(dateMealToday)
            loadMealPlan(dateMealToday)
        }
        setupRecyclerView()
    }

    private fun setupSpinner() {
        val mealTypes = arrayOf("Breakfast", "Lunch", "Dinner")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mealTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.planTypeSpinner.adapter = adapter

        binding.planTypeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedPlanType = mealTypes[position]
                    loadMealPlan(dateMealToday)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private suspend fun getUserData(){
        val userDataDeferred = CompletableDeferred<Unit>()

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    for (userSnapshot in snapshot.children) {
                        val userData = userSnapshot.getValue(UserData::class.java)
                        if (userData != null) {
                            val activityLevel = when (userData.activity) {
                                "Very Low" -> 1.2
                                "Low" -> 1.35
                                "Medium" -> 1.5
                                "High" -> 1.75
                                "Very High" -> 1.9
                                else -> 1.0
                            }
                            calorieTarget =
                                userData.bmr?.toDoubleOrNull()?.times(activityLevel) ?: 0.0
                            val formatDailyCalorie = String.format("%.2f", calorieTarget)

                            binding.tvName.text =
                                if (!userData.name.isNullOrEmpty()) "${userData.name}!" else "!"
                            binding.tvDailyCalorie.text = "${formatDailyCalorie} kcal"
                        }
                    }
                    userDataDeferred.complete(Unit)
                } catch (e: Exception) {
                    Log.e("FirebaseError", "Error parsing user data: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
                userDataDeferred.complete(Unit)
            }
        })

        userDataDeferred.await()
    }

    private fun loadMealPlan(date: String) {
        var selectedPlan = selectedPlanType
        database = Firebase.database.reference.child("MealPlanData")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val mealPlanDataList = mutableListOf<MealPlanData>()
                    val mealDataList = mutableListOf<MealData>()

                    for (userSnapshot in snapshot.children) {
                        for (mealPlanDataSnapshot in userSnapshot.children) {
                            val mealPlanData =
                                mealPlanDataSnapshot.getValue(MealPlanData::class.java)
                            if (mealPlanData != null && mealPlanData.userId == userId && mealPlanData.date == date && mealPlanData.type == selectedPlan) {
                                mealPlanDataList.add(mealPlanData)
                                mealDataList.add(mealPlanData.mealData)
                            }
                        }
                    }

                    allPlanList = mealPlanDataList
                    adapter.updateData(mealPlanDataList, mealDataList)
                } else {
                    Log.d("DEBUG", "No data available")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error: ${error.message}")
            }
        })
    }

    private fun loadAllMealPlan(date: String) {
        database = Firebase.database.reference.child("MealPlanData")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
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

                    val progress = ((calorieConsumedToday / calorieTarget) * 100).toInt()
                    binding.calorieProgressBar.progress = progress.coerceIn(0, 100)

                    val mainActivity = requireActivity() as MainActivity
                    if (calorieConsumedToday >= calorieTarget && !mainActivity.isGained) {
                        mainActivity.isGained = true
                        showCalorieNotification()
                    }
                    if (calorieConsumedToday >= calorieTarget) {
                        binding.calorieReachIndicator.visibility = View.VISIBLE
                    }
                } else {
                    Log.d("DEBUG", "No data available")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error: ${error.message}")
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = TodayPlanAdapter(requireContext(), mutableListOf(), mutableListOf())
        recyclerView = binding.rvMealPlanToday
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun authUser() {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            database = Firebase.database.reference.child("UserData").child(userId)
        } else {
            Log.e("FirebaseAuth", "User not logged in!")
        }
    }

    private fun showCalorieNotification() {
        val title = getString(R.string.notification_title)
        val message = getString(R.string.notification_message)

        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
            .setSubText(getString(R.string.notification_subtext))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "channel_01"
        private const val CHANNEL_NAME = "sicalor channel"
    }
}