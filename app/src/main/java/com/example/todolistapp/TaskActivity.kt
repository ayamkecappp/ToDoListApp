package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class TaskActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task)

        // Ambil referensi tombol kalender
        val btnCalendar = findViewById<ImageView>(R.id.btn_calendar)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Listener tombol kalender
        btnCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        // Tandai nav_task sebagai aktif
        bottomNav.selectedItemId = R.id.nav_menu

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    true
                }
                R.id.nav_menu -> {
                    // Sudah di TaskActivity, tidak perlu action
                    true
                }
                else -> false
            }
        }
    }
}
