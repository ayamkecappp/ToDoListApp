package com.example.todolistapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class HomeActivity : AppCompatActivity() {

    // ... (sisa variabel sama seperti kode yang saya berikan sebelumnya) ...
    private lateinit var tvTitle: TextView
    private lateinit var tvTimyChatText: TextView
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var chatJob: Job? = null
    private lateinit var tvStreak: TextView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var dayProgressBars: List<ProgressBar>
    private lateinit var dayEllipses: List<View>
    private lateinit var dayRunnerIcons: List<View>
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val userId = auth.currentUser?.uid
    private var currentStreak = 0
    private var lastCompletionDate: Date? = null
    private var streakDays = emptySet<Int>()
    private val PROFILE_PREFS_NAME = "ProfilePrefs"
    private val KEY_USERNAME = "username"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        if (userId == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        // Panggil fungsi yang sudah diperbaiki
        setupBottomNavigation()
        loadTimyGifs()
    }

    // ... (fungsi initializeViews, loadTimyGifs, dll. tetap sama) ...
    private fun initializeViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvTimyChatText = findViewById(R.id.tvTimyChatText)
        tvStreak = findViewById(R.id.tvStreak)
        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.itemIconTintList = null

        dayProgressBars = listOf(findViewById(R.id.full_week_progress_bar))
        dayEllipses = (0..6).map {
            findViewById<View>(resources.getIdentifier("day_marker_$it", "id", packageName))
        }
        dayRunnerIcons = listOf(findViewById(R.id.runner_icon))
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

    /**
     * INI BAGIAN YANG DIPERBAIKI
     * Menggantikan setOnItemSelectedListener yang ada di onCreate
     */
    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_tasks -> {
                    // Pastikan nama class sudah benar: 'TaskActivity'
                    val intent = Intent(this, TaskActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    // ... (sisa kode dari jawaban sebelumnya bisa disalin di sini) ...
    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.nav_home
        startChatLoop()
        loadDataFromFirestore()
    }

    override fun onPause() {
        super.onPause()
        stopChatLoop()
    }

    override fun onStart() {
        super.onStart()
        bottomNav.selectedItemId = R.id.nav_home
    }

    private fun loadDataFromFirestore() {
        if (userId == null) return

        lifecycleScope.launch {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                currentStreak = userDoc.getLong("currentStreak")?.toInt() ?: 0
                val lastCompletionTimestamp = userDoc.getTimestamp("lastTaskCompletionDate")
                lastCompletionDate = lastCompletionTimestamp?.toDate()
                val streakDaysList = userDoc.get("streakDays") as? List<Long>
                streakDays = streakDaysList?.map { it.toInt() }?.toSet() ?: emptySet()

                checkAndUpdateStreak()

            } catch (e: Exception) {
                Log.e("HomeActivity", "Error loading user data from Firestore", e)
                currentStreak = 0
                lastCompletionDate = null
                streakDays = emptySet()
                updateWeeklyProgressUI()
            }
        }
    }

    private fun checkAndUpdateStreak() {
        lifecycleScope.launch {
            val today = Calendar.getInstance()
            val hasCompletedToday = (TaskRepository.getCompletedTasksByDate(today)).isNotEmpty()

            val lastCal = Calendar.getInstance()
            if (lastCompletionDate != null) {
                lastCal.time = lastCompletionDate!!
            }

            var newStreak = currentStreak
            var newLastCompletionDate = lastCompletionDate
            var newStreakDays = streakDays.toMutableSet()
            var needsUpdate = false
            var showDialog = false

            val isSameDay = lastCompletionDate != null && today.get(Calendar.YEAR) == lastCal.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == lastCal.get(Calendar.DAY_OF_YEAR)

            when {
                (lastCompletionDate == null && hasCompletedToday) ||
                        (lastCompletionDate != null && !isSameDay && !isYesterday(lastCal) && hasCompletedToday) -> {
                    newStreak = 1
                    newLastCompletionDate = today.time
                    newStreakDays = mutableSetOf(getCurrentDayOfWeek())
                    needsUpdate = true
                    showDialog = true
                }

                lastCompletionDate != null && isYesterday(lastCal) && hasCompletedToday -> {
                    newStreak++
                    newLastCompletionDate = today.time
                    newStreakDays.add(getCurrentDayOfWeek())
                    if (getCurrentDayOfWeek() < (lastCal.get(Calendar.DAY_OF_WEEK) + 5) % 7) {
                        newStreakDays = mutableSetOf(getCurrentDayOfWeek())
                    }
                    needsUpdate = true
                    showDialog = true
                }

                lastCompletionDate != null && !isSameDay && !isYesterday(lastCal) && !hasCompletedToday -> {
                    newStreak = 0
                    newLastCompletionDate = null
                    newStreakDays.clear()
                    needsUpdate = true
                }
            }

            if (needsUpdate) {
                currentStreak = newStreak
                lastCompletionDate = newLastCompletionDate
                streakDays = newStreakDays

                updateStreakInFirestore(newStreak, newLastCompletionDate, newStreakDays.toList())
                if (showDialog && newStreak > 0) {
                    showStreakSuccessDialog(newStreak)
                }
            }
            updateWeeklyProgressUI()
        }
    }

    private fun updateStreakInFirestore(streak: Int, lastDate: Date?, days: List<Int>) {
        if (userId == null) return
        val userData = hashMapOf(
            "currentStreak" to streak,
            "lastTaskCompletionDate" to if (lastDate != null) Timestamp(lastDate) else null,
            "streakDays" to days
        )
        db.collection("users").document(userId)
            .set(userData, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("HomeActivity", "Error updating streak in Firestore", e)
            }
    }

    private fun updateWeeklyProgressUI() {
        val todayCalendar = Calendar.getInstance()
        val fullWeekProgressBar = dayProgressBars.firstOrNull() ?: return
        val runnerIcon = dayRunnerIcons.firstOrNull() ?: return

        tvStreak.text = currentStreak.toString()

        dayEllipses.forEach { it.visibility = View.VISIBLE }
        runnerIcon.visibility = View.GONE
        fullWeekProgressBar.visibility = View.INVISIBLE
        fullWeekProgressBar.translationX = 0f

        if (currentStreak > 0) {
            fullWeekProgressBar.visibility = View.VISIBLE
            runnerIcon.visibility = View.VISIBLE

            dayEllipses.forEachIndexed { index, ellipse ->
                ellipse.visibility = if (streakDays.contains(index)) View.GONE else View.VISIBLE
            }

            lifecycleScope.launch {
                val tasksToday = TaskRepository.getTasksByDate(todayCalendar)
                val completedToday = TaskRepository.getCompletedTasksByDate(todayCalendar)

                val todayProgress = if (tasksToday.isNotEmpty()) {
                    ((completedToday.size.toFloat() / tasksToday.size.toFloat()) * 100).toInt()
                } else {
                    if (streakDays.contains(getCurrentDayOfWeek())) 100 else 0
                }

                val minStreakDay = streakDays.minOrNull() ?: getCurrentDayOfWeek()
                val daysInSegment = getCurrentDayOfWeek() - minStreakDay
                val segmentProgressLength = (daysInSegment * 100) + todayProgress
                val startProgressValue = minStreakDay * 100

                fullWeekProgressBar.max = 700
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
                        fullWeekProgressBar.translationX = (offsetRatio * trackWidth)

                        val totalEndProgressValue = (getCurrentDayOfWeek() * 100) + todayProgress
                        val totalProgressRatio = totalEndProgressValue.toFloat() / 700f
                        val runnerTranslationX = (totalProgressRatio * trackWidth) + trackStartMargin - runnerIconHalfWidth
                        runnerIcon.translationX = runnerTranslationX
                    }
                }
            }
        }
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

    private fun getTimyMessages(userName: String): List<Spanned> {
        val boldUserName = "<b>$userName</b>"
        return listOf(
            "Hi! I'm Timy,\nYour Personal Time Assistant",
            "Nice to see you,\n$boldUserName",
            "Let's plan your day with me,\nTimy!",
        ).map { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY) }
    }

    private fun showStreakSuccessDialog(newStreak: Int) {
        val layoutResId = resources.getIdentifier("dialog_streak_success", "layout", packageName)
        if (layoutResId == 0) {
            Log.e("HomeActivity", "Layout 'dialog_streak_success.xml' not found.")
            return
        }

        try {
            val dialogView = LayoutInflater.from(this).inflate(layoutResId, null)
            val tvStreakMessage = dialogView.findViewById<TextView>(R.id.tv_streak_message)
            val btnOk = dialogView.findViewById<Button>(R.id.btn_ok)

            tvStreakMessage.text = "$newStreak streak"

            val alertDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            btnOk.setOnClickListener { alertDialog.dismiss() }
            alertDialog.show()
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error showing streak dialog", e)
        }
    }

    private fun getCurrentDayOfWeek(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
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

    private fun isYesterday(lastCal: Calendar): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return yesterday.get(Calendar.YEAR) == lastCal.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == lastCal.get(Calendar.DAY_OF_YEAR)
    }

    private val Int.dp: Float
        get() = (this * resources.displayMetrics.density)
}