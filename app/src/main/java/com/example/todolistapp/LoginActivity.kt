package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInHandler: GoogleSignInHandler
    private lateinit var facebookLoginHandler: FacebookLoginHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        auth = FirebaseAuth.getInstance()

        // Cek jika pengguna sudah login, langsung arahkan ke HomeActivity
        if (auth.currentUser != null) {
            navigateToHome()
            return // Hentikan eksekusi onCreate lebih lanjut
        }

        googleSignInHandler = GoogleSignInHandler(this, auth)
        facebookLoginHandler = FacebookLoginHandler(this, auth)

        // Menggunakan ID yang sesuai dengan login.xml Anda
        val emailEditText = findViewById<EditText>(R.id.input_username)
        val passwordEditText = findViewById<EditText>(R.id.input_password)
        val loginButton = findViewById<Button>(R.id.btn_login)
        val signUpText = findViewById<TextView>(R.id.signup)
        val googleLoginButton = findViewById<ImageButton>(R.id.btn_google)
        val facebookLoginButton = findViewById<ImageButton>(R.id.btn_facebook)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan kata sandi tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("LoginActivity", "signInWithEmail:success")
                        navigateToHome()
                    } else {
                        Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Autentikasi gagal.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        signUpText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        googleLoginButton.setOnClickListener {
            googleSignInHandler.signIn()
        }

        facebookLoginButton.setOnClickListener {
            facebookLoginHandler.signIn()
        }
    }

    // Blok onActivityResult sudah dihapus karena tidak lagi diperlukan

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        // Membersihkan stack aktivitas sehingga pengguna tidak bisa kembali ke halaman login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

