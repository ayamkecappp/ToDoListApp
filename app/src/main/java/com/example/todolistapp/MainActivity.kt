package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.todolistapp.R

class MainActivity : AppCompatActivity() {

    lateinit var usernameInput: EditText
    lateinit var passwordInput: EditText
    lateinit var loginBtn: Button
    lateinit var signupText: TextView
    lateinit var facebookBtn: ImageButton
    lateinit var googleBtn: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usernameInput = findViewById(R.id.input_username)
        passwordInput = findViewById(R.id.input_password)
        loginBtn = findViewById(R.id.btn_login)
        signupText = findViewById(R.id.signup)
        facebookBtn = findViewById(R.id.btn_facebook)
        googleBtn = findViewById(R.id.btn_google)

        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Login Success!", Toast.LENGTH_SHORT).show()
            }
        }

        signupText.setOnClickListener {
            Toast.makeText(this, "Go to Signup Page", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, SignupActivity::class.java))
        }

        facebookBtn.setOnClickListener {
            Toast.makeText(this, "Facebook login clicked", Toast.LENGTH_SHORT).show()
        }

        googleBtn.setOnClickListener {
            Toast.makeText(this, "Google login clicked", Toast.LENGTH_SHORT).show()
        }
    }
}
