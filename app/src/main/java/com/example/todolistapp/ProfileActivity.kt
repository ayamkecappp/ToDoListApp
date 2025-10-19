package com.example.todolistapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.hdodenhof.circleimageview.CircleImageView
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.util.Log
import java.lang.Exception
import com.google.android.material.tabs.TabLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2


    private lateinit var tvUsername: TextView
    private lateinit var ivProfilePicture: CircleImageView
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var tvCompletedTasksLabel: TextView
    private lateinit var tvMissedTasksLabel: TextView
    private lateinit var tvDeletedTasksLabel: TextView

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra("PROFILE_PHOTO_URI")
            if (uriString != null) {
                val imageUri = Uri.parse(uriString)
                setProfileImage(imageUri)
                saveProfileImageUri(imageUri)
            } else {
                Toast.makeText(this, "Gagal mendapatkan foto profil.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        sharedPrefs = getSharedPreferences(EditProfileActivity.PREFS_NAME, Context.MODE_PRIVATE)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        tvUsername = findViewById(R.id.tvUsername)
        bottomNav = findViewById(R.id.bottomNav)
        ivProfilePicture = findViewById(R.id.ivProfile)

        val ivSettings = findViewById<ImageView>(R.id.ivSettings)
        val btnEditProfile = findViewById<TextView>(R.id.btnEditProfile)
        val CompletedTasks = findViewById<LinearLayout>(R.id.CompletedTasks)
        val MissedTasks = findViewById<LinearLayout>(R.id.MissedTasks)
        val DeletedTasks = findViewById<LinearLayout>(R.id.DeletedTasks)
        val ivCamera = findViewById<ImageView>(R.id.ivCamera)

        tvCompletedTasksLabel = CompletedTasks.getChildAt(0) as TextView
        tvMissedTasksLabel = MissedTasks.getChildAt(0) as TextView
        tvDeletedTasksLabel = DeletedTasks.getChildAt(0) as TextView

        // Perbaikan: BottomNavigationView.itemIconTintList adalah property
        bottomNav.itemIconTintList = null


        // 2. LOGIKA VIEWPAGER

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

        // Perbaikan: Menggunakan 'item.itemId' dan 'R.id.nav_home'
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home-> {
                    startActivity(
                        Intent(
                            this,
                            HomeActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    )
                    finish()
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_tasks-> {
                    startActivity(
                        Intent(
                            this,
                            TaskActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    )
                    finish()
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }

        bottomNav.selectedItemId = R.id.nav_profile

        ivSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

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
    }

    override fun onStart() {
        super.onStart()
        // Perbaikan: Menggunakan BottomNavigationView.selectedItemId
        bottomNav.selectedItemId = R.id.nav_profile
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
        updateTaskCounts()
    }

    // FUNGSI untuk memperbarui hitungan tugas (Diubah ke Coroutine)
    private fun updateTaskCounts() {
        lifecycleScope.launch(Dispatchers.IO) { // Menggunakan lifecycleScope
            try {
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
        val username = sharedPrefs.getString(EditProfileActivity.KEY_USERNAME, "Username")
        val imageUriString = sharedPrefs.getString(EditProfileActivity.KEY_IMAGE_URI, null)

        tvUsername.text = username

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            try {
                ivProfilePicture.setImageURI(imageUri)
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Gagal memuat foto dari URI: ${e.message}")
                ivProfilePicture.setImageResource(R.drawable.ic_profile)
                sharedPrefs.edit().remove(EditProfileActivity.KEY_IMAGE_URI).apply()
            }
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_profile)
        }
    }

    private fun setProfileImage(uri: Uri) {
        try {
            ivProfilePicture.setImageURI(uri)
            Toast.makeText(this, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error menampilkan foto: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("ProfileActivity", "Error setting image URI: ${e.message}")
        }
    }

    private fun saveProfileImageUri(uri: Uri) {
        sharedPrefs.edit().apply {
            putString(EditProfileActivity.KEY_IMAGE_URI, uri.toString())
            apply()
        }
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            Log.w("ProfileActivity", "URI tidak mendukung izin persisten: ${e.message}")
        }
    }

    override fun finish() {
        super.finish()
    }
}