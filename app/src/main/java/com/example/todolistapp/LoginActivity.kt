package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// Import jika diperlukan
import android.text.Html

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login) // pakai layout login.xml

        val usernameInput = findViewById<EditText>(R.id.input_username)
        val passwordInput = findViewById<EditText>(R.id.input_password)
        val loginBtn = findViewById<Button>(R.id.btn_login)
        // --- TAMBAHKAN KODE INI ---
        val signupText = findViewById<TextView>(R.id.signup)
        val htmlText = getString(R.string.signup_prompt)
        signupText.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)


        // --- TAMBAHAN BARU UNTUK FORGOT PASSWORD ---
        val forgotPasswordText = findViewById<TextView>(R.id.forgot_password)


        forgotPasswordText.setOnClickListener {
            // Arahkan ke halaman Forgot Password
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        //untuk signup
        signupText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Klik tombol Login
        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                // Login sukses → pindah ke HomeActivity
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish() // supaya tidak bisa balik ke login dengan back
            }
        }

        // Klik Sign Up (contoh navigasi ke SignUpActivity jika ada)
        signupText.setOnClickListener {
            // Navigasi ke halaman Sign Up
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}
