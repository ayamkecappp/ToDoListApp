package com.example.todolistapp

import android.os.Bundle
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        // Tombol "Back to login"
        val backLogin = findViewById<TextView>(R.id.tvBackLogin)
        backLogin.setOnClickListener {
            finish() // menutup SignUpActivity, kembali ke LoginActivity
        }

        // Tombol "Sign Up"
        val btnSignup = findViewById<MaterialButton>(R.id.btn_signup)
        btnSignup.setOnClickListener {
            // Bisa tambah validasi input di sini sebelum kembali ke login
            finish() // kembali ke LoginActivity setelah signup
        }
    }
}
