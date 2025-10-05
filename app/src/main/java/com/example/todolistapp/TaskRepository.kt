package com.example.todolistapp

import java.util.Collections
import java.util.Calendar

data class Task(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val time: String,
    val category: String,
    val priority: String,
    val endTimeMillis: Long = 0L,
    val monthAdded: Int = Calendar.getInstance().apply { timeInMillis = id }.get(Calendar.MONTH)
)

object TaskRepository {
    private val tasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    private val deletedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    private val missedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())
    private val completedTasks: MutableList<Task> = Collections.synchronizedList(mutableListOf())

    fun addTask(task: Task) {
        tasks.add(0, task)
    }

    fun getTaskById(taskId: Long): Task? {
        val activeTask = tasks.find { it.id == taskId }
        if (activeTask != null) return activeTask
        return missedTasks.find { it.id == taskId }
    }

    fun updateTask(originalTaskId: Long, updatedTask: Task): Boolean {
        synchronized(tasks) {
            // 1. Cari dan hapus tugas asli dari daftar aktif
            val index = tasks.indexOfFirst { it.id == originalTaskId }
            if (index != -1) {
                tasks.removeAt(index)
                tasks.add(0, updatedTask) // Tambahkan tugas baru (dengan ID/tanggal yang mungkin baru)
                return true
            }

            // 2. Cari dan hapus dari tugas yang terlewat (jika pengguna mengedit tugas yang terlewat)
            val missedIndex = missedTasks.indexOfFirst { it.id == originalTaskId }
            if (missedIndex != -1) {
                missedTasks.removeAt(missedIndex)
                tasks.add(0, updatedTask) // Pindahkan kembali ke daftar aktif untuk tanggal barunya
                return true
            }

            return false
        }
    }


    fun completeTask(taskId: Long): Boolean {
        val taskToRemove = tasks.find { it.id == taskId }
        if (taskToRemove != null) {
            tasks.remove(taskToRemove)
            completedTasks.add(0, taskToRemove)
            return true
        }
        return false
    }

    fun getCompletedTasks(): List<Task> {
        return completedTasks.toList()
    }

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

    fun getAllTasks(): List<Task> {
        return tasks.toList()
    }

    fun processTasksForMissed() {
        val now = System.currentTimeMillis()
        val missed = tasks.filter { it.endTimeMillis != 0L && it.endTimeMillis < now }

        tasks.removeAll(missed)
        missedTasks.addAll(missed)
    }

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

    fun getTasksByDate(selectedDate: Calendar): List<Task> {
        processTasksForMissed()

        val selectedYear = selectedDate.get(Calendar.YEAR)
        val selectedMonth = selectedDate.get(Calendar.MONTH)
        val selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH)

        return tasks.filter { task ->
            // task.id merepresentasikan timestamp hari saat tugas dibuat
            val taskCalendar = Calendar.getInstance().apply { timeInMillis = task.id }

            taskCalendar.get(Calendar.YEAR) == selectedYear &&
                    taskCalendar.get(Calendar.MONTH) == selectedMonth &&
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    // NEW FUNCTION: Check if a day has any active task
    fun hasTasksOnDate(date: Calendar): Boolean {
        // Normalize the input date to start of day
        val startOfDay = date.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)

        val startOfDayMillis = startOfDay.timeInMillis

        // Iterate over active tasks
        return tasks.any { task ->
            // Normalize task ID to start of day for accurate comparison
            val taskCalendar = Calendar.getInstance().apply { timeInMillis = task.id }
            taskCalendar.set(Calendar.HOUR_OF_DAY, 0)
            taskCalendar.set(Calendar.MINUTE, 0)
            taskCalendar.set(Calendar.SECOND, 0)
            taskCalendar.set(Calendar.MILLISECOND, 0)

            taskCalendar.timeInMillis == startOfDayMillis
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