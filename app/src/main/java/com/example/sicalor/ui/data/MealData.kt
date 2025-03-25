package com.example.sicalor.ui.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MealData(
    var img: String = "",
    var calories: String = "",
    var carbs: String = "",
    var desc: String = "",
    var fat: String = "",
    var group: String = "",
    var name: String = "",
    var protein: String = "",
    var portion: String = ""
) : Parcelable
