package com.example.todolistapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.hdodenhof.circleimageview.CircleImageView
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import java.lang.Exception
import com.google.android.material.tabs.TabLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min
import kotlin.math.roundToInt
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

// [BARU] Konstanta untuk SharedPrefs Streak
private val STREAK_PREFS_NAME = "TimyTimePrefs"
private val KEY_STREAK = "current_streak"

class ProfileActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tvUsername: TextView
    private lateinit var ivProfilePicture: CircleImageView
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var streakPrefs: SharedPreferences // DEKLARASI BARU
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tvCompletedTasksLabel: TextView
    private lateinit var tvMissedTasksLabel: TextView
    private lateinit var tvDeletedTasksLabel: TextView
    private lateinit var tvStreakValue: TextView

    // --- Cloudinary and OkHttp client (Dicopy dari EditProfileActivity) ---
    private val CLOUD_NAME = "dk2jrlugl"
    private val UPLOAD_PRESET = "android_profile_upload"
    private val client = OkHttpClient()
    // --------------------------------------------------------------------

    // Launcher untuk Activity Result dari EditProfileActivity
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadProfileData()
            updateTaskCounts()
            updateStreakValue()
        }
    }

    // Launcher untuk Activity Result dari CameraActivity
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra("PROFILE_PHOTO_URI")
            if (uriString != null) {
                val newImageUri = Uri.parse(uriString)
                handleNewProfilePhoto(newImageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        // Inisialisasi dan Binding Views
        sharedPrefs = getSharedPreferences(EditProfileActivity.PREFS_NAME, Context.MODE_PRIVATE)
        // [BARU]: Inisialisasi streakPrefs
        streakPrefs = getSharedPreferences(STREAK_PREFS_NAME, Context.MODE_PRIVATE)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        tvUsername = findViewById(R.id.tvUsername)
        bottomNav = findViewById(R.id.bottomNav)
        ivProfilePicture = findViewById(R.id.ivProfile)
        tvStreakValue = findViewById(R.id.some_id) // BINDING

        val ivSettings = findViewById<ImageView>(R.id.ivSettings)
        val btnEditProfile = findViewById<TextView>(R.id.btnEditProfile)
        val CompletedTasks = findViewById<LinearLayout>(R.id.CompletedTasks)
        val MissedTasks = findViewById<LinearLayout>(R.id.MissedTasks)
        val DeletedTasks = findViewById<LinearLayout>(R.id.DeletedTasks)
        val ivCamera = findViewById<ImageView>(R.id.ivCamera)

        tvCompletedTasksLabel = CompletedTasks.getChildAt(0) as TextView
        tvMissedTasksLabel = MissedTasks.getChildAt(0) as TextView
        tvDeletedTasksLabel = DeletedTasks.getChildAt(0) as TextView

        // Setup Bottom Nav dan ViewPager (Kode yang sudah ada)
        bottomNav.itemIconTintList = null

        val adapter = ProductivityStatsAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Daily"
                1 -> tab.text = "Weekly"
                2 -> tab.text = "Monthly"
            }
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.setCurrentItem(tab.position, false)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        viewPager.setCurrentItem(0, false)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home-> {
                    startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    finish()
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_tasks-> {
                    startActivity(Intent(this, TaskActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    finish()
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }

        bottomNav.selectedItemId = R.id.nav_profile

        // Setup Listeners
        ivSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Listener untuk ikon kamera
        ivCamera.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            cameraLauncher.launch(intent)
        }

        CompletedTasks.setOnClickListener {
            startActivity(Intent(this, CompletedTasksActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        MissedTasks.setOnClickListener {
            startActivity(Intent(this, MissedTasksActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        DeletedTasks.setOnClickListener {
            startActivity(Intent(this, DeletedTasksActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        loadProfileData()
        updateTaskCounts()
        updateStreakValue()
    }

    override fun onStart() {
        super.onStart()
        bottomNav.selectedItemId = R.id.nav_profile
        updateStreakValue()
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
        updateTaskCounts()
        updateStreakValue()
    }

    /**
     * Mengambil nilai streak dari SharedPreferences dan memperbarui TextView.
     */
    private fun updateStreakValue() {
        // [UBAH]: Membaca langsung dari SharedPreferences, menghilangkan panggilan ke TaskRepository yang berat
        val streak = streakPrefs.getInt(KEY_STREAK, 0)
        tvStreakValue.text = streak.toString()
    }


    // --------------------------------------------------------------------------------------
    // --- FUNGSI UPLOAD GAMBAR & SIMPAN DATA BARU (Kode Tetap) ---
    // --------------------------------------------------------------------------------------
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqWidth || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqWidth && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getCompressedImageBytes(imageUri: Uri, targetSizeKB: Int): ByteArray? {
        var inputStream: InputStream? = null
        try {
            // 1. Decode Bounds (Kode asli)
            inputStream = contentResolver.openInputStream(imageUri)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val (originalWidth, originalHeight) = options.outWidth to options.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) {
                Log.e("ImageCompression", "Gagal membaca dimensi gambar.")
                return null
            }

            // 2. Kalkulasi inSampleSize (Kode asli)
            // (Kita gunakan maxDimension sementara untuk kalkulasi sample size)
            val maxDimension = 1024.0
            val tempScale = min(
                maxDimension / originalWidth,
                maxDimension / originalHeight
            ).let { if (it > 1.0) 1.0 else it }
            val tempWidth = (originalWidth * tempScale).roundToInt()
            val tempHeight = (originalHeight * tempScale).roundToInt()

            options.inSampleSize = calculateInSampleSize(options, tempWidth, tempHeight) // Memanggil helper yg sudah ada
            options.inJustDecodeBounds = false

            // 3. Decode Bitmap Awal (Kode asli)
            inputStream = contentResolver.openInputStream(imageUri)
            var bitmapToProcess = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmapToProcess == null) {
                Log.e("ImageCompression", "Gagal men-decode bitmap.")
                return null
            }

            // 4. --- TAMBAHAN: ROTASI BERDASARKAN EXIF ---
            try {
                inputStream = contentResolver.openInputStream(imageUri) // Buka stream BARU untuk EXIF
                if (inputStream != null) {
                    bitmapToProcess = rotateBitmapBasedOnExif(bitmapToProcess, inputStream) // Panggil helper baru
                    Log.d("ImageCompression", "Rotasi EXIF diterapkan.")
                }
            } catch (exifError: Exception) {
                Log.e("ImageCompression", "Gagal membaca EXIF/rotasi", exifError)
                // Lanjutkan proses tanpa rotasi jika gagal
            } finally {
                inputStream?.close() // Pastikan stream EXIF ditutup
            }
            // --- AKHIR TAMBAHAN ---

            // 5. Hitung Ulang Scaling SETELAH Rotasi
            val rotatedWidth = bitmapToProcess.width
            val rotatedHeight = bitmapToProcess.height

            val scaleAfterRotation = min(
                maxDimension / rotatedWidth,
                maxDimension / rotatedHeight
            ).let { if (it > 1.0) 1.0 else it }

            val finalNewWidth = (rotatedWidth * scaleAfterRotation).roundToInt()
            val finalNewHeight = (rotatedHeight * scaleAfterRotation).roundToInt()

            // 6. Scale Bitmap (Resize)
            val finalBitmap = Bitmap.createScaledBitmap(bitmapToProcess, finalNewWidth, finalNewHeight, true)
            if (finalBitmap != bitmapToProcess) {
                bitmapToProcess.recycle() // Bebaskan bitmap hasil decode/rotasi
            }

            // 7. Kompresi (Kode asli Anda)
            var quality = 95
            val outputStream = ByteArrayOutputStream()
            var compressedBytes: ByteArray
            val targetSizeBytes = targetSizeKB * 1024

            do {
                outputStream.reset()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                compressedBytes = outputStream.toByteArray()

                // Hapus log ini jika tidak diperlukan lagi
                // val currentSizeKB = compressedBytes.size / 1024
                // Log.d("ImageCompression", "Kualitas: $quality, Ukuran: ${currentSizeKB}KB")

                quality -= 5

            } while (compressedBytes.size > targetSizeBytes && quality > 40)

            finalBitmap.recycle() // Bebaskan bitmap hasil scaling
            outputStream.close()

            // Log.d("ImageCompression", "Ukuran final: ${compressedBytes.size / 1024}KB")
            return compressedBytes

        } catch (e: Exception) {
            Log.e("ImageCompression", "Gagal mengkompresi gambar", e)
            try { inputStream?.close() } catch (eClose: Exception) { /* abaikan */ }
            return null
        }
    }
    private fun extractSecureUrl(jsonResponse: String): String? {
        return try {
            val startIndex = jsonResponse.indexOf("\"secure_url\":\"") + 14
            if (startIndex < 14) return null
            val endIndex = jsonResponse.indexOf("\"", startIndex)
            if (endIndex == -1) return null
            jsonResponse.substring(startIndex, endIndex).replace("\\/", "/")
        } catch (e: Exception) {
            Log.e("CloudinaryUpload", "Error parsing secure_url: ${e.message}")
            null
        }
    }

    private fun rotateBitmapBasedOnExif(source: Bitmap, inputStream: InputStream): Bitmap {
        val exif = ExifInterface(inputStream)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90.0f)
                matrix.preScale(-1.0f, 1.0f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270.0f)
                matrix.preScale(-1.0f, 1.0f)
            }
            else -> return source // Tidak perlu rotasi
        }

        return try {
            // Buat bitmap baru yang sudah dirotasi
            val rotatedBitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            if (rotatedBitmap != source) {
                source.recycle() // Bebaskan memori bitmap asli jika rotasi berhasil
            }
            rotatedBitmap
        } catch (e: OutOfMemoryError) {
            Log.e("RotateBitmap", "OutOfMemoryError saat merotasi bitmap", e)
            source // Kembalikan bitmap asli jika error
        }
    }
    private suspend fun uploadImageToCloudinary(userId: String, imageUri: Uri): String? {
        if (userId.isEmpty()) return null

        return withContext(Dispatchers.IO) {
            try {
                val compressedBytes = getCompressedImageBytes(imageUri, 500)
                if (compressedBytes == null) return@withContext null

                val tempFile = File(cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { it.write(compressedBytes) }

                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val publicId = "profile_images/$userId/profile_$timestamp"

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", tempFile.name, tempFile.asRequestBody("image/jpeg".toMediaType()))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .addFormDataPart("folder", "profile_images/$userId")
                    .addFormDataPart("public_id", publicId)
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                tempFile.delete()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    return@withContext responseBody?.let { extractSecureUrl(it) }
                } else {
                    Log.e("CloudinaryUpload", "Upload failed: ${response.code} - ${response.message}. Body: ${response.body?.string()}")
                    return@withContext null
                }

            } catch (e: Exception) {
                Log.e("CloudinaryUpload", "Exception: ${e.message}", e)
                return@withContext null
            }
        }
    }

    // FUNGSI PENTING: Menangani foto baru, upload, dan update Firestore
    private fun handleNewProfilePhoto(imageUri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Silakan login untuk menyimpan foto.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Tampilkan gambar secara instan (preview)
        val options = RequestOptions().transform(CircleCrop())
        Glide.with(this).load(imageUri).apply(options).into(ivProfilePicture)
        Toast.makeText(this, "Foto sedang diunggah. Mohon tunggu...", Toast.LENGTH_LONG).show()

        lifecycleScope.launch {
            // 2. Simpan URI lokal ke SharedPreferences (fallback)
            sharedPrefs.edit().putString(EditProfileActivity.KEY_IMAGE_URI, imageUri.toString()).apply()

            // 3. Upload ke Cloudinary
            val uploadedUrl = uploadImageToCloudinary(userId, imageUri)

            if (uploadedUrl == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Gagal mengunggah foto. Perubahan mungkin hilang saat aplikasi ditutup.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            // 4. Update Firestore dengan URL permanen
            val success = withContext(Dispatchers.IO) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users")
                        .document(userId)
                        .update("profileImageUrl", uploadedUrl, "updatedAt", System.currentTimeMillis())
                        .await()
                    true
                } catch (e: Exception) {
                    Log.e("FirestoreUpdate", "Error updating profile image URL", e)
                    false
                }
            }

            // 5. Finalisasi
            withContext(Dispatchers.Main) {
                if (success) {
                    // Update SharedPreferences dengan URL permanen
                    sharedPrefs.edit().putString(EditProfileActivity.KEY_IMAGE_URI, uploadedUrl).apply()
                    // Muat ulang data untuk memastikan Glide memuat URL permanen
                    loadProfileData()
                    Toast.makeText(this@ProfileActivity, "Foto profil berhasil diperbarui dan disimpan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ProfileActivity, "Gagal menyimpan foto ke database.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --------------------------------------------------------------------------------------
    // --- FUNGSI LOAD DATA (Kode Tetap) ---
    // --------------------------------------------------------------------------------------

    private fun updateTaskCounts() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Asumsi TaskRepository.processTasksForMissed() sudah ada
                TaskRepository.processTasksForMissed()

                val completedCount = TaskRepository.getCompletedTasks().size
                val missedCount = TaskRepository.getMissedTasks().size
                val deletedCount = TaskRepository.getDeletedTasks().size

                withContext(Dispatchers.Main) {
                    tvCompletedTasksLabel.text = "Completed Tasks ($completedCount)"
                    tvMissedTasksLabel.text = "Missed Tasks ($missedCount)"
                    tvDeletedTasksLabel.text = "Deleted Tasks ($deletedCount)"
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Failed to update task counts: ${e.message}")
            }
        }
    }

    private fun loadProfileData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            tvUsername.text = "Guest"
            ivProfilePicture.setImageResource(R.drawable.ic_profile)
            return
        }

        lifecycleScope.launch {
            val profileData = withContext(Dispatchers.IO) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val document = db.collection("users").document(userId).get().await()
                    document.data
                } catch (e: Exception) {
                    Log.e("FirestoreLoad", "Error loading profile data", e)
                    null
                }
            }

            val options = RequestOptions().transform(CircleCrop())

            if (profileData != null) {
                val username = profileData["username"] as? String ?: "@username"
                val imageUrl = profileData["profileImageUrl"] as? String

                tvUsername.text = username
                ivProfilePicture.scaleX = 1f

                // Muat gambar
                if (imageUrl != null && imageUrl.isNotEmpty()) {
                    Glide.with(this@ProfileActivity)
                        .load(imageUrl)
                        .apply(options)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(ivProfilePicture)
                } else {
                    ivProfilePicture.setImageResource(R.drawable.ic_profile)
                }

                // Update SharedPreferences
                sharedPrefs.edit().apply {
                    putString(EditProfileActivity.KEY_USERNAME, username)
                    putString(EditProfileActivity.KEY_NAME, profileData["name"] as? String ?: "Nama Pengguna")
                    putString(EditProfileActivity.KEY_GENDER, profileData["gender"] as? String ?: "Male")
                    putString(EditProfileActivity.KEY_IMAGE_URI, imageUrl)
                    apply()
                }

            } else {
                // Fallback: Load dari SharedPreferences
                val savedUsername = sharedPrefs.getString(EditProfileActivity.KEY_USERNAME, "Username")
                val imageUriString = sharedPrefs.getString(EditProfileActivity.KEY_IMAGE_URI, null)
                tvUsername.text = savedUsername
                ivProfilePicture.scaleX = 1f

                if (imageUriString != null) {
                    try {
                        Glide.with(this@ProfileActivity)
                            .load(imageUriString)
                            .apply(options)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(ivProfilePicture)
                    } catch (e: Exception) {
                        ivProfilePicture.setImageResource(R.drawable.ic_profile)
                    }
                } else {
                    ivProfilePicture.setImageResource(R.drawable.ic_profile)
                }
            }
        }
    }

    override fun finish() {
        super.finish()
    }
}