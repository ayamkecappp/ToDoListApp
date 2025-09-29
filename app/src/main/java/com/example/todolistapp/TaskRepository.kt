package com.example.todolistapp

import java.util.Collections
import java.util.Calendar

// Data class untuk merepresentasikan sebuah Task
data class Task(
    // MODIFIKASI: Menerima id secara opsional. Jika diberikan, ia digunakan sebagai timestamp pembuatan.
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val time: String,
    val category: String,
    val priority: String,
    // monthAdded diambil dari ID (timestamp) saat Task dibuat (0=Jan, 11=Des)
    val monthAdded: Int = Calendar.getInstance().apply { timeInMillis = id }.get(Calendar.MONTH)
)

// Singleton untuk menyimpan dan mengelola daftar tugas
object TaskRepository {
    private val tasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())

    // --- FUNGSI DASAR ---

    fun addTask(task: Task) {
        // Tugas terbaru ditambahkan di awal (index 0)
        tasks.add(0, task)
    }

    fun getAllTasks(): List<Task> {
        return tasks.toList()
    }

    // --- FUNGSI FILTER BERDASARKAN TANGGAL (Untuk TaskActivity) ---

    /**
     * Mencari tugas yang jatuh pada tanggal tertentu (tahun, bulan, dan hari harus sama).
     */
    fun getTasksByDate(selectedDate: Calendar): List<Task> {
        val selectedYear = selectedDate.get(Calendar.YEAR)
        val selectedMonth = selectedDate.get(Calendar.MONTH)
        val selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH)

        return tasks.filter { task ->
            // Gunakan ID (timestamp) tugas untuk mendapatkan tanggal pembuatannya
            val taskCalendar = Calendar.getInstance().apply { timeInMillis = task.id }

            taskCalendar.get(Calendar.YEAR) == selectedYear &&
                    taskCalendar.get(Calendar.MONTH) == selectedMonth &&
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    // --- FUNGSI FILTER BERDASARKAN QUERY & BULAN (Untuk SearchFilterActivity) ---

    /**
     * Mencari tugas berdasarkan nama activity dan memfilter berdasarkan bulan.
     * @param query Teks pencarian pada title.
     * @param monthFilter Indeks bulan (0=Jan, 11=Des) atau -1 untuk semua bulan.
     */
    fun searchTasks(query: String, monthFilter: Int): List<Task> {
        val lowerCaseQuery = query.trim().lowercase()

        return tasks.filter { task ->
            val matchesTitle = task.title.lowercase().contains(lowerCaseQuery)

            val matchesMonth = if (monthFilter == -1) {
                true // -1 berarti semua bulan
            } else {
                // Gunakan monthAdded dari Task
                task.monthAdded == monthFilter
            }

            matchesTitle && matchesMonth
        }
    }
}