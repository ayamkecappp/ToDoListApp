package com.example.todolistapp

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

class CompletedTasksActivity : AppCompatActivity() {

    // Deklarasi view sesuai dengan ID di XML Anda
    private lateinit var rvCompletedTasks: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var backArrow: ImageView

    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.completed_tasks)

        // Inisialisasi Views
        rvCompletedTasks = findViewById(R.id.rvCompletedTasks)
        emptyStateContainer = findViewById(R.id.empty_state_container)
        backArrow = findViewById(R.id.ivBackArrow)

        setupRecyclerView()

        // Fungsikan tombol kembali
        backArrow.setOnClickListener {
            finish() // Menutup activity saat panah diklik
        }
    }

    override fun onResume() {
        super.onResume()
        loadCompletedTasks()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(mutableListOf())
        rvCompletedTasks.adapter = taskAdapter
        rvCompletedTasks.layoutManager = LinearLayoutManager(this)

        // Di halaman ini, kita tidak memerlukan aksi interaktif pada item tugas,
        // jadi listener bisa dibiarkan kosong.
    }

    private fun loadCompletedTasks() {
        lifecycleScope.launch {
            try {
                val completedTasks = TaskRepository.getTasksByStatus("completed")

                // --- INILAH LOGIKA YANG DIPERBAIKI ---
                // Memeriksa apakah daftar tugas kosong atau tidak
                if (completedTasks.isEmpty()) {
                    // Jika kosong, tampilkan gambar Timy dan sembunyikan daftar
                    emptyStateContainer.visibility = View.VISIBLE
                    rvCompletedTasks.visibility = View.GONE
                } else {
                    // Jika ada isinya, sembunyikan gambar Timy dan tampilkan daftar
                    emptyStateContainer.visibility = View.GONE
                    rvCompletedTasks.visibility = View.VISIBLE
                }
                // --- AKHIR DARI PERBAIKAN ---

                taskAdapter.updateTasks(completedTasks)
            } catch (e: Exception) {
                Toast.makeText(this@CompletedTasksActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                // Jika terjadi error, tampilkan juga empty state
                emptyStateContainer.visibility = View.VISIBLE
                rvCompletedTasks.visibility = View.GONE
            }
        }
    }
}