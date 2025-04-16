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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sicalor.adapter.SchedulePlanAdapter
import com.example.sicalor.databinding.FragmentScheduleBinding
import com.example.sicalor.ui.data.MealData
import com.example.sicalor.ui.data.MealPlanData
import com.example.sicalor.ui.data.NewMealPlanData
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
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

@SuppressLint("SimpleDateFormat")
class ScheduleFragment : Fragment(), SchedulePlanAdapter.MealAdapterInterface, MealUpdateFragment.OnDialogSaveBtnClickListener {
    private lateinit var database: DatabaseReference
    private lateinit var adapter: SchedulePlanAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private var _binding: FragmentScheduleBinding? = null
    private var fragment: MealUpdateFragment? = null
    private val binding get() = _binding!!
    private var dateMealToday: String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()).toString()
    private var selectedDate: String = ""
    private var selectedPlanType: String = "Breakfast"
    private var allPlanList: MutableList<MealPlanData>? = null

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

    override fun onDestroy() {
        super.onDestroy()
        allPlanList!!.clear()
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
        adapter.setListener(this)
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

                    if (allPlanList!!.isNotEmpty()) {
                        binding.noDataFoundPlaceholder.visibility = View.GONE
                        adapter.updateData(mealPlanDataList, mealDataList)
                    } else {
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

    private fun showSheetDialog() {
        val sheetDialog = AddMealFragment()

        sheetDialog.show(parentFragmentManager, "TAG")
    }

    private fun setupDatePicker() {
        binding.selectDate.setOnClickListener {
            val today = MaterialDatePicker.todayInUtcMilliseconds()

            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.before(today))
                .build()

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Meal Plan Date")
                .setSelection(today)
                .setCalendarConstraints(constraints)
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

    override fun onDeleteMealItem(schedulePlan: MealPlanData, position: Int) {
        val database = FirebaseDatabase.getInstance().getReference("MealPlanData")

        database.child(userId).child(schedulePlan.mealId).removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "Meal Plan Deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onEditMealItem(schedulePlan: MealPlanData, position: Int) {
        if (fragment != null)
            childFragmentManager.beginTransaction().remove(fragment!!).commit()

        val fragment = MealUpdateFragment().apply {
            arguments = Bundle().apply {
                putString("userId", schedulePlan.userId)
                putString("mealId", schedulePlan.mealId)
                putString("date", schedulePlan.date)
                putString("type", schedulePlan.type)
                putString("img", schedulePlan.mealData.img)
                putString("calories", schedulePlan.mealData.calories)
                putString("carbs", schedulePlan.mealData.carbs)
                putString("desc", schedulePlan.mealData.desc)
                putString("fat", schedulePlan.mealData.fat)
                putString("group", schedulePlan.mealData.group)
                putString("name", schedulePlan.mealData.name)
                putString("protein", schedulePlan.mealData.protein)
                putString("portion", schedulePlan.mealData.portion)
            }
        }
        fragment!!.setListener(this)
        fragment!!.show(childFragmentManager, MealUpdateFragment.TAG)
    }

    override fun onUpdateMeal(mealPlanData: NewMealPlanData) {
        Log.d("DEBUG", "Updated Meal: $mealPlanData")
    }
}