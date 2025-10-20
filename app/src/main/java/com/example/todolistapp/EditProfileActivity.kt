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
import java.lang.Exception
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

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

                    // PERBAIKAN: Hapus baris ini yang menyebabkan error pada file:// URI.
                    // Izin sementara sudah diberikan melalui Intent.FLAG_GRANT_READ_URI_PERMISSION
                    // yang disetel di CameraActivity.
                    /*
                    currentImageUri?.let {
                        contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    */
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
            // Loop selama gambar masih lebih besar dari ukuran target
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getCompressedImageBytes(imageUri: Uri, targetSizeKB: Int): ByteArray? {
        var inputStream: InputStream? = null
        try {
            // --- Langkah 1: Resize Gambar (Langkah Paling Penting) ---

            // Pertama, cek dimensi gambar tanpa memuatnya ke memori
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

            // Tentukan ukuran maksimum yang wajar, misal 1024x1024 piksel
            // Ini akan menangani gambar dari kamera yang berukuran sangat besar
            val maxDimension = 1024.0
            val scale = min(
                maxDimension / originalWidth,
                maxDimension / originalHeight
            ).let { if (it > 1.0) 1.0 else it } // Jangan perbesar gambar jika sudah kecil

            val newWidth = (originalWidth * scale).roundToInt()
            val newHeight = (originalHeight * scale).roundToInt()

            // Hitung inSampleSize agar memori lebih hemat
            options.inSampleSize = calculateInSampleSize(options, newWidth, newHeight)
            options.inJustDecodeBounds = false // Sekarang kita akan memuat bitmap-nya

            // Buka stream lagi untuk memuat bitmap yang sudah di-subsample
            inputStream = contentResolver.openInputStream(imageUri)
            val bitmapToResize = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmapToResize == null) {
                Log.e("ImageCompression", "Gagal men-decode bitmap.")
                return null
            }

            // Buat bitmap akhir dengan ukuran pasti
            val finalBitmap = Bitmap.createScaledBitmap(bitmapToResize, newWidth, newHeight, true)
            if (finalBitmap != bitmapToResize) {
                bitmapToResize.recycle() // Bebaskan memori bitmap lama
            }

            // --- Langkah 2: Kompresi Kualitas Berulang ---

            var quality = 95 // Mulai dari kualitas 95%
            val outputStream = ByteArrayOutputStream()
            var compressedBytes: ByteArray
            val targetSizeBytes = targetSizeKB * 1024

            do {
                outputStream.reset() // Hapus data kompresi sebelumnya
                // Kompresi ke format JPEG. Format ini WAJIB untuk kompresi foto.
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                compressedBytes = outputStream.toByteArray()

                val currentSizeKB = compressedBytes.size / 1024
                Log.d("ImageCompression", "Kualitas: $quality, Ukuran: ${currentSizeKB}KB")

                // Turunkan kualitas untuk iterasi berikutnya
                quality -= 5

            } while (compressedBytes.size > targetSizeBytes && quality > 40) // Loop selama ukuran > target DAN kualitas > 40%

            finalBitmap.recycle() // Bebaskan memori bitmap
            outputStream.close()

            Log.d("ImageCompression", "Ukuran final: ${compressedBytes.size / 1024}KB")
            return compressedBytes

        } catch (e: Exception) {
            Log.e("ImageCompression", "Gagal mengkompresi gambar", e)
            inputStream?.close() // Pastikan stream ditutup jika terjadi error
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
            text = "Save" // Teks "Save"

            backgroundTintList = ContextCompat.getColorStateList(context, R.color.dark_blue)
            setCornerRadius(12.dp)

            // PERBAIKAN: Set warna teks secara eksplisit menjadi Putih
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

        // Listener untuk membuka kamera
        tvEditPhoto.setOnClickListener { launchCamera() }
        ivProfilePicture.setOnClickListener { launchCamera() }

        // 3. Setup Dropdown Gender
        inputGender.setOnClickListener {
            showGenderDropdown()
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
            currentImageUri = Uri.parse(imageUriString)
            try {
                // HANYA COBA TAMPILKAN. Jika gagal, exception akan terjadi.
                ivProfilePicture.setImageURI(currentImageUri)
            } catch (e: Exception) {
                // Jika gagal, reset ke placeholder
                Log.e("EditProfile", "Error loading image URI on load: ${e.message}")
                currentImageUri = null
                sharedPrefs.edit().remove(KEY_IMAGE_URI).apply() // Hapus URI yang rusak
                ivProfilePicture.setImageResource(R.drawable.ic_profile)
            }
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_profile)
        }
    }

    private suspend fun uploadProfileImage(userId: String, imageUri: Uri): String? {
        if (userId.isEmpty()) {
            Log.e("UploadImage", "userId kosong!")
            return null
        }

        // Log informasi user
        Log.d("UploadImage", "Memulai upload untuk userId: $userId")

        // 1. Kompresi gambar ke target 300KB
        val targetKB = 300
        val compressedImageBytes = withContext(Dispatchers.IO) {
            getCompressedImageBytes(imageUri, targetKB)
        }

        if (compressedImageBytes == null) {
            Log.e("UploadImage", "Kompresi gambar gagal, upload dibatalkan.")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@EditProfileActivity, "Format gambar tidak didukung.", Toast.LENGTH_SHORT).show()
            }
            return null
        }

        Log.d("UploadImage", "Ukuran gambar terkompresi: ${compressedImageBytes.size / 1024}KB")

        return try {
            // Gunakan UploadTask dengan callback yang lebih eksplisit
            withContext(Dispatchers.IO) {
                val storageRef = FirebaseStorage.getInstance().reference
                val timestamp = System.currentTimeMillis()
                val fileName = "profile_images/$userId/profile_$timestamp.jpg"
                val imageRef = storageRef.child(fileName)

                Log.d("UploadImage", "Path storage: $fileName")

                // Upload file
                val uploadTaskSnapshot = imageRef.putBytes(compressedImageBytes).await()

                Log.d("UploadImage", "Upload selesai. Bytes: ${uploadTaskSnapshot.bytesTransferred}")
                Log.d("UploadImage", "Storage path: ${uploadTaskSnapshot.storage.path}")

                // SOLUSI UTAMA: Gunakan storage reference dari uploadTaskSnapshot
                // karena ini dijamin sudah benar dan file sudah ada
                val downloadUrl = uploadTaskSnapshot.storage.downloadUrl.await().toString()

                Log.d("UploadImage", "Download URL: $downloadUrl")

                downloadUrl
            }

        } catch (e: Exception) {
            Log.e("UploadImage", "Error detail: ", e)
            e.printStackTrace()

            withContext(Dispatchers.Main) {
                val errorMsg = when (e) {
                    is com.google.firebase.storage.StorageException -> {
                        Log.e("UploadImage", "StorageException code: ${e.errorCode}")
                        when (e.errorCode) {
                            com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND ->
                                "File tidak ditemukan di storage. Periksa rules."
                            com.google.firebase.storage.StorageException.ERROR_NOT_AUTHENTICATED ->
                                "User tidak login. Silakan login ulang."
                            com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED ->
                                "Tidak ada izin upload. Periksa Storage Rules."
                            else -> "Storage error (${e.errorCode}): ${e.message}"
                        }
                    }
                    else -> "Upload gagal: ${e.message}"
                }
                Toast.makeText(this@EditProfileActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
            null
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

        // Gunakan lifecycleScope untuk Coroutine
        lifecycleScope.launch {
            var imageUrl: String? = null // Variabel untuk menyimpan URL hasil upload

            // 1. Cek apakah ada gambar baru yang dipilih
            if (currentImageUri != null) {
                // 2. Unggah gambar ke Firebase Storage (jalankan di background thread)
                imageUrl = withContext(Dispatchers.IO) {
                    uploadProfileImage(userId, currentImageUri!!)
                }
                if (imageUrl == null) {
                    // Jika upload gagal, beri tahu pengguna dan hentikan proses simpan
                    Toast.makeText(this@EditProfileActivity, "Gagal mengunggah gambar profil.", Toast.LENGTH_SHORT).show()
                    return@launch // Hentikan coroutine
                }
            } else {
                // Jika tidak ada gambar baru, coba pertahankan URL lama (jika ada)
                imageUrl = sharedPrefs.getString(KEY_IMAGE_URI, null)
            }


            // 3. Buat objek data profil untuk Firestore
            val profileData = hashMapOf(
                "name" to newName,
                "username" to newUsername,
                "gender" to newGender,
                "profileImageUrl" to imageUrl // Simpan URL unduhan (bisa null)
                // Tambahkan field lain jika perlu
            )

            // 4. Simpan data ke Firestore (jalankan di background thread)
            val success = withContext(Dispatchers.IO) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    // Simpan ke collection 'users', document dengan ID user, sub-collection 'profile' (contoh)
                    // Atau langsung di document user jika struktur datanya sederhana
                    db.collection("users").document(userId).set(profileData).await() // Menggunakan set untuk overwrite atau create
                    true // Berhasil
                } catch (e: Exception) {
                    Log.e("FirestoreSave", "Error saving profile data", e)
                    false // Gagal
                }
            }

            // 5. Update UI dan SharedPreferences setelah simpan Firestore
            if (success) {
                // (Opsional) Simpan juga ke SharedPreferences jika masih diperlukan
                sharedPrefs.edit().apply {
                    putString(KEY_NAME, newName)
                    putString(KEY_USERNAME, newUsername)
                    putString(KEY_GENDER, newGender)
                    putString(KEY_IMAGE_URI, imageUrl) // Simpan URL baru/lama
                    apply()
                }
                Toast.makeText(this@EditProfileActivity, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this@EditProfileActivity, "Gagal menyimpan profil ke database.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fungsi untuk meluncurkan CameraActivity
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