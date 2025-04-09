package com.example.sicalor.ui.scan

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sicalor.databinding.ActivityCameraBinding
import com.example.sicalor.ui.data.BoundingBox
import com.example.sicalor.ui.scan.Constants.LABELS_PATH
import com.example.sicalor.ui.scan.Constants.MODEL_PATH
import com.example.sicalor.utils.createCustomTempFile
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("DEPRECATION")
class CameraActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: Detector
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var lastDetection: List<BoundingBox>? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val isFrontCamera = false
    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra(EXTRA_CAMERAX_IMAGE, uri.toString())
            }
            startActivity(intent)
            finish()
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }
    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageCapture?.targetRotation = rotation
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        supportActionBar?.hide()

        binding = ActivityCameraBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        detector = Detector(applicationContext, MODEL_PATH, LABELS_PATH, this)
        detector.setup()

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.switchCamera.setOnClickListener {
            cameraSelector =
                if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) CameraSelector.DEFAULT_BACK_CAMERA
                else CameraSelector.DEFAULT_FRONT_CAMERA
            startCamera()
        }
        binding.captureImage.setOnClickListener { captureImage() }
        binding.openGallery.setOnClickListener { startGallery() }
        binding.backButton.setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return

        val photoFile = createCustomTempFile(application)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val containerView = binding.root
                    val bitmap = Bitmap.createBitmap(
                        containerView.width,
                        containerView.height,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    containerView.draw(canvas)

                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val byteArray = stream.toByteArray()

                    val intent = Intent(this@CameraActivity, ResultActivity::class.java).apply {
                        putExtra(EXTRA_CAPTURE_IMAGE, byteArray)

                        lastDetection?.let {
                            putExtra(EXTRA_DETECTION_LIST, ArrayList(it))
                        }

                        putExtra(EXTRA_CAMERAX_IMAGE, savedUri.toString())
                    }
                    startActivity(intent)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Failed to capture image.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "onError: ${exception.message}")
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCase()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCase() {
        val provider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation
        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analyzer ->
                analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                    val bitmapBuffer = Bitmap.createBitmap(
                        imageProxy.width,
                        imageProxy.height,
                        Bitmap.Config.ARGB_8888
                    )
                    imageProxy.use {
                        bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
                    }
                    val matrix = Matrix().apply {
                        postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                        if (isFrontCamera) {
                            postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
                        }
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
                    )
                    detector.detect(rotatedBitmap)
                }
            }

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        provider.unbindAll()
        try {
            camera = provider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exception: Exception) {
            Toast.makeText(
                this@CameraActivity,
                "Failed to start camera.",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(TAG, "startCamera: ${exception.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        imageAnalyzer?.clearAnalyzer()
    }

    override fun onEmptyDetect() {
        binding.overlay.clear()
        lastDetection = null
    }

    @SuppressLint("SetTextI18n")
    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        lastDetection = boundingBoxes
        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }
    }

    companion object {
        private const val TAG = "CameraActivity"
        const val EXTRA_CAMERAX_IMAGE = "CameraX Image"
        const val EXTRA_CAPTURE_IMAGE = "capturedImage"
        const val EXTRA_DETECTION_LIST = "detectionList"
    }
}