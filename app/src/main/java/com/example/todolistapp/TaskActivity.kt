package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Import View untuk menemukan RelativeLayout reminderContainer
import android.widget.ImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity

class TaskActivity : AppCompatActivity() {

    private val TAG = "TaskActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task)

        // Temukan tombol "New Reminder" (reminderContainer adalah RelativeLayout)
        val btnNewReminder = findViewById<View>(R.id.reminderContainer)

        // Logika untuk New Reminder dengan animasi slide-in
        btnNewReminder?.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivity(intent)
            // Forward transition: Slide in from right, slide out to left
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }


        // Tombol kalender
        val btnCalendar = findViewById<ImageView?>(R.id.btn_calendar)
        btnCalendar?.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        } ?: Log.w(TAG, "btn_calendar tidak ditemukan di layout task.xml")

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView?>(R.id.bottomNav)
        if (bottomNav == null) {
            Log.w(TAG, "BottomNavigationView (id=bottomNav) tidak ditemukan di layout task.xml")
            return
        }

        // Pastikan item yang aktif adalah Task
        bottomNav.selectedItemId = R.id.nav_tasks

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Navigasi yang mulus (REORDER_TO_FRONT)
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    true
                }
                R.id.nav_tasks -> {
                    // Sudah di TaskActivity
                    true
                }
                R.id.nav_profile -> {
                    // Navigasi yang mulus (REORDER_TO_FRONT)
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Menimpa finish() untuk menambahkan transisi exit/kembali (slide-out ke kanan).
     */
    override fun finish() {
        super.finish()
        // Reverse transition: Activity sebelumnya masuk dari kiri, Activity ini keluar ke kanan
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}