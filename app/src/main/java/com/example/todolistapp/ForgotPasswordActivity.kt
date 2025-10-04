package com.example.todolistapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent // <-- BARIS KOREKSI

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pastikan nama file layout Anda benar (misal: forgot_password.xml)
        setContentView(R.layout.forgot_password)

        // Dapatkan referensi ke view dari layout Anda
        val emailInput = findViewById<EditText>(R.id.input_username)
        val verifyBtn = findViewById<Button>(R.id.btn_forgotpass)
        val backToLoginText = findViewById<TextView>(R.id.tvBackLogin)


        // Atur listener untuk tombol "Verify"
        verifyBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
            } else {
                // 1. Ganti simulasi pesan
                Toast.makeText(this, "Verification successful. Please set your new password.", Toast.LENGTH_LONG).show()

                // 2. PINDAH KE NEWPASSWORD ACTIVITY
                val intent = Intent(this, NewPasswordActivity::class.java)
                // (Opsional) Kirim data email ke activity berikutnya
                intent.putExtra("USER_EMAIL", email)
                startActivity(intent)

                // 3. JANGAN finish(), biarkan ForgotPasswordActivity di stack
                // finish() // Baris ini dihapus/dinonaktifkan
            }
        }

        // Atur listener untuk teks "Back to login"
        backToLoginText.setOnClickListener {
            finish() // Cukup tutup activity ini untuk kembali
        }
    }
}