package com.example.todolistapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Task(
    @DocumentId
    var id: String = "",
    var userId: String = "",

    var title: String = "",
    var details: String = "",
    var priority: String = "None",
    var category: String = "Home",

    var createdAt: Timestamp = Timestamp.now(),
    var dueDate: Timestamp = Timestamp.now(),

    var status: String = "pending",
    var flowDurationMillis: Long = 0,
    var completedAt: Timestamp? = null
) {
    // Constructor kosong yang diperlukan oleh Firestore
    constructor() : this("", "", "", "", "None", "Home", Timestamp.now(), Timestamp.now(), "pending", 0, null)
}