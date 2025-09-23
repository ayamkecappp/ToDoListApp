package com.example.todolistapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile) // pastikan ini nama layout XML kamu

        // Ambil BottomNavigationView
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Set item default yang dipilih (menggantikan atribut XML yang error)
        bottomNav.selectedItemId = R.id.nav_profile
    }
}
