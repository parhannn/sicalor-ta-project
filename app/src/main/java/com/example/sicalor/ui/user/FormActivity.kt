package com.example.sicalor.ui.user

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        binding.saveSubmit.setOnClickListener { initData() }
        binding.backButton.setOnClickListener { finish() }

    }

    private fun initData() {
        val name = binding.etName.text.toString()
        val gender = if (binding.rbMale.isChecked) "Male" else "Female"
        val age = binding.etAge.text.toString()
        val weight = binding.etWeight.text.toString()
        val height = binding.etHeight.text.toString()
        val allergy = binding.etAllergy.text.toString()
        val activity = binding.etActivity.text.toString()
        val bmr = calculateBMR(gender, weight.toDouble(), height.toDouble(), age.toInt()).toString()

        if (name.isNotEmpty() && gender.isNotEmpty() && age.isNotEmpty() && weight.isNotEmpty() && height.isNotEmpty() && allergy.isNotEmpty() && activity.isNotEmpty() && bmr.isNotEmpty()) {
            saveData(
                name,
                gender,
                age,
                weight,
                height,
                allergy,
                activity,
                bmr
            )
        } else {
            Toast.makeText(this@FormActivity, "Please Insert All Fields!", Toast.LENGTH_SHORT)
                .show()
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