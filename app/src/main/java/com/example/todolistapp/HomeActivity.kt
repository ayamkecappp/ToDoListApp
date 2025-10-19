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
import android.view.ViewGroup
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Button
import android.graphics.Color
import androidx.lifecycle.lifecycleScope

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

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun startChatLoop() {
        stopChatLoop()
        val profilePrefs = getSharedPreferences(PROFILE_PREFS_NAME, Context.MODE_PRIVATE)
        val userName = profilePrefs.getString(KEY_USERNAME, "Guest") ?: "Guest"
        val messages = getTimyMessages(userName)

        // Menggunakan scope coroutine yang didefinisikan di atas (bukan lifecycleScope)
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

    private fun showStreakSuccessDialog(newStreak: Int) {
        // ... (Logic dialog sama)
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
        lifecycleScope.launch(Dispatchers.IO) { // Ganti scope ke lifecycleScope dengan IO Dispatcher
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val todayCalendar = Calendar.getInstance()

            val currentStreak = prefs.getInt(KEY_STREAK, 0)
            val lastDateStr = prefs.getString(KEY_LAST_DATE, null)

            // Menggunakan fungsi suspend
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
                        newStreak = 0
                        shouldUpdate = true
                    }
                }
            }

            if (shouldUpdate) {
                val streakDays = prefs.getString(KEY_STREAK_DAYS, "") ?: ""
                val currentDay = getCurrentDayOfWeek()
                val existingDays = streakDays.split(",").mapNotNull { it.toIntOrNull() }

                val newStreakDays = if (newStreak > currentStreak) {
                    if (!existingDays.contains(currentDay)) {
                        if (streakDays.isEmpty()) currentDay.toString() else "$streakDays,$currentDay"
                    } else {
                        streakDays
                    }
                } else if (newStreak == 1) {
                    currentDay.toString()
                } else {
                    ""
                }

                prefs.edit().apply {
                    putInt(KEY_STREAK, newStreak)
                    putString(KEY_LAST_DATE, if (newStreak > 0) todayStr else null)
                    putString(KEY_STREAK_DAYS, newStreakDays)
                    apply()
                }

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
            val todayCalendar = Calendar.getInstance()
            val fullWeekProgressBar = dayProgressBars.firstOrNull()
            val runnerIcon = dayRunnerIcons.firstOrNull()

            val currentStreak = prefs.getInt(KEY_STREAK, 0)
            val streakDaysStr = prefs.getString(KEY_STREAK_DAYS, "") ?: ""
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