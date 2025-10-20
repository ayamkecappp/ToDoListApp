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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.coroutines.resume
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class EditProfileActivity : AppCompatActivity() {

    private lateinit var inputName: EditText
    private lateinit var inputUsername: EditText
    private lateinit var inputGender: EditText
    private lateinit var ivBackArrow: ImageView
    private lateinit var tvEditPhoto: TextView
    private lateinit var ivProfilePicture: CircleImageView
    private lateinit var sharedPrefs: SharedPreferences

    private var currentGender: String = "Male"
    private var currentImageUri: Uri? = null
    private val genders = arrayOf("Male", "Female")

    // Cloudinary credentials
    private val CLOUD_NAME = "dk2jrlugl"  // Ganti dengan Cloud Name Anda
    private val API_KEY = "791225435148732"  // Ganti dengan API Key Anda
    private val API_SECRET = "mlCaQzDFef6crC79tEJ0hEKWZ_s"  // Ganti dengan API Secret Anda
    private val UPLOAD_PRESET = "android_profile_upload"  // Ganti dengan Upload Preset Anda

    private val client = OkHttpClient()

    // Launcher untuk CameraActivity
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra("PROFILE_PHOTO_URI")
            if (uriString != null) {
                currentImageUri = Uri.parse(uriString)
                try {
                    ivProfilePicture.setImageURI(currentImageUri)
                    Toast.makeText(this, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("EditProfile", "Error setting image URI from camera: ${e.message}")
                    Toast.makeText(this, "Gagal memuat gambar dari kamera. Coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getCompressedImageBytes(imageUri: Uri, targetSizeKB: Int): ByteArray? {
        var inputStream: InputStream? = null
        try {
            // Langkah 1: Resize Gambar
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

            val maxDimension = 1024.0
            val scale = min(
                maxDimension / originalWidth,
                maxDimension / originalHeight
            ).let { if (it > 1.0) 1.0 else it }

            val newWidth = (originalWidth * scale).roundToInt()
            val newHeight = (originalHeight * scale).roundToInt()

            options.inSampleSize = calculateInSampleSize(options, newWidth, newHeight)
            options.inJustDecodeBounds = false

            inputStream = contentResolver.openInputStream(imageUri)
            val bitmapToResize = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmapToResize == null) {
                Log.e("ImageCompression", "Gagal men-decode bitmap.")
                return null
            }

            val finalBitmap = Bitmap.createScaledBitmap(bitmapToResize, newWidth, newHeight, true)
            if (finalBitmap != bitmapToResize) {
                bitmapToResize.recycle()
            }

            // Langkah 2: Kompresi Kualitas Berulang
            var quality = 95
            val outputStream = ByteArrayOutputStream()
            var compressedBytes: ByteArray
            val targetSizeBytes = targetSizeKB * 1024

            do {
                outputStream.reset()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                compressedBytes = outputStream.toByteArray()

                val currentSizeKB = compressedBytes.size / 1024
                Log.d("ImageCompression", "Kualitas: $quality, Ukuran: ${currentSizeKB}KB")

                quality -= 5

            } while (compressedBytes.size > targetSizeBytes && quality > 40)

            finalBitmap.recycle()
            outputStream.close()

            Log.d("ImageCompression", "Ukuran final: ${compressedBytes.size / 1024}KB")
            return compressedBytes

        } catch (e: Exception) {
            Log.e("ImageCompression", "Gagal mengkompresi gambar", e)
            inputStream?.close()
            return null
        }
    }

    companion object {
        const val PREFS_NAME = "ProfilePrefs"
        const val KEY_NAME = "name"
        const val KEY_USERNAME = "username"
        const val KEY_GENDER = "gender"
        const val KEY_IMAGE_URI = "image_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

        // 1. Muat Data Profil dan Gambar
        loadProfileData()

        // 2. Setup Listeners
        ivBackArrow.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveProfileData()
        }

        tvEditPhoto.setOnClickListener { launchCamera() }
        ivProfilePicture.setOnClickListener { launchCamera() }

        // 3. Setup Dropdown Gender
        inputGender.setOnClickListener {
            showGenderDropdown()
        }
    }

    private suspend fun uploadImageToCloudinary(userId: String, imageUri: Uri): String? {
        if (userId.isEmpty()) {
            Log.e("CloudinaryUpload", "userId kosong!")
            return null
        }

        Log.d("CloudinaryUpload", "Memulai upload untuk userId: $userId")

        return withContext(Dispatchers.IO) {
            try {
                // 1. Kompresi gambar terlebih dahulu
                val compressedBytes = getCompressedImageBytes(imageUri, 500) // 500KB max
                if (compressedBytes == null) {
                    Log.e("CloudinaryUpload", "Kompresi gambar gagal")
                    return@withContext null
                }

                // 2. Simpan ke file sementara
                val tempFile = File(cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use {
                    it.write(compressedBytes)
                }

                // 3. Generate signature untuk signed upload (lebih aman)
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
                    // Menggunakan cara manual tanpa library JSON
                    val secureUrl = responseBody?.let { extractSecureUrl(it) }

                    if (secureUrl != null) {
                        Log.d("CloudinaryUpload", "Upload berhasil: $secureUrl")
                        return@withContext secureUrl
                    } else {
                        Log.e("CloudinaryUpload", "Secure URL tidak ditemukan di response")
                        return@withContext null
                    }
                } else {
                    Log.e("CloudinaryUpload", "Upload gagal: ${response.code} - ${response.message}")
                    val errorBody = response.body?.string()
                    Log.e("CloudinaryUpload", "Error body: $errorBody")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@EditProfileActivity,
                            "Upload gagal: ${response.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@withContext null
                }

            } catch (e: Exception) {
                Log.e("CloudinaryUpload", "Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Error upload: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@withContext null
            }
        }
    }

    // Fungsi helper untuk extract secure_url dari JSON response
    private fun extractSecureUrl(jsonResponse: String): String? {
        return try {
            // Cari "secure_url":"..."
            val startIndex = jsonResponse.indexOf("\"secure_url\":\"") + 14
            if (startIndex < 14) return null

            val endIndex = jsonResponse.indexOf("\"", startIndex)
            if (endIndex == -1) return null

            val url = jsonResponse.substring(startIndex, endIndex)
            // Unescape URL jika perlu
            url.replace("\\/", "/")
        } catch (e: Exception) {
            Log.e("CloudinaryUpload", "Error parsing secure_url: ${e.message}")
            null
        }
    }

    private fun loadProfileData() {
        val name = sharedPrefs.getString(KEY_NAME, "Nama Pengguna")
        val username = sharedPrefs.getString(KEY_USERNAME, "@username")
        currentGender = sharedPrefs.getString(KEY_GENDER, "Male")!!
        val imageUriString = sharedPrefs.getString(KEY_IMAGE_URI, null)

        inputName.setText(name)
        inputUsername.setText(username)
        inputGender.setText(currentGender)

        // Muat gambar jika URI ada
        if (imageUriString != null) {
            if (imageUriString.startsWith("http")) {
                // Ini adalah URL dari Cloudinary
                // TODO: Gunakan Glide untuk load image
                // Glide.with(this).load(imageUriString).into(ivProfilePicture)
                Log.d("EditProfile", "Image URL dari Cloudinary: $imageUriString")
                // Sementara set icon default
                ivProfilePicture.setImageResource(R.drawable.ic_profile)
            } else {
                // Ini adalah URI lokal
                currentImageUri = Uri.parse(imageUriString)
                try {
                    ivProfilePicture.setImageURI(currentImageUri)
                } catch (e: Exception) {
                    Log.e("EditProfile", "Error loading image URI: ${e.message}")
                    currentImageUri = null
                    sharedPrefs.edit().remove(KEY_IMAGE_URI).apply()
                    ivProfilePicture.setImageResource(R.drawable.ic_profile)
                }
            }
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_profile)
        }
    }

    private fun saveProfileData() {
        val newName = inputName.text.toString().trim()
        val newUsername = inputUsername.text.toString().trim()
        val newGender = currentGender
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (newName.isEmpty() || newUsername.isEmpty()) {
            Toast.makeText(this, "Nama dan Username tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == null) {
            Toast.makeText(this, "User tidak login.", Toast.LENGTH_SHORT).show()
            return
        }

        // Tampilkan loading
        Toast.makeText(this, "Menyimpan profil...", Toast.LENGTH_SHORT).show()

        // Gunakan lifecycleScope untuk Coroutine
        lifecycleScope.launch {
            var imageUrl: String? = null

            // 1. Cek apakah ada gambar baru untuk diupload
            if (currentImageUri != null) {
                // 2. Upload ke Cloudinary
                imageUrl = uploadImageToCloudinary(userId, currentImageUri!!)

                if (imageUrl == null) {
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Gagal mengunggah gambar profil.",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Gunakan URL lama jika upload gagal
                    imageUrl = sharedPrefs.getString(KEY_IMAGE_URI, null)
                } else {
                    Log.d("SaveProfile", "Image uploaded successfully: $imageUrl")
                }
            } else {
                // Jika tidak ada gambar baru, pertahankan URL lama
                imageUrl = sharedPrefs.getString(KEY_IMAGE_URI, null)
            }

            // 3. Buat objek data profil untuk Firestore
            val profileData = hashMapOf(
                "name" to newName,
                "username" to newUsername,
                "gender" to newGender,
                "profileImageUrl" to imageUrl,
                "updatedAt" to System.currentTimeMillis()
            )

            // 4. Simpan data ke Firestore
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

            // 5. Update UI dan SharedPreferences
            if (success) {
                sharedPrefs.edit().apply {
                    putString(KEY_NAME, newName)
                    putString(KEY_USERNAME, newUsername)
                    putString(KEY_GENDER, newGender)
                    putString(KEY_IMAGE_URI, imageUrl)
                    apply()
                }
                Toast.makeText(
                    this@EditProfileActivity,
                    "Profil berhasil diperbarui!",
                    Toast.LENGTH_SHORT
                ).show()
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(
                    this@EditProfileActivity,
                    "Gagal menyimpan profil ke database.",
                    Toast.LENGTH_SHORT
                ).show()
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