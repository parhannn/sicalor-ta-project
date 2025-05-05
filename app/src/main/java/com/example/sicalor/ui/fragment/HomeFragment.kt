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
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sicalor.R
import com.example.sicalor.adapter.TodayPlanAdapter
import com.example.sicalor.databinding.FragmentHomeBinding
import com.example.sicalor.ui.MainActivity
import com.example.sicalor.ui.data.CalorieHistoryData
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
    private lateinit var adapter: TodayPlanAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var infoSlideFragment: InfoSlideFragment
    private var _binding: FragmentHomeBinding? = null
    private var calorieTarget: Double = 0.0
    private var calorieConsumedToday: Double = 0.0
    private val binding get() = _binding!!
    private var selectedPlanType: String = "Breakfast"
    private var dateMealToday: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
        Date()
    ).toString()
    private var allPlanList: MutableList<MealPlanData>? = null
    private var carbsNeedGrams: Double = 0.0
    private var proteinNeedGrams: Double = 0.0
    private var fatNeedGrams: Double = 0.0

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

    override fun onDestroyView() {
        super.onDestroyView()
        allPlanList?.clear()
    }

    private fun setupUI() {
        getUserData {
            setupSpinner()
            loadAllMealPlan(dateMealToday)
            loadMealPlan(dateMealToday)
            setupRecyclerView()

            val mainActivity = activity as MainActivity
            if (!mainActivity.isClosed) {
                if (isAdded && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    infoSlideFragment = InfoSlideFragment()
                    infoSlideFragment.show(parentFragmentManager, "InfoSlideFragment")
                }
            }
        }
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

    private fun getUserData(onComplete: () -> Unit){
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val userData = userSnapshot.getValue(UserData::class.java)
                    if (userData != null) {
                        calorieTarget = userData.dailyCalorie.toDouble()
                        val formatDailyCalorie = String.format("%.2f", calorieTarget)

                        binding.tvName.text = if (!userData.name.isNullOrEmpty()) "${userData.name}!" else "!"
                        binding.tvDailyCalorie.text = "$formatDailyCalorie kcal"

                        onComplete()
                        break
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadMealPlan(date: String) {
        var selectedPlan = selectedPlanType
        database = Firebase.database.reference.child("MealPlanData")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    binding.noDataFoundPlaceholder.visibility = View.GONE
                    val mealPlanDataList = mutableListOf<MealPlanData>()
                    val mealDataList = mutableListOf<MealData>()

                    for (userSnapshot in snapshot.children) {
                        for (mealPlanDataSnapshot in userSnapshot.children) {
                            val value = mealPlanDataSnapshot.value

                            if (value is Map<*, *>) {
                                try {
                                    val mealPlanData = mealPlanDataSnapshot.getValue(MealPlanData::class.java)
                                    if (mealPlanData != null &&
                                        mealPlanData.userId == userId &&
                                        mealPlanData.date == date &&
                                        mealPlanData.type == selectedPlan
                                    ) {
                                        mealPlanDataList.add(mealPlanData)
                                        mealDataList.add(mealPlanData.mealData)
                                    }
                                } catch (e: Exception) {
                                    Log.e("FirebaseParseError", "Failed to parse MealPlanData: ${e.message}")
                                }
                            } else {
                                Log.w("Firebase", "Skipping invalid mealPlanData (type not Map): $value")
                            }
                        }
                    }

                    allPlanList = mealPlanDataList

                    if (allPlanList!!.isNotEmpty()) {
                        loadCalorieHistory(date)
                        binding.noDataFoundPlaceholder.visibility = View.GONE
                        adapter.updateData(mealPlanDataList, mealDataList)
                    } else {
                        loadCalorieHistory(date)
                        binding.noDataFoundPlaceholder.visibility = View.VISIBLE
                        adapter.updateData(emptyList(), emptyList())
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

    private fun loadAllMealPlan(date: String) {
        database = Firebase.database.reference.child("MealPlanData")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val mealPlanDataList = mutableListOf<MealPlanData>()
                    val mealDataList = mutableListOf<MealData>()
                    var totalCalories = 0.0
                    var totalCarbs = 0.0
                    var totalFat = 0.0
                    var totalProtein = 0.0
                    var groupOne = 0
                    var groupTwo = 0
                    var groupThree = 0
                    var groupFour = 0
                    var groupFive = 0
                    var groupSix = 0

                    carbsNeedGrams = (calorieTarget * 0.55)
                    proteinNeedGrams = (calorieTarget * 0.15)
                    fatNeedGrams = (calorieTarget * 0.25)

                    for (userSnapshot in snapshot.children) {
                        for (mealPlanDataSnapshot in userSnapshot.children) {
                            val value = mealPlanDataSnapshot.value
                            if (value is Map<*, *>) {
                                try {
                                    val mealPlanData = mealPlanDataSnapshot.getValue(MealPlanData::class.java)
                                    if (mealPlanData != null && mealPlanData.userId == userId && mealPlanData.date == date) {
                                        mealPlanDataList.add(mealPlanData)
                                        mealDataList.add(mealPlanData.mealData)
                                        totalCalories += mealPlanData.mealData.calories.toDouble()
                                        totalCarbs += mealPlanData.mealData.carbs.toDouble()
                                        totalFat += mealPlanData.mealData.fat.toDouble()
                                        totalProtein += mealPlanData.mealData.protein.toDouble()

                                        when (mealPlanData.mealData.group) {
                                            "Golongan 1" -> groupOne++
                                            "Golongan 2" -> groupTwo++
                                            "Golongan 3" -> groupThree++
                                            "Golongan 4" -> groupFour++
                                            "Golongan 5" -> groupFive++
                                            "Golongan 6" -> groupSix++
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("FirebaseParseError", "Failed to parse MealPlanData: ${e.message}")
                                }
                            } else {
                                Log.w("Firebase", "Skipping invalid mealPlanData (type not Map): $value")
                            }
                        }
                    }

                    calorieConsumedToday = totalCalories

                    val formatDailyCarbs = String.format("%.2f", totalCarbs)
                    val formatDailyFat = String.format("%.2f", totalFat)
                    val formatDailyProtein = String.format("%.2f", totalProtein)

                    binding.tvCalorieConsumed.text = String.format("%.2f", totalCalories)
                    binding.tvCarbsConsumed.text = "$formatDailyCarbs g"
                    binding.tvFatConsumed.text = "$formatDailyFat g"
                    binding.tvProteinConsumed.text = "$formatDailyProtein g"

                    val progress = ((calorieConsumedToday / calorieTarget) * 100).toInt()
                    val carbsProgress = ((totalCarbs / carbsNeedGrams) * 100).toInt()
                    val fatProgress = ((totalFat / fatNeedGrams) * 100).toInt()
                    val proteinProgress = ((totalProtein / proteinNeedGrams) * 100).toInt()

                    binding.calorieProgressBar.progress = progress.coerceIn(0, 100)
                    binding.carbsProgressBar.progress = carbsProgress.coerceIn(0, 100)
                    binding.fatProgressBar.progress = fatProgress.coerceIn(0, 100)
                    binding.proteinProgressBar.progress = proteinProgress

                    val mainActivity = requireActivity() as MainActivity
                    if (calorieConsumedToday > calorieTarget && !mainActivity.isGained) {
                        mainActivity.isGained = true
                        showCalorieNotification()
                    }
                    if (calorieConsumedToday > calorieTarget) {
                        binding.calorieReachIndicator.visibility = View.VISIBLE
                    }

                    updateNutrientIndicators(
                        groupFour,
                        totalCarbs,
                        totalProtein,
                        carbsNeedGrams,
                        proteinNeedGrams
                    )
                } else {
                    Log.d("DEBUG", "No data available")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error: ${error.message}")
            }
        })
    }

    private fun updateNutrientIndicators(
        groupFour: Int,
        currentCarbs: Double,
        currentProtein: Double,
        carbsNeedGrams: Double,
        proteinNeedGrams: Double
    ) {
        binding.needCarbsIndicator.visibility = View.GONE
        binding.needProteinIndicator.visibility = View.GONE
        binding.needFiberIndicator.visibility = View.GONE

        if (groupFour == 0) {
            binding.needFiberIndicator.visibility = View.VISIBLE
        }
        if (currentProtein < proteinNeedGrams) {
            binding.needProteinIndicator.visibility = View.VISIBLE
        }
        if (currentCarbs < carbsNeedGrams) {
            binding.needCarbsIndicator.visibility = View.VISIBLE
        }
    }

    private fun loadCalorieHistory(date: String) {
        val calorieRef = Firebase.database.reference
            .child("CalorieHistoryData")
            .child(userId)
            .child(date)

        calorieRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val calorieHistoryData = snapshot.getValue(CalorieHistoryData::class.java)
                    if (calorieHistoryData != null) {
                        binding.tvCalorieConsumedCount.text = calorieHistoryData.updatedConsumed
                        binding.tvCalorieRemainingCount.text = calorieHistoryData.remainingCalories
                    }
                } else {
                    binding.tvCalorieConsumed.text = "0.00"
                    binding.tvCalorieRemainingCount.text = "0.00"
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