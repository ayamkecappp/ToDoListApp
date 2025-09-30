package com.example.todolistapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SendFeedbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_feedback)

        val ivBackArrow = findViewById<ImageView>(R.id.ivBackArrow)
        val btnSendFeedback = findViewById<Button>(R.id.btnSendFeedback)

        ivBackArrow.setOnClickListener {
            finish() // Kembali ke Activity sebelumnya (SettingsActivity)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnSendFeedback.setOnClickListener {
            // Implementasi logika kirim feedback di sini
            Toast.makeText(this, "Feedback sent successfully!", Toast.LENGTH_SHORT).show()
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