package com.example.todolistapp // Pastikan ini sesuai dengan nama package Anda

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NewPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pastikan nama file layout New Password Anda benar
        setContentView(R.layout.new_password)

        // Dapatkan referensi ke View
        // Perhatikan ID di layout Anda (newpasswordxml)
        val newPasswordInput = findViewById<EditText>(R.id.input_username) // Di layout Anda, ID ini dipakai untuk input pertama
        val confirmPasswordInput = findViewById<EditText>(R.id.input_password) // ID ini dipakai untuk input kedua
        val saveBtn = findViewById<Button>(R.id.btn_signup) // Di layout Anda, ID ini dipakai untuk tombol "Verify"
        val backToLoginText = findViewById<TextView>(R.id.tvBackLogin)

        // Ubah properti View agar lebih sesuai dengan "New Password"
        newPasswordInput.hint = "New Password" // Mengganti Hint dari XML
        newPasswordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD // Mengganti input type
        saveBtn.text = "Save New Password" // Mengganti teks tombol

        // Listener untuk Tombol "Save New Password"
        saveBtn.setOnClickListener {
            val newPass = newPasswordInput.text.toString().trim()
            val confirmPass = confirmPasswordInput.text.toString().trim()

            if (newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill in both password fields.", Toast.LENGTH_SHORT).show()
            } else if (newPass != confirmPass) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            } else {
                // Logika ganti password berhasil
                Toast.makeText(this, "Password updated successfully! Please log in.", Toast.LENGTH_LONG).show()

                // Arahkan kembali ke Login Activity
                val intent = Intent(this, LoginActivity::class.java)
                // Hapus semua Activity lain di atas Login Activity (agar tidak bisa "Back" ke NewPassword)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        // Listener untuk Teks "Back to login"
        backToLoginText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP // Kembali ke Login yang sudah ada
            startActivity(intent)
            finish()
        }
    }
}