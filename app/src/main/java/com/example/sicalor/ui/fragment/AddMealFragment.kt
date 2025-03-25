package com.example.sicalor.ui.fragment

import android.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sicalor.adapter.MealAdapter
import com.example.sicalor.databinding.FragmentAddMealBinding
import com.example.sicalor.ui.data.FoodData
import com.example.sicalor.ui.data.MealData
import com.example.sicalor.ui.data.MealPlanData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
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

class AddMealFragment : BottomSheetDialogFragment() {
    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MealAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var searchView: SearchView
    private lateinit var userId: String
    private var _binding: FragmentAddMealBinding? = null
    private val binding get() = _binding!!
    private var allFoodList: List<FoodData> = emptyList()
    private var selectedDate: String = ""
    private var selectedPlanType: String = "Breakfast"
    private var setPortion: String = ""
    private var selectedMeal: MealData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser!!.uid
        database = Firebase.database.reference.child("MealPlanData")
        setupUI()
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener { dismiss() }
        setupDatePicker()
        setupSpinner()
        setupPortion()
        setupSearchView()
        setupRecyclerView()
        setupSubmitButton()
    }

    private fun setupSubmitButton() {
        binding.submitPlanButton.setOnClickListener {
            if (selectedDate.isEmpty() || selectedMeal == null) {
                Toast.makeText(requireContext(), "Please select date and meal!", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val savedMealId = generateMealId()
            val savedUserId = userId
            val savedSelectedDate = selectedDate
            val savedSelectedPlanType = selectedPlanType
            val savedSelectedMeal = selectedMeal!!

            val mealPlanData = MealPlanData(
                savedUserId,
                savedMealId,
                savedSelectedDate,
                savedSelectedPlanType,
                savedSelectedMeal
            )

            val database = FirebaseDatabase.getInstance().getReference("MealPlanData")

            Log.d("DEBUG", "MealPlanData to be added: $mealPlanData")

            database
                .child(userId).child(savedMealId).setValue(mealPlanData)
                .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(requireContext(), "Meal added successfully!", Toast.LENGTH_SHORT)
                        .show()
                    dismiss()
                } else {
                    Log.e("FirebaseError", "Error: ${it.exception?.message}")
                    Toast.makeText(requireContext(), "Failed to add meal", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun generateMealId(): String {
        val timestamp = System.currentTimeMillis().toString()
        val randomPart = (1000..9999).random().toString()
        return "MEAL_$timestamp$randomPart"
    }

    private fun setupPortion() {
        setPortion = binding.etPortion.text.toString()
    }

    private fun setupRecyclerView() {
        adapter = MealAdapter(requireContext(), mutableListOf())
        recyclerView = binding.mealRecyclerview
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter.setOnItemClickCallback(object : MealAdapter.OnItemClickCallback {
            override fun onItemClicked(data: FoodData) {
                val portionText = binding.etPortion.text.toString()
                if (portionText.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter portion!", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                val portionValue = portionText.toDouble()
                val newCalorie = (portionValue * data.calories.toDouble()) / data.portion.toDouble()

                selectedMeal = MealData(
                    calories = newCalorie.toString(),
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
        })
    }

    private fun setupDatePicker() {
        binding.selectDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Meal Plan Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.show(parentFragmentManager, "DATE_PICKER")

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selection))
                binding.selectedDate.text = selectedDate
                Log.d("DEBUG", "Selected Date: $selectedDate")
            }
        }
    }

    private fun filterFoods(query: String) {
        val filteredFoods = if (query.isEmpty()) {
            allFoodList
        } else {
            allFoodList.filter { it.name.contains(query, ignoreCase = true) }
        }

        Log.d("FoodFragment", "Filtered foods: $filteredFoods for query: $query")
        adapter.updateData(filteredFoods)
    }

    private fun setupSearchView() {
        searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filterFoods(it) }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterFoods(it) }
                return false
            }
        })
        searchView.setOnCloseListener {
            adapter.updateData(allFoodList)
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
                    adapter.updateData(foodList)
                } else {
                    Log.d("TAG", "No data available")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
    }

    private fun setupSpinner() {
        val mealTypes = arrayOf("Breakfast", "Lunch", "Dinner")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, mealTypes)
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
}