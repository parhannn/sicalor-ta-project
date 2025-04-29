package com.example.sicalor.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sicalor.R
import com.example.sicalor.databinding.ItemMealPlanBinding
import com.example.sicalor.ui.data.CalorieHistoryData
import com.example.sicalor.ui.data.MealData
import com.example.sicalor.ui.data.MealPlanData

@Suppress("DEPRECATION")
class SchedulePlanAdapter(
    private val context: Context,
    private var schedulePlanList: List<MealPlanData>,
    private var mealList: List<MealData>
) : RecyclerView.Adapter<SchedulePlanAdapter.ViewHolder>() {
    private var listener: MealAdapterInterface? = null

    fun setListener (listener: MealAdapterInterface) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemMealPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    fun updateData(newSchedulePlanList: List<MealPlanData>, newMealList: List<MealData>) {
        schedulePlanList = newSchedulePlanList
        mealList = newMealList
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemMealPlanBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(schedulePlan: MealPlanData, meal: MealData) {
            binding.foodName.text = meal.name
            binding.foodCalories.text = "${meal.calories} kcal"
            binding.foodPortion.text = "${meal.portion} g"

            when (schedulePlan.type) {
                "Breakfast" -> {
                    binding.foodType.text = "${schedulePlan.type} 🍳"
                    binding.foodType.setTextColor(context.resources.getColor(R.color.colorBreakfast))
                }
                "Lunch" -> {
                    binding.foodType.text = "${schedulePlan.type} 🍚"
                    binding.foodType.setTextColor(context.resources.getColor(R.color.colorLunch))
                }
                "Dinner" -> {
                    binding.foodType.text = "${schedulePlan.type} 🍝"
                    binding.foodType.setTextColor(context.resources.getColor(R.color.colorDinner))
                }
            }

            Glide.with(binding.foodImage.context)
                .load(meal.img)
                .placeholder(R.drawable.ic_food_1)
                .into(binding.foodImage)

            binding.btnEdit.setOnClickListener {
                listener?.onEditMealItem(schedulePlan, position)
            }
            binding.btnDelete.setOnClickListener {
                showDeleteDialog(schedulePlan, position)
            }
        }
    }

    private fun showDeleteDialog(schedulePlan: MealPlanData, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete this meal schedule?")
            .setPositiveButton("Yes") { _, _ ->
                listener?.onDeleteMealItem(schedulePlan, position)
            }
            .setNegativeButton("No", null)
            .show()
    }

    interface MealAdapterInterface {
        fun onDeleteMealItem(schedulePlan: MealPlanData, position: Int)
        fun onEditMealItem(schedulePlan: MealPlanData, position: Int)
    }
}