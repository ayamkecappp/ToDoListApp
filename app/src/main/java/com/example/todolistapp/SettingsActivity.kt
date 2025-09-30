// main/java/com/example/todolistapp/SettingsActivity.kt (Modified)
package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        // Cari ImageView panah kiri
        val backArrow = findViewById<ImageView>(R.id.ivBackArrow)
        backArrow.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // --- LOGIKA UNTUK STREAK BADGE (Baru) ---
        val tvStreakBadge = findViewById<TextView>(R.id.tvStreakBadge)
        tvStreakBadge.setOnClickListener {
            val intent = Intent(this, StreakBadgeActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        // --- END LOGIKA STREAK BADGE ---

        // Logika REPORT A BUG yang sudah ada
        val tvReportaBug = findViewById<TextView>(R.id.tvReportaBug)
        tvReportaBug.setOnClickListener {
            val intent = Intent(this, ReportBugActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Logika SEND FEEDBACK yang sudah ada
        val tvSendFeedback = findViewById<TextView>(R.id.tvSendFeedback)
        tvSendFeedback.setOnClickListener {
            val intent = Intent(this, SendFeedbackActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Logika ABOUT US yang sudah ada
        val tvAboutUs = findViewById<TextView>(R.id.tvAboutUs)
        tvAboutUs.setOnClickListener {
            val intent = Intent(this, AboutUsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Logika Privacy Policy yang sudah ada
        val tvPrivacyPolicy = findViewById<TextView>(R.id.tvPrivacyPolicy)
        tvPrivacyPolicy.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}