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
    // TAMBAHAN BARU: Timestamp kapan tugas harus selesai
    val endTimeMillis: Long = 0L,
    // monthAdded diambil dari ID (timestamp) saat Task dibuat (0=Jan, 11=Des)
    val monthAdded: Int = Calendar.getInstance().apply { timeInMillis = id }.get(Calendar.MONTH)
)

// Singleton untuk menyimpan dan mengelola daftar tugas
object TaskRepository {
    private val tasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    private val deletedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    private val missedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    // TAMBAHAN BARU: List untuk menyimpan tugas yang telah selesai
    private val completedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())

    // --- FUNGSI DASAR ---

    fun addTask(task: Task) {
        // Tugas terbaru ditambahkan di awal (index 0)
        tasks.add(0, task)
    }

    // --- FUNGSI BARU: MARK AS COMPLETED ---
    /**
     * Menghapus tugas dari daftar aktif dan memindahkannya ke daftar completedTasks.
     */
    fun completeTask(taskId: Long): Boolean {
        val taskToRemove = tasks.find { it.id == taskId }
        if (taskToRemove != null) {
            tasks.remove(taskToRemove)
            completedTasks.add(0, taskToRemove) // Tambahkan ke completedTasks, terbaru di atas
            return true
        }
        return false
    }

    // --- FUNGSI BARU: GET COMPLETED TASKS ---
    fun getCompletedTasks(): List<Task> {
        return completedTasks.toList()
    }

    // --- FUNGSI PROSES TUGAS TERLEWAT (MISSED TASKS) ---
    fun processTasksForMissed() {
        val now = System.currentTimeMillis()
        val missed = tasks.filter { it.endTimeMillis != 0L && it.endTimeMillis < now }

        tasks.removeAll(missed)
        missedTasks.addAll(missed)
    }

    // --- FUNGSI GET MISSED & DELETED TASKS (TETAP) ---
    fun getMissedTasks(): List<Task> {
        return missedTasks.sortedByDescending { it.endTimeMillis }
    }

    fun deleteTask(taskId: Long): Boolean {
        val taskToRemove = tasks.find { it.id == taskId }
        if (taskToRemove != null) {
            tasks.remove(taskToRemove)
            deletedTasks.add(0, taskToRemove)
            return true
        }
        return false
    }

    fun getDeletedTasks(): List<Task> {
        return deletedTasks.toList()
    }

    // --- FUNGSI FILTER BERDASARKAN TANGGAL ---
    fun getTasksByDate(selectedDate: Calendar): List<Task> {
        processTasksForMissed()

        val selectedYear = selectedDate.get(Calendar.YEAR)
        val selectedMonth = selectedDate.get(Calendar.MONTH)
        val selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH)

        return tasks.filter { task ->
            val taskCalendar = Calendar.getInstance().apply { timeInMillis = task.id }

            taskCalendar.get(Calendar.YEAR) == selectedYear &&
                    taskCalendar.get(Calendar.MONTH) == selectedMonth &&
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    // --- FUNGSI SEARCH TASKS (TETAP) ---
    fun searchTasks(query: String, monthFilter: Int): List<Task> {
        processTasksForMissed()

        val lowerCaseQuery = query.trim().lowercase()

        return tasks.filter { task ->
            val matchesTitle = task.title.lowercase().contains(lowerCaseQuery)

            val matchesMonth = if (monthFilter == -1) {
                true
            } else {
                task.monthAdded == monthFilter
            }

            matchesTitle && matchesMonth
        }
    }
}