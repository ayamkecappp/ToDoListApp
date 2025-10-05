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
import com.google.android.material.tabs.TabLayoutMediator // Import ini penting
import androidx.fragment.app.FragmentActivity // Digunakan oleh Adapter

class ProfileActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2


    private lateinit var tvUsername: TextView
    private lateinit var ivProfilePicture: CircleImageView
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var bottomNav: BottomNavigationView

    // DEKLARASI untuk label hitungan tugas
    private lateinit var tvCompletedTasksLabel: TextView
    private lateinit var tvMissedTasksLabel: TextView
    private lateinit var tvDeletedTasksLabel: TextView

    // Activity Result Launcher untuk CameraActivity
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra("PROFILE_PHOTO_URI")
            if (uriString != null) {
                val imageUri = Uri.parse(uriString)
                setProfileImage(imageUri) // Tampilkan foto yang baru diambil
                saveProfileImageUri(imageUri) // Simpan URI ke SharedPrefs
            } else {
                Toast.makeText(this, "Gagal mendapatkan foto profil.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        // Inisialisasi SharedPreferences (Harus Paling Awal)
        sharedPrefs = getSharedPreferences(EditProfileActivity.PREFS_NAME, Context.MODE_PRIVATE)

        // ===============================================
        // 1. INISIALISASI SEMUA VIEW PENTING TERLEBIH DAHULU
        // ===============================================

        // Hubungkan Views untuk Tab dan Pager
        tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        viewPager = findViewById<ViewPager2>(R.id.viewPager)

        // Hubungkan Views
        tvUsername = findViewById(R.id.tvUsername)
        bottomNav = findViewById(R.id.bottomNav) // Inisialisasi BottomNav
        ivProfilePicture = findViewById(R.id.ivProfile)

        val ivSettings = findViewById<ImageView>(R.id.ivSettings)
        val btnEditProfile = findViewById<TextView>(R.id.btnEditProfile)
        val CompletedTasks = findViewById<LinearLayout>(R.id.CompletedTasks)
        val MissedTasks = findViewById<LinearLayout>(R.id.MissedTasks)
        val DeletedTasks = findViewById<LinearLayout>(R.id.DeletedTasks)
        val ivCamera = findViewById<ImageView>(R.id.ivCamera) // Ikon Kamera

        // INISIALISASI untuk label hitungan tugas (mengambil TextView pertama di dalam LinearLayout)
        tvCompletedTasksLabel = CompletedTasks.getChildAt(0) as TextView
        tvMissedTasksLabel = MissedTasks.getChildAt(0) as TextView
        tvDeletedTasksLabel = DeletedTasks.getChildAt(0) as TextView

        // Hapus tint default pada ikon
        bottomNav.itemIconTintList = null

        // Hapus tint default pada ikon
        bottomNav.itemIconTintList = null


        // ===============================================
        // 2. LOGIKA VIEWPAGER (Sekarang viewPager sudah aman digunakan)
        // ===============================================

        // 1. Setup Adapter untuk ViewPager2
        val adapter = ProductivityStatsAdapter(this)
        viewPager.adapter = adapter


        // 2. Sinkronisasi TabLayout dan ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Daily"
                1 -> tab.text = "Weekly"
                2 -> tab.text = "Monthly"
            }
        }.attach()

        // KUNCI 3: Listener Tab Instan
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.setCurrentItem(tab.position, false)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // PENTING: Set item yang dipilih ke Daily (Indeks 0)
        viewPager.setCurrentItem(0, false)

        // Listener navigasi navbar (Mempertahankan logika navigasi)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home-> {
                    startActivity(
                        Intent(
                            this,
                            HomeActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) // FLAG NO ANIMATION
                    )
                    finish() // Tutup Activity saat ini
                    // MEMAKSA TRANSISI INSTAN (Hard Cut)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_tasks-> {
                    startActivity(
                        Intent(
                            this,
                            TaskActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) // FLAG NO ANIMATION
                    )
                    finish() // Tutup Activity saat ini
                    // MEMAKSA TRANSISI INSTAN (Hard Cut)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> true // Sudah di ProfileActivity
                else -> false
            }
        }

        // Atur item yang dipilih saat Activity dibuat
        bottomNav.selectedItemId = R.id.nav_profile

        // Listener untuk Settings
        ivSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Klik Edit Profile
        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Listener untuk ikon kamera: Meluncurkan CameraActivity menggunakan Launcher
        ivCamera.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            cameraLauncher.launch(intent)
        }

        // Listener untuk daftar tugas di bawah
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

        // Muat data saat onCreate
        loadProfileData()
        updateTaskCounts()
    }

    override fun onStart() {
        super.onStart()
        // Memastikan ikon Profile ditandai sebagai aktif setiap kali Activity menjadi terlihat
        bottomNav.selectedItemId = R.id.nav_profile
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
        updateTaskCounts()
        // PERBAIKAN: Hapus overridePendingTransition di onResume untuk menghilangkan slide-in saat kembali
    }

    // FUNGSI untuk memperbarui hitungan tugas
    private fun updateTaskCounts() {
        // PENTING: Panggil processTasksForMissed untuk memastikan daftar missed terupdate
        TaskRepository.processTasksForMissed()

        val completedCount = TaskRepository.getCompletedTasks().size
        val missedCount = TaskRepository.getMissedTasks().size
        val deletedCount = TaskRepository.getDeletedTasks().size

        tvCompletedTasksLabel.text = "Completed Tasks ($completedCount)"
        tvMissedTasksLabel.text = "Missed Tasks ($missedCount)"
        tvDeletedTasksLabel.text = "Deleted Tasks ($deletedCount)"
    }


    private fun loadProfileData() {
        // Mengambil KEY_USERNAME untuk ditampilkan di tvUsername
        val username = sharedPrefs.getString(EditProfileActivity.KEY_USERNAME, "Username")
        val imageUriString = sharedPrefs.getString(EditProfileActivity.KEY_IMAGE_URI, null)

        // Menampilkan USERNAME di field tvUsername
        tvUsername.text = username

        // Muat gambar jika URI ada
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            try {
                // Coba tampilkan, jika gagal akan masuk ke catch.
                ivProfilePicture.setImageURI(imageUri)
            } catch (e: Exception) {
                // Jika URI gagal dimuat (misalnya, file cache terhapus), gunakan placeholder.
                Log.e("ProfileActivity", "Gagal memuat foto dari URI: ${e.message}")
                ivProfilePicture.setImageResource(R.drawable.ic_profile)
                // Hapus URI yang rusak dari SharedPreferences
                sharedPrefs.edit().remove(EditProfileActivity.KEY_IMAGE_URI).apply()
            }
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_profile)
        }
    }

    /**
     * Memperbarui ImageView profil secara langsung.
     */
    private fun setProfileImage(uri: Uri) {
        try {
            ivProfilePicture.setImageURI(uri)
            Toast.makeText(this, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error menampilkan foto: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("ProfileActivity", "Error setting image URI: ${e.message}")
        }
    }

    /**
     * Menyimpan URI gambar ke SharedPreferences agar persisten setelah Activity ditutup.
     */
    private fun saveProfileImageUri(uri: Uri) {
        sharedPrefs.edit().apply {
            putString(EditProfileActivity.KEY_IMAGE_URI, uri.toString())
            apply()
        }
        // Mengambil izin persisten agar URI foto dari kamera tetap dapat dibaca
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
        // PERBAIKAN: Hapus overridePendingTransition di finish untuk menghilangkan slide-out saat keluar
    }
}