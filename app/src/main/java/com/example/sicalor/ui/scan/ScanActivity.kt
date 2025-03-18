package com.example.sicalor.ui.scan

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.sicalor.databinding.ActivityScanBinding

class ScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanBinding
    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScanBinding.inflate(layoutInflater)

        setContentView(binding.root)

        supportActionBar?.hide()

        handleUI()

        binding.uploadButton.setOnClickListener {
            Toast.makeText(this, "Belum ada model, jadi belum bisa upload", Toast.LENGTH_LONG).show()
        }
        binding.backButton.setOnClickListener { onBackPressed() }
    }

    private fun handleUI() {
        val imageUriString = intent.getStringExtra(CameraActivity.EXTRA_CAMERAX_IMAGE)
        if (imageUriString != null) {
            currentImageUri = imageUriString.toUri()
            showImage()
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
        }
    }
}