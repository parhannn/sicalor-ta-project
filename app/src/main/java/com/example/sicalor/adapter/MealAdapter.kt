package com.example.sicalor.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sicalor.R
import com.example.sicalor.databinding.ItemFoodPlanBinding
import com.example.sicalor.ui.data.FoodData

@Suppress("DEPRECATION")
class MealAdapter(
    private val context: Context,
    private var foodList: MutableList<FoodData>
) : RecyclerView.Adapter<MealAdapter.ViewHolder>() {
    private lateinit var onItemClickCallback: OnItemClickCallback
    private var selectedPosition: Int = RecyclerView.NO_POSITION
    private val itemsPerPage = 10

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
        foodList.clear()
        foodList.addAll(newFoodList.take(itemsPerPage))
        notifyDataSetChanged()
    }

    fun loadMoreData(allData: List<FoodData>) {
        val startPosition = foodList.size
        foodList.addAll(allData)
        notifyItemRangeInserted(startPosition, allData.size)
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

            if (position == selectedPosition) {
                binding.cardItem.background = ContextCompat.getDrawable(context, R.color.colorPrimary)
            } else {
                binding.cardItem.background = ContextCompat.getDrawable(context, R.color.colorPrimarySoft)
            }

            itemView.setOnClickListener {
                val prevSelectedPosition = selectedPosition
                selectedPosition = adapterPosition

                notifyItemChanged(prevSelectedPosition)
                notifyItemChanged(selectedPosition)

                onItemClickCallback?.onItemClicked(food)
            }
        }
    }

    interface OnItemClickCallback {
        fun onItemClicked(data: FoodData)
    }
}