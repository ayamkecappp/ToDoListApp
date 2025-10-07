package com.example.todolistapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Collections
import java.util.Calendar

data class Task(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val time: String,
    val category: String,
    val priority: String,
    val endTimeMillis: Long = 0L,
    val monthAdded: Int = Calendar.getInstance().apply { timeInMillis = id }.get(Calendar.MONTH),
    val flowDurationMillis: Long = 0L,
    val details: String = "",
    val actionDateMillis: Long? = null
)

object TaskRepository {
    private val tasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    private val deletedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    private val missedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    private val completedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())

    private var sharedPreferences: SharedPreferences? = null
    private val gson = Gson()

    private const val PREFS_NAME = "TaskRepositoryPrefs"
    private const val KEY_TASKS = "tasks"
    private const val KEY_DELETED_TASKS = "deleted_tasks"
    private const val KEY_MISSED_TASKS = "missed_tasks"
    private const val KEY_COMPLETED_TASKS = "completed_tasks"

    /**
     * Inisialisasi repository dengan context
     * WAJIB dipanggil di onCreate() setiap Activity yang menggunakan TaskRepository
     */
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadAllData()
    }

    /**
     * Helper untuk load list task dari JSON string
     */
    private fun loadTaskList(prefs: SharedPreferences, key: String): List<Task> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Task>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Load semua data dari SharedPreferences
     */
    private fun loadAllData() {
        sharedPreferences?.let { prefs ->
            tasks.clear()
            tasks.addAll(loadTaskList(prefs, KEY_TASKS))

            deletedTasks.clear()
            deletedTasks.addAll(loadTaskList(prefs, KEY_DELETED_TASKS))

            missedTasks.clear()
            missedTasks.addAll(loadTaskList(prefs, KEY_MISSED_TASKS))

            completedTasks.clear()
            completedTasks.addAll(loadTaskList(prefs, KEY_COMPLETED_TASKS))
        }
    }

    /**
     * Save semua data ke SharedPreferences
     * Dipanggil otomatis setiap kali ada perubahan data
     */
    private fun saveAllData() {
        sharedPreferences?.edit()?.apply {
            putString(KEY_TASKS, gson.toJson(tasks))
            putString(KEY_DELETED_TASKS, gson.toJson(deletedTasks))
            putString(KEY_MISSED_TASKS, gson.toJson(missedTasks))
            putString(KEY_COMPLETED_TASKS, gson.toJson(completedTasks))
            apply()
        }
    }

    /**
     * Menambahkan task baru ke repository
     */
    fun addTask(task: Task) {
        tasks.add(0, task)
        saveAllData()
    }

    /**
     * Mengambil task berdasarkan ID
     * Mencari di tasks, missedTasks, dan deletedTasks
     */
    fun getTaskById(taskId: Long): Task? {
        val activeTask = tasks.find { it.id == taskId }
        if (activeTask != null) return activeTask

        val missedTask = missedTasks.find { it.id == taskId }
        if (missedTask != null) return missedTask

        return deletedTasks.find { it.id == taskId }
    }

    /**
     * Update task yang sudah ada
     * Bisa dari tasks, missedTasks, atau deletedTasks
     * Task yang di-update akan dipindahkan ke tasks (active)
     */
    fun updateTask(originalTaskId: Long, updatedTask: Task): Boolean {
        synchronized(tasks) {
            val activeIndex = tasks.indexOfFirst { it.id == originalTaskId }
            if (activeIndex != -1) {
                tasks.removeAt(activeIndex)
                tasks.add(0, updatedTask)
                saveAllData()
                return true
            }

            val missedIndex = missedTasks.indexOfFirst { it.id == originalTaskId }
            if (missedIndex != -1) {
                missedTasks.removeAt(missedIndex)
                tasks.add(0, updatedTask)
                saveAllData()
                return true
            }

            val deletedIndex = deletedTasks.indexOfFirst { it.id == originalTaskId }
            if (deletedIndex != -1) {
                deletedTasks.removeAt(deletedIndex)
                tasks.add(0, updatedTask)
                saveAllData()
                return true
            }

            return false
        }
    }

    /**
     * Menandai task sebagai selesai
     * Task dipindahkan dari tasks ke completedTasks
     */
    fun completeTask(taskId: Long): Boolean {
        val taskToRemove = tasks.find { it.id == taskId }
        if (taskToRemove != null) {
            tasks.remove(taskToRemove)
            val completedTask = taskToRemove.copy(actionDateMillis = System.currentTimeMillis())
            completedTasks.add(0, completedTask)
            saveAllData()
            return true
        }
        return false
    }

    /**
     * Mengambil semua task yang sudah selesai
     */
    fun getCompletedTasks(): List<Task> {
        return completedTasks.toList()
    }

    /**
     * Mengambil task yang selesai pada tanggal tertentu
     * Digunakan untuk streak calculation
     */
    fun getCompletedTasksByDate(selectedDate: Calendar): List<Task> {
        val selectedYear = selectedDate.get(Calendar.YEAR)
        val selectedMonth = selectedDate.get(Calendar.MONTH)
        val selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH)

        return completedTasks.filter { task ->
            val timeToUse = task.actionDateMillis ?: task.id
            val taskCalendar = Calendar.getInstance().apply { timeInMillis = timeToUse }

            taskCalendar.get(Calendar.YEAR) == selectedYear &&
                    taskCalendar.get(Calendar.MONTH) == selectedMonth &&
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    /**
     * Mengambil semua task aktif
     */
    fun getAllTasks(): List<Task> {
        return tasks.toList()
    }

    /**
     * Memproses task yang sudah melewati deadline (missed)
     * Dipanggil otomatis saat load tasks
     */
    fun processTasksForMissed() {
        val now = System.currentTimeMillis()
        val missedTime = now

        val tasksToKeep = mutableListOf<Task>()
        val updatedMissedTasks = mutableListOf<Task>()

        for (task in tasks) {
            val isFlowTimer = task.time.contains("(Flow)")

            val isMissed = if (isFlowTimer) {
                // Flow Timer: Cek berdasarkan tanggal task
                val taskDate = Calendar.getInstance().apply {
                    timeInMillis = task.id
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                taskDate.timeInMillis < now
            } else if (task.endTimeMillis != 0L) {
                // Time Range: Cek berdasarkan endTimeMillis
                task.endTimeMillis < now
            } else {
                // No Time: Cek berdasarkan tanggal task
                val taskDate = Calendar.getInstance().apply {
                    timeInMillis = task.id
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                taskDate.timeInMillis < now
            }

            if (isMissed) {
                updatedMissedTasks.add(task.copy(actionDateMillis = missedTime))
            } else {
                tasksToKeep.add(task)
            }
        }

        tasks.clear()
        tasks.addAll(tasksToKeep)
        missedTasks.addAll(updatedMissedTasks)
        saveAllData()
    }

    /**
     * Mengambil semua task yang missed
     * Diurutkan berdasarkan actionDateMillis terbaru
     */
    fun getMissedTasks(): List<Task> {
        return missedTasks.sortedByDescending {
            it.actionDateMillis ?: if (it.endTimeMillis != 0L) it.endTimeMillis else it.id
        }
    }

    /**
     * Menghapus task (soft delete)
     * Task dipindahkan ke deletedTasks
     */
    fun deleteTask(taskId: Long): Boolean {
        val taskToRemove = tasks.find { it.id == taskId }
        if (taskToRemove != null) {
            tasks.remove(taskToRemove)
            val deletedTask = taskToRemove.copy(actionDateMillis = System.currentTimeMillis())
            deletedTasks.add(0, deletedTask)
            saveAllData()
            return true
        }
        return false
    }

    /**
     * Mengambil semua task yang dihapus
     */
    fun getDeletedTasks(): List<Task> {
        return deletedTasks.toList()
    }

    /**
     * Reschedule task yang dihapus
     * Task dipindahkan dari deletedTasks ke tasks
     */
    fun rescheduleDeletedTask(taskId: Long, newTask: Task): Boolean {
        synchronized(deletedTasks) {
            val deletedTask = deletedTasks.find { it.id == taskId }
            if (deletedTask != null) {
                deletedTasks.remove(deletedTask)
                tasks.add(0, newTask)
                saveAllData()
                return true
            }
            return false
        }
    }

    /**
     * Mengambil task berdasarkan tanggal tertentu
     * Otomatis memproses missed tasks sebelumnya
     */
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

    /**
     * Cek apakah ada task pada tanggal tertentu
     * Digunakan untuk menampilkan titik indikator di kalender
     */
    fun hasTasksOnDate(date: Calendar): Boolean {
        val startOfDay = date.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)

        val startOfDayMillis = startOfDay.timeInMillis

        return tasks.any { task ->
            val taskCalendar = Calendar.getInstance().apply { timeInMillis = task.id }
            taskCalendar.set(Calendar.HOUR_OF_DAY, 0)
            taskCalendar.set(Calendar.MINUTE, 0)
            taskCalendar.set(Calendar.SECOND, 0)
            taskCalendar.set(Calendar.MILLISECOND, 0)

            taskCalendar.timeInMillis == startOfDayMillis
        }
    }

    /**
     * Mencari task berdasarkan query dan filter bulan
     * Otomatis memproses missed tasks sebelumnya
     */
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