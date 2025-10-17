package com.example.todolistapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects // << SOLUSI ERROR 4
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import com.google.firebase.Timestamp
import com.example.todolistapp.Task // << SOLUSI ERROR 1

object  TaskRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Helper untuk mendapatkan referensi ke koleksi task milik user yang sedang login
    private fun getTasksCollection() = auth.currentUser?.uid?.let { uid ->
        db.collection("users").document(uid).collection("tasks")
    }

    // Fungsi untuk MENAMBAHKAN task baru
    suspend fun addTask(task: Task) {
        val collection = getTasksCollection() ?: return
        // SOLUSI ERROR 2 (pastikan 'userId' ada di Task.kt)
        task.userId = auth.currentUser!!.uid
        collection.add(task).await()
    }

    // Fungsi untuk MENDAPATKAN SEMUA task berdasarkan statusnya
    suspend fun getTasksByStatus(status: String): List<Task> {
        val collection = getTasksCollection() ?: return emptyList()
        return try {
            val snapshot = collection
                .whereEqualTo("status", status)
                .get()
                .await()
            // SOLUSI ERROR 3 (akan teratasi setelah Task diimpor)
            snapshot.toObjects(Task::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Mengambil semua tugas (pending, completed, dll) pada tanggal tertentu.
     */
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
                .whereGreaterThanOrEqualTo("dueDate", startTimestamp)
                .whereLessThanOrEqualTo("dueDate", endTimestamp)
                .get()
                .await()
            snapshot.toObjects() // Menggunakan toObjects dari ktx
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Mengambil hanya tugas yang sudah SELESAI pada tanggal tertentu.
     */
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
            snapshot.toObjects() // Menggunakan toObjects dari ktx
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Fungsi untuk MEMPERBARUI task yang sudah ada
    suspend fun updateTask(task: Task) {
        val collection = getTasksCollection() ?: return
        if (task.id.isNotEmpty()) {
            collection.document(task.id).set(task).await()
        }
    }

    // Fungsi untuk MENGUBAH STATUS task (untuk complete, delete, dll)
    suspend fun updateTaskStatus(taskId: String, newStatus: String) {
        val collection = getTasksCollection() ?: return
        val updates = mutableMapOf<String, Any>("status" to newStatus)
        if (newStatus == "completed") {
            updates["completedAt"] = Timestamp.now()
        }
        collection.document(taskId).update(updates).await()
    }

    suspend fun getTaskById(taskId: String): Task? {
        val collection = getTasksCollection() ?: return null
        return try {
            val documentSnapshot = collection.document(taskId).get().await()
            documentSnapshot.toObject(Task::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateMissedTasks() {
        val collection = getTasksCollection() ?: return
        try {
            // 1. Ambil semua tugas yang statusnya masih 'pending'
            val pendingTasksSnapshot = collection
                .whereEqualTo("status", "pending")
                .get()
                .await()

            val pendingTasks = pendingTasksSnapshot.toObjects(Task::class.java)
            val now = com.google.firebase.Timestamp.now()

            // 2. Lakukan perulangan dan cek tanggalnya
            for (task in pendingTasks) {
                if (task.dueDate < now) {
                    // 3. Jika sudah lewat, update statusnya menjadi 'missed'
                    collection.document(task.id).update("status", "missed").await()
                }
            }
        } catch (e: Exception) {
            // Tangani error jika ada, misalnya dengan logging
            android.util.Log.e("TaskRepository", "Error updating missed tasks", e)
        }
    }


}