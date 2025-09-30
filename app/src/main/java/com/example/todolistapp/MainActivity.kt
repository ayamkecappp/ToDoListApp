package com.example.todolistapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.hypot

class MainActivity : AppCompatActivity() {

    // Durasi animasi yang disesuaikan agar mirip dengan video
    private val CIRCLE_EXPAND_DURATION = 1000L // Lingkaran membesar dalam 1 detik
    private val SLIDE_IN_DURATION = 1200L      // Animasi slide Timy & Title lebih lambat dari lingkaran
    private val SLIDE_START_DELAY = 600L       // Mulai slide saat lingkaran sudah separuh jalan
    private val TOTAL_HOLD_TIME = 3500L        // Total waktu dari mulai hingga navigasi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Menggunakan layout splash_screen.xml
        setContentView(R.layout.splash_screen)

        val circleBackground = findViewById<View>(R.id.circleBackground)
        val imgTimy = findViewById<ImageView>(R.id.imgTimy)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)

        // Jadikan Timy dan Title terlihat
        imgTimy.visibility = View.VISIBLE
        tvTitle.visibility = View.VISIBLE

        // Animasi dimulai hanya setelah layout dihitung
        circleBackground.post {
            // Hitung radius akhir untuk menutupi seluruh layar
            val finalRadius = hypot(
                circleBackground.width.toDouble(),
                circleBackground.height.toDouble()
            ).toFloat() * 1.5f

            // --- 1. Animasi Lingkaran Membesar (Circle Reveal) ---
            val scaleX = ObjectAnimator.ofFloat(circleBackground, View.SCALE_X, 1f, finalRadius / circleBackground.width)
            val scaleY = ObjectAnimator.ofFloat(circleBackground, View.SCALE_Y, 1f, finalRadius / circleBackground.height)
            scaleX.duration = CIRCLE_EXPAND_DURATION
            scaleY.duration = CIRCLE_EXPAND_DURATION

            // --- 2. Animasi Timy dari bawah ke tengah (TranslationY) ---
            // Dimulai dari 200dp ke 0dp (posisi asli di tengah)
            val timyTranslateY = ObjectAnimator.ofFloat(imgTimy, View.TRANSLATION_Y, 200f, 0f)
            timyTranslateY.duration = SLIDE_IN_DURATION
            timyTranslateY.startDelay = SLIDE_START_DELAY

            // --- 3. Animasi Title dari atas ke tengah (TranslationY) ---
            // Dimulai dari -200dp ke 0dp (posisi asli di tengah)
            val titleTranslateY = ObjectAnimator.ofFloat(tvTitle, View.TRANSLATION_Y, -200f, 0f)
            titleTranslateY.duration = SLIDE_IN_DURATION
            titleTranslateY.startDelay = SLIDE_START_DELAY

            val animatorSet = AnimatorSet().apply {
                // Gunakan interpolator yang sama dengan video untuk akselerasi/deselerasi
                interpolator = AccelerateDecelerateInterpolator()

                // Play all animations
                play(scaleX).with(scaleY)
                play(timyTranslateY)
                play(titleTranslateY)
            }

            // Ubah warna teks menjadi putih saat Timy dan Title mulai bergerak
            // Ini meniru perubahan warna saat background menjadi navy.
            Handler(mainLooper).postDelayed({
                tvTitle.setTextColor(resources.getColor(R.color.white, theme))
            }, SLIDE_START_DELAY)

            animatorSet.start()

            // 4. Navigasi ke LoginActivity setelah TOTAL_HOLD_TIME
            Handler(mainLooper).postDelayed({
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }, TOTAL_HOLD_TIME)
        }
    }
}