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
import androidx.core.app.ActivityCompat
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

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var ivPreview: ImageView
    private lateinit var btnCapture: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var layoutConfirmation: LinearLayout
    private lateinit var btnTakeAgain: Button
    private lateinit var btnSetProfile: Button

    private var imageCapture: ImageCapture? = null
    // Mengubah default selector menjadi kamera depan
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
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 10)
        }

        // --- Listeners ---
        btnCapture.setOnClickListener { takePhoto() }
        btnSwitchCamera.setOnClickListener { switchCamera() }
        btnTakeAgain.setOnClickListener { restartCamera() }
        btnSetProfile.setOnClickListener { setAsProfilePhoto() }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Atur target rotation pada ImageCapture
            imageCapture = ImageCapture.Builder()
                .setTargetRotation(previewView.display.rotation)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                // Terapkan efek mirroring pada live preview (untuk umpan balik selfie yang benar)
                if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                    previewView.scaleX = -1f // Mirroring untuk live preview
                } else {
                    previewView.scaleX = 1f // Normal untuk kamera belakang
                }

                setUiMode(false) // Mode Preview Camera
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        startCamera() // Restart camera dengan selector baru
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Buat file sementara di cache
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
                    Toast.makeText(applicationContext, "Gagal mengambil foto: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showPreview(uri: Uri?) {
        if (uri == null) return
        ivPreview.setImageURI(uri)

        // BARU: Jika kamera depan, biarkan pratinjau hasil foto (ivPreview) menjadi mirror
        // Jika ImageCapture (default) membalik gambar saat menyimpan,
        // maka pratinjau hasil (ivPreview) harus dibalik agar terlihat mirror/selfie.
        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            ivPreview.scaleX = -1f // Mirroring untuk pratinjau hasil foto kamera depan
        } else {
            ivPreview.scaleX = 1f // Normal untuk kamera belakang
        }

        setUiMode(true) // Mode Pratinjau
    }

    private fun restartCamera() {
        // Hapus file sementara jika ada
        capturedImageUri?.path?.let { File(it).delete() }
        capturedImageUri = null
        startCamera() // Kembali ke mode kamera
    }

    private fun setAsProfilePhoto() {
        if (capturedImageUri == null) {
            Toast.makeText(this, "Tidak ada foto untuk diset.", Toast.LENGTH_SHORT).show()
            return
        }

        // Kirim URI kembali ke ProfileActivity
        val resultIntent = Intent().apply {
            putExtra("PROFILE_PHOTO_URI", capturedImageUri.toString())
            // Tambahkan izin baca agar ProfileActivity bisa mengakses file sementara ini
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    /**
     * Mengatur visibility UI antara mode kamera dan mode pratinjau.
     * @param isPreviewMode true jika ingin menampilkan pratinjau dan konfirmasi.
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
            // Jika di mode pratinjau, kembali ke mode kamera saat Back ditekan
            restartCamera()
        } else {
            super.onBackPressed()
        }
    }
}