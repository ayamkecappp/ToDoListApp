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
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class HomeActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvTimyChatText: TextView
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var chatJob: Job? = null

    private lateinit var tvStreak: TextView
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var dayProgressBars: List<ProgressBar>
    private lateinit var dayEllipses: List<View>
    private lateinit var dayRunnerLayouts: List<View>
    private lateinit var dayRunnerIcons: List<View>

    private val PROFILE_PREFS_NAME = "ProfilePrefs"
    private val KEY_USERNAME = "username"

    private val PREFS_NAME = "TimyTimePrefs"
    private val KEY_STREAK = "current_streak"
    private val KEY_LAST_DATE = "last_completion_date"
    private val KEY_STREAK_DAYS = "streak_days"
    private lateinit var prefs: SharedPreferences

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

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        dayRunnerLayouts = emptyList()
    }

    override fun onResume() {
        super.onResume()
        checkAndUpdateStreak()
        updateWeeklyProgressUI()
        bottomNav.selectedItemId = R.id.nav_home
        startChatLoop()
    }

    override fun onPause() {
        super.onPause()
        stopChatLoop()
    }

    private fun startChatLoop() {
        stopChatLoop()
        val profilePrefs = getSharedPreferences(PROFILE_PREFS_NAME, Context.MODE_PRIVATE)
        val userName = profilePrefs.getString(KEY_USERNAME, "Guest") ?: "Guest"
        val messages = getTimyMessages(userName)

        chatJob = scope.launch {
            var index = 0
            while (isActive) {
                tvTimyChatText.text = messages[index % messages.size]
                index++
                delay(5000L)
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

    private fun checkAndUpdateStreak() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val todayCalendar = Calendar.getInstance()

        val currentStreak = prefs.getInt(KEY_STREAK, 0)
        val lastDateStr = prefs.getString(KEY_LAST_DATE, null)

        val completedToday = TaskRepository.getCompletedTasksByDate(todayCalendar)
        val hasCompletedToday = completedToday.isNotEmpty()

        when {
            lastDateStr == null -> {
                if (hasCompletedToday) {
                    prefs.edit().apply {
                        putInt(KEY_STREAK, 1)
                        putString(KEY_LAST_DATE, todayStr)
                        putString(KEY_STREAK_DAYS, getCurrentDayOfWeek().toString())
                        apply()
                    }
                }
            }

            lastDateStr == todayStr -> {
                // Sudah di-update hari ini
            }

            isYesterday(lastDateStr, todayStr) -> {
                if (hasCompletedToday) {
                    val streakDays = prefs.getString(KEY_STREAK_DAYS, "") ?: ""
                    val currentDay = getCurrentDayOfWeek()
                    val existingDays = streakDays.split(",").mapNotNull { it.toIntOrNull() }

                    val newStreakDays = if (!existingDays.contains(currentDay)) {
                        if (streakDays.isEmpty()) currentDay.toString() else "$streakDays,$currentDay"
                    } else {
                        streakDays
                    }

                    prefs.edit().apply {
                        putInt(KEY_STREAK, currentStreak + 1)
                        putString(KEY_LAST_DATE, todayStr)
                        putString(KEY_STREAK_DAYS, newStreakDays)
                        apply()
                    }
                } else {
                    prefs.edit().apply {
                        putInt(KEY_STREAK, 0)
                        putString(KEY_LAST_DATE, null)
                        putString(KEY_STREAK_DAYS, "")
                        apply()
                    }
                }
            }

            else -> {
                if (hasCompletedToday) {
                    prefs.edit().apply {
                        putInt(KEY_STREAK, 1)
                        putString(KEY_LAST_DATE, todayStr)
                        putString(KEY_STREAK_DAYS, getCurrentDayOfWeek().toString())
                        apply()
                    }
                } else {
                    prefs.edit().apply {
                        putInt(KEY_STREAK, 0)
                        putString(KEY_LAST_DATE, null)
                        putString(KEY_STREAK_DAYS, "")
                        apply()
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
        val todayCalendar = Calendar.getInstance()
        val fullWeekProgressBar = dayProgressBars.firstOrNull()
        val runnerIcon = dayRunnerIcons.firstOrNull()

        val currentStreak = prefs.getInt(KEY_STREAK, 0)
        val streakDaysStr = prefs.getString(KEY_STREAK_DAYS, "") ?: ""

        tvStreak.text = currentStreak.toString()

        dayEllipses.forEach { it.visibility = View.VISIBLE }
        dayRunnerIcons.forEach { it.visibility = View.GONE }

        if (fullWeekProgressBar != null && runnerIcon != null) {
            if (currentStreak > 0 && streakDaysStr.isNotEmpty()) {
                val streakDays = streakDaysStr.split(",").mapNotNull { it.toIntOrNull() }

                if (streakDays.isNotEmpty()) {
                    fullWeekProgressBar.visibility = View.VISIBLE
                    runnerIcon.visibility = View.VISIBLE

                    dayEllipses.forEachIndexed { index, ellipse ->
                        ellipse.visibility = if (streakDays.contains(index)) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                    }

                    val firstStreakDay = streakDays.first()
                    val lastStreakDay = streakDays.last()
                    val progressPerDay = 100

                    // Hitung progress hari ini
                    val tasksToday = TaskRepository.getTasksByDate(todayCalendar)
                    val completedToday = TaskRepository.getCompletedTasksByDate(todayCalendar)
                    val todayProgress = if (tasksToday.isNotEmpty()) {
                        ((completedToday.size.toFloat() / tasksToday.size.toFloat()) * progressPerDay).toInt()
                    } else {
                        0
                    }

                    // Total progress dari hari pertama streak hingga sekarang
                    val completedDaysProgress = (currentStreak - 1) * progressPerDay
                    val totalProgress = completedDaysProgress + todayProgress

                    // Set progress bar: mulai dari firstStreakDay, progress sesuai streak
                    fullWeekProgressBar.max = 700 // 7 hari × 100
                    fullWeekProgressBar.progress = (firstStreakDay * progressPerDay) + totalProgress

                    // Posisikan runner icon sesuai progress
                    val progressBarWidth = resources.displayMetrics.widthPixels - 40.dp
                    val runnerPosition = (((firstStreakDay * progressPerDay + totalProgress).toFloat() / 700f) * progressBarWidth)
                    runnerIcon.translationX = runnerPosition
                } else {
                    fullWeekProgressBar.visibility = View.INVISIBLE
                    runnerIcon.visibility = View.GONE
                }
            } else {
                fullWeekProgressBar.visibility = View.INVISIBLE
                runnerIcon.visibility = View.GONE
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