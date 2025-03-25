package com.example.sicalor.ui.fragment

import android.R
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sicalor.adapter.SchedulePlanAdapter
import com.example.sicalor.databinding.FragmentScheduleBinding
import com.example.sicalor.ui.data.MealData
import com.example.sicalor.ui.data.MealPlanData
import com.google.android.material.datepicker.MaterialDatePicker
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

@SuppressLint("SimpleDateFormat")
class ScheduleFragment : Fragment() {
    private lateinit var database: DatabaseReference
    private lateinit var adapter: SchedulePlanAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!
    private var dateMealToday: String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()).toString()
    private var selectedDate: String = ""
    private var selectedPlanType: String = "Breakfast"
    private var allPlanList: List<MealPlanData> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authUser()
        setupUI()
    }

    private fun setupUI() {
        binding.addButton.setOnClickListener { showSheetDialog() }
        selectedDate = dateMealToday
        binding.selectedDate.text = dateMealToday

        setupSpinner()
        setupDatePicker()
        setupRecyclerView()
        loadMealPlan(selectedDate)
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
                    loadMealPlan(selectedDate)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun setupRecyclerView() {
        adapter = SchedulePlanAdapter(requireContext(), mutableListOf(), mutableListOf())
        recyclerView = binding.mealPlanRecyclerview
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
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

    private fun showSheetDialog() {
        val sheetDialog = AddMealFragment()

        sheetDialog.show(parentFragmentManager, "TAG")
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
                loadMealPlan(selectedDate)
            }
        }
    }

    private fun authUser() {
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser!!.uid
    }
}