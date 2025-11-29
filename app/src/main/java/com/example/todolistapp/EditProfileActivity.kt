package com.example.todolistapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.min
import kotlin.math.roundToInt
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix


class EditProfileActivity : AppCompatActivity() {

    // HAPUS KONSTANTA SHAREDPREFERENCES UNTUK USERNAME
    // private val PROFILE_PREFS_NAME = "ProfilePrefs"
    // private val KEY_USERNAME = "username"

    private lateinit var inputName: EditText
    private lateinit var inputUsername: EditText
    private lateinit var inputGender: EditText
    private lateinit var ivBackArrow: ImageView
    private lateinit var tvEditPhoto: TextView
    private lateinit var ivProfilePicture: CircleImageView
    //private lateinit var sharedPrefs: SharedPreferences

    private var currentGender: String = "Male"
    private var currentImageUri: Uri? = null
    private val genders = arrayOf("Male", "Female")
    private var currentPermanentImageUrl: String? = null
    // Cloudinary credentials
    private val CLOUD_NAME = "dk2jrlugl"
    private val UPLOAD_PRESET = "android_profile_upload"

    private val client = OkHttpClient()

    // Launcher untuk CameraActivity
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra("PROFILE_PHOTO_URI")
            if (uriString != null) {
                currentImageUri = Uri.parse(uriString) // 1. Set variabel instance
                try {
                    // 2. HAPUS baris SharedPreferences dari kode asli Anda
                    // sharedPrefs.edit().putString(KEY_IMAGE_URI, currentImageUri.toString()).apply() // <-- HAPUS

                    // 3. Muat pratinjau langsung dari variabel instance
                    val options = RequestOptions().transform(CircleCrop())
                    Glide.with(this).load(currentImageUri).apply(options).into(ivProfilePicture)
                    Toast.makeText(this, "Profile photo successfully updated!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("EditProfile", "Error setting image URI from camera: ${e.message}")
                    Toast.makeText(this, "Failed to load image from camera. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
                Log.e("ImageCompression", "Failed to read image dimensions.")
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

            options.inSampleSize = calculateInSampleSize(options, tempWidth, tempHeight)
            options.inJustDecodeBounds = false

            // 3. Decode Bitmap Awal (Kode asli)
            inputStream = contentResolver.openInputStream(imageUri)
            var bitmapToProcess = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmapToProcess == null) {
                Log.e("ImageCompression", "Failed to decode bitmap.")
                return null
            }

            // 4. --- TAMBAHAN: ROTASI BERDASARKAN EXIF ---
            try {
                inputStream = contentResolver.openInputStream(imageUri) // Buka stream BARU untuk EXIF
                if (inputStream != null) {
                    bitmapToProcess = rotateBitmapBasedOnExif(bitmapToProcess, inputStream)
                    Log.d("ImageCompression", "EXIF rotation applied.")
                }
            } catch (exifError: Exception) {
                Log.e("ImageCompression", "Failed to read EXIF/rotation", exifError)
                // Lanjutkan proses tanpa rotasi jika gagal
            } finally {
                inputStream?.close() // Pastikan stream EXIF ditutup
            }
            // --- AKHIR TAMBAHAN ---

            // 5. Hitung Ulang Scaling SETELAH Rotasi
            // Dimensi bitmapToProcess mungkin sudah berubah (misal: 1000x800 jadi 800x1000)
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

                val currentSizeKB = compressedBytes.size / 1024
                Log.d("ImageCompression", "Quality: $quality, Size: ${currentSizeKB}KB")

                quality -= 5

            } while (compressedBytes.size > targetSizeBytes && quality > 40)

            finalBitmap.recycle() // Bebaskan bitmap hasil scaling
            outputStream.close()

            Log.d("ImageCompression", "Final size: ${compressedBytes.size / 1024}KB")
            return compressedBytes

        } catch (e: Exception) {
            Log.e("ImageCompression", "Failed to compress image", e)
            try { inputStream?.close() } catch (eClose: Exception) { /* abaikan */ }
            return null
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
                matrix.preRotate(90.0f)
                matrix.preScale(-1.0f, 1.0f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preRotate(270.0f)
                matrix.preScale(-1.0f, 1.0f)
            }
            // Tambahkan case lain jika diperlukan
            else -> return source // Tidak perlu rotasi
        }
        return try {
            val rotatedBitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            if (rotatedBitmap != source) {
                source.recycle() // Bebaskan memori bitmap asli jika sudah dirotasi
            }
            rotatedBitmap
        } catch (e: OutOfMemoryError) {
            Log.e("RotateBitmap", "OutOfMemoryError saat merotasi bitmap", e)
            source // Kembalikan bitmap asli jika error
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)


        // Hubungkan Views
        inputName = findViewById(R.id.inputName)
        inputUsername = findViewById(R.id.inputUsername)
        inputGender = findViewById(R.id.inputGender)
        ivBackArrow = findViewById(R.id.ivBackArrow)
        tvEditPhoto = findViewById(R.id.tvEditPhoto)
        ivProfilePicture = findViewById(R.id.ivProfilePicture)

        // Dapatkan LinearLayout di dalam ScrollView
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val linearLayout = scrollView.getChildAt(0) as ViewGroup

        // Membuat tombol Save Changes secara dinamis
        val btnSave = MaterialButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 48.dp, 0, 48.dp)
                marginStart = 24.dp
                marginEnd = 24.dp
            }
            text = "Save"
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.dark_blue)
            cornerRadius = 12.dp
            setTextColor(ContextCompat.getColor(context, R.color.white))
            linearLayout.addView(this)
        }

        // Muat Data Profil dan Gambar. Panggilan ini dilakukan di onCreate dan onResume
        loadProfileData()

        // 2. Setup Listeners
        // Setup Listeners - Letakkan SETELAH loadProfileData()
        ivBackArrow.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveProfileData()
        }

