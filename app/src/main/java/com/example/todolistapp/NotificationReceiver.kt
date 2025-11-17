package com.example.todolistapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationReceiver : BroadcastReceiver() {

    private val TAG = "NotificationReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")

        when (intent.action) {
            NotificationHelper.ACTION_TASK_REMINDER -> {
                handleTaskReminder(context)
            }
            NotificationHelper.ACTION_INACTIVITY_REMINDER -> {
                handleInactivityReminder(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule notifications after device reboot
                NotificationHelper.scheduleTaskReminderCheck(context)
                NotificationHelper.scheduleInactivityCheck(context)
                Log.d(TAG, "Notifications rescheduled after boot")
            }
        }
    }

    /**
     * Handle task reminder - cek apakah ada task yang jatuh tempo besok
     */
    private fun handleTaskReminder(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasksDueTomorrow = NotificationHelper.getTasksDueTomorrow(context)

                if (tasksDueTomorrow.isNotEmpty()) {
                    // Ambil task paling pertama (paling mendekati)
                    val closestTask = tasksDueTomorrow.sortedBy { it.endTimeMillis }.firstOrNull()

                    closestTask?.let { task ->
                        withContext(Dispatchers.Main) {
                            NotificationHelper.showTaskReminderNotification(
                                context,
                                task.title,
                                tasksDueTomorrow.size
                            )
                        }
                    }

                    Log.d(TAG, "Task reminder sent for ${tasksDueTomorrow.size} tasks")
                } else {
                    Log.d(TAG, "No tasks due tomorrow")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling task reminder", e)
            }
        }
    }

    /**
     * Handle inactivity reminder - cek apakah user sudah tidak aktif >3 hari
     */
    private fun handleInactivityReminder(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (NotificationHelper.isUserInactive(context)) {
                    withContext(Dispatchers.Main) {
                        NotificationHelper.showInactivityNotification(context)
                    }
                    Log.d(TAG, "Inactivity reminder sent")
                } else {
                    Log.d(TAG, "User is still active, no inactivity reminder needed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling inactivity reminder", e)
            }
        }
    }
}