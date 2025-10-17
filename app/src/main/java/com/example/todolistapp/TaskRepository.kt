package com.example.todolistapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import com.google.firebase.Timestamp
import java.util.UUID
import kotlinx.coroutines.runBlocking
import android.util.Log
import java.util.Date

// Data Class Task yang kompatibel dengan Firestore
data class Task(
    // ID Unik (Document ID) - Harus var agar bisa diubah
    var id: String = UUID.randomUUID().toString(),
    // Field yang wajib ada untuk query/ownership - Harus var
    var userId: String = "",
    var status: String = "pending", // Status: pending, completed, missed, deleted
    // DueDate sebagai Timestamp (wajib untuk query Firebase) - Harus var
    var dueDate: Timestamp = Timestamp.now(),
    var completedAt: Timestamp? = null,
    var deletedAt: Timestamp? = null, // Tambahan untuk tracking delete time
    var missedAt: Timestamp? = null, // Tambahan untuk tracking missed time

    // Field dari Task lama
    val title: String = "",
    val time: String = "", // String representasi waktu (e.g., "10:00 - 11:00" atau "30m (Flow)")
    val category: String = "", // Location
    val priority: String = "None",
    val endTimeMillis: Long = 0L, // Digunakan hanya jika time adalah range/manual time
    val flowDurationMillis: Long = 0L, // Durasi Flow Timer
    val details: String = "",
) {
    // Konstruktor tanpa argumen untuk deserialisasi Firebase
    constructor() : this(id = UUID.randomUUID().toString())
}

object TaskRepository {
    private const val TAG = "TaskRepository"

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Helper untuk mendapatkan referensi ke koleksi task milik user yang sedang login
    private fun getTasksCollection() = auth.currentUser?.uid?.let { uid ->
        db.collection("users").document(uid).collection("tasks")
    }

    // ===============================================
    // SUSPEND FUNCTIONS (FIREBASE ASYNC)
    // ===============================================

    suspend fun addTask(task: Task) {
        val collection = getTasksCollection() ?: throw IllegalStateException("User not logged in.")
        if (auth.currentUser == null) throw IllegalStateException("User not logged in.")
        task.userId = auth.currentUser!!.uid
        collection.document(task.id).set(task).await()
        Log.d(TAG, "Task added/updated in Firestore: ${task.id}")
    }

    suspend fun updateTask(task: Task) {
        val collection = getTasksCollection() ?: throw IllegalStateException("User not logged in.")
        if (task.id.isNotEmpty()) {
            collection.document(task.id).set(task).await()
            Log.d(TAG, "Task updated in Firestore: ${task.id}")
        }
    }

