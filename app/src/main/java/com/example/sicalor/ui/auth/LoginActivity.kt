package com.example.sicalor.ui.auth

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.sicalor.R
import com.example.sicalor.databinding.ActivityLoginBinding
import com.example.sicalor.ui.MainActivity
import com.example.sicalor.ui.fragment.ForgotPasswordFragment
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity(), ForgotPasswordFragment.OnDialogForgotPasswordListener {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var forgotPasswordFragment: ForgotPasswordFragment
    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications permission rejected", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        supportActionBar?.hide()

        binding = ActivityLoginBinding.inflate(layoutInflater)

        setContentView(binding.root)

        auth = Firebase.auth
        forgotPasswordFragment = ForgotPasswordFragment()
        forgotPasswordFragment.setListener(this)

        binding.googleSignInBtn.setOnClickListener {
            signInWithGoogle()
        }

        binding.loginSubmit.setOnClickListener {
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString()
            loginUser(email, password)
        }

        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        binding.forgotPassword.setOnClickListener {
            forgotPasswordFragment.show(supportFragmentManager, "ForgotPasswordFragment")
        }

        checkNotificationPermission()
    }

    private fun loginUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        } else {
            showLoading(true)
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        showLoading(false)
                        val user = auth.currentUser
                        if (user != null && user.isEmailVerified) {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Please verify your email first",
                                Toast.LENGTH_SHORT
                            ).show()
                            FirebaseAuth.getInstance().signOut()
                        }
                    } else {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "Login failed, your credential is incorrect!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.your_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        showLoading(true)

        lifecycleScope.launch {
            try {
                val result: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )
                handleSignIn(result)
            } catch (exception: GetCredentialException) {
                showLoading(false)
                Log.d("Error", exception.message.toString())

                val errorMessage = exception.toString()

                if (errorMessage.contains("No credentials available", ignoreCase = true)) {
                    Toast.makeText(this@LoginActivity, "You don't have any connected Google account yet", Toast.LENGTH_LONG).show()
                } else if(errorMessage.contains("activity is cancelled by the user", ignoreCase = true)) {
                    Toast.makeText(this@LoginActivity, "Authentication Canceled", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@LoginActivity, "Authentication Failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                    showLoading(false)
                } else {
                    Log.d(TAG, "Unknown credential type")
                }
            }

            else -> {
                Log.d(TAG, "Unknown credential type")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    val user: FirebaseUser? = auth.currentUser
                    updateUI(user)
                } else {
                    Toast.makeText(this, "You don't have a connected Google account yet", Toast.LENGTH_LONG).show()
                    updateUI(null)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val fromNotification = intent.getBooleanExtra("from_notification", false)
            val mainIntent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                putExtra("from_notification", fromNotification)
            }
            startActivity(mainIntent)
            finish()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loginProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginSubmit.isEnabled = !isLoading
        binding.loginEmail.isEnabled = !isLoading
        binding.loginPassword.isEnabled = !isLoading
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {

                }

                else -> {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onSendEmailForgotPassword(email: String) {
        Log.d("DEBUG", "Email sent to $email")
    }
}