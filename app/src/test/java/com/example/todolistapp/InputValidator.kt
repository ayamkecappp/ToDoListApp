package com.example.todolistapp

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Penting: Gunakan Robolectric agar bisa mentest 'android.util.Patterns' tanpa emulator
// Jika Anda tidak menggunakan Robolectric, test Email mungkin error "Method not mocked"
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class InputValidatorTest {

    // ==========================================
    // 1. TEST USERNAME
    // ==========================================
    @Test
    fun `username valid returns true`() {
        assertTrue(InputValidator.isValidUsername("timy.ganteng"))
        assertTrue(InputValidator.isValidUsername("timy_123"))
        assertTrue(InputValidator.isValidUsername("USER.NAME"))
    }

    @Test
    fun `username with space returns false`() {
        assertFalse(InputValidator.isValidUsername("timy ganteng"))
    }

    @Test
    fun `username with special char returns false`() {
        assertFalse(InputValidator.isValidUsername("timy@ganteng"))
        assertFalse(InputValidator.isValidUsername("timy#1"))
        assertFalse(InputValidator.isValidUsername("<script>"))
    }

    @Test
    fun `username too short or too long returns false`() {
        assertFalse(InputValidator.isValidUsername("ab")) // Terlalu pendek (min 3)
        assertFalse(InputValidator.isValidUsername("ini.username.yang.sangat.amat.panjang.sekali")) // > 20
    }

    // ==========================================
    // 2. TEST EMAIL
    // ==========================================
    @Test
    fun `email valid returns true`() {
        // Catatan: Ini butuh Robolectric atau Mocking android.util.Patterns
        assertTrue(InputValidator.isValidEmail("timy@example.com"))
        assertTrue(InputValidator.isValidEmail("nama.saya@domain.co.id"))
    }

    @Test
    fun `email invalid returns false`() {
        assertFalse(InputValidator.isValidEmail("timy.com")) // Tanpa @
        assertFalse(InputValidator.isValidEmail("timy@com")) // Domain tidak lengkap
        assertFalse(InputValidator.isValidEmail("")) // Kosong
        assertFalse(InputValidator.isValidEmail("   ")) // Spasi saja
    }

    // ==========================================
    // 3. TEST PASSWORD
    // ==========================================
    @Test
    fun `password valid returns true`() {
        assertTrue(InputValidator.isValidPassword("rahasia123")) // > 6 char, no space
        assertTrue(InputValidator.isValidPassword("P@ssw0rd"))
    }

    @Test
    fun `password too short returns false`() {
        assertFalse(InputValidator.isValidPassword("12345")) // 5 char
    }

    @Test
    fun `password with space returns false`() {
        assertFalse(InputValidator.isValidPassword("rahasia 123")) // Ada spasi
    }

    // ==========================================
    // 4. TEST SANITASI TEKS
    // ==========================================
    @Test
    fun `sanitize text removes html tags`() {
        val input = "Halo <script>alert('hack')</script> Dunia"
        val expected = "Halo alert('hack') Dunia"
        assertEquals(expected, InputValidator.sanitizeText(input))
    }

    @Test
    fun `sanitize text removes complex tags`() {
        val input = "Click <a href='http://evil.com'>Here</a> now"
        val expected = "Click Here now"
        assertEquals(expected, InputValidator.sanitizeText(input))
    }

    @Test
    fun `sanitize text trims whitespace`() {
        val input = "   Halo Dunia   "
        val expected = "Halo Dunia"
        assertEquals(expected, InputValidator.sanitizeText(input))
    }

    @Test
    fun `sanitize text limits length`() {
        // Buat string panjang 600 karakter
        val longText = "a".repeat(600)
        val sanitized = InputValidator.sanitizeText(longText)

        // Harusnya dipotong jadi 500
        assertEquals(500, sanitized.length)
    }

    // ==========================================
    // 5. TEST TASK TITLE
    // ==========================================
    @Test
    fun `task title valid returns true`() {
        assertTrue(InputValidator.isValidTaskTitle("Belajar Kotlin"))
    }

    @Test
    fun `task title empty returns false`() {
        assertFalse(InputValidator.isValidTaskTitle(""))
        assertFalse(InputValidator.isValidTaskTitle("   ")) // Dianggap kosong setelah trim
    }

    @Test
    fun `task title too long returns false`() {
        val longTitle = "a".repeat(101) // 101 karakter
        assertFalse(InputValidator.isValidTaskTitle(longTitle))
    }
}