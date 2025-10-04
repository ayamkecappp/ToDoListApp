package com.example.todolistapp

import java.util.Collections
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

data class Task(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val time: String,
    val category: String,
    val priority: String,
    val endTimeMillis: Long = 0L,
    val monthAdded: Int = Calendar.getInstance().apply { timeInMillis = id }.get(Calendar.MONTH),
    val status: Status = Status.ACTIVE // BARU: Tambahkan status tugas
) {
    enum class Status {
        ACTIVE, MISSED, DELETED, COMPLETED
    }
}

object TaskRepository {
    // Gunakan satu set daftar sesuai dengan status
    private val tasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    private val deletedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    private val missedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    private val completedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())

    // Helper untuk memindahkan/menghapus tugas antar list berdasarkan ID
    private fun removeTaskFromAllLists(taskId: Long) {
        tasks.removeAll { it.id == taskId }
        missedTasks.removeAll { it.id == taskId }
        deletedTasks.removeAll { it.id == taskId }
        completedTasks.removeAll { it.id == taskId }
    }

    /**
     * Memperbarui atau memindahkan tugas antar daftar berdasarkan status baru.
     * Tugas akan dipindahkan ke daftar yang sesuai dengan statusnya.
     */
    fun updateTask(updatedTask: Task): Boolean {
        removeTaskFromAllLists(updatedTask.id)

        // Perbaikan: Menggunakan blok terpisah { ... ; true } untuk memastikan kembalian adalah Boolean,
        // karena List.add(index, element) mengembalikan Unit.
        return when (updatedTask.status) {
            Task.Status.ACTIVE -> {
                tasks.add(0, updatedTask)
                true
            }
            Task.Status.MISSED -> {
                missedTasks.add(0, updatedTask)
                true
            }
            Task.Status.DELETED -> {
                deletedTasks.add(0, updatedTask)
                true
            }
            Task.Status.COMPLETED -> {
                completedTasks.add(0, updatedTask)
                true
            }
        }
    }


    fun addTask(task: Task) {
        tasks.add(0, task)
    }

    /**
     * Mencari tugas di daftar aktif
     */
    fun getTaskById(taskId: Long): Task? {
        return tasks.find { it.id == taskId }
    }

    /**
     * Mencari tugas di SEMUA daftar (aktif, terlewat, dihapus, selesai)
     */
    fun findTaskInAnyList(taskId: Long): Task? {
        return tasks.find { it.id == taskId }
            ?: missedTasks.find { it.id == taskId }
            ?: deletedTasks.find { it.id == taskId }
            ?: completedTasks.find { it.id == taskId }
    }


    fun completeTask(taskId: Long): Boolean {
        val taskToUpdate = tasks.find { it.id == taskId } ?: return false
        val completedTask = taskToUpdate.copy(status = Task.Status.COMPLETED)
        return updateTask(completedTask)
    }

    fun getCompletedTasks(): List<Task> {
        return completedTasks.toList()
    }

    /**
     * Mendapatkan completed tasks berdasarkan tanggal TASK (dari ID task)
     */
    fun getCompletedTasksByDate(selectedDate: Calendar): List<Task> {
        val selectedYear = selectedDate.get(Calendar.YEAR)
        val selectedMonth = selectedDate.get(Calendar.MONTH)
        val selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH)

        return completedTasks.filter { task ->
            val taskCalendar = Calendar.getInstance().apply { timeInMillis = task.id }

            taskCalendar.get(Calendar.YEAR) == selectedYear &&
                    taskCalendar.get(Calendar.MONTH) == selectedMonth &&
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    /**
     * Mendapatkan semua active tasks (tidak termasuk completed, deleted, atau missed)
     */
    fun getAllTasks(): List<Task> {
        return tasks.toList()
    }

    /**
     * Mendapatkan semua tanggal (Long/millisecond) dari semua tugas aktif yang ada.
     * Digunakan untuk menampilkan 'dot' di kalender.
     */
    fun getAllActiveTaskDates(): Set<Long> {
        // Hanya ambil tanggal (reset jam, menit, detik) dari task.id
        return tasks.map { task ->
            val cal = Calendar.getInstance().apply { timeInMillis = task.id }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.toSet()
    }

    fun processTasksForMissed() {
        val now = System.currentTimeMillis()

        // Cek hanya tugas di daftar aktif yang memiliki endTimeMillis > 0
        val newlyMissed = tasks.filter { it.endTimeMillis != 0L && it.endTimeMillis < now }

        // Pindahkan newlyMissed ke daftar missedTasks
        tasks.removeAll(newlyMissed)
        newlyMissed.forEach {
            // Pastikan tidak ada duplikasi di missedTasks (walau seharusnya tidak terjadi)
            if (missedTasks.none { m -> m.id == it.id }) {
                missedTasks.add(0, it.copy(status = Task.Status.MISSED))
            }
        }
    }

    fun getMissedTasks(): List<Task> {
        // Urutkan berdasarkan waktu berakhir terdekat (descending)
        return missedTasks.sortedByDescending { it.endTimeMillis }
    }

    fun deleteTask(taskId: Long): Boolean {
        val taskToUpdate = tasks.find { it.id == taskId } ?: return false
        val deletedTask = taskToUpdate.copy(status = Task.Status.DELETED)
        return updateTask(deletedTask)
    }

    fun getDeletedTasks(): List<Task> {
        return deletedTasks.toList()
    }

    fun getTasksByDate(selectedDate: Calendar): List<Task> {
        processTasksForMissed()

        val selectedYear = selectedDate.get(Calendar.YEAR)
        val selectedMonth = selectedDate.get(Calendar.MONTH)
        val selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH)

        return tasks.filter { task ->
            // Cek berdasarkan ID (tanggal dibuat)
            val taskCalendar = Calendar.getInstance().apply { timeInMillis = task.id }

            taskCalendar.get(Calendar.YEAR) == selectedYear &&
                    taskCalendar.get(Calendar.MONTH) == selectedMonth &&
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

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