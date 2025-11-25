package com.example.todolistapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class ReportBugActivity : AppCompatActivity() {

    private lateinit var uploadArea: LinearLayout
    private lateinit var tvUploadStatus: TextView
    private var selectedFileUri: Uri? = null

    // Launcher untuk memilih file (gambar)
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            // Tampilkan status file yang dipilih (misalnya nama file atau "File Selected")
            tvUploadStatus.text = "File selected (${getFileName(uri)})"
            Toast.makeText(this, "Screenshot selected successfully.", Toast.LENGTH_SHORT).show()
        } else {
            selectedFileUri = null
            tvUploadStatus.text = "Upload Files" // Kembali ke status default
            Toast.makeText(this, "File selection canceled.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_bug)

        val ivBackArrow = findViewById<ImageView>(R.id.ivBackArrow)
        val btnSubmitBug = findViewById<Button>(R.id.btnSubmitBug)
        uploadArea = findViewById(R.id.uploadArea)
        // Menambahkan TextView baru untuk menampilkan status unggahan
        // Asumsi struktur layout: uploadArea (LinearLayout) -> ImageView & TextView
        // Kita akan mencari TextView di dalam uploadArea untuk menampilkan status

        // Asumsi: TextView untuk "Upload Files" adalah child kedua di dalam uploadArea (di bawah ImageView)
        tvUploadStatus = uploadArea.getChildAt(1) as? TextView ?: TextView(this)


        ivBackArrow.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Menambahkan listener untuk area upload
        uploadArea.setOnClickListener {
            openFilePicker()
        }

        btnSubmitBug.setOnClickListener {
            // Logika validasi dan pengiriman bug report di sini
            if (selectedFileUri != null) {
                // Di sini Anda bisa mengirim selectedFileUri ke server atau layanan
                Toast.makeText(this, "Bug report submitted successfully with 1 attachment.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Bug report submitted successfully.", Toast.LENGTH_LONG).show()
            }

            // Kembali ke Settings setelah kirim
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun openFilePicker() {
        // Membuka pemilih file untuk gambar (*/* untuk semua tipe file, namun kita fokus ke gambar)
        filePickerLauncher.launch("image/*")
    }

    /**
     * Fungsi helper untuk mendapatkan nama file dari URI (untuk display)
     */
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex("_display_name")
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result ?: "File Selected"
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}