package com.example.sicalor.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sicalor.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding = ActivityRegisterBinding.inflate(layoutInflater)

        supportActionBar?.hide()

        setContentView(binding.root)

        binding.registerSubmit.setOnClickListener {
            val email = binding.registerEmail.text.toString()
            val password = binding.registerPassword.text.toString()
            val confirmPassword = binding.registerPasswordConfirm.text.toString()

            registerUser(email, password, confirmPassword)
        }

        binding.loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser(
        email: String,
        password: String,
        confirmPassword: String
    ) {
        showLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful && password == confirmPassword) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                Toast.makeText(
                                    this,
                                    "Verification email sent to $email. Please verify your email so you can login again.",
                                    Toast.LENGTH_LONG
                                ).show()
                                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Failed to send verification email",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else if (password != confirmPassword) {
                    showLoading(false)
                    Toast.makeText(this, "Password doesn't match", Toast.LENGTH_SHORT).show()
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.registerProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.registerSubmit.isEnabled = !isLoading
        binding.registerEmail.isEnabled = !isLoading
        binding.registerPassword.isEnabled = !isLoading
        binding.registerPasswordConfirm.isEnabled = !isLoading
    }
}