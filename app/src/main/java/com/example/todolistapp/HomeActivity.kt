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
import android.graphics.Color // Tambahkan import ini jika diperlukan

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

    /**
     * Menampilkan dialog YAY, you on fire (X streak)
     * Membutuhkan layout: dialog_streak_success.xml
     */
    private fun showStreakSuccessDialog(newStreak: Int) {
        // Cek apakah resource layout ada sebelum mencoba menggunakannya
        val layoutResId = resources.getIdentifier("dialog_streak_success", "layout", packageName)

        if (layoutResId == 0) {
            Log.e("HomeActivity", "FATAL: Layout 'dialog_streak_success.xml' not found. Dialog cannot be shown.")
            return
        }

        // Gunakan try-catch untuk menangkap NullPointerException/InflateException
        try {
            val dialogView = LayoutInflater.from(this).inflate(layoutResId, null)

            // Menggunakan elvis operator (?.) untuk mencegah crash jika view tidak ditemukan
            val tvStreakMessage = dialogView.findViewById<TextView>(R.id.tv_streak_message)
            val btnOk = dialogView.findViewById<Button>(R.id.btn_ok)

            if (tvStreakMessage == null || btnOk == null) {
                Log.e("HomeActivity", "FATAL: Views inside dialog_streak_success not found (tv_streak_message or btn_ok). Check IDs.")
                return
            }

            tvStreakMessage.text = "$newStreak streak"

            // Set warna teks (opsional, untuk memastikan tampilan)
            tvStreakMessage.setTextColor(resources.getColor(R.color.orange, theme))

            val alertDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            // Membuat background dialog transparan agar custom shape di XML terlihat
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
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val todayCalendar = Calendar.getInstance()

        val currentStreak = prefs.getInt(KEY_STREAK, 0)
        val lastDateStr = prefs.getString(KEY_LAST_DATE, null)

        val completedToday = TaskRepository.getCompletedTasksByDate(todayCalendar)
        val hasCompletedToday = completedToday.isNotEmpty()

        when {
            // Kasus 1: Belum ada streak sama sekali
            lastDateStr == null -> {
                if (hasCompletedToday) {
                    // Mulai streak pertama (Streak 1)
                    prefs.edit().apply {
                        putInt(KEY_STREAK, 1)
                        putString(KEY_LAST_DATE, todayStr)
                        putString(KEY_STREAK_DAYS, getCurrentDayOfWeek().toString())
                        apply()
                    }
                    showStreakSuccessDialog(1) // Panggil dialog untuk streak 1
                }
            }

            // Kasus 2: Sudah di-update hari ini
            lastDateStr == todayStr -> {
                // Streak dipertahankan dan tidak ada yang diubah.
            }

            // Kasus 3: Hari ini adalah hari setelah lastDateStr (Beruntun, misalnya streak 1 -> streak 2)
            isYesterday(lastDateStr, todayStr) -> {
                if (hasCompletedToday) {
                    val newStreak = currentStreak + 1

                    // Tambah streak karena hari ini selesai
                    val streakDays = prefs.getString(KEY_STREAK_DAYS, "") ?: ""
                    val currentDay = getCurrentDayOfWeek()
                    val existingDays = streakDays.split(",").mapNotNull { it.toIntOrNull() }

                    val newStreakDays = if (!existingDays.contains(currentDay)) {
                        // Tambahkan hari ini ke daftar streak days
                        if (streakDays.isEmpty()) currentDay.toString() else "$streakDays,$currentDay"
                    } else {
                        streakDays
                    }

                    prefs.edit().apply {
                        putInt(KEY_STREAK, newStreak) // Meningkatkan streak
                        putString(KEY_LAST_DATE, todayStr) // Update tanggal terakhir
                        putString(KEY_STREAK_DAYS, newStreakDays)
                        apply()
                    }
                    showStreakSuccessDialog(newStreak) // Panggil dialog untuk streak yang bertambah
                } else {
                    // JANGAN DIRESET. Streak dipertahankan untuk hari ini, akan direset besok jika hari ini terlewat.
                }
            }

            // Kasus 4: Jeda lebih dari satu hari (Streak putus)
            else -> {
                if (hasCompletedToday) {
                    // Streak putus, tapi hari ini sudah selesai -> Mulai streak baru dari 1
                    prefs.edit().apply {
                        putInt(KEY_STREAK, 1)
                        putString(KEY_LAST_DATE, todayStr)
                        putString(KEY_STREAK_DAYS, getCurrentDayOfWeek().toString())
                        apply()
                    }
                    showStreakSuccessDialog(1) // Panggil dialog karena streak dimulai dari 1
                } else {
                    // Streak putus dan hari ini juga belum selesai -> Reset total ke 0
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
        val todayDayOfWeek = getCurrentDayOfWeek() // 0=Mon, 6=Sun

        tvStreak.text = currentStreak.toString()

        // Default visibility for ellipses and runner
        dayEllipses.forEach { it.visibility = View.VISIBLE }
        dayRunnerIcons.forEach { it.visibility = View.GONE }

        if (fullWeekProgressBar != null && runnerIcon != null) {
            if (currentStreak > 0 && streakDaysStr.isNotEmpty()) {
                val streakDays = streakDaysStr.split(",").mapNotNull { it.toIntOrNull() }.toSet()

                // Cari hari pertama streak aktif dalam minggu ini (indeks terendah)
                val minStreakDay = streakDays.minOrNull() ?: todayDayOfWeek

                if (streakDays.isNotEmpty()) {
                    fullWeekProgressBar.visibility = View.VISIBLE
                    runnerIcon.visibility = View.VISIBLE

                    // --- 1. Update Ellipses (Ellipses are GONE if the day is part of the streak) ---
                    dayEllipses.forEachIndexed { index, ellipse ->
                        ellipse.visibility = if (streakDays.contains(index)) {
                            View.GONE // Elips disembunyikan untuk hari-hari dalam streak
                        } else {
                            View.VISIBLE
                        }
                    }

                    // --- 2. Calculate Today's Progress (0-100) ---
                    val progressPerDay = 100
                    val totalDaysInWeek = 7
                    val tasksToday = TaskRepository.getTasksByDate(todayCalendar)
                    val completedToday = TaskRepository.getCompletedTasksByDate(todayCalendar)

                    val todayProgress = if (tasksToday.isNotEmpty()) {
                        ((completedToday.size.toFloat() / tasksToday.size.toFloat()) * progressPerDay).toInt().coerceIn(0, 100)
                    } else {
                        // Jika tidak ada tugas, asumsikan 100% progres jika hari ini adalah bagian dari streak
                        if (streakDays.contains(todayDayOfWeek)) progressPerDay else 0
                    }

                    // --- 3. Calculate Progress Bar Fill and Runner Position ---

                    // Jarak progres dari hari awal streak (minStreakDay) hingga hari ini
                    val daysInSegment = todayDayOfWeek - minStreakDay

                    // Total progres *aktual* dalam segmen yang terlihat
                    val segmentProgressLength = (daysInSegment * progressPerDay) + todayProgress

                    // Offset awal untuk menggeser progress bar
                    val startProgressValue = minStreakDay * progressPerDay

                    fullWeekProgressBar.max = totalDaysInWeek * progressPerDay // Max 700

                    // Mengatur Progress Bar untuk mengisi sepanjang segmen streak
                    fullWeekProgressBar.progress = segmentProgressLength

                    // --- 4. Menggeser Progress Bar & Posisi Runner ---
                    val layoutDays = findViewById<View>(R.id.layoutDays)
                    layoutDays.post {
                        val parentContainer = layoutDays as? ViewGroup

                        if (parentContainer != null && parentContainer.childCount > 0) {
                            val parentWidth = parentContainer.getChildAt(0).width

                            val trackStartMargin = 10.dp
                            val trackWidth = parentWidth - (2 * trackStartMargin)
                            val runnerIconHalfWidth = runnerIcon.width / 2

                            // Geser Progress Bar ke kanan sesuai offset awal streak (minStreakDay)
                            val offsetRatio = startProgressValue.toFloat() / 700f
                            val progressBarTranslationX = (offsetRatio * trackWidth).toInt()

                            fullWeekProgressBar.translationX = progressBarTranslationX.toFloat()

                            // Posisi Runner dihitung dari ujung kiri PABRIK (posisi 0)
                            val totalEndProgressValue = (todayDayOfWeek * progressPerDay) + todayProgress
                            val totalProgressRatio = totalEndProgressValue.toFloat() / 700f

                            // Runner Translation adalah dari ujung kiri track parent
                            val runnerTranslationXCorrected = (totalProgressRatio * trackWidth).toInt() + trackStartMargin - runnerIconHalfWidth

                            runnerIcon.translationX = runnerTranslationXCorrected.toFloat()
                        } else {
                            Log.e("HomeActivity", "layoutDays is not a valid ViewGroup or has no children.")
                        }
                    }

                } else {
                    fullWeekProgressBar.visibility = View.INVISIBLE
                    runnerIcon.visibility = View.GONE
                    fullWeekProgressBar.translationX = 0f // Reset translation
                }
            } else { // No streak yet or streak is 0
                fullWeekProgressBar.visibility = View.INVISIBLE
                runnerIcon.visibility = View.GONE
                fullWeekProgressBar.translationX = 0f // Reset translation
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