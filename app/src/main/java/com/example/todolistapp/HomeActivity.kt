package com.example.todolistapp


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class HomeActivity : AppCompatActivity() {


    private lateinit var tvTitle: TextView
    private lateinit var tvSpeech: TextView
    private lateinit var tvStreak: TextView
    private lateinit var bottomNav: BottomNavigationView


    private lateinit var dayProgressBars: List<ProgressBar>
    private lateinit var dayEllipses: List<View>
    private lateinit var dayRunnerLayouts: List<View>
    private lateinit var dayRunnerIcons: List<View>


    private val PREFS_NAME = "TimyTimePrefs"
    private val KEY_STREAK = "current_streak"
    private val KEY_LAST_DATE = "last_completion_date"
    private val KEY_TASKS_TOTAL_TODAY = "tasks_total_today"
    private val KEY_TASKS_COMPLETED_TODAY = "tasks_completed_today"
    private lateinit var prefs: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)


        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


        tvTitle = findViewById(R.id.tvTitle)
        tvSpeech = findViewById(R.id.tvSpeech)
        tvStreak = findViewById(R.id.tvStreak)
        bottomNav = findViewById(R.id.bottomNav)


        bottomNav.itemIconTintList = null
        initializeViews()


        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_tasks -> {
                    startActivity(
                        Intent(
                            this,
                            TaskActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    )
                    true
                }


                R.id.nav_profile -> {
                    startActivity(
                        Intent(
                            this,
                            ProfileActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    )
                    true
                }


                else -> false
            }
        }
    }


    private fun initializeViews() {
        // Hanya gunakan yang ADA di XML
        dayProgressBars = listOf(findViewById(R.id.full_week_progress_bar)) // Hanya 1 progress bar

        // Untuk marker (ellipse kecil di bawah hari)
        dayEllipses = listOf(
            findViewById(R.id.day_marker_0),
            findViewById(R.id.day_marker_1),
            findViewById(R.id.day_marker_2),
            findViewById(R.id.day_marker_3),
            findViewById(R.id.day_marker_4),
            findViewById(R.id.day_marker_5),
            findViewById(R.id.day_marker_6)
        )

        // Runner icon hanya 1
        dayRunnerIcons = listOf(findViewById(R.id.runner_icon))

        // Ini tidak ada di XML, jadi HAPUS atau buat dummy list kosong
        dayRunnerLayouts = emptyList()
    }


    override fun onResume() {
        super.onResume()

        // Reset progress bar visual per Senin
        val todayCalendar = Calendar.getInstance()
        if (todayCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            val lastResetStr = prefs.getString("last_weekly_reset", null)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (lastResetStr != todayStr) {
                // Reset visual progress bar (bukan streak counter)
                prefs.edit().putString("last_weekly_reset", todayStr).apply()
            }
        }

        updateWeeklyProgressUI()
        bottomNav.selectedItemId = R.id.nav_home
    }


    override fun onStart() {
        super.onStart()
        bottomNav.selectedItemId = R.id.nav_home
    }


    private fun updateWeeklyProgressUI() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val todayCalendar = Calendar.getInstance()

        // Ambil data dari SharedPreferences
        var currentStreak = prefs.getInt(KEY_STREAK, 0)
        val lastCompletionDateStr = prefs.getString(KEY_LAST_DATE, null)

        // Hitung posisi hari ini dalam minggu (0=Senin, 6=Minggu)
        val dayOfWeek = when (todayCalendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        tvStreak.text = currentStreak.toString()

        // Reset UI markers (ellipses)
        dayEllipses.forEach { it.visibility = View.VISIBLE }
        dayRunnerIcons.forEach { it.visibility = View.GONE }

        // Cek apakah hari ini ada task yang completed
        val tasksToday = TaskRepository.getTasksByDate(todayCalendar)
        val completedToday = TaskRepository.getCompletedTasksByDate(todayCalendar)

        // Hitung progress untuk progress bar utama (1 bar horizontal)
        val fullWeekProgressBar = dayProgressBars.firstOrNull()
        val runnerIcon = dayRunnerIcons.firstOrNull()

        if (fullWeekProgressBar != null && runnerIcon != null) {
            // Reset progress bar
            fullWeekProgressBar.progress = 0
            fullWeekProgressBar.max = 700 // 7 hari × 100

            // Cek apakah streak aktif
            val isStreakActive =
                currentStreak > 0 && (lastCompletionDateStr == todayStr || isYesterday(
                    lastCompletionDateStr,
                    todayStr
                ))

            if (isStreakActive) {
                // Hitung progress berdasarkan hari dalam minggu
                val progressPerDay = 100
                val completedDays = dayOfWeek // Hari sebelum hari ini dianggap complete

                // Progress untuk hari-hari sebelumnya (100% per hari)
                val previousDaysProgress = completedDays * progressPerDay

                // Progress untuk hari ini
                val todayProgress = if (tasksToday.isNotEmpty()) {
                    ((completedToday.size.toFloat() / tasksToday.size.toFloat()) * progressPerDay).toInt()
                } else {
                    0
                }

                // Total progress
                val totalProgress = previousDaysProgress + todayProgress
                fullWeekProgressBar.progress = totalProgress

                // Tampilkan runner icon
                runnerIcon.visibility = View.VISIBLE

                // Posisikan runner icon berdasarkan progress
                // Geser runner icon sesuai progress (0dp untuk 0%, hingga width-10dp untuk 100%)
                val progressBarWidth =
                    resources.displayMetrics.widthPixels - 40.dp // approx width minus margins
                val runnerPosition = (totalProgress.toFloat() / 700f) * progressBarWidth
                runnerIcon.translationX = runnerPosition
            } else {
                // Tidak ada streak aktif
                fullWeekProgressBar.progress = 0
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