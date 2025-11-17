package com.example.todolistapp

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.Timestamp
import java.util.*

object NotificationHelper {
    private const val TAG = "NotificationHelper"

    // Channel IDs
    const val CHANNEL_ID_TASK_REMINDER = "task_reminder_channel"
    const val CHANNEL_ID_INACTIVITY = "inactivity_reminder_channel"

    // Notification IDs
    const val NOTIFICATION_ID_TASK = 1001
    const val NOTIFICATION_ID_INACTIVITY = 1002

    // Action Keys
    const val ACTION_TASK_REMINDER = "com.example.todolistapp.TASK_REMINDER"
    const val ACTION_INACTIVITY_REMINDER = "com.example.todolistapp.INACTIVITY_REMINDER"

    // Extra Keys
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_COUNT = "extra_task_count"

    // Request Codes
    private const val REQUEST_CODE_TASK_REMINDER = 2001
    private const val REQUEST_CODE_INACTIVITY = 2002

    // Inactivity threshold (3 hari dalam milidetik)
    private const val INACTIVITY_THRESHOLD_MILLIS = 3L * 24 * 60 * 60 * 1000

    /**
     * Membuat notification channels (dipanggil saat aplikasi pertama kali dijalankan)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel untuk Task Reminder
            val taskChannel = NotificationChannel(
                CHANNEL_ID_TASK_REMINDER,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming tasks"
                enableVibration(true)
                enableLights(true)
            }

            // Channel untuk Inactivity Reminder
            val inactivityChannel = NotificationChannel(
                CHANNEL_ID_INACTIVITY,
                "Inactivity Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders when you haven't used the app"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(taskChannel)
            notificationManager.createNotificationChannel(inactivityChannel)

            Log.d(TAG, "Notification channels created")
        }
    }

    /**
     * Menampilkan notifikasi untuk task reminder
     */
    fun showTaskReminderNotification(context: Context, taskTitle: String, taskCount: Int) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = if (taskCount == 1) {
            "You have a task due tomorrow: $taskTitle"
        } else {
            "You have $taskCount tasks due tomorrow, including: $taskTitle"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TASK_REMINDER)
            .setSmallIcon(R.drawable.ic_timy_tasks)
            .setContentTitle("Upcoming Task Reminder")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_TASK, notification)
        }

        Log.d(TAG, "Task reminder notification shown: $taskTitle")
    }

    /**
     * Menampilkan notifikasi untuk inactivity reminder
     */
    fun showInactivityNotification(context: Context) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_INACTIVITY)
            .setSmallIcon(R.drawable.ic_timy_tasks)
            .setContentTitle("We miss you! 👋")
            .setContentText("You haven't checked your tasks recently. Let's stay productive!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You haven't checked your tasks recently. Let's stay productive and achieve your goals!"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_INACTIVITY, notification)
        }

        Log.d(TAG, "Inactivity reminder notification shown")
    }

    /**
     * Menjadwalkan pengecekan task reminder harian (jam 8 malam)
     */
    fun scheduleTaskReminderCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TASK_REMINDER,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Jadwalkan untuk jam 8 malam setiap hari
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Jika sudah lewat jam 8 malam hari ini, jadwalkan untuk besok
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Gunakan setRepeating untuk alarm harian
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Log.d(TAG, "Task reminder check scheduled for ${calendar.time}")
    }

    /**
     * Menjadwalkan pengecekan inactivity (setiap 24 jam)
     */
    fun scheduleInactivityCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_INACTIVITY_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_INACTIVITY,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Cek setiap 24 jam
        val triggerTime = System.currentTimeMillis() + AlarmManager.INTERVAL_DAY

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Log.d(TAG, "Inactivity check scheduled")
    }

    /**
     * Membatalkan semua notifikasi yang dijadwalkan
     */
    fun cancelAllScheduledNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel task reminder
        val taskIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
        }
        val taskPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TASK_REMINDER,
            taskIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        taskPendingIntent?.let { alarmManager.cancel(it) }

        // Cancel inactivity reminder
        val inactivityIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_INACTIVITY_REMINDER
        }
        val inactivityPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_INACTIVITY,
            inactivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        inactivityPendingIntent?.let { alarmManager.cancel(it) }

        Log.d(TAG, "All scheduled notifications cancelled")
    }

    /**
     * Update waktu terakhir aplikasi dibuka (untuk tracking inactivity)
     */
    fun updateLastAppOpenTime(context: Context) {
        val prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_app_open_time", System.currentTimeMillis()).apply()
        Log.d(TAG, "Last app open time updated")
    }

    /**
     * Mendapatkan waktu terakhir aplikasi dibuka
     */
    fun getLastAppOpenTime(context: Context): Long {
        val prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        return prefs.getLong("last_app_open_time", System.currentTimeMillis())
    }

    /**
     * Cek apakah user sudah tidak aktif (>3 hari)
     */
    fun isUserInactive(context: Context): Boolean {
        val lastOpenTime = getLastAppOpenTime(context)
        val timeSinceLastOpen = System.currentTimeMillis() - lastOpenTime
        return timeSinceLastOpen >= INACTIVITY_THRESHOLD_MILLIS
    }

    /**
     * Mendapatkan task yang jatuh tempo besok (H-1)
     */
    suspend fun getTasksDueTomorrow(context: Context): List<Task> {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return TaskRepository.getTasksByDate(tomorrow)
    }
}