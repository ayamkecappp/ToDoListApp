package com.example.todolistapp

import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    // Mendefinisikan transisi slide
    private val FORWARD_TRANSITION_IN = R.anim.slide_in_right
    private val FORWARD_TRANSITION_OUT = R.anim.slide_out_left
    private val BACK_TRANSITION_IN = R.anim.slide_in_left
    private val BACK_TRANSITION_OUT = R.anim.slide_out_right

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Konfigurasi Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // --- PENGATURAN ONCLICK LISTENER ---

        // 1. Tombol Kembali
        findViewById<ImageView>(R.id.ivBackArrow).setOnClickListener {
            finish()
        }

        // Streak Badge
        findViewById<View>(R.id.rectangleSettings1).setOnClickListener {
            startActivityWithTransition(StreakBadgeActivity::class.java)
        }

        // Log Out -> Menampilkan Custom Dialog Konfirmasi
        findViewById<View>(R.id.rectangleSettings6).setOnClickListener {
            showCustomLogoutDialog()
        }
        // Anda juga bisa memasang listener di TextView jika perlu
        findViewById<TextView>(R.id.tvLogOut).setOnClickListener {
            showCustomLogoutDialog()
        }

        // Delete Account (Placeholder)
        findViewById<View>(R.id.rectangleSettings7).setOnClickListener {
            Toast.makeText(this, "Fungsionalitas Hapus Akun dipicu.", Toast.LENGTH_SHORT).show()
            showCustomDeleteAccountDialog()
        }

        // Report a Bug
        findViewById<View>(R.id.rectangleSettings8).setOnClickListener {
            startActivityWithTransition(ReportBugActivity::class.java)
        }

        // Send Feedback
        findViewById<View>(R.id.rectangleSettings9).setOnClickListener {
            startActivityWithTransition(SendFeedbackActivity::class.java)
        }

        // About Us
        findViewById<View>(R.id.rectangleSettings10).setOnClickListener {
            startActivityWithTransition(AboutUsActivity::class.java)
        }

        // Privacy Policy
        findViewById<View>(R.id.rectangleSettings11).setOnClickListener {
            startActivityWithTransition(PrivacyPolicyActivity::class.java)
        }
    }

    /**
     * Menampilkan custom dialog konfirmasi Logout.
     */
    private fun showCustomLogoutDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_save_success)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessageTitle)
        val btnYes = dialog.findViewById<TextView>(R.id.btnView)
        val btnNo = dialog.findViewById<TextView>(R.id.btnIgnore)

        tvMessage?.text = "Apakah Anda yakin ingin keluar dari akun?"
        tvMessage?.setTextColor(resources.getColor(R.color.dark_blue, theme))

        btnYes?.text = "Ya"
        btnNo?.text = "Tidak"
        btnYes?.setTextColor(resources.getColor(R.color.dark_blue, theme))
        btnNo?.setTextColor(resources.getColor(R.color.dark_blue, theme))

        btnYes?.setOnClickListener {
            dialog.dismiss()
            performLogout() // Panggil fungsi logout yang sudah benar
        }

        btnNo?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // --- FUNGSI BARU UNTUK HAPUS AKUN ---

    /**
     * Menampilkan custom dialog konfirmasi Hapus Akun.
     */
    private fun showCustomDeleteAccountDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_save_success) // Bisa gunakan layout yang sama
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessageTitle)
        val btnYes = dialog.findViewById<TextView>(R.id.btnView)
        val btnNo = dialog.findViewById<TextView>(R.id.btnIgnore)

        // Ubah teks untuk konfirmasi hapus akun
        tvMessage?.text = "Apakah Anda yakin ingin menghapus akun ini secara permanen?"
        tvMessage?.setTextColor(resources.getColor(R.color.dark_blue, theme)) // Atau warna merah jika ada

        btnYes?.text = "Ya, Hapus"
        btnNo?.text = "Batal"
        btnYes?.setTextColor(resources.getColor(R.color.dark_blue, theme)) // Atau warna merah
        btnNo?.setTextColor(resources.getColor(R.color.dark_blue, theme))

        btnYes?.setOnClickListener {
            dialog.dismiss()
            performDeleteAccountData() // Panggil fungsi hapus akun
        }

        btnNo?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Menjalankan proses penghapusan akun Firebase.
     */
    /**
     * LANGKAH 1: Menghapus semua data pengguna dari Firestore.
     */
    private fun performDeleteAccountData() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Tidak ada pengguna yang login", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = user.uid

        // Tampilkan dialog loading di sini (SANGAT DISARANKAN)
        // ...

        // 1. Hapus semua 'tasks' milik pengguna
        // ASUMSI: Anda punya koleksi 'tasks' dan setiap dokumen task punya field 'userId'
        firestore.collection("tasks")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { tasksSnapshot ->
                val batch = firestore.batch()
                for (document in tasksSnapshot) {
                    batch.delete(document.reference)
                }

                // Jalankan penghapusan batch untuk semua task
                batch.commit().addOnCompleteListener { taskBatch ->
                    if (taskBatch.isSuccessful) {
                        Log.d(TAG, "Semua tasks pengguna berhasil dihapus.")

                        // 2. Hapus dokumen profil pengguna (jika ada)
                        // ASUMSI: Anda punya koleksi 'users' dengan ID dokumen = uid
                        firestore.collection("users").document(uid)
                            .delete()
                            .addOnSuccessListener {
                                Log.d(TAG, "Dokumen profil pengguna berhasil dihapus.")

                                // 3. (Opsional) Hapus foto profil dari Storage
                                // ASUMSI: Foto profil disimpan di "profile_pictures/UID.jpg"
                                // val photoRef = storage.reference.child("profile_pictures/$uid.jpg")
                                // photoRef.delete().addOnCompleteListener { ... }

                                // 4. Setelah semua data bersih, baru panggil penghapusan Akun Auth
                                deleteFirebaseAuthUser(user)

                            }.addOnFailureListener { e ->
                                Log.w(TAG, "Gagal menghapus profil Firestore.", e)
                                Toast.makeText(this, "Gagal menghapus data: ${e.message}", Toast.LENGTH_LONG).show()
                                // Sembunyikan dialog loading
                            }
                    } else {
                        Log.w(TAG, "Gagal menghapus tasks.", taskBatch.exception)
                        Toast.makeText(this, "Gagal menghapus data: ${taskBatch.exception?.message}", Toast.LENGTH_LONG).show()
                        // Sembunyikan dialog loading
                    }
                }
            }.addOnFailureListener { e ->
                Log.w(TAG, "Gagal mengambil tasks.", e)
                Toast.makeText(this, "Gagal mengambil data: ${e.message}", Toast.LENGTH_LONG).show()
                // Sembunyikan dialog loading
            }
    }

    /**
     * LANGKAH 2: Menghapus akun Firebase Auth (setelah data Firestore bersih).
     */
    private fun deleteFirebaseAuthUser(user: com.google.firebase.auth.FirebaseUser) {
        user.delete()
            .addOnCompleteListener { task ->
                // Sembunyikan dialog loading di sini
                if (task.isSuccessful) {
                    // Akun berhasil dihapus
                    Log.d(TAG, "Akun Firebase berhasil dihapus.")
                    Toast.makeText(this, "Akun Anda telah berhasil dihapus.", Toast.LENGTH_LONG).show()

                    // Setelah akun dihapus, jalankan proses logout
                    performLogout()
                } else {
                    // Gagal menghapus akun
                    Log.w(TAG, "Gagal menghapus akun.", task.exception)

                    // Cek apakah error karena perlu re-autentikasi
                    if (task.exception is FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(this, "Gagal menghapus akun. Silakan login ulang dan coba lagi.", Toast.LENGTH_LONG).show()
                        // Arahkan ke logout agar pengguna bisa login ulang
                        performLogout()
                    } else {
                        Toast.makeText(this, "Gagal menghapus akun: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    /**
     * Menjalankan proses logout dari semua layanan dan kembali ke LoginActivity.
     */
    private fun performLogout() {
        // 1. Logout dari Firebase
        auth.signOut()

        // 2. Logout dari Google dan Facebook
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Logout dari Facebook (dijalankan setelah Google selesai)
            LoginManager.getInstance().logOut()

            // 3. BERSIHKAN SharedPreferences PROFIL <-- TAMBAHAN
            //val profilePrefs = getSharedPreferences(EditProfileActivity.PREFS_NAME, Context.MODE_PRIVATE)
            //profilePrefs.edit().clear().apply() // Hapus semua data di ProfilePrefs

            // 4. Arahkan kembali ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            Glide.get(applicationContext).clearMemory() // Hapus cache memori
            Thread {
                Glide.get(applicationContext).clearDiskCache() // Hapus cache disk (di background thread)
            }.start()
            startActivity(intent)
            finish() // Tutup SettingsActivity
        }
    }

    // Fungsi bantuan untuk memulai Activity dengan transisi
    private fun <T> startActivityWithTransition(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        overridePendingTransition(FORWARD_TRANSITION_IN, FORWARD_TRANSITION_OUT)
    }

    override fun finish() {
        super.finish()
        // Menerapkan transisi mundur saat Activity ditutup
        overridePendingTransition(BACK_TRANSITION_IN, BACK_TRANSITION_OUT)
    }
}