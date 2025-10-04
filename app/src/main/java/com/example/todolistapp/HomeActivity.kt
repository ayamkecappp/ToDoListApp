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
import kotlinx.coroutines.* // <<-- IMPORT COROUTINES DITAMBAHKAN
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import java.lang.Exception
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

    // Kunci PREFS dari EditProfileActivity
    private val PROFILE_PREFS_NAME = "ProfilePrefs"
    private val KEY_USERNAME = "username"

    private val PREFS_NAME = "TimyTimePrefs"
    private val KEY_STREAK = "current_streak"
    private val KEY_LAST_DATE = "last_completion_date"
    private val KEY_STREAK_START_DATE = "streak_start_date"
    private val KEY_TASKS_TOTAL_TODAY = "tasks_total_today"
    private val KEY_TASKS_COMPLETED_TODAY = "tasks_completed_today"
    private lateinit var prefs: SharedPreferences


    // ==============================================
    // 💡 FUNGSI MEMBUAT PESAN DENGAN BOLD/HTML 💡
    // ==============================================
    private fun getTimyMessages(userName: String): List<Spanned> {

        // Format username agar tebal (bold) menggunakan tag HTML <b>
        val boldUserName = "<b>$userName</b>"

        val rawMessages = listOf(
            "Hi! I’m Timy,\nYour Personal Time Assistant", // Chat 1
            "Nice to see you,\n$boldUserName",             // Chat 2 (dengan username tebal)
            "Let’s plan your day with me,\nTimy!",          // Chat 3
        )

        // Gunakan Core KTX Extension Function untuk Html.fromHtml
        // Ini adalah cara modern dan aman, menghilangkan kebutuhan untuk pengecekan SDK manual
        return rawMessages.map { rawMessage ->
            Html.fromHtml(rawMessage, android.text.Html.FROM_HTML_MODE_LEGACY) // FROM_HTML_MODE_LEGACY adalah nilai default
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

        // Ambil referensi TextView untuk Chat Bubble
        tvTimyChatText = findViewById(R.id.tvTimyChatText)

        bottomNav.itemIconTintList = null
        initializeViews()

        loadTimyGifs() // Asumsi fungsi ini sudah Anda implementasikan

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
        dayProgressBars = listOf(findViewById(R.id.full_week_progress_bar))

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
        dayRunnerLayouts = emptyList()
    }

    override fun onResume() {
        super.onResume()
        updateWeeklyProgressUI()
        bottomNav.selectedItemId = R.id.nav_home

        // MULAI LOOP KETIKA ACTIVITY TERLIHAT
        startChatLoop()
    }

    override fun onPause() {
        super.onPause()
        // HENTIKAN LOOP KETIKA ACTIVITY TIDAK TERLIHAT
        stopChatLoop()
    }

    // ==============================================
    // 💡 FUNGSI LOGIKA CHAT LOOP 💡
    // ==============================================
    private fun startChatLoop() {
        stopChatLoop()

        // Ambil data username dari SharedPreferences yang digunakan di EditProfileActivity
        val profilePrefs = getSharedPreferences(PROFILE_PREFS_NAME, Context.MODE_PRIVATE)
        val userName = profilePrefs.getString(KEY_USERNAME, "Guest") ?: "Guest"

        // Dapatkan list pesan yang sudah diformat HTML
        val messages = getTimyMessages(userName)

        chatJob = scope.launch {
            var index = 0
            while (isActive) {
                // Tampilkan pesan dalam format Spanned (dengan bold)
                tvTimyChatText.text = messages[index % messages.size]

                // Pindah ke pesan berikutnya dan reset jika sudah mencapai akhir list
                index++

                // Tunggu 5 detik sebelum ganti pesan
                delay(5000L)
            }
        }
    }

    private fun stopChatLoop() {
        chatJob?.cancel()
        chatJob = null
    }

    // ==============================================
    // 💡 FUNGSI BARU UNTUK MEMUAT GIF DENGAN GLIDE
    // ==============================================
    private fun loadTimyGifs() {
        try {
            val timyFace = findViewById<ImageView>(R.id.imgTimyFaceGif)
            val timyLeftArm = findViewById<ImageView>(R.id.imgTimyLeftArmGif)
            val timyRightArm = findViewById<ImageView>(R.id.imgTimyRightArmGif)

            // Muat GIF untuk Muka
            Glide.with(this)
                .asGif()
                .load(R.drawable.timy_home_muka) // Pastikan nama aset GIF di drawable benar
                .into(timyFace)

            // Muat GIF untuk Tangan Kiri
            Glide.with(this)
                .asGif()
                .load(R.drawable.timy_home_tangan_kiri) // Pastikan nama aset GIF di drawable benar
                .into(timyLeftArm)

            // Muat GIF untuk Tangan Kanan
            Glide.with(this)
                .asGif()
                .load(R.drawable.timy_home_tangan_kanan) // Pastikan nama aset GIF di drawable benar
                .into(timyRightArm)

            // CATATAN: Badan (imgTimyBodyStatic) akan di-load sebagai gambar statis dari src-nya di XML.

        } catch (e: Exception) {
            // Ini untuk penanganan error jika Glide gagal memuat atau findViewById gagal
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        bottomNav.selectedItemId = R.id.nav_home
    }

    private fun updateWeeklyProgressUI() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val todayCalendar = Calendar.getInstance()

        val fullWeekProgressBar = dayProgressBars.firstOrNull()
        val runnerIcon = dayRunnerIcons.firstOrNull()

        // Ambil data dari SharedPreferences
        val currentStreak = prefs.getInt(KEY_STREAK, 0)
        val lastCompletionDateStr = prefs.getString(KEY_LAST_DATE, null)
        val streakStartDateStr = prefs.getString(KEY_STREAK_START_DATE, null)

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

        // Reset semua ellipse dulu - tampilkan semua
        dayEllipses.forEach { it.visibility = View.VISIBLE }
        dayRunnerIcons.forEach { it.visibility = View.GONE }

        if (fullWeekProgressBar != null && runnerIcon != null) {
            // Cek apakah streak aktif
            val isStreakActive = currentStreak > 0 &&
                    (lastCompletionDateStr == todayStr || isYesterday(lastCompletionDateStr, todayStr))

            if (isStreakActive && streakStartDateStr != null) {
                // Tampilkan progress bar dan runner
                fullWeekProgressBar.visibility = View.VISIBLE
                runnerIcon.visibility = View.VISIBLE

                // Parse tanggal mulai streak
                val startDate = sdf.parse(streakStartDateStr)!!
                val startCalendar = Calendar.getInstance().apply { time = startDate }

                // Hitung hari mulai streak dalam minggu ini
                val streakStartDayOfWeek = when (startCalendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }

                // FIX: Sembunyikan ellipse HANYA di rentang streak aktif
                dayEllipses.forEachIndexed { index, ellipse ->
                    // Sembunyikan ellipse dari hari mulai streak sampai hari ini (inklusif)
                    // Gunakan <= untuk memastikan hari pertama juga tersembunyi
                    ellipse.visibility = if (index >= streakStartDayOfWeek && index <= dayOfWeek) {
                        View.GONE  // Sembunyikan di rentang aktif
                    } else {
                        View.VISIBLE  // Tampilkan di luar rentang
                    }
                }

                // Hitung progress
                val progressPerDay = 100

                // FIX: Progress bar dimulai dari posisi hari streak dimulai
                val streakDays = dayOfWeek - streakStartDayOfWeek + 1

                // Hitung progress hari ini
                val tasksToday = TaskRepository.getTasksByDate(todayCalendar)
                val completedToday = TaskRepository.getCompletedTasksByDate(todayCalendar)
                val todayProgress = if (tasksToday.isNotEmpty()) {
                    ((completedToday.size.toFloat() / tasksToday.size.toFloat()) * progressPerDay).toInt()
                } else {
                    0
                }

                // Total progress = hari-hari sebelumnya (penuh) + progress hari ini
                val completedDaysProgress = (streakDays - 1) * progressPerDay
                val totalProgress = completedDaysProgress + todayProgress

                // Set progress bar
                fullWeekProgressBar.max = 700  // 7 hari × 100
                fullWeekProgressBar.progress = (streakStartDayOfWeek * progressPerDay) + totalProgress

                // FIX: Posisikan runner icon berdasarkan progress aktual
                val progressBarWidth = resources.displayMetrics.widthPixels - 40.dp
                val runnerPosition = ((streakStartDayOfWeek * progressPerDay + totalProgress).toFloat() / 700f) * progressBarWidth
                runnerIcon.translationX = runnerPosition

            } else {
                // Tidak ada streak aktif - sembunyikan progress bar dan runner
                fullWeekProgressBar.visibility = View.INVISIBLE
                runnerIcon.visibility = View.GONE
                // Ellipse sudah di-reset ke VISIBLE di atas
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