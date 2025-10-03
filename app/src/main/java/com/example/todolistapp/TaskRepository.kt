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