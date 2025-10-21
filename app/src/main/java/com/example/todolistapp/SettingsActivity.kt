package com.example.todolistapp

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import com.bumptech.glide.Glide

class SettingsActivity : AppCompatActivity() {

    // Mendefinisikan transisi slide
    private val FORWARD_TRANSITION_IN = R.anim.slide_in_right
    private val FORWARD_TRANSITION_OUT = R.anim.slide_out_left
    private val BACK_TRANSITION_IN = R.anim.slide_in_left
    private val BACK_TRANSITION_OUT = R.anim.slide_out_right

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Konfigurasi Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // --- PENGATURAN ONCLICK LISTENER ---

        // 1. Tombol Kembali
        findViewById<ImageView>(R.id.ivBackArrow).setOnClickListener {
            finish()
        }

        // Streak Badge
        findViewById<View>(R.id.rectangleSettings1).setOnClickListener {
            startActivityWithTransition(StreakBadgeActivity::class.java)
        }

        // Log Out -> Menampilkan Custom Dialog Konfirmasi
        findViewById<View>(R.id.rectangleSettings6).setOnClickListener {
            showCustomLogoutDialog()
        }
        // Anda juga bisa memasang listener di TextView jika perlu
        findViewById<TextView>(R.id.tvLogOut).setOnClickListener {
            showCustomLogoutDialog()
        }

        // Delete Account (Placeholder)
        findViewById<View>(R.id.rectangleSettings7).setOnClickListener {
            Toast.makeText(this, "Fungsionalitas Hapus Akun dipicu.", Toast.LENGTH_SHORT).show()
        }

        // Report a Bug
        findViewById<View>(R.id.rectangleSettings8).setOnClickListener {
            startActivityWithTransition(ReportBugActivity::class.java)
        }

        // Send Feedback
        findViewById<View>(R.id.rectangleSettings9).setOnClickListener {
            startActivityWithTransition(SendFeedbackActivity::class.java)
        }

        // About Us
        findViewById<View>(R.id.rectangleSettings10).setOnClickListener {
            startActivityWithTransition(AboutUsActivity::class.java)
        }

        // Privacy Policy
        findViewById<View>(R.id.rectangleSettings11).setOnClickListener {
            startActivityWithTransition(PrivacyPolicyActivity::class.java)
        }
    }

    /**
     * Menampilkan custom dialog konfirmasi Logout.
     */
    private fun showCustomLogoutDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_save_success)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessageTitle)
        val btnYes = dialog.findViewById<TextView>(R.id.btnView)
        val btnNo = dialog.findViewById<TextView>(R.id.btnIgnore)

        tvMessage?.text = "Apakah Anda yakin ingin keluar dari akun?"
        tvMessage?.setTextColor(resources.getColor(R.color.dark_blue, theme))

        btnYes?.text = "Ya"
        btnNo?.text = "Tidak"
        btnYes?.setTextColor(resources.getColor(R.color.dark_blue, theme))
        btnNo?.setTextColor(resources.getColor(R.color.dark_blue, theme))

        btnYes?.setOnClickListener {
            dialog.dismiss()
            performLogout() // Panggil fungsi logout yang sudah benar
        }

        btnNo?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Menjalankan proses logout dari semua layanan dan kembali ke LoginActivity.
     */
    private fun performLogout() {
        // 1. Logout dari Firebase
        auth.signOut()

        // 2. Logout dari Google dan Facebook
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Logout dari Facebook (dijalankan setelah Google selesai)
            LoginManager.getInstance().logOut()

            // 3. BERSIHKAN SharedPreferences PROFIL <-- TAMBAHAN
            val profilePrefs = getSharedPreferences(EditProfileActivity.PREFS_NAME, Context.MODE_PRIVATE)
            profilePrefs.edit().clear().apply() // Hapus semua data di ProfilePrefs

            // 4. Arahkan kembali ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            Glide.get(applicationContext).clearMemory() // Hapus cache memori
            Thread {
                Glide.get(applicationContext).clearDiskCache() // Hapus cache disk (di background thread)
            }.start()
            startActivity(intent)
            finish() // Tutup SettingsActivity
        }
    }

    // Fungsi bantuan untuk memulai Activity dengan transisi
    private fun <T> startActivityWithTransition(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        overridePendingTransition(FORWARD_TRANSITION_IN, FORWARD_TRANSITION_OUT)
    }

    override fun finish() {
        super.finish()
        // Menerapkan transisi mundur saat Activity ditutup
        overridePendingTransition(BACK_TRANSITION_IN, BACK_TRANSITION_OUT)
    }
}