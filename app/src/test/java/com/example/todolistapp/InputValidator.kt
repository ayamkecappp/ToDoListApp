package com.example.todolistapp

import org.junit.Assert.*
import org.junit.Test

class InputValidatorTest {

    // --- TEST USERNAME ---
    @Test
    fun `username valid returns true`() {
        assertTrue(InputValidator.isValidUsername("timy.ganteng"))
        assertTrue(InputValidator.isValidUsername("timy_123"))
    }

    @Test
    fun `username with space returns false`() {
        assertFalse(InputValidator.isValidUsername("timy ganteng"))
    }

    @Test
    fun `username with special char returns false`() {
        assertFalse(InputValidator.isValidUsername("timy@ganteng"))
        assertFalse(InputValidator.isValidUsername("<script>"))
    }

    // --- TEST SANITASI TEKS ---
    @Test
    fun `sanitize text removes html tags`() {
        val input = "Halo <script>alert('hack')</script> Dunia"
        val expected = "Halo alert('hack') Dunia" // Tag hilang, teks aman sisa
        assertEquals(expected, InputValidator.sanitizeText(input))
    }

    @Test
    fun `sanitize text trims whitespace`() {
        val input = "   Halo Dunia   "
        val expected = "Halo Dunia"
        assertEquals(expected, InputValidator.sanitizeText(input))
    }
}