    suspend fun getTaskById(taskId: String): Task? {
        val collection = getTasksCollection() ?: return null
        return try {
            val documentSnapshot = collection.document(taskId).get().await()
            documentSnapshot.toObject(Task::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching task by ID: $taskId", e)
            null
        }
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: String) {
        val collection = getTasksCollection() ?: return
        val updates = mutableMapOf<String, Any>("status" to newStatus)
        val now = Timestamp.now()

        if (newStatus == "completed") {
            updates["completedAt"] = now
        }
        if (newStatus == "deleted") {
            updates["deletedAt"] = now
        }
        if (newStatus == "missed") {
            updates["missedAt"] = now
        }

        collection.document(taskId).update(updates).await()
        Log.d(TAG, "Task status updated to $newStatus for ID: $taskId")
    }

    // Fungsi untuk MENDAPATKAN SEMUA task berdasarkan statusnya
    suspend fun getTasksByStatus(status: String): List<Task> {
        val collection = getTasksCollection() ?: return emptyList()
        return try {
            val snapshot = collection
                .whereEqualTo("status", status)
                .get()
                .await()
            snapshot.toObjects()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks by status: $status", e)
            emptyList()
        }
    }

    // Fungsi untuk MENDAPATKAN task pada tanggal tertentu (status pending/missed)
    suspend fun getTasksByDate(selectedDate: Calendar): List<Task> {
        val collection = getTasksCollection() ?: return emptyList()

        val startOfDay = (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfDay = (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startTimestamp = Timestamp(startOfDay.time)
        val endTimestamp = Timestamp(endOfDay.time)

        return try {
            val snapshot = collection
                .whereIn("status", listOf("pending", "missed"))
                .whereGreaterThanOrEqualTo("dueDate", startTimestamp)
                .whereLessThanOrEqualTo("dueDate", endTimestamp)
                .get()
                .await()
            snapshot.toObjects()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks by date", e)
            emptyList()
        }
    }

    // Fungsi untuk MENDAPATKAN task yang sudah selesai pada tanggal tertentu
    suspend fun getCompletedTasksByDate(selectedDate: Calendar): List<Task> {
        val collection = getTasksCollection() ?: return emptyList()

        val startOfDay = (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfDay = (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startTimestamp = Timestamp(startOfDay.time)
        val endTimestamp = Timestamp(endOfDay.time)

        return try {
            val snapshot = collection
                .whereEqualTo("status", "completed")
                .whereGreaterThanOrEqualTo("completedAt", startTimestamp)
                .whereLessThanOrEqualTo("completedAt", endTimestamp)
                .get()
                .await()
            snapshot.toObjects()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching completed tasks by date", e)
            emptyList()
        }
    }

    suspend fun updateMissedTasks() {
        val collection = getTasksCollection() ?: return
        try {
            val pendingTasksSnapshot = collection
                .whereEqualTo("status", "pending")
                .get()
                .await()

            val pendingTasks = pendingTasksSnapshot.toObjects(Task::class.java)
            val now = com.google.firebase.Timestamp.now()

            for (task in pendingTasks) {
                // Menggunakan waktu dueDate untuk perbandingan missed
                if (task.dueDate.toDate().time < now.toDate().time) {
                    collection.document(task.id).update("status", "missed", "missedAt", now).await()
                    Log.d(TAG, "Task marked as missed: ${task.id}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating missed tasks", e)
        }
    }

    // ===============================================
    // SYNCHRONOUS WRAPPER FUNCTIONS (Untuk kompatibilitas UI Thread)
    // ===============================================

    // Mengganti getCompletedTasks (untuk CompletedTasksActivity)
    fun getCompletedTasks(): List<Task> = runBlocking {
        getTasksByStatus("completed").sortedByDescending { it.completedAt?.toDate()?.time ?: 0L }
    }

    // Mengganti getMissedTasks (untuk MissedTasksActivity)
    fun getMissedTasks(): List<Task> = runBlocking {
        updateMissedTasks()
        getTasksByStatus("missed").sortedByDescending { it.missedAt?.toDate()?.time ?: 0L }
    }

    // Mengganti getDeletedTasks (untuk DeletedTasksActivity)
    fun getDeletedTasks(): List<Task> = runBlocking {
        getTasksByStatus("deleted").sortedByDescending { it.deletedAt?.toDate()?.time ?: 0L }
    }

    // Mengganti getTasksByDateSync untuk TaskActivity/CalendarActivity
    fun getTasksByDateSync(selectedDate: Calendar): List<Task> = runBlocking {
        updateMissedTasks()
        getTasksByDate(selectedDate)
    }

    // Mengganti processTasksForMissed() untuk Compatibility
    fun processTasksForMissed() = runBlocking {
        updateMissedTasks()
    }

    // Mengganti completeTask(taskId: Long) untuk Compatibility (ID sekarang String)
    fun completeTask(taskId: String): Boolean = runBlocking {
        try {
            updateTaskStatus(taskId, "completed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete task: $taskId", e)
            false
        }
    }

    // Mengganti deleteTask(taskId: Long) untuk Compatibility (ID sekarang String)
    fun deleteTask(taskId: String): Boolean = runBlocking {
        try {
            updateTaskStatus(taskId, "deleted")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete task: $taskId", e)
            false
        }
    }

    // Mengganti getTaskById(taskId: Long) untuk Compatibility (ID sekarang String)
    fun getTaskByIdSync(taskId: String): Task? = runBlocking {
        getTaskById(taskId)
    }

    // Mengganti updateTask(originalTaskId: Long, updatedTask: Task) untuk Compatibility
    fun updateTaskSync(originalTaskId: String, updatedTask: Task): Boolean = runBlocking {
        try {
            val existingTask = getTaskById(originalTaskId)
            if (existingTask == null) return@runBlocking false

            if (originalTaskId != updatedTask.id) {
                // Skenario Reschedule (ID baru)
                db.collection("users").document(auth.currentUser!!.uid).collection("tasks").document(originalTaskId).delete().await()
                addTask(updatedTask)
            } else {
                // Skenario Update Konten (ID tetap)
                updateTask(updatedTask)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update task sync: $originalTaskId", e)
            false
        }
    }

    // Mengganti hasTasksOnDate untuk CalendarActivity
    fun hasTasksOnDate(date: Calendar): Boolean = runBlocking {
        // Hanya perlu task pending
        val pendingTasks = getTasksByDate(date).filter { it.status == "pending" }
        pendingTasks.isNotEmpty()
    }

    // Mengganti searchTasks untuk SearchFilterActivity
    fun searchTasks(query: String, monthFilter: Int): List<Task> = runBlocking {
        // Logika sederhana: ambil semua pending/missed lalu filter di klien
        val allTasks = getTasksByStatus("pending") + getTasksByStatus("missed")

        val lowerCaseQuery = query.trim().lowercase()

        return@runBlocking allTasks.filter { task ->
            val matchesTitle = task.title.lowercase().contains(lowerCaseQuery)

            // Menggunakan extension function
            val taskMonth = task.dueDate.toDate().get(Calendar.MONTH)
            val matchesMonth = if (monthFilter == -1) {
                true
            } else {
                taskMonth == monthFilter
            }

            matchesTitle && matchesMonth
        }
    }

    // Mengganti rescheduleDeletedTask untuk DeletedTasksActivity (Reschedule/Restore)
    fun rescheduleDeletedTask(taskId: String, newTask: Task): Boolean = runBlocking {
        try {
            // Hapus dokumen lama (status deleted)
            db.collection("users").document(auth.currentUser!!.uid).collection("tasks").document(taskId).delete().await()
            // Tambahkan sebagai task baru (status pending)
            addTask(newTask.copy(status = "pending"))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule deleted task: $taskId", e)
            false
        }
    }
}

// Extension function untuk mengonversi Date ke Calendar
fun Date.get(field: Int): Int {
    val cal = Calendar.getInstance()
    cal.time = this
    return cal.get(field)
}