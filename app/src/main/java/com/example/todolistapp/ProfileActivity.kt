package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        // Ambil BottomNavigationView
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Set item default yang dipilih Profile
        bottomNav.selectedItemId = R.id.nav_profile

        // Listener navigasi navbar
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    true
                }
                R.id.nav_tasks -> {
                    val intent = Intent(this, TaskActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }

        // Klik icon settings
        val ivSettings = findViewById<ImageView>(R.id.ivSettings)
        ivSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        //Cam
        val ivCamera = findViewById<ImageView>(R.id.ivCamera)
        ivCamera.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        // Klik Completed Tasks
        val CompletedTasks = findViewById<LinearLayout>(R.id.CompletedTasks)
        CompletedTasks.setOnClickListener {
            val intent = Intent(this, CompletedTasksActivity::class.java)
            startActivity(intent)
        }

        // Klik Missed Tasks
        val MissedTasks = findViewById<LinearLayout>(R.id.MissedTasks)
        MissedTasks.setOnClickListener {
            val intent = Intent(this, MissedTasksActivity::class.java)
            startActivity(intent)
        }

        // Klik Deleted Tasks
        val DeletedTasks = findViewById<LinearLayout>(R.id.DeletedTasks)
        DeletedTasks.setOnClickListener {
            val intent = Intent(this, DeletedTasksActivity::class.java)
            startActivity(intent)
        }
    }
}
