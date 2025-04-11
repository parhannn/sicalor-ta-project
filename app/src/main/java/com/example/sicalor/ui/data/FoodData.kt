package com.example.sicalor.ui.data

import java.io.Serializable

data class FoodData(
    val img: String = "",
    val calories: String = "",
    val carbs: String = "",
    val desc: String = "",
    val fat: String = "",
    val group: String = "",
    val name: String = "",
    val protein: String = "",
    val portion: String = ""
) : Serializable
