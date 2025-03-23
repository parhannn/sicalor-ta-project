package com.example.sicalor.ui.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
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
) : Parcelable
