package com.example.sicalor.ui.data

data class MealPlanData(
    val userId: String = "",
    var mealId: String = "",
    val date: String = "",
    val type: String = "",
    val mealData: MealData = MealData()
)
