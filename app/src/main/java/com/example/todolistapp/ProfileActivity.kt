package com.example.todolistapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

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
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra("PROFILE_PHOTO_URI")
            if (uriString != null) {
                val imageUri = Uri.parse(uriString)
                setProfileImage(imageUri)
                saveProfileImageUri(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        sharedPrefs = getSharedPreferences(EditProfileActivity.PREFS_NAME, Context.MODE_PRIVATE)
        initializeViews()
        setupListeners()
        setupViewPager()
    }

    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.nav_profile
        loadProfileData()
        updateTaskCounts() // Memuat jumlah tugas dari Firestore
    }

    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        tvUsername = findViewById(R.id.tvUsername)
        bottomNav = findViewById(R.id.bottomNav)
        ivProfilePicture = findViewById(R.id.ivProfile)
        bottomNav.itemIconTintList = null

        val completedTasksLayout = findViewById<LinearLayout>(R.id.CompletedTasks)
        val missedTasksLayout = findViewById<LinearLayout>(R.id.MissedTasks)
        val deletedTasksLayout = findViewById<LinearLayout>(R.id.DeletedTasks)

        tvCompletedTasksLabel = completedTasksLayout.getChildAt(0) as TextView
        tvMissedTasksLabel = missedTasksLayout.getChildAt(0) as TextView
        tvDeletedTasksLabel = deletedTasksLayout.getChildAt(0) as TextView
    }

    private fun setupListeners() {
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
                R.id.nav_profile -> true
                else -> false
            }
        }

        findViewById<ImageView>(R.id.ivSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<TextView>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        findViewById<ImageView>(R.id.ivCamera).setOnClickListener {
            cameraLauncher.launch(Intent(this, CameraActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.CompletedTasks).setOnClickListener {
            startActivity(Intent(this, CompletedTasksActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.MissedTasks).setOnClickListener {
            startActivity(Intent(this, MissedTasksActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.DeletedTasks).setOnClickListener {
            startActivity(Intent(this, DeletedTasksActivity::class.java))
        }
    }

    private fun setupViewPager() {
        val adapter = ProductivityStatsAdapter(this)
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Daily"
                1 -> "Weekly"
                2 -> "Monthly"
                else -> null
            }
        }.attach()
    }

    // --- FUNGSI YANG DIPERBAIKI TOTAL ---
    private fun updateTaskCounts() {
        lifecycleScope.launch {
            try {
                // Panggil fungsi suspend dari TaskRepository
                val completedCount = TaskRepository.getTasksByStatus("completed").size
                val missedCount = TaskRepository.getTasksByStatus("missed").size
                val deletedCount = TaskRepository.getTasksByStatus("deleted").size

                // Update UI dengan hasil dari Firestore
                tvCompletedTasksLabel.text = "Completed Tasks ($completedCount)"
                tvMissedTasksLabel.text = "Missed Tasks ($missedCount)"
                tvDeletedTasksLabel.text = "Deleted Tasks ($deletedCount)"

            } catch (e: Exception) {
                Log.e("ProfileActivity", "Gagal mengupdate jumlah tugas: ${e.message}")
                Toast.makeText(this@ProfileActivity, "Gagal memuat statistik", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfileData() {
        val username = sharedPrefs.getString(EditProfileActivity.KEY_USERNAME, "Username")
        val imageUriString = sharedPrefs.getString(EditProfileActivity.KEY_IMAGE_URI, null)
        tvUsername.text = username
        if (imageUriString != null) {
            try {
                ivProfilePicture.setImageURI(Uri.parse(imageUriString))
            } catch (e: Exception) {
                ivProfilePicture.setImageResource(R.drawable.ic_profile)
            }
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_profile)
        }
    }

    private fun setProfileImage(uri: Uri) {
        ivProfilePicture.setImageURI(uri)
    }

    private fun saveProfileImageUri(uri: Uri) {
        sharedPrefs.edit().putString(EditProfileActivity.KEY_IMAGE_URI, uri.toString()).apply()
        // Mengambil izin persisten agar URI bisa diakses nanti
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            Log.w("ProfileActivity", "Gagal mengambil izin persisten untuk URI: ${e.message}")
        }
    }
}