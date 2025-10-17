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

class EditTaskActivity : AppCompatActivity() {

    // Deklarasi view sesuai dengan ID di XML Anda
    private lateinit var etActivity: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var etLocation: EditText
    private lateinit var etPriority: EditText
    private lateinit var etDetails: EditText
    private lateinit var btnSave: Button

    private var currentTask: Task? = null
    // Kita gunakan satu Calendar untuk menyimpan tanggal dan waktu
    private var selectedDateTime: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_task) // Menggunakan layout Anda

        // Inisialisasi semua view
        etActivity = findViewById(R.id.inputActivity)
        etDate = findViewById(R.id.inputDate)
        etTime = findViewById(R.id.inputTime)
        etLocation = findViewById(R.id.inputLocation)
        etPriority = findViewById(R.id.inputPriority)
        etDetails = findViewById(R.id.inputDetails)
        btnSave = findViewById(R.id.btnSave)

        // Ambil ID tugas yang dikirim dari activity sebelumnya
        val taskId = intent.getStringExtra("TASK_ID")
        if (taskId == null) {
            Toast.makeText(this, "Error: Task ID tidak ditemukan", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Muat data dari Firestore dan tampilkan ke UI
        loadTaskData(taskId)

        // Setup listener untuk input tanggal dan waktu
        etDate.setOnClickListener { showDatePicker() }
        etTime.setOnClickListener { showTimePicker() }

        // Setup listener untuk tombol simpan
        btnSave.setOnClickListener { updateTaskData() }
    }

    private fun loadTaskData(taskId: String) {
        lifecycleScope.launch {
            currentTask = TaskRepository.getTaskById(taskId)
            if (currentTask != null) {
                // Simpan tanggal dari Firestore ke Calendar
                selectedDateTime.time = currentTask!!.dueDate.toDate()

                // Tampilkan data ke semua EditText
                etActivity.setText(currentTask!!.title)
                etDetails.setText(currentTask!!.details)
                etLocation.setText(currentTask!!.category)
                etPriority.setText(currentTask!!.priority)

                // Format dan tampilkan tanggal & waktu
                updateDateInView()
                updateTimeInView()

            } else {
                Toast.makeText(this@EditTaskActivity, "Gagal memuat data tugas", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDateTime.set(Calendar.YEAR, year)
                selectedDateTime.set(Calendar.MONTH, month)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDateTime.set(Calendar.MINUTE, minute)
                updateTimeInView()
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            true // 24-hour format
        )
        timePickerDialog.show()
    }

    // Helper untuk update teks tanggal di EditText
    private fun updateDateInView() {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        etDate.setText(sdf.format(selectedDateTime.time))
    }

    // Helper untuk update teks waktu di EditText
    private fun updateTimeInView() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        etTime.setText(sdf.format(selectedDateTime.time))
    }

    private fun updateTaskData() {
        if (currentTask == null) return

        val newTitle = etActivity.text.toString().trim()
        if (newTitle.isEmpty()) {
            Toast.makeText(this, "Activity tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Perbarui objek 'currentTask' dengan data baru dari semua input
        currentTask!!.title = newTitle
        currentTask!!.details = etDetails.text.toString().trim()
        currentTask!!.category = etLocation.text.toString().trim()
        currentTask!!.priority = etPriority.text.toString().trim()
        currentTask!!.dueDate = Timestamp(selectedDateTime.time) // Update timestamp

        lifecycleScope.launch {
            try {
                TaskRepository.updateTask(currentTask!!)
                Toast.makeText(this@EditTaskActivity, "Tugas berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                finish() // Kembali ke halaman daftar tugas
            } catch (e: Exception) {
                Toast.makeText(this@EditTaskActivity, "Gagal memperbarui: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}