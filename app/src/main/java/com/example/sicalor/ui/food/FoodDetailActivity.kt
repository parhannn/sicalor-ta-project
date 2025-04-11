package com.example.sicalor.ui.food

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.sicalor.R
import com.example.sicalor.databinding.ActivityFoodDetailBinding
import com.example.sicalor.ui.data.FoodData

@Suppress("DEPRECATION")
class FoodDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFoodDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        binding = ActivityFoodDetailBinding.inflate(layoutInflater)

        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val foodData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getSerializableExtra<FoodData>(KEY_FOOD, FoodData::class.java)
        } else {
            intent.getSerializableExtra(KEY_FOOD) as? FoodData
        }
        if (foodData != null) {
            getFoodData(foodData)
        }

        binding.closeButton.setOnClickListener {
            clearFoodData()
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getFoodData(foodData: FoodData) {
        binding.foodName.text = foodData.name
        binding.foodGroup.text = when (foodData.group) {
            "Golongan 1" -> "Golongan 1 (Sumber Karbohidrat)"
            "Golongan 2" -> "Golongan 2 (Sumber Protein Hewani)"
            "Golongan 3" -> "Golongan 3 (Sumber Protein Nabati)"
            "Golongan 4" -> "Golongan 4 (Sayuran)"
            "Golongan 5" -> "Golongan 5 (Buah-Buahan & Gula)"
            else -> "unknown"
        }
        binding.foodDescription.text = foodData.desc
        binding.foodPortion.text = "${foodData.portion} g"
        binding.foodCalories.text = "${foodData.calories} kcal"
        binding.foodCarbs.text = "${foodData.carbs} g"
        binding.foodFat.text = "${foodData.fat} g"
        binding.foodProtein.text = "${foodData.protein} g"
        Glide.with(binding.foodImage.context)
            .load(foodData.img)
            .placeholder(R.drawable.ic_food_1)
            .into(binding.foodImage)
    }

    private fun clearFoodData() {
        binding.foodName.text = ""
        binding.foodGroup.text = ""
        binding.foodDescription.text = ""
        binding.foodPortion.text = ""
        binding.foodCalories.text = ""
        binding.foodCarbs.text = ""
        binding.foodFat.text = ""
        binding.foodProtein.text = ""

        Glide.with(binding.foodImage.context).clear(binding.foodImage)

        Glide.get(this).clearMemory()
        Thread {
            Glide.get(this).clearDiskCache()
        }.start()
    }

    companion object {
        const val KEY_FOOD = "food"
    }
}