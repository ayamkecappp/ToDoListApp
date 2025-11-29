// main/java/com/example/todolistapp/TimyApplication.kt
package com.example.todolistapp

import android.app.Application

class TimyApplication : Application() {

    companion object {
        // Flag untuk menandai apakah aplikasi baru dibuka (process dimulai)
        var isAppJustLaunched = true

        // Flag untuk menandai user baru saja login
        var isJustLoggedIn = false
    }

    override fun onCreate() {
        super.onCreate()
        isAppJustLaunched = true
    }
}