// Pastikan tvEditPhoto dan ivProfilePicture mendapat listener yang benar
        tvEditPhoto.setOnClickListener {
            Log.d("EditProfile", "tvEditPhoto clicked - launching camera")
            launchCamera()
        }

        ivProfilePicture.setOnClickListener {
            Log.d("EditProfile", "ivProfilePicture clicked - launching camera")
            launchCamera()
        }

// Setup Dropdown Gender
        inputGender.setOnClickListener {
            showGenderDropdown()
        }
    }

    override fun onResume() {
        super.onResume()
        // PENTING: Muat ulang data saat onResume untuk menyinkronkan gambar
        loadProfileData()
    }

    private suspend fun uploadImageToCloudinary(userId: String, imageUri: Uri): String? {
        if (userId.isEmpty()) {
            Log.e("CloudinaryUpload", "userId is empty!")
            return null
        }

        Log.d("CloudinaryUpload", "Starting upload for userId: $userId")

        return withContext(Dispatchers.IO) {
            try {
                // 1. Kompresi gambar
                val compressedBytes = getCompressedImageBytes(imageUri, 500) // 500KB max
                if (compressedBytes == null) {
                    Log.e("CloudinaryUpload", "Image compression failed")
                    return@withContext null
                }

                // 2. Simpan ke file sementara
                val tempFile = File(cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use {
                    it.write(compressedBytes)
                }

                // 3. Generate timestamp and public ID
                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val publicId = "profile_images/$userId/profile_$timestamp"

                // 4. Buat multipart request
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        tempFile.name,
                        tempFile.asRequestBody("image/jpeg".toMediaType())
                    )
                    .addFormDataPart("upload_preset", UPLOAD_PRESET) // Untuk unsigned upload
                    .addFormDataPart("folder", "profile_images/$userId")
                    .addFormDataPart("public_id", publicId)
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                    .post(requestBody)
                    .build()

                // 5. Execute request
                val response = client.newCall(request).execute()

                // 6. Hapus file sementara
                tempFile.delete()

                // 7. Parse response
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("CloudinaryUpload", "Response: $responseBody")

                    // Parse JSON untuk mendapatkan secure_url
                    val secureUrl = responseBody?.let { extractSecureUrl(it) }

                    if (secureUrl != null) {
                        Log.d("CloudinaryUpload", "Upload successful: $secureUrl")
                        return@withContext secureUrl
                    } else {
                        Log.e("CloudinaryUpload", "Secure URL not found in response")
                        return@withContext null
                    }
                } else {
                    Log.e("CloudinaryUpload", "Upload failed: ${response.code} - ${response.message}")
                    val errorBody = response.body?.string()
                    Log.e("CloudinaryUpload", "Error body: $errorBody")
                    return@withContext null
                }

            } catch (e: Exception) {
                Log.e("CloudinaryUpload", "Exception: ${e.message}", e)
                return@withContext null
            }
        }
    }

    // Fungsi helper untuk extract secure_url dari JSON response
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

    private fun loadProfileData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User is not logged in.", Toast.LENGTH_SHORT).show()
            // Set default jika user null
            inputName.setText("Name")
            inputUsername.setText("@username")
            inputGender.setText(currentGender) // Default "Male"
            ivProfilePicture.setImageResource(R.drawable.ic_profile)

            // HAPUS LOGIKA SHARED PREFERENCES UNTUK USERNAME

            return
        }

        // Reset URI lokal (karena kita baru memuat)
        currentImageUri = null

        // Tampilkan loading (opsional, tapi disarankan)
        // Misal: progressBar.visibility = View.VISIBLE
        Toast.makeText(this, "Loading profile...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val document = withContext(Dispatchers.IO) {
                    db.collection("users").document(userId).get().await()
                }

                if (document.exists()) {
                    // Ambil data dari dokumen Firestore
                    val name = document.getString("name") ?: "Name"
                    val username = document.getString("username") ?: "@username"
                    currentGender = document.getString("gender") ?: "Male" // Tetap set currentGender

                    // Simpan URL permanen ke variabel instance
                    currentPermanentImageUrl = document.getString("profileImageUrl")

                    // Set view
                    inputName.setText(name)
                    inputUsername.setText(username)
                    inputGender.setText(currentGender)

                    // HAPUS SINKRONISASI SHARED PREFERENCES UNTUK USERNAME
                    // END HAPUS

                    if (!currentPermanentImageUrl.isNullOrEmpty()) {
                        val options = RequestOptions().transform(CircleCrop())
                        Glide.with(this@EditProfileActivity)
                            .load(currentPermanentImageUrl)
                            .apply(options)
                            .into(ivProfilePicture)
                    } else {
                        ivProfilePicture.setImageResource(R.drawable.ic_profile)
                    }
                } else {
                    // Jika user baru dan dokumen belum ada, set default
                    inputName.setText("Name")
                    inputUsername.setText("@username")
                    inputGender.setText(currentGender)
                    ivProfilePicture.setImageResource(R.drawable.ic_profile)
                    currentPermanentImageUrl = null // Pastikan null

                    // HAPUS SETTING DEFAULT DI SHARED PREFERENCES
                    // END HAPUS
                }
            } catch (e: Exception) {
                Log.e("FirestoreLoad", "Error loading profile data", e)
                Toast.makeText(this@EditProfileActivity, "Failed to load profile.", Toast.LENGTH_SHORT).show()
            } finally {
                // Sembunyikan loading (opsional)
                // Misal: progressBar.visibility = View.GONE
            }
        }
    }

    private fun saveProfileData() {
        val rawName = inputName.text.toString()
        val rawUsername = inputUsername.text.toString()
        val newGender = currentGender
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val newName = InputValidator.sanitizeText(rawName)
        if (newName.isEmpty()) {
            inputName.error = "Nama tidak boleh kosong"
            return
        }
        val newUsername = rawUsername.trim()
        if (!InputValidator.isValidUsername(rawUsername)) {
            inputUsername.error = "Username hanya boleh huruf, angka, titik(.), atau garis bawah(_)"
            return
        }

        if (userId == null) {
            Toast.makeText(this, "User is not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Saving profile...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            // Mulai dengan URL yang tadi kita ambil dari Firestore
            var finalImageString: String? = currentPermanentImageUrl

            // 1. Cek apakah ada URI LOKAL BARU (dari kamera)
            val isLocalUriForUpload = currentImageUri != null

            if (isLocalUriForUpload) {
                // 1a. Upload ke Cloudinary
                val uploadedUrl = uploadImageToCloudinary(userId, currentImageUri!!)

                if (uploadedUrl == null) {
                    // Upload GAGAL, beri tahu pengguna.
                    // 'finalImageString' tidak diubah (tetap menggunakan URL lama)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditProfileActivity, "Failed to upload profile image. Photo data not changed.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Upload SUKSES, URL Cloudinary menjadi string gambar baru
                    finalImageString = uploadedUrl
                }
            }
            // Tidak perlu 'else if (currentImageUri == null)'
            // Jika tidak ada URI lokal baru, 'finalImageString' (URL lama) akan otomatis digunakan.

            // 2. Simpan data ke Firestore (INI ADALAH SUMBER DATA BARU UNTUK HOMEACTIVITY)
            val profileData = hashMapOf(
                "name" to newName,
                "username" to newUsername,
                "gender" to newGender,
                "profileImageUrl" to (finalImageString ?: ""), // Pakai final string
                "updatedAt" to System.currentTimeMillis()
            )

            val success = withContext(Dispatchers.IO) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users")
                        .document(userId)
                        .set(profileData)
                        .await()
                    true
                } catch (e: Exception) {
                    Log.e("FirestoreSave", "Error saving profile data", e)
                    false
                }
            }

            // 3. Navigasi (HAPUS SEMUA SINKRONISASI SHARED PREFERENCES)
            if (success) {
                // HAPUS SINKRONISASI SHARED PREFERENCES
                // END HAPUS

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Profile successfully updated!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Failed to save profile to database.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    private fun showGenderDropdown() {
        val listPopupWindow = ListPopupWindow(this).apply {
            anchorView = inputGender

            val adapter = GenderAdapter(this@EditProfileActivity, genders)
            setAdapter(adapter)

            width = inputGender.width
            isModal = true
            setBackgroundDrawable(ResourcesCompat.getDrawable(resources, R.drawable.bg_popup_rounded_12dp, theme))
        }

        listPopupWindow.setOnItemClickListener { parent, view, position, id ->
            val selectedGender = genders[position]

            currentGender = selectedGender
            inputGender.setText(selectedGender)

            listPopupWindow.dismiss()
        }

        listPopupWindow.show()
    }

    private inner class GenderAdapter(context: Context, items: Array<String>) :
        ArrayAdapter<String>(context, 0, items) {

        private val inflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.list_item_priority, parent, false)
            val item = getItem(position)!!

            val tvOption = view.findViewById<TextView>(R.id.tvPriorityOption)
            val ivCheckmark = view.findViewById<ImageView>(R.id.ivCheckmark)

            tvOption.text = item

            if (item == currentGender) {
                ivCheckmark.visibility = View.VISIBLE
                ivCheckmark.setColorFilter(ContextCompat.getColor(context, R.color.dark_blue))
            } else {
                ivCheckmark.visibility = View.INVISIBLE
            }

            return view
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}