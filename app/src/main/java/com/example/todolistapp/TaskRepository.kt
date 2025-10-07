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
    val monthAdded: Int = Calendar.getInstance().apply { timeInMillis = id }.get(Calendar.MONTH),
    val flowDurationMillis: Long = 0L,
    val actionDateMillis: Long? = null
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

        val missedTask = missedTasks.find { it.id == taskId }
        if (missedTask != null) return missedTask

        return deletedTasks.find { it.id == taskId }
    }

    fun updateTask(originalTaskId: Long, updatedTask: Task): Boolean {
        synchronized(tasks) {
            val activeIndex = tasks.indexOfFirst { it.id == originalTaskId }
            if (activeIndex != -1) {
                tasks.removeAt(activeIndex)
                tasks.add(0, updatedTask)
                return true
            }

            val missedIndex = missedTasks.indexOfFirst { it.id == originalTaskId }
            if (missedIndex != -1) {
                missedTasks.removeAt(missedIndex)
                tasks.add(0, updatedTask)
                return true
            }

            val deletedIndex = deletedTasks.indexOfFirst { it.id == originalTaskId }
            if (deletedIndex != -1) {
                deletedTasks.removeAt(deletedIndex)
                tasks.add(0, updatedTask)
                return true
            }

            return false
        }
    }

    fun completeTask(taskId: Long): Boolean {
        val taskToRemove = tasks.find { it.id == taskId }
        if (taskToRemove != null) {
            tasks.remove(taskToRemove)
            val completedTask = taskToRemove.copy(actionDateMillis = System.currentTimeMillis())
            completedTasks.add(0, completedTask)
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
            val timeToUse = task.actionDateMillis ?: task.id
            val taskCalendar = Calendar.getInstance().apply { timeInMillis = timeToUse }

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
        val missedTime = now

        val tasksToKeep = mutableListOf<Task>()
        val updatedMissedTasks = mutableListOf<Task>()

        for (task in tasks) {
            // ✅ FIX: Cek apakah task menggunakan Flow Timer
            val isFlowTimer = task.time.contains("(Flow)")

            val isMissed = if (isFlowTimer) {
                // ✅ Flow Timer: Hanya cek berdasarkan tanggal task, BUKAN endTimeMillis
                val taskDate = Calendar.getInstance().apply {
                    timeInMillis = task.id
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                taskDate.timeInMillis < now
            } else if (task.endTimeMillis != 0L) {
                // ✅ Time Range: Cek berdasarkan endTimeMillis
                task.endTimeMillis < now
            } else {
                // ✅ No Time: Cek berdasarkan tanggal task
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
    }

    fun getMissedTasks(): List<Task> {
        return missedTasks.sortedByDescending {
            it.actionDateMillis ?: if (it.endTimeMillis != 0L) it.endTimeMillis else it.id
        }
    }

    fun deleteTask(taskId: Long): Boolean {
        val taskToRemove = tasks.find { it.id == taskId }
        if (taskToRemove != null) {
            tasks.remove(taskToRemove)
            val deletedTask = taskToRemove.copy(actionDateMillis = System.currentTimeMillis())
            deletedTasks.add(0, deletedTask)
            return true
        }
        return false
    }

    fun getDeletedTasks(): List<Task> {
        return deletedTasks.toList()
    }

    fun rescheduleDeletedTask(taskId: Long, newTask: Task): Boolean {
        synchronized(deletedTasks) {
            val deletedTask = deletedTasks.find { it.id == taskId }
            if (deletedTask != null) {
                deletedTasks.remove(deletedTask)
                tasks.add(0, newTask)
                return true
            }
            return false
        }
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