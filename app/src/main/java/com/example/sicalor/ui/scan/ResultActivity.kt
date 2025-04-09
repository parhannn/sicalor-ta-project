package com.example.sicalor.ui.scan

import android.R
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sicalor.adapter.MealAdapter
import com.example.sicalor.databinding.ActivityResultBinding
import com.example.sicalor.ui.data.BoundingBox
import com.example.sicalor.ui.data.FoodData
import com.example.sicalor.ui.data.MealData
import com.example.sicalor.ui.data.MealPlanData
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var rvDetectedMeal: RecyclerView
    private lateinit var rvAllMeal: RecyclerView
    private lateinit var detectedMealAdapter: MealAdapter
    private lateinit var allMealAdapter: MealAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var userId: String
    private lateinit var searchView: SearchView
    private var allFoodList: List<FoodData> = emptyList()
    private var currentImageUri: Uri? = null
    private var detectedFoodList: List<BoundingBox> = emptyList()
    private var currentImageBitmap: Bitmap? = null
    private var currentPage = 1
    private var fullFoodList: List<FoodData> = emptyList()
    private var isLoading = false
    private var selectedMeal: MealData? = null
    private val itemsPerPage = 5
    private var selectedDate: String = ""
    private var selectedPlanType: String = "Breakfast"
    private var setPortion: String = ""
    private var filteredFoodList: List<FoodData> = emptyList()
    private var isFiltering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding = ActivityResultBinding.inflate(layoutInflater)

        setContentView(binding.root)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser!!.uid
        database = Firebase.database.reference.child("MealPlanData")

        handleUI()
        setupDatePicker()
        setupPortion()
        setupSpinner()
        setupSubmitButton()
    }

    private fun setupSubmitButton() {
        binding.addMealButton.setOnClickListener {
            if (selectedDate.isEmpty() || selectedMeal == null) {
                Toast.makeText(
                    this@ResultActivity,
                    "Please select date and meal!",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val database = FirebaseDatabase.getInstance().getReference("MealPlanData").child(userId)

            database.get().addOnSuccessListener { snapshot ->
                var isDuplicate = false

                for (mealSnapshot in snapshot.children) {
                    val meal = mealSnapshot.getValue(MealPlanData::class.java)

                    if (meal != null &&
                        meal.mealData.name == selectedMeal!!.name &&
                        meal.type == selectedPlanType &&
                        meal.date == selectedDate
                    ) {
                        isDuplicate = true
                        break
                    }
                }

                if (isDuplicate) {
                    Toast.makeText(
                        this@ResultActivity,
                        "Meal with the same name, date, and type already exists!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val savedMealId = generateMealId()
                    val mealPlanData = MealPlanData(
                        userId,
                        savedMealId,
                        selectedDate,
                        selectedPlanType,
                        selectedMeal!!
                    )

                    Log.d("DEBUG", "MealPlanData to be added: $mealPlanData")

                    database.child(savedMealId).setValue(mealPlanData)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                Snackbar.make(
                                    this@ResultActivity.findViewById(android.R.id.content),
                                    "Meal added successfully!",
                                    Snackbar.LENGTH_SHORT
                                )
                                    .show()
                            } else {
                                Log.e("FirebaseError", "Error: ${it.exception?.message}")
                                Toast.makeText(
                                    this@ResultActivity,
                                    "Failed to add meal",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            }.addOnFailureListener {
                Log.e("FirebaseError", "Error: ${it.message}")
                Toast.makeText(this@ResultActivity, "Error checking meal plan", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun generateMealId(): String {
        val timestamp = System.currentTimeMillis().toString()
        val randomPart = (1000..9999).random().toString()
        return "MEAL_$timestamp$randomPart"
    }

    private fun handleUI() {
        binding.backButton.setOnClickListener { onBackPressed() }
        val imageUriString = intent.getStringExtra(CameraActivity.EXTRA_CAMERAX_IMAGE)
        val imageByteArray = intent.getByteArrayExtra(CameraActivity.EXTRA_CAPTURE_IMAGE)
        val detectionList =
            intent.getSerializableExtra(CameraActivity.EXTRA_DETECTION_LIST) as? ArrayList<BoundingBox>
        val bitmap = imageByteArray?.let {
            BitmapFactory.decodeByteArray(imageByteArray, 0, it.size)
        }

        if (imageUriString != null) {
            currentImageUri = imageUriString.toUri()
            showImageUri()
        }
        if (bitmap != null) {
            currentImageBitmap = bitmap
            showImageBitmap()
        }

        if (detectionList != null) {
            detectedFoodList = detectionList
            val detectedNames = detectedFoodList.map { it.clsName }.distinct()
            fetchFoodFromDatabase(detectedNames)
        } else {
            Toast.makeText(this, "No food detected.", Toast.LENGTH_SHORT).show()
            binding.tvNoFoodDetected.visibility = View.VISIBLE
        }
    }

    private fun showImageUri() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
        }
    }

    private fun fetchFoodFromDatabase(detectedNames: List<String>) {
        database = FirebaseDatabase.getInstance().reference.child("Food")

        val normalizedDetectedNames = detectedNames.map { it.lowercase() }

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val detectedList = mutableListOf<FoodData>()
                val allOtherFoods = mutableListOf<FoodData>()

                for (data in snapshot.children) {
                    val food = data.getValue(FoodData::class.java)
                    if (food != null) {
                        val normalizedFoodName = food.name.lowercase()
                        if (normalizedDetectedNames.any {
                                normalizedFoodName.contains(it) || it.contains(
                                    normalizedFoodName
                                )
                            }) {
                            detectedList.add(food)
                            binding.tvNoFoodDetected.visibility = View.GONE
                        } else {
                            allOtherFoods.add(food)
                            binding.tvNoFoodDetected.visibility = View.VISIBLE
                        }
                    }
                }

                setupRecyclerView(detectedList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error: ${error.message}")
            }
        })
    }

    private fun setupPortion() {
        setPortion = binding.etPortion.text.toString()
    }

    private fun setupDatePicker() {
        binding.selectDate.setOnClickListener {
            val today = MaterialDatePicker.todayInUtcMilliseconds()

            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.from(today))
                .build()

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Meal Plan Date")
                .setSelection(today)
                .setCalendarConstraints(constraints)
                .build()

            datePicker.show(supportFragmentManager, "DATE_PICKER")

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selection))
                binding.selectedDate.text = selectedDate
                Log.d("DEBUG", "Selected Date: $selectedDate")
            }
        }
    }

    private fun setupSpinner() {
        val mealTypes = arrayOf("Breakfast", "Lunch", "Dinner")
        val adapter = ArrayAdapter(this@ResultActivity, R.layout.simple_spinner_item, mealTypes)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
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
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun setupRecyclerView(foodList: List<FoodData>) {
        fullFoodList = foodList
        currentPage = 0
        detectedMealAdapter = MealAdapter(this, mutableListOf())
        rvDetectedMeal = binding.detectedMealRecyclerview
        rvDetectedMeal.layoutManager = LinearLayoutManager(this)
        rvDetectedMeal.adapter = detectedMealAdapter

        loadMoreData(detectedMealAdapter)

        detectedMealAdapter.setOnItemClickCallback(object : MealAdapter.OnItemClickCallback {
            override fun onItemClicked(data: FoodData) {
                handleClick(data)
            }
        })

        rvDetectedMeal.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!isLoading && lastVisibleItem >= totalItemCount - 3) {
                    loadMoreData(detectedMealAdapter)
                }
            }
        })

        allMealAdapter = MealAdapter(this, mutableListOf())
        rvAllMeal = binding.mealRecyclerview
        rvAllMeal.layoutManager = LinearLayoutManager(this)
        rvAllMeal.adapter = allMealAdapter

        loadMoreData(allMealAdapter)

        setupSearchView(allMealAdapter)

        allMealAdapter.setOnItemClickCallback(object : MealAdapter.OnItemClickCallback {
            override fun onItemClicked(data: FoodData) {
                handleClick(data)
            }
        })

        rvAllMeal.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!isLoading && lastVisibleItem >= totalItemCount - 3) {
                    loadMoreData(allMealAdapter)
                }
            }
        })
    }

    private fun handleClick(data: FoodData) {
        val portionText = binding.etPortion.text.toString()
        if (portionText.isEmpty()) {
            Toast.makeText(this@ResultActivity, "Please enter portion!", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val portionValue = portionText.toDouble()
        val newCalorie = (portionValue * data.calories.toDouble()) / data.portion.toDouble()

        selectedMeal = MealData(
            calories = String.format(Locale.ENGLISH, "%.2f", newCalorie),
            name = data.name,
            group = data.group,
            desc = data.desc,
            protein = data.protein,
            carbs = data.carbs,
            fat = data.fat,
            portion = portionText,
            img = data.img
        )
        Log.d("DEBUG", "Selected Meal: $selectedMeal")
    }

    private fun filterFoods(query: String, adapter: MealAdapter) {
        if (query.isEmpty()) {
            isFiltering = false
            currentPage = 1
            adapter.updateData(allFoodList.take(itemsPerPage))
        } else {
            isFiltering = true
            filteredFoodList = allFoodList.filter { it.name.contains(query, ignoreCase = true) }
            currentPage = 1
            adapter.updateData(filteredFoodList.take(itemsPerPage))
        }

        Log.d("FoodFragment", "Filtered foods: ${filteredFoodList.size} for query: $query")
    }

    private fun setupSearchView(adapter: MealAdapter) {
        searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filterFoods(it, adapter) }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterFoods(it, adapter) }
                return false
            }
        })
        searchView.setOnCloseListener {
            isFiltering = false
            currentPage = 1
            adapter.updateData(allFoodList.take(itemsPerPage))
            false
        }

        loadMeal()
    }

    private fun loadMeal() {
        database = Firebase.database.reference.child("Food")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val foodList = mutableListOf<FoodData>()

                    for (foodSnapshot in snapshot.children) {
                        val food = foodSnapshot.getValue(FoodData::class.java)
                        if (food != null) {
                            foodList.add(food)
                        }
                    }

                    allFoodList = foodList
                    allMealAdapter.updateData(allFoodList.take(itemsPerPage))
                } else {
                    Log.d("TAG", "No data available")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
    }

    private fun loadMoreData(adapter: MealAdapter) {
        if (isLoading) return
        isLoading = true

        Handler(Looper.getMainLooper()).postDelayed({
            val start = currentPage * itemsPerPage
            val end = minOf(start + itemsPerPage, fullFoodList.size)

            if (start < end) {
                val newData = fullFoodList.subList(start, end)
                adapter.loadMoreData(newData)
                currentPage++
            }

            isLoading = false
        }, 500)
    }

    private fun showImageBitmap() {
        currentImageBitmap?.let {
            binding.overlay.setImageBitmap(it)
        }
    }
}