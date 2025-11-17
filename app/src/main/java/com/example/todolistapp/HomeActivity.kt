// main/java/com/example/todolistapp/HomeActivity.kt
package com.example.todolistapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import android.util.Log
import android.view.ViewGroup
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Button
import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// TAMBAHKAN IMPOR FIREBASE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
// END TAMBAH

// HAPUS DEFINISI DUPLIKASI STREAKSTATE DI SINI.
// ASUMSI: StreakState diimpor dari TaskRepository.kt atau file model bersama.

class HomeActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvTimyChatText: TextView
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var chatJob: Job? = null

    private lateinit var tvStreak: TextView
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var dayProgressBars: List<ProgressBar>
    private lateinit var dayEllipses: List<View>
    private lateinit var dayRunnerIcons: List<View>

    // HAPUS KONSTANTA SHARED PREFERENCES UNTUK USERNAME
    // private val PROFILE_PREFS_NAME = "ProfilePrefs"
    // private val KEY_USERNAME = "username"

    // HAPUS SEMUA DEKLARASI SHAREDPREFERENCES UNTUK STREAK
    // private val PREFS_NAME = "TimyTimePrefs"
    // private val KEY_STREAK = "current_streak"
    // private val KEY_LAST_DATE = "last_completion_date"
    // private val KEY_STREAK_DAYS = "streak_days"
    // private lateinit var prefs: SharedPreferences


    private fun getTimyMessages(userName: String): List<Spanned> {
        val boldUserName = "<b>$userName</b>"
        val rawMessages = listOf(
            "Hi! I'm Timy,\nYour Personal Time Assistant",
            "Nice to see you,\n$boldUserName",
            "Let's plan your day with me,\nTimy!",
        )
        return rawMessages.map { rawMessage ->
            Html.fromHtml(rawMessage, android.text.Html.FROM_HTML_MODE_LEGACY)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        // Inisialisasi notification channels
        NotificationHelper.createNotificationChannels(this)

        // Jadwalkan notification checks
        NotificationHelper.scheduleTaskReminderCheck(this)
        NotificationHelper.scheduleInactivityCheck(this)

        // Request notification permission untuk Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // HAPUS INISIALISASI SHAREDPREFERENCES UNTUK STREAK
        // prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        tvTitle = findViewById(R.id.tvTitle)
        tvTimyChatText = findViewById(R.id.tvTimyChatText)
        tvStreak = findViewById(R.id.tvStreak)
        bottomNav = findViewById(R.id.bottomNav)

        bottomNav.itemIconTintList = null
        initializeViews()
        loadTimyGifs()

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_tasks -> {
                    startActivity(
                        Intent(this, TaskActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    )
                    true
                }
                R.id.nav_profile -> {
                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun initializeViews() {
        dayProgressBars = listOf(findViewById(R.id.full_week_progress_bar))

        dayEllipses = listOf(
            findViewById(R.id.day_marker_0),
            findViewById(R.id.day_marker_1),
            findViewById(R.id.day_marker_2),
            findViewById(R.id.day_marker_3),
            findViewById(R.id.day_marker_4),
            findViewById(R.id.day_marker_5),
            findViewById(R.id.day_marker_6)
        )

        dayRunnerIcons = listOf(findViewById(R.id.runner_icon))
    }

    override fun onResume() {
        super.onResume()

        NotificationHelper.updateLastAppOpenTime(this)

        checkAndUpdateStreak()
        updateWeeklyProgressUI()
        bottomNav.selectedItemId = R.id.nav_home
        startChatLoop() // Memanggil fungsi fetch username dari Firebase
    }

    override fun onPause() {
        super.onPause()
        stopChatLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // FUNGSI BARU UNTUK MENGAMBIL USERNAME DARI FIREBASE
    private suspend fun fetchUserNameFromFirebase(): String {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        return if (userId != null) {
            try {
                val db = FirebaseFirestore.getInstance()
                // Mengambil data secara sinkron menggunakan await()
                val document = db.collection("users").document(userId).get().await()

                if (document.exists()) {
                    // Mengambil username dari Firestore, default ke "Guest"
                    val username = document.getString("username") ?: "Guest"
                    username
                } else {
                    "Guest" // Dokumen user tidak ditemukan
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error fetching username from Firebase", e)
                "Guest" // Error fetching data
            }
        } else {
            "Guest" // User tidak login
        }
    }
    // FUNGSI startChatLoop() DIMODIFIKASI UNTUK MENGGUNAKAN COROUTINE UNTUK FETCH DATA
    private fun startChatLoop() {
        stopChatLoop()

        // Gunakan scope coroutine untuk operasi asinkron (fetch Firebase)
        chatJob = scope.launch(Dispatchers.IO) {
            val userName = fetchUserNameFromFirebase() // Ambil username dari Firebase

            withContext(Dispatchers.Main) { // Pindah ke Main Thread untuk update UI
                val messages = getTimyMessages(userName)
                var index = 0
                while (isActive) {
                    tvTimyChatText.text = messages[index % messages.size]
                    index++
                    delay(5000L)
                }
            }
        }
    }

    private fun stopChatLoop() {
        chatJob?.cancel()
        chatJob = null
    }

    private fun loadTimyGifs() {
        try {
            val timyFace = findViewById<ImageView>(R.id.imgTimyFaceGif)
            val timyLeftArm = findViewById<ImageView>(R.id.imgTimyLeftArmGif)
            val timyRightArm = findViewById<ImageView>(R.id.imgTimyRightArmGif)

            Glide.with(this).asGif().load(R.drawable.timy_home_muka).into(timyFace)
            Glide.with(this).asGif().load(R.drawable.timy_home_tangan_kiri).into(timyLeftArm)
            Glide.with(this).asGif().load(R.drawable.timy_home_tangan_kanan).into(timyRightArm)
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error loading GIFs", e)
        }
    }

    override fun onStart() {
        super.onStart()
        bottomNav.selectedItemId = R.id.nav_home
    }

    private fun showStreakSuccessDialog(newStreak: Int) {
        val layoutResId = resources.getIdentifier("dialog_streak_success", "layout", packageName)
        if (layoutResId == 0) return

        try {
            val dialogView = LayoutInflater.from(this).inflate(layoutResId, null)

            val tvStreakMessage = dialogView.findViewById<TextView>(R.id.tv_streak_message)
            val btnOk = dialogView.findViewById<Button>(R.id.btn_ok)

            if (tvStreakMessage == null || btnOk == null) return

            tvStreakMessage.text = "$newStreak streak"
            tvStreakMessage.setTextColor(resources.getColor(R.color.orange, theme))

            val alertDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnOk.setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.show()
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error showing streak dialog: ${e.message}", e)
        }
    }


    private fun checkAndUpdateStreak() {
        lifecycleScope.launch(Dispatchers.IO) { // Gunakan IO Dispatcher untuk operasi database
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val todayCalendar = Calendar.getInstance()

            // GANTI: Ambil status streak dari TaskRepository
            val currentState = TaskRepository.getCurrentUserStreakState() // MENGGUNAKAN TaskRepository
            val currentStreak = currentState.currentStreak
            val lastDateStr = currentState.lastCompletionDate

            // Menggunakan fungsi suspend dari TaskRepository
            val completedToday = TaskRepository.getCompletedTasksByDate(todayCalendar)
            val hasCompletedToday = completedToday.isNotEmpty()

            var newStreak = currentStreak
            var shouldUpdate = false
            var streakIncreased = false

            when {
                // Kasus 1: Belum ada streak sama sekali
                lastDateStr == null -> {
                    if (hasCompletedToday) {
                        newStreak = 1
                        shouldUpdate = true
                        streakIncreased = true
                    }
                }
                // Kasus 2: Sudah di-update hari ini
                lastDateStr == todayStr -> {}
                // Kasus 3: Hari ini adalah hari setelah lastDateStr (Beruntun)
                isYesterday(lastDateStr, todayStr) -> {
                    if (hasCompletedToday) {
                        newStreak = currentStreak + 1
                        shouldUpdate = true
                        streakIncreased = true
                    }
                }
                // Kasus 4: Jeda lebih dari satu hari (Streak putus)
                else -> {
                    if (hasCompletedToday) {
                        newStreak = 1
                        shouldUpdate = true
                        streakIncreased = true
                    } else {
                        // Reset streak HANYA jika hari ini tidak ada task yang selesai
                        newStreak = 0
                        shouldUpdate = true
                    }
                }
            }

            if (shouldUpdate) {
                // Gunakan streakDays dari state sebelumnya
                val streakDays = currentState.streakDays
                val currentDay = getCurrentDayOfWeek()
                val existingDays = streakDays.split(",").mapNotNull { it.toIntOrNull() }.toSet()

                val newStreakDays = if (newStreak > currentStreak) {
                    if (!existingDays.contains(currentDay)) {
                        // Tambahkan hari ini ke streak days
                        if (streakDays.isEmpty()) currentDay.toString() else "$streakDays,$currentDay"
                    } else {
                        // Hari ini sudah ada dalam streak days, pertahankan
                        streakDays
                    }
                } else if (newStreak == 1) {
                    // Streak baru dimulai hari ini
                    currentDay.toString()
                } else {
                    // Streak putus (newStreak=0), reset hari
                    ""
                }

                // Menggunakan StreakState dari TaskRepository
                val newState = StreakState(
                    currentStreak = newStreak,
                    lastCompletionDate = if (newStreak > 0) todayStr else null,
                    streakDays = newStreakDays
                )

                // GANTI: Simpan status streak ke TaskRepository (remote)
                TaskRepository.saveCurrentUserStreakState(newState) // MENGGUNAKAN TaskRepository

                if (streakIncreased) {
                    withContext(Dispatchers.Main) { // Pindah ke Main Thread untuk UI
                        showStreakSuccessDialog(newStreak)
                    }
                }
            }
        }
    }

    private fun getCurrentDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    private fun updateWeeklyProgressUI() {
        lifecycleScope.launch(Dispatchers.IO) { // Pindah ke IO Dispatcher
            // Pastikan missed tasks terupdate DULU
            TaskRepository.updateMissedTasks()

            val todayCalendar = Calendar.getInstance()
            val fullWeekProgressBar = dayProgressBars.firstOrNull()
            val runnerIcon = dayRunnerIcons.firstOrNull()

            // GANTI: Ambil status streak dari TaskRepository
            val currentState = TaskRepository.getCurrentUserStreakState() // MENGGUNAKAN TaskRepository
            val currentStreak = currentState.currentStreak
            val streakDaysStr = currentState.streakDays
            val todayDayOfWeek = getCurrentDayOfWeek()

            // Dapatkan data tugas dari background thread
            val tasksToday = TaskRepository.getTasksByDate(todayCalendar)
            val completedToday = TaskRepository.getCompletedTasksByDate(todayCalendar)

            withContext(Dispatchers.Main) { // Pindah ke Main Thread untuk UI
                tvStreak.text = currentStreak.toString()

                dayEllipses.forEach { it.visibility = View.VISIBLE }
                dayRunnerIcons.forEach { it.visibility = View.GONE }

                if (fullWeekProgressBar != null && runnerIcon != null) {
                    if (currentStreak > 0 && streakDaysStr.isNotEmpty()) {
                        val streakDays = streakDaysStr.split(",").mapNotNull { it.toIntOrNull() }.toSet()

                        val minStreakDay = streakDays.minOrNull() ?: todayDayOfWeek

                        if (streakDays.isNotEmpty()) {
                            fullWeekProgressBar.visibility = View.VISIBLE
                            runnerIcon.visibility = View.VISIBLE

                            dayEllipses.forEachIndexed { index, ellipse ->
                                // Menggunakan DayOfWeek (0=Senin..6=Minggu)
                                val isStreakDay = streakDays.contains(index)
                                ellipse.visibility = if (isStreakDay) View.GONE else View.VISIBLE
                            }

                            val progressPerDay = 100
                            val totalDaysInWeek = 7

                            val totalTasks = tasksToday.size + completedToday.size

                            val todayProgress = if (totalTasks > 0) {
                                ((completedToday.size.toFloat() / totalTasks.toFloat()) * progressPerDay).toInt().coerceIn(0, progressPerDay)
                            } else {
                                if (streakDays.contains(todayDayOfWeek)) progressPerDay else 0
                            }

                            val daysInSegment = todayDayOfWeek - minStreakDay
                            val segmentProgressLength = (daysInSegment * progressPerDay) + todayProgress
                            val startProgressValue = minStreakDay * progressPerDay

                            fullWeekProgressBar.max = totalDaysInWeek * progressPerDay
                            fullWeekProgressBar.progress = segmentProgressLength

                            val layoutDays = findViewById<View>(R.id.layoutDays)
                            layoutDays.post {
                                val parentContainer = layoutDays as? ViewGroup
                                if (parentContainer != null && parentContainer.childCount > 0) {
                                    val parentWidth = parentContainer.width
                                    val trackStartMargin = 10.dp
                                    val trackWidth = parentWidth - (2 * trackStartMargin)
                                    val runnerIconHalfWidth = runnerIcon.width / 2

                                    val offsetRatio = startProgressValue.toFloat() / 700f
                                    val progressBarTranslationX = (offsetRatio * trackWidth).toInt()

                                    fullWeekProgressBar.translationX = progressBarTranslationX.toFloat()

                                    val totalEndProgressValue = (todayDayOfWeek * progressPerDay) + todayProgress
                                    val totalProgressRatio = totalEndProgressValue.toFloat() / 700f

                                    val runnerTranslationXCorrected = (totalProgressRatio * trackWidth).toInt() + trackStartMargin - runnerIconHalfWidth

                                    runnerIcon.translationX = runnerTranslationXCorrected.toFloat()
                                } else {
                                    Log.e("HomeActivity", "layoutDays is not a valid ViewGroup or has no children.")
                                }
                            }

                        } else {
                            fullWeekProgressBar.visibility = View.INVISIBLE
                            runnerIcon.visibility = View.GONE
                            fullWeekProgressBar.translationX = 0f
                        }
                    } else {
                        fullWeekProgressBar.visibility = View.INVISIBLE
                        runnerIcon.visibility = View.GONE
                        fullWeekProgressBar.translationX = 0f
                    }
                }
            }
        }
    }

    private fun isYesterday(lastDateStr: String?, todayStr: String): Boolean {
        if (lastDateStr == null) return false
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = sdf.format(yesterday.time)
        return lastDateStr == yesterdayStr
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}