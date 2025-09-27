package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

// Catatan: Menghapus import com.example.todolistapp.databinding.ActivityMainBinding
// untuk mengatasi "Unresolved reference 'ActivityMainBinding'".

class MainActivity : AppCompatActivity() {

    // Menghapus deklarasi binding: private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Memuat R.layout.home karena layout ini sudah menyertakan R.id.bottomNav.
        setContentView(R.layout.home)
        // Mengatasi "Unresolved reference 'ActivityMainBinding'" & error terkait setContentView.

        // Menggunakan findViewById untuk menemukan BottomNavigationView
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        // Mengatasi "Unresolved reference 'bottomNav'".

        // Navigasi Bottom Bar
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Saat ini di Home (R.layout.home), tidak perlu navigasi
                    true
                }
                R.id.nav_tasks -> {
                    val intent = Intent(this, TaskActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Kode untuk tombol floating action button (FAB) — dihapus.
        // Karena ID 'fab' tidak ada di R.layout.home atau layout yang dimuat,
        // kode FAB dihapus untuk mengatasi "Unresolved reference 'fab'".
    }
}