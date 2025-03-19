package com.example.sicalor.ui.user

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sicalor.R
import com.example.sicalor.databinding.ActivityFormBinding
import com.example.sicalor.ui.data.UserData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database

class FormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFormBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private lateinit var activityLevelList: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser!!.uid
        database = Firebase.database.reference.child("UserData")
            .child(userId)
        activityLevelList = resources.getStringArray(R.array.activity_level)

        binding.activitySpinner.adapter =ArrayAdapter(this, android.R.layout.simple_list_item_1, activityLevelList)
        binding.saveSubmit.setOnClickListener { initData() }
        binding.backButton.setOnClickListener { finish() }

    }

    private fun initData() {
        val name = binding.etName.text.toString().trim()
        val gender = if (binding.rbMale.isChecked) "Male" else "Female"
        val age = binding.etAge.text.toString().trim()
        val weight = binding.etWeight.text.toString().trim()
        val height = binding.etHeight.text.toString().trim()
        val allergy = binding.etAllergy.text.toString().trim()
        val activity: String = when (binding.activitySpinner.selectedItemPosition) {
            0 -> "Very Low"
            1 -> "Low"
            2 -> "Medium"
            3 -> "High"
            4 -> "Very High"
            else -> "Unknown"
        }
        var isValid = true

        if (name.isEmpty()) {
            binding.etName.error = "Name cannot be empty"
            isValid = false
        }

        if (gender.isEmpty()) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (age.isEmpty() || !age.matches(Regex("\\d+"))) {
            binding.etAge.error = "Enter a valid age"
            isValid = false
        }

        if (weight.isEmpty() || !weight.matches(Regex("\\d+(\\.\\d+)?"))) {
            binding.etWeight.error = "Enter a valid weight"
            isValid = false
        }

        if (height.isEmpty() || !height.matches(Regex("\\d+(\\.\\d+)?"))) {
            binding.etHeight.error = "Enter a valid height"
            isValid = false
        }

        if (allergy.isEmpty()) {
            binding.etAllergy.error = "This field cannot be empty"
            isValid = false
        }

        if (isValid) {
            val bmr = calculateBMR(gender, weight.toDouble(), height.toDouble(), age.toInt()).toString()
            saveData(name, gender, age, weight, height, allergy, activity, bmr)
        }
    }

    private fun calculateBMR(gender: String, weight: Double, height: Double, age: Int): Double {
        return if (gender == "Male") {
            66.47 + (13.75 * weight) + (5.003 * height) - (6.75 * age)
        } else {
            655.1 + (9.56 * weight) + (1.85 * height) - (4.68 * age)
        }
    }

    private fun saveData(
        name: String,
        gender: String,
        age: String,
        weight: String,
        height: String,
        allergy: String,
        activity: String,
        bmr: String
    ) {
        val userData = UserData(
            name,
            gender,
            age,
            weight,
            height,
            allergy,
            activity,
            bmr
        )
        database
            .push().setValue(userData)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this@FormActivity, "Data Saved Successfully!", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                } else {
                    Toast.makeText(this@FormActivity, "Failed to Save Data!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }
}