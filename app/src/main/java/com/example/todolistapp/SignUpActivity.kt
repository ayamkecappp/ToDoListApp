package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Menggunakan ID yang sesuai dengan signup.xml Anda
        val emailEditText = findViewById<EditText>(R.id.input_username)
        val passwordEditText = findViewById<EditText>(R.id.input_password)
        val confirmPasswordEditText = findViewById<EditText>(R.id.input_confirm_password)
        val registerButton = findViewById<Button>(R.id.btn_signup) // ID diperbaiki dari btn_register
        val loginText = findViewById<TextView>(R.id.tvBackLogin) // ID diperbaiki dari login

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (!InputValidator.isValidEmail(email)) {
                emailEditText.error = "Format email tidak valid"
                return@setOnClickListener
            }

            if (!InputValidator.isValidPassword(password)) {
                passwordEditText.error = "Password minimal 6 karakter dan tanpa spasi"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                confirmPasswordEditText.error = "Password tidak cocok"
                return@setOnClickListener
            }

            // Buat pengguna baru dengan Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("SignUpActivity", "createUserWithEmail:success")
                        Toast.makeText(this, "Registration successful.", Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    } else {
                        Log.w("SignUpActivity", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        loginText.setOnClickListener {
            // Kembali ke halaman login
            finish()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

