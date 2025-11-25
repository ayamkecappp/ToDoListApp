package com.example.todolistapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth // <-- TAMBAHKAN IMPORT INI

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth // <-- TAMBAHKAN INI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password)

        auth = FirebaseAuth.getInstance() // <-- TAMBAHKAN INISIALISASI

        val emailInput = findViewById<EditText>(R.id.input_username)
        val verifyBtn = findViewById<Button>(R.id.btn_forgotpass)
        val backToLoginText = findViewById<TextView>(R.id.tvBackLogin)


        // Atur listener untuk tombol "Verify"
        verifyBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
            } else {
                // KIRIM EMAIL RESET KATA SANDI DARI FIREBASE
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Berhasil mengirim email
                            Toast.makeText(this, "Password reset email has been sent. Please check your email.", Toast.LENGTH_LONG).show()

                            // Kembali ke halaman Login setelah email terkirim
                            finish()
                        } else {
                            // Gagal mengirim email
                            Toast.makeText(this, "Failed to send email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }

                // HAPUS LOGIKA LAMA (PINDAH KE NEWPASSWORD ACTIVITY)
                // Toast.makeText(this, "Verification successful. Please set your new password.", Toast.LENGTH_LONG).show()
                // val intent = Intent(this, NewPasswordActivity::class.java)
                // intent.putExtra("USER_EMAIL", email)
                // startActivity(intent)
            }
        }

        // Atur listener untuk teks "Back to login"
        backToLoginText.setOnClickListener {
            finish() // Cukup tutup activity ini untuk kembali
        }
    }
}