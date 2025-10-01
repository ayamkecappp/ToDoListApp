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

class SettingsActivity : AppCompatActivity() {

    // Mendefinisikan transisi slide
    private val FORWARD_TRANSITION_IN = R.anim.slide_in_right
    private val FORWARD_TRANSITION_OUT = R.anim.slide_out_left
    private val BACK_TRANSITION_IN = R.anim.slide_in_left
    private val BACK_TRANSITION_OUT = R.anim.slide_out_right

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        // 1. Tombol Kembali
        val backArrow = findViewById<ImageView>(R.id.ivBackArrow)
        backArrow.setOnClickListener {
            finish()
        }

        // --- SECTION GENERAL ---

        // Streak Badge -> StreakBadgeActivity (rectangleSettings1)
        val rectStreakBadge = findViewById<View>(R.id.rectangleSettings1)
        rectStreakBadge.setOnClickListener {
            val intent = Intent(this, StreakBadgeActivity::class.java)
            startActivity(intent)
            overridePendingTransition(FORWARD_TRANSITION_IN, FORWARD_TRANSITION_OUT)
        }

        // --- SECTION ACCOUNT ---

        // Log Out -> Menampilkan Custom Dialog Konfirmasi (rectangleSettings6)
        val rectLogOut = findViewById<View>(R.id.rectangleSettings6)
        rectLogOut.setOnClickListener {
            showCustomLogoutDialog()
        }

        // Delete Account (Placeholder Toast) (rectangleSettings7)
        val rectDeleteAccount = findViewById<View>(R.id.rectangleSettings7)
        rectDeleteAccount.setOnClickListener {
            Toast.makeText(this, "Fungsionalitas Hapus Akun dipicu.", Toast.LENGTH_SHORT).show()
        }

        // --- SECTION SUPPORT ---

        // Report a Bug -> ReportBugActivity (rectangleSettings8)
        val rectReportBug = findViewById<View>(R.id.rectangleSettings8)
        rectReportBug.setOnClickListener {
            val intent = Intent(this, ReportBugActivity::class.java)
            startActivity(intent)
            overridePendingTransition(FORWARD_TRANSITION_IN, FORWARD_TRANSITION_OUT)
        }

        // Send Feedback -> SendFeedbackActivity (rectangleSettings9)
        val rectSendFeedback = findViewById<View>(R.id.rectangleSettings9)
        rectSendFeedback.setOnClickListener {
            // Memastikan navigasi ke SendFeedbackActivity
            val intent = Intent(this, SendFeedbackActivity::class.java)
            startActivity(intent)
            overridePendingTransition(FORWARD_TRANSITION_IN, FORWARD_TRANSITION_OUT)
        }

        // --- SECTION MORE ---

        // About Us -> AboutUsActivity (rectangleSettings10)
        val rectAboutUs = findViewById<View>(R.id.rectangleSettings10)
        rectAboutUs.setOnClickListener {
            val intent = Intent(this, AboutUsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(FORWARD_TRANSITION_IN, FORWARD_TRANSITION_OUT)
        }

        // Privacy Policy -> PrivacyPolicyActivity (rectangleSettings11)
        val rectPrivacyPolicy = findViewById<View>(R.id.rectangleSettings11)
        rectPrivacyPolicy.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
            overridePendingTransition(FORWARD_TRANSITION_IN, FORWARD_TRANSITION_OUT)
        }
    }

    /**
     * Menampilkan custom dialog konfirmasi Logout menggunakan ID yang benar dari dialog_save_success.xml.
     */
    private fun showCustomLogoutDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_save_success)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        // Menggunakan ID yang benar dari dialog_save_success.xml
        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessageTitle)
        val btnYes = dialog.findViewById<TextView>(R.id.btnView)
        val btnNo = dialog.findViewById<TextView>(R.id.btnIgnore)

        // Mengatur konten dialog untuk konfirmasi Logout
        tvMessage?.text = "Apakah Anda yakin ingin keluar dari akun?"
        tvMessage?.setTextColor(resources.getColor(R.color.dark_blue, theme))

        // Mengatur teks tombol
        btnYes?.text = "Ya"
        btnNo?.text = "Tidak"

        btnYes?.setTextColor(resources.getColor(R.color.dark_blue, theme))
        btnNo?.setTextColor(resources.getColor(R.color.dark_blue, theme))

        // Listener untuk tombol Ya (Logout)
        btnYes?.setOnClickListener {
            dialog.dismiss()
            performLogout()
        }

        // Listener untuk tombol Tidak (Batal)
        btnNo?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Menjalankan proses logout dan menavigasi ke LoginActivity.
     */
    private fun performLogout() {
        // Navigasi ke LoginActivity dan hapus semua Activity sebelumnya
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }


    override fun finish() {
        super.finish()
        // Menerapkan transisi mundur saat Activity ditutup
        overridePendingTransition(BACK_TRANSITION_IN, BACK_TRANSITION_OUT)
    }
}