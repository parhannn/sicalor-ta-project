package com.example.sicalor.ui.user

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sicalor.R
import com.example.sicalor.databinding.ActivityFormBinding
import com.example.sicalor.ui.MainActivity
import com.example.sicalor.ui.data.UserData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class FormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFormBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private lateinit var activityLevelList: Array<String>
    private var isNotNull = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser!!.uid
        database = Firebase.database.reference.child("UserData")
            .child(userId)
        activityLevelList = resources.getStringArray(R.array.activity_level)

        getUserData()

        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.activitySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, activityLevelList)
        binding.saveSubmit.setOnClickListener { initData() }
        binding.backButton.setOnClickListener { finish() }

    }

    private fun getUserData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val userData = userSnapshot.getValue(UserData::class.java)

                    if (userData != null) {
                        binding.rbMale.isChecked = userData.gender == "Male"
                        binding.rbFemale.isChecked = userData.gender == "Female"
                        binding.etName.setText(userData.name)
                        binding.etAge.setText(userData.age)
                        binding.etWeight.setText(userData.weight)
                        binding.etHeight.setText(userData.height)
                        binding.etAllergy.setText(userData.allergy)
                        binding.activitySpinner.setSelection(
                            when (userData.activity) {
                                "Very Low" -> 0
                                "Low" -> 1
                                "Medium" -> 2
                                "High" -> 3
                                "Very High" -> 4
                                else -> 5
                            }
                        )

                        isNotNull = true
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Error Form", "Error checking user data: $error")
            }
        })
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

        val activityLevel = when (activity) {
            "Very Low" -> 1.2
            "Low" -> 1.35
            "Medium" -> 1.5
            "High" -> 1.75
            "Very High" -> 1.9
            else -> 1.0
        }

        if (isValid) {
            val bmr = calculateBMR(gender, weight.toDouble(), height.toDouble(), age.toInt()).toString()
            val dailyCalorie = calculateDailyCalorie(bmr.toDouble(), activityLevel).toString()
            saveData(name, gender, age, weight, height, allergy, activity, bmr, dailyCalorie)
        }
    }

    private fun calculateBMR(gender: String, weight: Double, height: Double, age: Int): Double {
        return if (gender == "Male") {
            66.47 + (13.75 * weight) + (5.003 * height) - (6.75 * age)
        } else {
            655.1 + (9.56 * weight) + (1.85 * height) - (4.68 * age)
        }
    }

    private fun calculateDailyCalorie(bmr: Double, activityLevel: Double): Double {
        return bmr * activityLevel
    }

    private fun saveData(
        name: String,
        gender: String,
        age: String,
        weight: String,
        height: String,
        allergy: String,
        activity: String,
        bmr: String,
        dailyCalorie: String
    ) {
        val userData = UserData(
            name,
            gender,
            age,
            weight,
            height,
            allergy,
            activity,
            bmr,
            dailyCalorie
        )

        if (isNotNull) {
            database.removeValue()
        }

        database
            .push().setValue(userData)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val fromNotification = intent.getBooleanExtra("from_notification", false)
                    Toast.makeText(this@FormActivity, "Data Saved Successfully!", Toast.LENGTH_SHORT)
                        .show()

                    if (fromNotification) {
                        startActivity(Intent(this@FormActivity, MainActivity::class.java))
                        finish()
                    } else {
                        finish()
                    }
                } else {
                    Toast.makeText(this@FormActivity, "Failed to Save Data!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }
}