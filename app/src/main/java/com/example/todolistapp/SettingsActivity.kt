package com.example.todolistapp

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings) // Pastikan file layout bernama settings.xml

        // Cari ImageView panah kiri
        val backArrow = findViewById<ImageView>(R.id.ivBackArrow)
        backArrow.setOnClickListener {
            finish() // langsung kembali ke ProfileActivity sebelumnya
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
