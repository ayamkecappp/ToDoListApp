package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MissedTasksActivity : AppCompatActivity() {

    // Deklarasi view sesuai dengan ID di XML Anda
    private lateinit var rvMissedTasks: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var backArrow: ImageView

    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.missed_tasks)

        // Inisialisasi Views
        rvMissedTasks = findViewById(R.id.rvMissedTasks)
        emptyStateContainer = findViewById(R.id.empty_state_container)
        backArrow = findViewById(R.id.ivBackArrow)

        setupRecyclerView()
        setupAdapterListeners()

        // Fungsikan tombol kembali
        backArrow.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadMissedTasks()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(mutableListOf())
        rvMissedTasks.adapter = taskAdapter
        rvMissedTasks.layoutManager = LinearLayoutManager(this)
    }

    private fun loadMissedTasks() {
        lifecycleScope.launch {
            try {
                // Pastikan tugas terlewat sudah di-update
                TaskRepository.updateMissedTasks()
                val missedTasks = TaskRepository.getTasksByStatus("missed")

                // Logika untuk menampilkan/menyembunyikan empty state
                if (missedTasks.isEmpty()) {
                    emptyStateContainer.visibility = View.VISIBLE
                    rvMissedTasks.visibility = View.GONE
                } else {
                    emptyStateContainer.visibility = View.GONE
                    rvMissedTasks.visibility = View.VISIBLE
                }

                taskAdapter.updateTasks(missedTasks)
            } catch (e: Exception) {
                Toast.makeText(this@MissedTasksActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                emptyStateContainer.visibility = View.VISIBLE
                rvMissedTasks.visibility = View.GONE
            }
        }
    }

    private fun setupAdapterListeners() {
        // Kita gunakan tombol 'Edit' sebagai tombol 'Reschedule' (Jadwalkan Ulang)
        taskAdapter.onEditClickListener = { task ->
            Toast.makeText(this, "Jadwalkan ulang '${task.title}'", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, EditTaskActivity::class.java).apply {
                putExtra("TASK_ID", task.id)
            }
            startActivity(intent)
        }
    }
}