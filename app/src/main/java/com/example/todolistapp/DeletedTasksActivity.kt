package com.example.todolistapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class DeletedTasksActivity : AppCompatActivity() {

    // Deklarasi view sesuai dengan ID di XML Anda
    private lateinit var rvDeletedTasks: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var backArrow: ImageView

    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.deleted_tasks)

        // Inisialisasi Views
        rvDeletedTasks = findViewById(R.id.rvDeletedTasks)
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
        loadDeletedTasks()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(mutableListOf())
        rvDeletedTasks.adapter = taskAdapter
        rvDeletedTasks.layoutManager = LinearLayoutManager(this)
    }

    private fun loadDeletedTasks() {
        lifecycleScope.launch {
            try {
                val deletedTasks = TaskRepository.getTasksByStatus("deleted")

                // Logika untuk menampilkan/menyembunyikan empty state
                if (deletedTasks.isEmpty()) {
                    emptyStateContainer.visibility = View.VISIBLE
                    rvDeletedTasks.visibility = View.GONE
                } else {
                    emptyStateContainer.visibility = View.GONE
                    rvDeletedTasks.visibility = View.VISIBLE
                }

                taskAdapter.updateTasks(deletedTasks)
            } catch (e: Exception) {
                Toast.makeText(this@DeletedTasksActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                emptyStateContainer.visibility = View.VISIBLE
                rvDeletedTasks.visibility = View.GONE
            }
        }
    }

    private fun setupAdapterListeners() {
        // Kita gunakan tombol 'Flow Timer' sebagai tombol 'Restore' (Pulihkan)
        taskAdapter.onFlowTimerClickListener = { task ->
            AlertDialog.Builder(this)
                .setTitle("Pulihkan Tugas")
                .setMessage("Apakah Anda yakin ingin memulihkan '${task.title}'?")
                .setPositiveButton("Pulihkan") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            // Kembalikan statusnya menjadi "pending"
                            TaskRepository.updateTaskStatus(task.id, "pending")
                            Toast.makeText(this@DeletedTasksActivity, "'${task.title}' telah dipulihkan", Toast.LENGTH_SHORT).show()
                            loadDeletedTasks() // Muat ulang daftar agar tugas yang dipulihkan hilang
                        } catch (e: Exception) {
                            Toast.makeText(this@DeletedTasksActivity, "Gagal memulihkan", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}