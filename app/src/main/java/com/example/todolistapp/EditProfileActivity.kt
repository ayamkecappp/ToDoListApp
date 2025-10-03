package com.example.todolistapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import java.lang.Exception

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

                    currentImageUri?.let {
                        contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    Toast.makeText(this, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("EditProfile", "Error setting image URI from camera or taking permission: ${e.message}")
                    Toast.makeText(this, "Gagal memuat gambar dari kamera. Coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
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

    private fun saveProfileData() {
        val newName = inputName.text.toString().trim()
        val newUsername = inputUsername.text.toString().trim()
        val newGender = currentGender

        if (newName.isEmpty() || newUsername.isEmpty()) {
            Toast.makeText(this, "Nama dan Username tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        sharedPrefs.edit().apply {
            putString(KEY_NAME, newName)
            putString(KEY_USERNAME, newUsername)
            putString(KEY_GENDER, newGender)
            // Simpan URI gambar yang baru
            putString(KEY_IMAGE_URI, currentImageUri?.toString())
            apply()
        }

        Toast.makeText(this, "Profile berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
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