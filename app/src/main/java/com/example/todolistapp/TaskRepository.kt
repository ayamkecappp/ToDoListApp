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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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
    } ?: run {
        Log.e(TAG, "User not logged in or UID is null.")
        null
    }

    // ===============================================
    // SUSPEND FUNCTIONS (FIREBASE ASYNC)
    // ===============================================

    suspend fun addTask(task: Task) = withContext(Dispatchers.IO) {
        val collection = getTasksCollection() ?: throw IllegalStateException("User not logged in.")
        if (auth.currentUser == null) throw IllegalStateException("User not logged in.")
        task.userId = auth.currentUser!!.uid
        collection.document(task.id).set(task).await()
        Log.d(TAG, "Task added/updated in Firestore: ${task.id}")
    }

    suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        val collection = getTasksCollection() ?: return@withContext
        if (task.id.isNotEmpty()) {
            collection.document(task.id).set(task).await()
            Log.d(TAG, "Task updated in Firestore: ${task.id}")
        }
    }

    suspend fun getTaskById(taskId: String): Task? = withContext(Dispatchers.IO) {
        val collection = getTasksCollection() ?: return@withContext null
        return@withContext try {
            val documentSnapshot = collection.document(taskId).get().await()
            documentSnapshot.toObject(Task::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching task by ID: $taskId", e)
            null
        }
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: String) = withContext(Dispatchers.IO) {
        val collection = getTasksCollection() ?: return@withContext
        val updates = mutableMapOf<String, Any>("status" to newStatus)
        val now = Timestamp.now()

        if (newStatus == "completed") {
            updates["completedAt"] = now
        } else {
            updates.remove("completedAt")
        }

        if (newStatus == "deleted") {
            updates["deletedAt"] = now
        } else {
            updates.remove("deletedAt")
        }

        if (newStatus == "missed") {
            updates["missedAt"] = now
        } else {
            updates.remove("missedAt")
        }

        collection.document(taskId).update(updates).await()
        Log.d(TAG, "Task status updated to $newStatus for ID: $taskId")
    }

    // Fungsi untuk MENDAPATKAN SEMUA task berdasarkan statusnya
    suspend fun getTasksByStatus(status: String): List<Task> = withContext(Dispatchers.IO) {
        val collection = getTasksCollection() ?: return@withContext emptyList()
        return@withContext try {
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

    /**
     * HANYA mengambil tugas dengan status PENDING pada tanggal tertentu (untuk daftar utama TaskActivity).
     */
    suspend fun getTasksByDate(selectedDate: Calendar): List<Task> = withContext(Dispatchers.IO) {
        val collection = getTasksCollection() ?: return@withContext emptyList()

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

        return@withContext try {
            val snapshot = collection
                .whereIn("status", listOf("pending"))
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

    /**
     * HANYA mengambil tugas dengan status PENDING pada tanggal tertentu (untuk indikator kalender TaskActivity).
     */
    suspend fun getTasksForDateIndicator(selectedDate: Calendar): List<Task> = withContext(Dispatchers.IO) {
        val collection = getTasksCollection() ?: return@withContext emptyList()

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

        return@withContext try {
            val snapshot = collection
                .whereIn("status", listOf("pending"))
                .whereGreaterThanOrEqualTo("dueDate", startTimestamp)
                .whereLessThanOrEqualTo("dueDate", endTimestamp)
                .get()
                .await()
            snapshot.toObjects()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks by date for indicator", e)
            emptyList()
        }
    }

    /**
     * Mengambil semua tugas PENDING dalam rentang 3 bulan (untuk CalendarActivity).
     */
    suspend fun getTasksInDateRangeForCalendar(currentMonth: Calendar): List<Task> = withContext(Dispatchers.IO) {
        val collection = getTasksCollection() ?: return@withContext emptyList()

        // 1. Tentukan tanggal mulai (Awal bulan sebelumnya)
        val startCal = currentMonth.clone() as Calendar
        startCal.add(Calendar.MONTH, -1)
        startCal.set(Calendar.DAY_OF_MONTH, 1)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        // 2. Tentukan tanggal akhir (Akhir bulan setelahnya)
        val endCal = currentMonth.clone() as Calendar
        endCal.add(Calendar.MONTH, 1)
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)

        val startTimestamp = Timestamp(startCal.time)
        val endTimestamp = Timestamp(endCal.time)

        return@withContext try {
            val snapshot = collection
                .whereIn("status", listOf("pending"))
                .whereGreaterThanOrEqualTo("dueDate", startTimestamp)
                .whereLessThanOrEqualTo("dueDate", endTimestamp)
                .get()
                .await()
            snapshot.toObjects()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks in date range", e)
            emptyList()
        }
    }


    // Fungsi untuk MENDAPATKAN task yang sudah selesai pada tanggal tertentu
    suspend fun getCompletedTasksByDate(selectedDate: Calendar): List<Task> = withContext(Dispatchers.IO) {
        val collection = getTasksCollection() ?: return@withContext emptyList()

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

        return@withContext try {
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

    /**
     * Memproses tugas yang terlewat (missed) dengan memeriksa semua tugas PENDING.
     */
    suspend fun updateMissedTasks() = withContext(Dispatchers.IO) {
        val collection = getTasksCollection() ?: return@withContext
        try {
            // Hanya cek tugas yang statusnya PENDING
            val pendingTasksSnapshot = collection
                .whereEqualTo("status", "pending")
                .get()
                .await()

            val pendingTasks = pendingTasksSnapshot.toObjects(Task::class.java)
            val now = com.google.firebase.Timestamp.now()
            var batch = db.batch()
            var batchCount = 0

            for (task in pendingTasks) {
                // Menggunakan waktu dueDate untuk perbandingan missed
                if (task.dueDate.toDate().time < now.toDate().time) {
                    val missedTaskRef = collection.document(task.id)
                    batch.update(missedTaskRef, "status", "missed", "missedAt", now)
                    batchCount++
                    Log.d(TAG, "Task batched for missed: ${task.id}")
                }

                if (batchCount >= 490) { // Commit batch jika mendekati batas (500)
                    batch.commit().await()
                    batch = db.batch()
                    batchCount = 0
                    Log.d(TAG, "Committed batch of missed tasks.")
                }
            }
            // Commit sisa batch
            if (batchCount > 0) {
                batch.commit().await()
                Log.d(TAG, "Committed final batch of missed tasks.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating missed tasks", e)
        }
    }

    // ===============================================
    // SYNCHRONOUS WRAPPER FUNCTIONS (Menggunakan runBlocking untuk kompatibilitas)
    // ===============================================

    fun getCompletedTasks(): List<Task> = runBlocking {
        withContext(Dispatchers.IO) { // Pastikan operasi berjalan di IO
            // Pastikan urutan descending
            getTasksByStatus("completed").sortedByDescending { it.completedAt?.toDate()?.time ?: 0L }
        }
    }

    fun getMissedTasks(): List<Task> = runBlocking {
        withContext(Dispatchers.IO) {
            updateMissedTasks() // Pastikan update berjalan di IO
            // Pastikan urutan descending
            getTasksByStatus("missed").sortedByDescending { it.missedAt?.toDate()?.time ?: 0L }
        }
    }

    fun getDeletedTasks(): List<Task> = runBlocking {
        withContext(Dispatchers.IO) {
            // Pastikan urutan descending
            getTasksByStatus("deleted").sortedByDescending { it.deletedAt?.toDate()?.time ?: 0L }
        }
    }

    fun getTasksByDateSync(selectedDate: Calendar): List<Task> = runBlocking {
        withContext(Dispatchers.IO) {
            updateMissedTasks() // Pastikan update berjalan di IO
            getTasksByDate(selectedDate)
        }
    }

    fun processTasksForMissed() = runBlocking {
        updateMissedTasks() // Pastikan update berjalan di IO
    }

    fun completeTask(taskId: String): Boolean = runBlocking {
        withContext(Dispatchers.IO) {
            try {
                updateTaskStatus(taskId, "completed")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete task: $taskId", e)
                false
            }
        }
    }

    fun deleteTask(taskId: String): Boolean = runBlocking {
        withContext(Dispatchers.IO) {
            try {
                updateTaskStatus(taskId, "deleted")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete task: $taskId", e)
                false
            }
        }
    }

    fun getTaskByIdSync(taskId: String): Task? = runBlocking {
        getTaskById(taskId)
    }

    /**
     * Mengelola pembaruan/pemindahan tugas.
     * Jika ID tugas lama (originalTaskId) berbeda dengan ID tugas baru (updatedTask.id),
     * itu dianggap sebagai pemindahan (reschedule), dan tugas lama akan dihapus.
     */
    fun updateTaskSync(originalTaskId: String, updatedTask: Task): Boolean = runBlocking {
        withContext(Dispatchers.IO) {
            try {
                val existingTask = getTaskById(originalTaskId)
                if (existingTask == null) return@withContext false

                if (originalTaskId != updatedTask.id) {
                    // Skenario Reschedule/Pindah Tanggal (ID baru) - Hapus yang lama, buat yang baru
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
    }

    /**
     * Mengecek apakah ada tugas PENDING pada tanggal tertentu.
     */
    fun hasTasksOnDate(date: Calendar): Boolean = runBlocking {
        withContext(Dispatchers.IO) {
            // Panggil update missed agar data lebih akurat
            updateMissedTasks()
            // Menggunakan fungsi untuk indikator kalender
            val tasks = getTasksForDateIndicator(date)
            tasks.isNotEmpty()
        }
    }

    // Mengganti searchTasks untuk SearchFilterActivity
    fun searchTasks(query: String, monthFilter: Int): List<Task> = runBlocking {
        withContext(Dispatchers.IO) {
            // Hanya mengambil tugas dengan status "pending"
            val allTasks = getTasksByStatus("pending")

            val lowerCaseQuery = query.trim().lowercase()

            return@withContext allTasks.filter { task ->
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
    }

    // Mengganti rescheduleDeletedTask untuk DeletedTasksActivity (Reschedule/Restore)
    fun rescheduleDeletedTask(taskId: String, newTask: Task): Boolean = runBlocking {
        withContext(Dispatchers.IO) {
            try {
                // Hapus dokumen lama (status deleted)
                db.collection("users").document(auth.currentUser!!.uid).collection("tasks").document(taskId).delete().await()
                // Tambahkan sebagai task baru (status pending)
                addTask(newTask.copy(status = "pending", deletedAt = null)) // Pastikan deletedAt di-reset
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule deleted task: $taskId", e)
                false
            }
        }
    }
}

// Extension function untuk mengonversi Date ke Calendar
fun Date.get(field: Int): Int {
    val cal = Calendar.getInstance()
    cal.time = this
    return cal.get(field)
}