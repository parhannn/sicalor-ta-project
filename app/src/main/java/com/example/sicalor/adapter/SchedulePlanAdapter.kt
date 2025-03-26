package com.example.sicalor.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sicalor.R
import com.example.sicalor.databinding.ItemMealPlanBinding
import com.example.sicalor.ui.data.MealData
import com.example.sicalor.ui.data.MealPlanData

class SchedulePlanAdapter(
    private val context: Context,
    private var schedulePlanList: List<MealPlanData>,
    private var mealList: List<MealData>
) :RecyclerView.Adapter<SchedulePlanAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMealPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val schedulePlan = schedulePlanList[position]
        val meal = mealList.find { it.name == schedulePlan.mealData.name }
        if (meal != null) {
            holder.bind(schedulePlan, meal)
        }
    }

    override fun getItemCount(): Int = schedulePlanList.size

    fun updateData(newSchedulePlanList: List<MealPlanData>, newMealList: List<MealData>){
        schedulePlanList = newSchedulePlanList
        mealList = newMealList
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemMealPlanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(schedulePlan: MealPlanData, meal: MealData) {
            binding.foodName.text = meal.name
            binding.foodCalories.text = "${meal.calories} kcal"
            binding.foodPortion.text = "${meal.portion} g"
            binding.foodType.text = schedulePlan.type
            Glide.with(binding.foodImage.context)
                .load(meal.img)
                .placeholder(R.drawable.ic_food_1)
                .into(binding.foodImage)
        }
    }
}