package com.example.todolistapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTaskActivity : AppCompatActivity() {

    // Deklarasi view sesuai dengan ID di addtask.xml Anda
    private lateinit var etActivity: EditText
    private lateinit var etTime: EditText
    private lateinit var etLocation: EditText
    private lateinit var etPriority: EditText
    private lateinit var etDetails: EditText
    private lateinit var btnSave: Button
    // Kita tambahkan referensi untuk input tanggal juga
    private lateinit var etDate: EditText

    // Gunakan satu Calendar untuk menyimpan tanggal dan waktu yang dipilih
    private var selectedDateTime: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addtask) // Menggunakan layout addtask.xml

        // Inisialisasi semua view
        etActivity = findViewById(R.id.inputActivity)
        etTime = findViewById(R.id.inputTime)
        etLocation = findViewById(R.id.inputLocation)
        etPriority = findViewById(R.id.inputPriority)
        etDetails = findViewById(R.id.inputDetails)
        btnSave = findViewById(R.id.btnSave)



        // Setup listener untuk input waktu yang akan memicu dialog tanggal terlebih dahulu
        etTime.setOnClickListener {
            showDatePicker()
        }

        // Setup listener untuk tombol simpan
        btnSave.setOnClickListener {
            saveNewTaskToFirestore()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDateTime.set(Calendar.YEAR, year)
                selectedDateTime.set(Calendar.MONTH, month)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                // Setelah tanggal dipilih, langsung tampilkan dialog waktu
                showTimePicker()
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        )
        // Mencegah memilih tanggal di masa lalu
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDateTime.set(Calendar.MINUTE, minute)
                // Setelah waktu dipilih, update teks di EditText
                updateTimeInView()
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            true // format 24 jam
        )
        timePickerDialog.show()
    }

    // Helper untuk update teks waktu di EditText
    private fun updateTimeInView() {
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        etTime.setText(sdf.format(selectedDateTime.time))
    }

    private fun saveNewTaskToFirestore() {
        val title = etActivity.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Activity tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        if (etTime.text.toString().isEmpty()) {
            Toast.makeText(this, "Silakan pilih waktu terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Buat objek Task baru dari semua inputan pengguna
        val newTask = Task(
            // id dan userId akan diisi oleh Repository
            title = title,
            details = etDetails.text.toString().trim(),
            category = etLocation.text.toString().trim(),
            priority = etPriority.text.toString().trim().ifEmpty { "None" }, // Default value jika kosong
            createdAt = Timestamp.now(),
            dueDate = Timestamp(selectedDateTime.time), // Simpan tanggal & waktu yang dipilih
            status = "pending" // Status awal selalu "pending"
        )

        lifecycleScope.launch {
            try {
                TaskRepository.addTask(newTask)
                Toast.makeText(this@AddTaskActivity, "Tugas berhasil disimpan!", Toast.LENGTH_SHORT).show()
                finish() // Kembali ke halaman daftar tugas setelah berhasil
            } catch (e: Exception) {
                Toast.makeText(this@AddTaskActivity, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}