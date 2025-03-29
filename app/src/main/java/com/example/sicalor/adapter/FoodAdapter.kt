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
    private var foodList: MutableList<FoodData>
) : RecyclerView.Adapter<FoodAdapter.ViewHolder>() {
    private lateinit var onItemClickCallback: OnItemClickCallback
    private val itemsPerPage = 10

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
        foodList.clear()
        foodList.addAll(newFoodList.take(itemsPerPage))
        notifyDataSetChanged()
    }

    fun loadMoreData(allData: List<FoodData>) {
        val startPosition = foodList.size
        foodList.addAll(allData)
        notifyItemRangeInserted(startPosition, allData.size)
    }

    inner class ViewHolder(private val binding: ItemFoodBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(food: FoodData) {
            binding.foodName.text = food.name
            binding.foodGroup.text = when (food.group) {
                "Golongan 1" -> "Golongan 1 (Sumber Karbohidrat)"
                "Golongan 2" -> "Golongan 2 (Sumber Protein Hewani)"
                "Golongan 3" -> "Golongan 3 (Sumber Protein Nabati)"
                "Golongan 4" -> "Golongan 4 (Sayuran)"
                "Golongan 5" -> "Golongan 5 (Buah-Buahan & Gula)"
                else -> "Unknown"
            }
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

    interface OnItemClickCallback {
        fun onItemClicked(data: FoodData)
    }
}