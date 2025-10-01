// SplashActivity.kt
package com.example.todolistapp

import android.view.animation.BounceInterpolator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.hypot
// Import ini tidak perlu diulangi
// import com.example.todolistapp.LoginActivity
// import com.example.todolistapp.R

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY_MS = 300L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Inisialisasi Views
        val rootLayout = findViewById<ConstraintLayout>(R.id.root_layout)
        val blueBackgroundReveal = findViewById<View>(R.id.blue_background_reveal)
        val timyText = findViewById<TextView>(R.id.timy_time_text)
        val timyImage = findViewById<ImageView>(R.id.timy_senyum)

        // Mulai animasi setelah jeda singkat (Opening 1 & 2)
        Handler(Looper.getMainLooper()).postDelayed({
            // 1. Mulai Circular Reveal (Opening 2 ke Opening 3)
            animateCircularReveal(rootLayout, blueBackgroundReveal, timyText, timyImage)
        }, SPLASH_DELAY_MS)
    }

    // SplashActivity.kt (Fungsi yang Diubah)

    private fun animateCircularReveal(root: View, revealView: View, timyText: TextView, timyImage: ImageView) {

        // Inisialisasi View Lingkaran Baru
        val bouncingCircle = findViewById<View>(R.id.bouncing_circle)

        root.post {
            val cx = bouncingCircle.width / 2 // Gunakan pusat lingkaran kecil
            val cy = bouncingCircle.height / 2

            // Posisi tengah layar untuk Circular Reveal
            val revealCenterX = revealView.width / 2
            val revealCenterY = revealView.height / 2
            val finalRadius = hypot(revealCenterX.toDouble(), revealCenterY.toDouble()).toFloat()

            // --- 1. SETUP POSISI AWAL LINGKARAN (DI ATAS) ---
            // Kita hitung seberapa jauh harus digeser ke atas dari posisi tengah (0f).
            val initialTranslationY = -(root.height.toFloat() / 2f + bouncingCircle.height)
            bouncingCircle.translationY = initialTranslationY
            bouncingCircle.visibility = View.VISIBLE

            // --- 2. ANIMASI PERGERAKAN DENGAN BOUNCE (Lingkaran Turun) ---
            bouncingCircle.animate()
                .translationY(0f) // Bergerak ke posisi akhirnya (0f/tengah)
                .setDuration(700)
                .setInterpolator(BounceInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {

                        // Sembunyikan lingkaran kecil setelah bounce
                        bouncingCircle.visibility = View.INVISIBLE

                        // --- 3. ANIMASI CIRCULAR REVEAL (Kotak Biru Menyebar) ---
                        revealView.visibility = View.VISIBLE // Tampilkan kotak biru penuh

                        // Mulai dari pusat lingkaran kecil (yang sudah ada di tengah)
                        val revealAnim = ViewAnimationUtils.createCircularReveal(
                            revealView,
                            revealCenterX,
                            revealCenterY,
                            0f,
                            finalRadius
                        )

                        revealAnim.duration = 600
                        revealAnim.interpolator = AccelerateDecelerateInterpolator()

                        revealAnim.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                animateTextFadeIn(timyText, timyImage)
                            }
                        })

                        revealAnim.start()
                    }
                })
                .start()
        }
    }

    private fun animateTextFadeIn(timyText: TextView, timyImage: ImageView) {
        // Teks diposting agar kita bisa mengakses posisi akhirnya setelah layout terukur.
        timyText.post {
            // Ambil posisi Y akhir (setelah diatur oleh ConstraintLayout Chain)
            val finalPosition = timyText.y

            // Hitung posisi tengah absolut (tanpa memperhitungkan status bar)
            val screenHeight = resources.displayMetrics.heightPixels
            val centerScreenY = (screenHeight / 2f)

            // Hitung posisi awal teks di tengah absolut relatif terhadap posisi akhirnya
            val initialTranslationY = centerScreenY - (timyText.height / 2f) - finalPosition

            // Terapkan posisi awal teks (di tengah absolut)
            timyText.translationY = initialTranslationY

            // Mulai Fade In
            timyText.animate()
                .alpha(1f)
                .setDuration(400)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // 3. Karakter Timy muncul dan Teks bergerak (Opening 5)
                        animateCharacterMovement(timyText, timyImage, initialTranslationY)
                    }
                })
                .start()
        }
    }

    private fun animateCharacterMovement(timyText: TextView, timyImage: ImageView, initialTranslationY: Float) {

        val density = resources.displayMetrics.density

        /* --- 1. Animasi Karakter Timy: Dari Bawah Naik & Turun Sedikit --- */

        // Tentukan seberapa jauh di bawah layar Timy dimulai (misalnya 400dp)
        val startOffset = 400f * density

        // Tentukan puncak overshoot (misalnya 50dp di atas posisi akhir 0f)
        val overshootY = -50f * density

        // Atur posisi awal Timy: Jauh di bawah posisi akhirnya (0f)
        timyImage.translationY = startOffset
        timyImage.alpha = 1.0f

        // ➡️ Animasi Karakter Timy (Dua Langkah)

        // Langkah 1: Naik Cepat (ke posisi overshoot)
        timyImage.animate()
            .translationY(overshootY)
            .setDuration(400) // Cepat naik
            .withEndAction {
                // Langkah 2: Turun Perlahan (mendekati tulisan)
                timyImage.animate()
                    .translationY(0f) // Turun ke posisi final
                    .setDuration(400) // Lebih lambat turunnya
                    .start()
            }
            .start()


        /* --- 2. Animasi Teks "Timy Time": Dari Tengah Absolut Turun ke Bawah Timy --- */

        // Animasi Teks: Bergerak dari initialTranslationY ke posisi akhir (0f)
        // Kita buat pergerakan ini dimulai sedikit lebih lambat dari Timy.

        timyText.animate()
            .translationY(0f) // Target 0f adalah posisi rantai akhirnya
            .setDuration(800)
            .setStartDelay(200) // Mulai 0.2 detik setelah Timy mulai naik
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 4. Pindah ke Halaman Login
                    Handler(Looper.getMainLooper()).postDelayed({
                        startLoginScreen()
                    }, 800)
                }
            })
            .start()
    }

    private fun startLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        // Gunakan transisi yang mulus
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}