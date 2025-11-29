package com.example.todolistapp

import android.util.Patterns
import java.util.regex.Pattern

object InputValidator {

    // Validasi Email yang ketat
    fun isValidEmail(email: String): Boolean {
        return if (email.isBlank()) false else Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Validasi Password (Min 6 char, harus ada huruf dan angka - Opsional tapi disarankan)
    fun isValidPassword(password: String): Boolean {
        // Contoh: Minimal 6 karakter, tidak boleh spasi
        val passwordPattern = Pattern.compile("^[^\\s]{6,}\$")
        return password.isNotEmpty() && passwordPattern.matcher(password).matches()
    }

    // Sanitasi Input Teks (Untuk Judul Task, Detail, Lokasi)
    // Mencegah karakter kontrol berbahaya atau script tag sederhana
    fun sanitizeText(input: String): String {
        var clean = input.trim()
        // Menghapus tag HTML dasar jika ada yang mencoba memasukkan script
        clean = clean.replace(Regex("<.*?>"), "")
        // Membatasi panjang karakter agar tidak spam database (misal max 500 char)
        return if (clean.length > 500) clean.substring(0, 500) else clean
    }

    // Validasi Judul Task (Tidak boleh kosong, max 100 char)
    fun isValidTaskTitle(title: String): Boolean {
        val cleanTitle = sanitizeText(title)
        return cleanTitle.isNotEmpty() && cleanTitle.length <= 100
    }

    fun isValidUsername(username: String): Boolean {
        val pattern = Pattern.compile("^[a-zA-Z0-9._]+$")
        return username.isNotEmpty() && pattern.matcher(username).matches() && username.length in 3..20
    }
}