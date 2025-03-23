package com.example.sicalor.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sicalor.R
import com.example.sicalor.databinding.ItemFoodBinding
import com.example.sicalor.ui.data.FoodData
import com.example.sicalor.ui.food.FoodDetailActivity

class FoodAdapter(
    private val context: Context,
    private var foodList: List<FoodData>
) : RecyclerView.Adapter<FoodAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = foodList[position]
        holder.bind(food)
    }

    override fun getItemCount(): Int = foodList.size

    fun updateData(newFoodList: List<FoodData>){
        foodList = newFoodList
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemFoodBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(food: FoodData) {
            binding.foodName.text = food.name
            binding.foodGroup.text = food.group
            Glide.with(binding.foodImage.context)
                .load(food.img)
                .placeholder(R.drawable.ic_food_1)
                .into(binding.foodImage)

            itemView.setOnClickListener {
                val intent = Intent(context, FoodDetailActivity::class.java).apply {
                    putExtra("food", food)
                }
                context.startActivity(intent)
            }
        }
    }
}