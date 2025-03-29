package com.example.sicalor.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.sicalor.databinding.FragmentMealOptionBinding
import com.example.sicalor.ui.data.MealData
import com.example.sicalor.ui.data.MealPlanData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MealOptionFragment(
    private val schedulePlan: MealPlanData,
    private val onUpdateFood: MealData,
    private val onDeleteFood: MealPlanData
) : BottomSheetDialogFragment() {
    private var _binding: FragmentMealOptionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMealOptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.foodName.text = schedulePlan.mealData.name
        binding.foodCalories.text = "${schedulePlan.mealData.calories} kcal"
        binding.etPortion.setText(schedulePlan.mealData.portion)

        val scheduleTypes = listOf("Breakfast", "Lunch", "Dinner")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, scheduleTypes)
        binding.planTypeSpinner.adapter = adapter
        binding.planTypeSpinner.setSelection(scheduleTypes.indexOf(schedulePlan.type))

        binding.btnSave.setOnClickListener {
//            val updatedFood = schedulePlan.mealData.copy(
//                portion = binding.etPortion.text.toString()
//            )
//            val updatedPlan = schedulePlan.copy(
//                type = binding.planTypeSpinner.selectedItem.toString(),
//            )
//
//            onUpdateFood(updatedFood, updatedPlan)
//            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}