package com.example.todolistapp

import android.view.View
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Button
import android.content.Intent
import android.app.Activity
import com.google.common.util.concurrent.ListenableFuture // Import ListenableFuture

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var ivPreview: ImageView
    private lateinit var btnCapture: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var layoutConfirmation: LinearLayout
    private lateinit var btnTakeAgain: Button
    private lateinit var btnSetProfile: Button

    private var imageCapture: ImageCapture? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    private var capturedImageUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        ivPreview = findViewById(R.id.ivPreview)
        btnCapture = findViewById(R.id.btnCapture)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        layoutConfirmation = findViewById(R.id.layoutConfirmation)
        btnTakeAgain = findViewById(R.id.btnTakeAgain)
        btnSetProfile = findViewById(R.id.btnSetProfile)

        // Cek permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionsLauncher.launch(Manifest.permission.CAMERA)
        }

        // --- Listeners ---
        btnCapture.setOnClickListener { takePhoto() }
        btnSwitchCamera.setOnClickListener { switchCamera() }
        btnTakeAgain.setOnClickListener { restartCamera() }
        btnSetProfile.setOnClickListener { setAsProfilePhoto() }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }


    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(previewView.display.rotation)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                    previewView.scaleX = -1f
                } else {
                    previewView.scaleX = 1f
                }

                setUiMode(false)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to open camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        startCamera()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(externalCacheDir, "IMG_$timestamp.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    capturedImageUri = Uri.fromFile(photoFile)
                    showPreview(capturedImageUri)
                }
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(applicationContext, "Failed to capture photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showPreview(uri: Uri?) {
        if (uri == null) return
        ivPreview.setImageURI(uri)

        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            ivPreview.scaleX = -1f
        } else {
            ivPreview.scaleX = 1f
        }

        setUiMode(true)
    }

    private fun restartCamera() {
        capturedImageUri?.path?.let { File(it).delete() }
        capturedImageUri = null
        startCamera()
    }

    private fun setAsProfilePhoto() {
        if (capturedImageUri == null) {
            Toast.makeText(this, "No photo to set.", Toast.LENGTH_SHORT).show()
            return
        }

        val resultIntent = Intent().apply {
            putExtra("PROFILE_PHOTO_URI", capturedImageUri.toString())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    /**
     * Mengatur visibility UI antara mode kamera dan mode pratinjau.
     */
    private fun setUiMode(isPreviewMode: Boolean) {
        if (isPreviewMode) {
            previewView.visibility = View.GONE
            ivPreview.visibility = View.VISIBLE
            btnCapture.visibility = View.GONE
            btnSwitchCamera.visibility = View.GONE
            layoutConfirmation.visibility = View.VISIBLE
        } else {
            previewView.visibility = View.VISIBLE
            ivPreview.visibility = View.GONE
            btnCapture.visibility = View.VISIBLE
            btnSwitchCamera.visibility = View.VISIBLE
            layoutConfirmation.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        if (ivPreview.visibility == View.VISIBLE) {
            restartCamera()
        } else {
            super.onBackPressed()
        }
    }
}