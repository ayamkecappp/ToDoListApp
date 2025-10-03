package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import android.app.Activity
import de.hdodenhof.circleimageview.CircleImageView // PENTING: Import CircleImageView

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: CircleImageView // UBAH TIPE DATA
    private lateinit var bottomNav: BottomNavigationView // Dipertahankan untuk Bottom Nav
    // Activity Result Launcher untuk CameraActivity
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra("PROFILE_PHOTO_URI")
            if (uriString != null) {
                val imageUri = Uri.parse(uriString)
                setProfileImage(imageUri)
            } else {
                Toast.makeText(this, "Gagal mendapatkan foto profil.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        // Ambil BottomNavigationView
        bottomNav = findViewById(R.id.bottomNav)

        bottomNav.itemIconTintList = null

        ivProfile = findViewById(R.id.ivProfile) as CircleImageView // Inisialisasi ivProfile sebagai CircleImageView


        // Listener navigasi navbar
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    true
                }
                R.id.nav_tasks -> {
                    startActivity(Intent(this, TaskActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    true
                }
                R.id.nav_profile -> true // Sudah di ProfileActivity
                else -> false
            }
        }

        // Klik icon settings
        val ivSettings = findViewById<ImageView>(R.id.ivSettings)
        ivSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Klik ivCamera untuk membuka CameraActivity
        val ivCamera = findViewById<ImageView>(R.id.ivCamera)
        ivCamera.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            cameraLauncher.launch(intent) // Gunakan launcher
        }

        // Klik Completed Tasks
        val CompletedTasks = findViewById<LinearLayout>(R.id.CompletedTasks)
        CompletedTasks.setOnClickListener {
            val intent = Intent(this, CompletedTasksActivity::class.java)
            startActivity(intent)
        }

        // Klik Missed Tasks
        val MissedTasks = findViewById<LinearLayout>(R.id.MissedTasks)
        MissedTasks.setOnClickListener {
            val intent = Intent(this, MissedTasksActivity::class.java)
            startActivity(intent)
        }

        // Klik Deleted Tasks
        val DeletedTasks = findViewById<LinearLayout>(R.id.DeletedTasks)
        DeletedTasks.setOnClickListener {
            val intent = Intent(this, DeletedTasksActivity::class.java)
            startActivity(intent)
        }
    }


    /**
     * Kunci Perbaikan: Set status item setiap kali Activity terlihat.
     */
    override fun onStart() {
        super.onStart()
        // Pastikan ikon Profile ditandai sebagai aktif
        bottomNav.selectedItemId = R.id.nav_profile
    }

    private fun setProfileImage(uri: Uri) {
        try {
            // Karena URI berasal dari Intent dengan FLAG_GRANT_READ_URI_PERMISSION,
            // kita bisa langsung set ImageURI
            ivProfile.setImageURI(uri)
            Toast.makeText(this, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error menampilkan foto: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}