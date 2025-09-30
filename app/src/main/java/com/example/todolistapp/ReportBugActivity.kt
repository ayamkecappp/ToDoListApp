package com.example.todolistapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ReportBugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_bug)

        val ivBackArrow = findViewById<ImageView>(R.id.ivBackArrow)
        val btnSubmitBug = findViewById<Button>(R.id.btnSubmitBug)

        ivBackArrow.setOnClickListener {
            finish() // Kembali ke Activity sebelumnya (SettingsActivity)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnSubmitBug.setOnClickListener {
            // Logika validasi dan pengiriman bug report di sini
            Toast.makeText(this, "Bug Report submitted. Thank you!", Toast.LENGTH_LONG).show()

            // Kembali ke Settings setelah kirim
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}