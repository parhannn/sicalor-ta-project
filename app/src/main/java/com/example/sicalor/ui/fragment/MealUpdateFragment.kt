package com.example.sicalor.ui.fragment

import android.R
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.sicalor.databinding.FragmentMealUpdateBinding
import com.example.sicalor.ui.data.CalorieHistoryData
import com.example.sicalor.ui.data.MealData
import com.example.sicalor.ui.data.MealPlanData
import com.example.sicalor.ui.data.NewMealPlanData
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.util.Locale

class MealUpdateFragment : DialogFragment() {
    private lateinit var binding: FragmentMealUpdateBinding
    private var listener: OnDialogSaveBtnClickListener? = null
    private var mealPlanData: MealPlanData? = null
    private var updatedMealPlan: NewMealPlanData = NewMealPlanData()
    private lateinit var database: DatabaseReference
    private var calorieTarget: Double = 0.0

    fun setListener(listener: OnDialogSaveBtnClickListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMealUpdateBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().reference.child("MealPlanData")
        return binding.root
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = arguments
        if (bundle != null) {
            Log.d(TAG, "Data diterima: $bundle")

            mealPlanData = MealPlanData(
                bundle.getString("userId").toString(),
                bundle.getString("mealId").toString(),
                bundle.getString("date").toString(),
                bundle.getString("type").toString(),
                MealData(
                    bundle.getString("img").toString(),
                    bundle.getString("calories").toString(),
                    bundle.getString("carbs").toString(),
                    bundle.getString("desc").toString(),
                    bundle.getString("fat").toString(),
                    bundle.getString("group").toString(),
                    bundle.getString("name").toString(),
                    bundle.getString("protein").toString(),
                    bundle.getString("portion").toString()
                )
            )

            Log.d(TAG, "MealPlanData setelah parsing: $mealPlanData")

            binding.foodName.text = mealPlanData?.mealData?.name
            binding.foodCalories.text = mealPlanData?.mealData?.calories
            binding.etPortion.setText(mealPlanData?.mealData?.portion)

            val mealTypes = arrayOf("Breakfast", "Lunch", "Dinner")
            val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, mealTypes)
            adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            binding.planTypeSpinner.adapter = adapter
            binding.planTypeSpinner.setSelection(
                when (mealPlanData?.type) {
                    "Breakfast" -> 0
                    "Lunch" -> 1
                    "Dinner" -> 2
                    else -> 3
                }
            )
        } else {
            Log.e(TAG, "Arguments kosong atau null")
        }

        binding.btnSave.setOnClickListener {
            val oldPortion = mealPlanData?.mealData?.portion?.toDoubleOrNull() ?: 1.0
            val oldCalories = mealPlanData?.mealData?.calories?.toDoubleOrNull() ?: 0.0
            val newPortion = binding.etPortion.text.toString().toDoubleOrNull() ?: oldPortion
            val newCalories = if (oldPortion != 0.0) (newPortion / oldPortion) * oldCalories else oldCalories
            val newCarb = if (oldPortion !== 0.0) (newPortion /oldPortion) * mealPlanData?.mealData?.carbs?.toDouble()!! else mealPlanData?.mealData?.carbs?.toDouble()!!
            val newProtein = if (oldPortion !== 0.0) (newPortion /oldPortion) * mealPlanData?.mealData?.protein?.toDouble()!! else mealPlanData?.mealData?.protein?.toDouble()!!
            val newFat = if (oldPortion !== 0.0) (newPortion /oldPortion) * mealPlanData?.mealData?.fat?.toDouble()!! else mealPlanData?.mealData?.fat?.toDouble()!!
            val newPlanType = binding.planTypeSpinner.selectedItem.toString()

            val updatedMealData = MealData(
                mealPlanData!!.mealData.img,
                String.format(Locale.ENGLISH,"%.2f", newCalories),
                String.format(Locale.ENGLISH,"%.2f", newCarb),
                mealPlanData!!.mealData.desc,
                String.format(Locale.ENGLISH,"%.2f", newFat),
                mealPlanData!!.mealData.group,
                mealPlanData!!.mealData.name,
                String.format(Locale.ENGLISH,"%.2f", newProtein),
                String.format(Locale.ENGLISH,"%.2f", newPortion)
            )

            updatedMealPlan = NewMealPlanData(
                mealPlanData!!.userId,
                mealPlanData!!.mealId,
                mealPlanData!!.date,
                newPlanType,
                updatedMealData
            )

            Log.d(TAG, "Updated MealPlanData: $updatedMealPlan")

            val database = FirebaseDatabase.getInstance().getReference("MealPlanData")

            database.child(mealPlanData!!.userId).child(mealPlanData!!.mealId)
                .setValue(updatedMealPlan)
                .addOnSuccessListener {
                    Log.d(TAG, "Update sukses: $updatedMealPlan")
                    Toast.makeText(requireContext(), "Meal Updated", Toast.LENGTH_SHORT).show()
                    listener?.onUpdateMeal(updatedMealPlan)
                    val calorieRef =
                        FirebaseDatabase.getInstance().getReference("CalorieHistoryData")
                            .child(mealPlanData!!.userId)
                            .child(mealPlanData!!.date)

                    val calorieDiffer = newCalories - oldCalories

                    calorieRef.get().addOnSuccessListener { snapshot ->
                        val history = snapshot.getValue(CalorieHistoryData::class.java)

                        if (history != null) {
                            val consumed = history.updatedConsumed.toDoubleOrNull() ?: 0.0
                            val remaining = history.remainingCalories.toDoubleOrNull() ?: 0.0
                            val updatedConsumed = consumed + calorieDiffer
                            val updatedRemaining = remaining - calorieDiffer

                            val updatedHistory = CalorieHistoryData(
                                userId = mealPlanData!!.userId,
                                date = mealPlanData!!.date,
                                updatedConsumed = String.format(Locale.ENGLISH, "%.2f", updatedConsumed),
                                remainingCalories = String.format(Locale.ENGLISH, "%.2f", updatedRemaining)
                            )

                            calorieRef.setValue(updatedHistory)
                                .addOnSuccessListener {
                                    Log.d(TAG, "CalorieHistory updated successfully")
                                }
                                .addOnFailureListener {
                                    Log.e(TAG, "Failed to update CalorieHistory: ${it.message}")
                                }
                        }
                    }
                    dismiss()
                }
                .addOnFailureListener {
                    Log.e(TAG, "Gagal update: ${it.message}")
                    Toast.makeText(requireContext(), "Failed to update meal", Toast.LENGTH_SHORT).show()
                }
        }
    }

    companion object {
        const val TAG = "MealUpdateFragment"
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    interface OnDialogSaveBtnClickListener {
        fun onUpdateMeal(mealPlanData: NewMealPlanData)
    }
}