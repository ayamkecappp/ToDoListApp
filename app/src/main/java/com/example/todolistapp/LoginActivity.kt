package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
// Tambahkan import untuk Intent Flags
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInHandler: GoogleSignInHandler
    private lateinit var facebookLoginHandler: FacebookLoginHandler

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Teruskan hasil login ke FacebookLoginHandler
        facebookLoginHandler.onActivityResult(requestCode, resultCode, data)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        auth = FirebaseAuth.getInstance()

        // Cek jika pengguna sudah login, langsung arahkan ke HomeActivity
        if (auth.currentUser != null) {
            navigateToHome(false) // Navigasi tanpa animasi jika sudah login
            return
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
        val greetingText = findViewById<TextView>(R.id.greeting)
        val forgotPasswordText = findViewById<TextView>(R.id.forgot_password)
        val avatarImage = findViewById<ImageView>(R.id.avatar) // Ditambahkan

        // --- PERBAIKAN UNTUK TEKS SIGN UP DENGAN HTML ---
        val htmlText = getString(R.string.signup_prompt)
        signUpText.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)

        // ==============================================
        // 🔑 LOGIKA ANIMASI STARTUP 🔑
        // ==============================================
        val viewsToAnimate = listOf(avatarImage, greetingText, emailEditText, passwordEditText, forgotPasswordText, loginButton, signUpText, googleLoginButton, facebookLoginButton)
        for ((index, view) in viewsToAnimate.withIndex()) {
            view.alpha = 0f
            view.translationY = 50f // Mulai dari posisi sedikit di bawah
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(100L * index) // Setiap elemen muncul berurutan
                .setDuration(500)
                .setInterpolator(null)
                .start()
        }



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

        // --- FUNGSI UNTUK LUPA KATA SANDI ---
        forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        signUpText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            // Terapkan animasi transisi saat menuju halaman sign up
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        googleLoginButton.setOnClickListener {
            googleSignInHandler.signIn()
        }

        facebookLoginButton.setOnClickListener {
            facebookLoginHandler.signIn()
        }

        // Menangani tombol kembali untuk keluar dari aplikasi
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun navigateToHome(useAnimation: Boolean = true) {
        val intent = Intent(this, HomeActivity::class.java)
        // Membersihkan stack aktivitas sehingga pengguna tidak bisa kembali ke halaman login
        intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        if (useAnimation) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        finish()
    }
}