package com.example.sicalor.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sicalor.R
import com.example.sicalor.databinding.ItemFoodPlanBinding
import com.example.sicalor.ui.data.FoodData

class MealAdapter(
    private val context: Context,
    private var foodList: List<FoodData>
) : RecyclerView.Adapter<MealAdapter.ViewHolder>() {
    private lateinit var onItemClickCallback: OnItemClickCallback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFoodPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = foodList[position]
        holder.bind(food)
    }

    override fun getItemCount(): Int = foodList.size

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    fun updateData(newFoodList: List<FoodData>){
        foodList = newFoodList
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemFoodPlanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(food: FoodData) {
            binding.foodName.text = food.name
            binding.foodCalories.text = "${food.calories} kcal"
            binding.foodPortion.text = "${food.portion} g"
            Glide.with(binding.foodImage.context)
                .load(food.img)
                .placeholder(R.drawable.ic_food_1)
                .into(binding.foodImage)

            itemView.setOnClickListener {
                Toast.makeText(context, "You choose ${food.name}", Toast.LENGTH_SHORT).show()
                onItemClickCallback?.onItemClicked(food)
            }
        }
    }

    interface OnItemClickCallback {
        fun onItemClicked(data: FoodData)
    }
}