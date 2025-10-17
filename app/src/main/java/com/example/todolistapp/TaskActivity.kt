package com.example.todolistapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TaskActivity : AppCompatActivity() {

    // Deklarasi view sesuai dengan ID di task.xml Anda
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var tvNoActivity: TextView
    private lateinit var reminderContainer: View // Tombol "New Reminder"
    private lateinit var octoberText: TextView // Teks untuk tanggal

    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task) // Menggunakan layout task.xml Anda

        // Inisialisasi Views dari layout dengan ID yang benar
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView) // <-- INI PERBAIKANNYA
        tvNoActivity = findViewById(R.id.tvNoActivity)
        reminderContainer = findViewById(R.id.reminderContainer)
        octoberText = findViewById(R.id.octoberText)

        // Setup Adapter dan RecyclerView
        setupRecyclerView()

        // Setup listener untuk semua aksi di adapter
        setupAdapterListeners()

        // Fungsikan tombol "New Reminder"
        reminderContainer.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        // Atur teks tanggal hari ini di header
        setupHeaderText()
    }

    override fun onResume() {
        super.onResume()
        // Muat data dari Firestore setiap kali halaman ini ditampilkan
        loadPendingTasks()
    }

    private fun setupRecyclerView() {
        // Buat instance adapter dengan list kosong pada awalnya
        taskAdapter = TaskAdapter(mutableListOf())
        tasksRecyclerView.adapter = taskAdapter
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupHeaderText() {
        // Membuat format tanggal seperti "17 October 2025"
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())
        octoberText.text = currentDate
    }

    private fun loadPendingTasks() {
        lifecycleScope.launch {
            try {
                val pendingTasks = TaskRepository.getTasksByStatus("pending")

                // Tampilkan atau sembunyikan teks "You don't have remainder"
                if (pendingTasks.isEmpty()) {
                    tvNoActivity.visibility = View.VISIBLE
                    tasksRecyclerView.visibility = View.GONE
                } else {
                    tvNoActivity.visibility = View.GONE
                    tasksRecyclerView.visibility = View.VISIBLE
                }

                taskAdapter.updateTasks(pendingTasks)
            } catch (e: Exception) {
                tvNoActivity.visibility = View.VISIBLE
                tasksRecyclerView.visibility = View.GONE
                Toast.makeText(this@TaskActivity, "Gagal memuat tugas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAdapterListeners() {
        // Aksi saat checkbox di-klik
        taskAdapter.onTaskCheckedListener = { task, isChecked ->
            if (isChecked) {
                lifecycleScope.launch {
                    try {
                        TaskRepository.updateTaskStatus(task.id, "completed")
                        Toast.makeText(this@TaskActivity, "'${task.title}' Selesai!", Toast.LENGTH_SHORT).show()
                        loadPendingTasks()
                    } catch (e: Exception) {
                        Toast.makeText(this@TaskActivity, "Gagal update status", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Aksi saat tombol Flow Timer di-klik
        taskAdapter.onFlowTimerClickListener = { task ->
            val intent = Intent(this, FlowTimerActivity::class.java).apply {
                putExtra("EXTRA_TASK_NAME", task.title)
                putExtra("EXTRA_FLOW_DURATION", task.flowDurationMillis)
            }
            startActivity(intent)
        }

        // Aksi saat tombol Edit di-klik
        taskAdapter.onEditClickListener = { task ->
            val intent = Intent(this, EditTaskActivity::class.java)
            intent.putExtra("TASK_ID", task.id)
            startActivity(intent)
        }

        // Aksi saat tombol Delete di-klik
        taskAdapter.onDeleteClickListener = { task ->
            AlertDialog.Builder(this)
                .setTitle("Hapus Tugas")
                .setMessage("Apakah Anda yakin ingin menghapus '${task.title}'?")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            TaskRepository.updateTaskStatus(task.id, "deleted")
                            Toast.makeText(this@TaskActivity, "Tugas dihapus", Toast.LENGTH_SHORT).show()
                            loadPendingTasks()
                        } catch (e: Exception) {
                            Toast.makeText(this@TaskActivity, "Gagal menghapus", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}