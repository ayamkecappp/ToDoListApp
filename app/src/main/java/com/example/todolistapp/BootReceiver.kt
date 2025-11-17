package com.example.todolistapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver untuk menangani boot completion dan menjadwalkan ulang notifikasi
 */
class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, rescheduling notifications")

            // Reschedule semua notifikasi setelah device reboot
            NotificationHelper.scheduleTaskReminderCheck(context)
            NotificationHelper.scheduleInactivityCheck(context)

            Log.d(TAG, "All notifications rescheduled after boot")
        }
    }
}