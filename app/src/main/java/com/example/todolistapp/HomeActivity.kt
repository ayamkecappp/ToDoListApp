package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvSpeech: TextView
    private lateinit var tvStreak: TextView
    private lateinit var progressStreak: ProgressBar
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home) // pakai file home.xml

        // Hubungkan ID dari XML ke variabel
        tvTitle = findViewById(R.id.tvTitle)
        tvSpeech = findViewById(R.id.tvSpeech)
        tvStreak = findViewById(R.id.tvStreak)
        progressStreak = findViewById(R.id.progressStreak)
        bottomNav = findViewById(R.id.bottomNav)


        // Contoh update progress streak
        progressStreak.progress = 90
        tvStreak.text = "131" // contoh update daily streak

        // Setup bottom navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Sudah di Home
                    true
                }
                R.id.nav_task-> {
                    val intent = Intent(this, TaskActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {  // <-- ini untuk profile
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }


    }
}
