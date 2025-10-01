package com.example.todolistapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SendFeedbackActivity : AppCompatActivity() {

    private lateinit var stars: List<ImageView>
    private var currentRating: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_feedback)

        val ivBackArrow = findViewById<ImageView>(R.id.ivBackArrow)
        val btnSendFeedback = findViewById<Button>(R.id.btnSendFeedback)

        // Inisialisasi ImageView bintang
        stars = listOf(
            findViewById(R.id.ivStar1),
            findViewById(R.id.ivStar2),
            findViewById(R.id.ivStar3),
            findViewById(R.id.ivStar4),
            findViewById(R.id.ivStar5)
        )

        // Menetapkan listener klik untuk setiap bintang
        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                setRating(index + 1)
            }
        }

        ivBackArrow.setOnClickListener {
            finish() // Kembali ke Activity sebelumnya (SettingsActivity)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnSendFeedback.setOnClickListener {
            // Implementasi logika kirim feedback di sini
            Toast.makeText(this, "Feedback sent successfully! Rating: $currentRating/5", Toast.LENGTH_LONG).show()
            // Kembali ke Settings setelah kirim
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Set kondisi awal: 0 bintang (semua abu-abu)
        setRating(0)
    }

    /**
     * Mengatur pewarnaan bintang berdasarkan rating yang dipilih.
     * Bintang yang dipilih dan bintang sebelumnya akan berwarna kuning (@color/medium_priority).
     */
    private fun setRating(rating: Int) {
        currentRating = rating
        val selectedColor = ContextCompat.getColor(this, R.color.medium_priority) // Yellow
        val unselectedColor = ContextCompat.getColor(this, R.color.gray) // Dark Grey

        stars.forEachIndexed { index, star ->
            if (index < rating) {
                star.setColorFilter(selectedColor)
            } else {
                star.setColorFilter(unselectedColor)
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}