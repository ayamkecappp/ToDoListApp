package com.example.todolistapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.widget.Button
import android.util.Log
import android.view.animation.AnimationUtils
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope

/**
 * Catatan: Asumsi StreakState diimpor dari TaskRepository.kt atau file model bersama.
 */

class TaskActivity : AppCompatActivity() {

    private val TAG = "TaskActivity"
    private lateinit var tasksContainer: LinearLayout
    private lateinit var tvNoActivity: TextView
    private lateinit var octoberText: TextView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var calendarMain: HorizontalScrollView
    private lateinit var dateItemsContainer: LinearLayout

    private lateinit var currentCalendar: Calendar
    private var selectedDate: Calendar = Calendar.getInstance()

    private var hasScrolledToToday = false

    // Konstanta UI
    private val COLOR_ACTIVE_SELECTION = Color.parseColor("#283F6D")
    private val COLOR_DEFAULT_TEXT = Color.BLACK
    private val COLOR_SELECTED_TEXT = Color.WHITE
    private val COLOR_DOT_INDICATOR = Color.parseColor("#283F6D")
    private val CORNER_RADIUS_DP = 20
    private val BORDER_WIDTH_DP = 2
    private val BORDER_COLOR = Color.parseColor("#E0E0E0")
    private val ITEM_WIDTH_DP = 68
    private val NUM_DAYS_TO_SHOW = 14 // Rentang 14 hari

    // Konstanta Intent/Prefs
    private val EXTRA_SELECTED_DATE_MILLIS = "EXTRA_SELECTED_DATE_MILLIS"

    companion object {
        const val RESULT_TASK_DELETED = 101
    }

    // Launcher untuk Activity Result
    private val taskEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || result.resultCode == RESULT_TASK_DELETED) {
            loadAllContent()
        }
    }

    private var missedTaskCheckJob: Job? = null

    // MODIFIKASI fungsi onResume() yang sudah ada
    override fun onResume() {
        super.onResume()

        NotificationHelper.updateLastAppOpenTime(this)

        currentCalendar = Calendar.getInstance()
        loadAllContent() // Sudah ada
        startMissedTaskChecker()
    }

    // MODIFIKASI fungsi onPause() yang sudah ada
    override fun onPause() {
        super.onPause()
        stopMissedTaskChecker()
    }

    // ✅ TAMBAHKAN kedua fungsi baru ini di akhir class (sebelum closing bracket)
    private fun startMissedTaskChecker() {
        missedTaskCheckJob = lifecycleScope.launch {
            while (isActive) {
                delay(60_000L) // Cek setiap 1 menit
                withContext(Dispatchers.IO) {
                    TaskRepository.updateMissedTasks()
                }
                loadAllContent() // Refresh UI
            }
        }
    }

    private fun stopMissedTaskChecker() {
        missedTaskCheckJob?.cancel()
        missedTaskCheckJob = null
    }

    private val addTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadAllContent()
        }
    }

    private val priorityColorMap = mapOf(
        "Low" to R.color.low_priority,
        "Medium" to R.color.medium_priority,
        "High" to R.color.high_priority
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task)

        // Inisialisasi View
        tasksContainer = findViewById(R.id.tasksContainer)
        tvNoActivity = findViewById(R.id.tvNoActivity)
        octoberText = findViewById(R.id.octoberText)
        calendarMain = findViewById(R.id.calendar_main)
        dateItemsContainer = findViewById(R.id.date_items_container)
        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.itemIconTintList = null

        // Inisialisasi Kalender
        currentCalendar = Calendar.getInstance()
        selectedDate = Calendar.getInstance().apply {
            // Set ke 00:00:00:000 agar perbandingan tanggal akurat
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        handleIncomingTaskIntent(intent)
        loadAllContent() // Panggilan pemuatan utama

        // Listeners
        findViewById<ImageView?>(R.id.btn_search)?.setOnClickListener {
            startActivity(Intent(this, SearchFilterActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        findViewById<View>(R.id.reminderContainer)?.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java).apply {
                val selectedDateCopy = selectedDate.clone() as Calendar
                // Set ke tengah hari agar AddTask bisa menentukan dueDate dengan benar
                selectedDateCopy.set(Calendar.HOUR_OF_DAY, 12)
                putExtra(EXTRA_SELECTED_DATE_MILLIS, selectedDateCopy.timeInMillis)
            }
            addTaskLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        findViewById<ImageView?>(R.id.btn_calendar)?.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(
                        Intent(this, HomeActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    )
                    true
                }
                R.id.nav_tasks -> true
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

    override fun onStart() {
        super.onStart()
        bottomNav.selectedItemId = R.id.nav_tasks
    }



    /**
     * Fungsi Utama Pemuatan Konten (Optimized)
     */
    private fun loadAllContent() {
        lifecycleScope.launch(Dispatchers.Main) {
            // 1. Jalankan operasi IO berat di background
            // Pastikan missed tasks terupdate DULU
            launch(Dispatchers.IO) { TaskRepository.updateMissedTasks() }.join()

            // Gunakan async untuk memuat data kalender dan tugas hari ini secara PARALEL
            val calendarDataDeferred = async(Dispatchers.IO) { getCalendarData() }
            val tasksDeferred = async(Dispatchers.IO) {
                // Mengambil tugas PENDING untuk tanggal yang dipilih (hanya 1 query)
                TaskRepository.getTasksByDate(selectedDate.clone() as Calendar)
            }

            val calendarData = calendarDataDeferred.await()
            val tasks = tasksDeferred.await()

            // 2. Update UI di Main thread
            updateCalendarUI(calendarData)
            updateTasksUI(tasks)
            setDynamicMonthYear() // Update bulan/tahun header

            // 3. Scroll ke posisi yang benar
            calendarMain.post {
                if (!hasScrolledToToday) {
                    scrollToSelectedDate(smooth = false)
                    hasScrolledToToday = true
                } else {
                    scrollToSelectedDate(smooth = true)
                }
            }
        }
    }

    /**
     * Update UI untuk Tasks dengan sorting berdasarkan Priority
     */
    private fun updateTasksUI(tasks: List<Task>) {
        tasksContainer.removeAllViews()

        // Definisikan urutan priority (High = 0, Medium = 1, Low = 2, None = 3)
        val priorityOrder = mapOf(
            "High" to 0,
            "Medium" to 1,
            "Low" to 2,
            "None" to 3
        )

        // Sort tasks berdasarkan priority, kemudian reverse agar item terbaru di atas
        val sortedTasks = tasks.sortedWith(
            compareBy<Task> { priorityOrder[it.priority] ?: 3 }
                .thenByDescending { it.id } // Item terbaru di atas untuk priority yang sama
        )

        sortedTasks.forEach { addNewTaskToUI(it) }
        updateEmptyState(tasks.size)
    }

    /**
     * Update UI untuk Kalender (Dioptimalkan untuk kebersihan)
     */
    private fun updateCalendarUI(calendarData: List<CalendarDayData>) {
        dateItemsContainer.removeAllViews()
        calendarData.forEach { data ->
            val view = createDateItemView(
                data.month, data.dayOfWeek, data.dayOfMonth,
                data.isSelected, data.dayMillis, data.hasTasks
            )
            dateItemsContainer.addView(view)
        }
    }


    private fun createDotDrawable(color: Int): GradientDrawable {
        val size = 6.dp
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setSize(size, size)
        }
    }

    // Data class untuk menampung data kalender
    private data class CalendarDayData(
        val month: String,
        val dayOfWeek: String,
        val dayOfMonth: Int,
        val isSelected: Boolean,
        val dayMillis: Long,
        val hasTasks: Boolean
    )

    /**
     * [OPTIMASI UTAMA] Mengambil semua tugas PENDING dalam jendela 14 hari dengan 1 query,
     * lalu memprosesnya secara in-memory.
     */
    private suspend fun getTasksForDateWindow(): Set<String> = withContext(Dispatchers.IO) {
        val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 1. Tentukan rentang 14 hari yang akan ditampilkan
        val startCal = selectedDate.clone() as Calendar
        startCal.add(Calendar.DAY_OF_YEAR, -(NUM_DAYS_TO_SHOW / 2))
        startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0)

        val endCal = selectedDate.clone() as Calendar
        endCal.add(Calendar.DAY_OF_YEAR, (NUM_DAYS_TO_SHOW / 2) + 1)
        endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59); endCal.set(Calendar.MILLISECOND, 999)

        // 2. Fetch semua tugas dalam rentang yang besar (Misal: 3 bulan untuk memastikan cakupan)
        // Kita menggunakan fungsi TaskRepository yang mengambil 3 bulan, lalu memfilter hasilnya.
        val allTasksInLargeRange = TaskRepository.getTasksInDateRangeForCalendar(Calendar.getInstance())

        // 3. Filter dan map hasilnya ke set tanggal (String)
        return@withContext allTasksInLargeRange.filter {
            val taskTime = it.dueDate.toDate().time
            taskTime >= startCal.timeInMillis && taskTime <= endCal.timeInMillis
        }.map {
            dateOnlyFormat.format(it.dueDate.toDate())
        }.toSet()
    }


    /**
     * Mengambil data calendar untuk rentang 14 hari dengan OPTIMASI QUERY.
     */
    private suspend fun getCalendarData(): List<CalendarDayData> = withContext(Dispatchers.IO) {
        val dataList = mutableListOf<CalendarDayData>()
        val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // [OPTIMASI] Lakukan 1 query besar dan dapatkan semua tanggal yang memiliki task
        val hasTaskMapKeys = getTasksForDateWindow()


        val startCal = Calendar.getInstance()
        val tempStartCal = startCal.clone() as Calendar

        // Tentukan tanggal mulai (sekitar 7 hari sebelum hari ini)
        tempStartCal.add(Calendar.DAY_OF_YEAR, -(NUM_DAYS_TO_SHOW / 2))
        tempStartCal.set(Calendar.HOUR_OF_DAY, 0)
        tempStartCal.set(Calendar.MINUTE, 0)
        tempStartCal.set(Calendar.SECOND, 0)
        tempStartCal.set(Calendar.MILLISECOND, 0)

        for (i in 0 until NUM_DAYS_TO_SHOW) {
            val cal = tempStartCal.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, i)

            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val month = SimpleDateFormat("MMM", Locale("in", "ID")).format(cal.time).lowercase(Locale.getDefault())
            val dayOfWeek = SimpleDateFormat("EEE", Locale("en", "US")).format(cal.time)

            // Pengecekan tanggal yang sedang aktif dipilih
            val isSelected = (cal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR))

            val dateForCheck = cal.clone() as Calendar
            dateForCheck.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            dateForCheck.set(Calendar.HOUR_OF_DAY, 0)
            dateForCheck.set(Calendar.MINUTE, 0)
            dateForCheck.set(Calendar.SECOND, 0)
            dateForCheck.set(Calendar.MILLISECOND, 0)

            val dateKey = dateOnlyFormat.format(dateForCheck.time)

            // [OPTIMASI] Lakukan pengecekan O(1) in-memory
            val hasTasks = hasTaskMapKeys.contains(dateKey)

            dataList.add(CalendarDayData(month, dayOfWeek, dayOfMonth, isSelected, cal.timeInMillis, hasTasks))
        }
        return@withContext dataList
    }


    /**
     * Membuat tampilan chip tanggal untuk kalender horizontal.
     */
    private fun createDateItemView(
        month: String,
        dayOfWeek: String,
        dayOfMonth: Int,
        isSelected: Boolean,
        dayMillis: Long,
        hasTasks: Boolean
    ): View {
        val context = this

        val backgroundColor = if (isSelected) COLOR_ACTIVE_SELECTION else Color.WHITE
        val textColor = if (isSelected) COLOR_SELECTED_TEXT else COLOR_DEFAULT_TEXT

        val containerLayoutParams = LinearLayout.LayoutParams(
            ITEM_WIDTH_DP.dp,
            70.dp
        ).apply {
            setMargins(4.dp, 0, 4.dp, 0)
        }

        val container = LinearLayout(context).apply {
            layoutParams = containerLayoutParams
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = createRoundedBackgroundWithBorder(backgroundColor, isSelected)
            elevation = 4f
            setPadding(6.dp, 6.dp, 6.dp, 6.dp)
            tag = dayMillis
        }

        val textLayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val monthText = TextView(context).apply {
            text = month
            textSize = 12f
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setTextColor(textColor)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = textLayoutParams
        }
        container.addView(monthText)

        val dayOfWeekText = TextView(context).apply {
            text = dayOfWeek
            textSize = 11f
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setTextColor(textColor)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = textLayoutParams
        }
        container.addView(dayOfWeekText)

        val dayNumberText = TextView(context).apply {
            text = dayOfMonth.toString()
            textSize = 15f
            val font = ResourcesCompat.getFont(context, R.font.lexend)
            typeface = Typeface.create(font, Typeface.BOLD)
            setTextColor(textColor)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = textLayoutParams
            setPadding(0, 0, 0, 1.dp)
        }
        container.addView(dayNumberText)

        if (hasTasks) {
            val dotColor = if (isSelected) Color.WHITE else COLOR_DOT_INDICATOR
            val dotView = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = 6.dp
                    height = 6.dp
                }
                background = createDotDrawable(dotColor)
            }
            container.addView(dotView)
        }

        // LOGIKA KLIK: Mengubah tanggal yang dipilih dan memuat ulang data
        container.setOnClickListener {
            val newSelectedMillis = it.tag as Long

            val newSelectedDate = Calendar.getInstance()
            newSelectedDate.timeInMillis = newSelectedMillis

            newSelectedDate.set(Calendar.HOUR_OF_DAY, 0)
            newSelectedDate.set(Calendar.MINUTE, 0)
            newSelectedDate.set(Calendar.SECOND, 0)
            newSelectedDate.set(Calendar.MILLISECOND, 0)

            this@TaskActivity.selectedDate = newSelectedDate

            loadAllContent() // Panggil pemuatan utama
            // Scroll ke tanggal baru
            calendarMain.post { scrollToSelectedDate(smooth = true) }
        }

        return container
    }

    private fun scrollToSelectedDate(smooth: Boolean = false) {
        val daysInYear = NUM_DAYS_TO_SHOW // 365 days
        val daysBeforeToday = daysInYear / 2 // ~182 days before today

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startRangeDate = today.clone() as Calendar
        startRangeDate.add(Calendar.DAY_OF_YEAR, -daysBeforeToday)

        val diffMillis = selectedDate.timeInMillis - startRangeDate.timeInMillis
        val daysDifference = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        // Chip width + margins (4dp left + 4dp right) = 68 + 8 = 76 dp
        val itemWidthPx = ITEM_WIDTH_DP.dp + 8.dp

        // Hitung posisi horizontal
        val potentialScrollPosition = daysDifference * itemWidthPx

        // Tentukan posisi scroll agar tanggal yang dipilih berada di tengah
        val centerOffset = (calendarMain.width / 2) - (ITEM_WIDTH_DP.dp / 2)
        val finalScrollPosition = (potentialScrollPosition - centerOffset).coerceAtLeast(0)


        if (smooth) {
            calendarMain.smoothScrollTo(finalScrollPosition, 0)
        } else {
            calendarMain.scrollTo(finalScrollPosition, 0)
        }
    }

    private fun setDynamicMonthYear() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        octoberText.text = sdf.format(selectedDate.time)
    }

    private fun createRoundedBackgroundWithBorder(fillColor: Int, isSelected: Boolean): GradientDrawable {
        val cornerRadiusPx = CORNER_RADIUS_DP.dp.toFloat()
        val borderWidthPx = BORDER_WIDTH_DP.dp

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            cornerRadius = cornerRadiusPx

            if (!isSelected) {
                setStroke(borderWidthPx, BORDER_COLOR)
            }
        }
    }

    private fun handleIncomingTaskIntent(intent: Intent?) {
        val selectedMillis = intent?.getLongExtra(EXTRA_SELECTED_DATE_MILLIS, -1L) ?: -1L

        if (intent != null && selectedMillis != -1L) {
            val newDate = selectedDate.clone() as Calendar
            newDate.timeInMillis = selectedMillis

            newDate.set(Calendar.HOUR_OF_DAY, 0)
            newDate.set(Calendar.MINUTE, 0)
            newDate.set(Calendar.SECOND, 0)
            newDate.set(Calendar.MILLISECOND, 0)

            this.selectedDate = newDate

            intent.removeExtra(EXTRA_SELECTED_DATE_MILLIS)
        }
    }

    private fun showStreakSuccessDialog(newStreak: Int) {
        val layoutResId = resources.getIdentifier("dialog_streak_success", "layout", packageName)

        if (layoutResId == 0) {
            Log.e("TaskActivity", "FATAL: Layout 'dialog_streak_success.xml' not found. Dialog cannot be shown.")
            return
        }

        try {
            val dialogView = LayoutInflater.from(this).inflate(layoutResId, null)

            val tvStreakMessage = dialogView.findViewById<TextView>(R.id.tv_streak_message)
            val btnOk = dialogView.findViewById<Button>(R.id.btn_ok)

            if (tvStreakMessage == null || btnOk == null) {
                Log.e("TaskActivity", "FATAL: Views inside dialog_streak_success not found (tv_streak_message or btn_ok). Check IDs.")
                return
            }

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
            Log.e("TaskActivity", "Error showing streak dialog: ${e.message}", e)
        }
    }

    // MEMPERBAIKI PEMANGGILAN SUSPEND FUNCTION DAN MENGHAPUS LOCAL STORAGE
    private suspend fun updateStreakOnTaskComplete(): Int = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val todayCalendar = Calendar.getInstance()

        // GANTI: Ambil status streak dari TaskRepository
        val currentState = TaskRepository.getCurrentUserStreakState()
        val oldStreak = currentState.currentStreak
        val lastDateStr = currentState.lastCompletionDate

        // MEMANGGIL SUSPEND FUNCTION
        val completedToday = TaskRepository.getCompletedTasksByDate(todayCalendar)
        val hasCompletedToday = completedToday.isNotEmpty()

        var newStreak = oldStreak
        var shouldUpdate = false
        var streakIncreased = false

        when {
            lastDateStr == null -> {
                if (hasCompletedToday) {
                    newStreak = 1
                    shouldUpdate = true
                    streakIncreased = true
                }
            }

            lastDateStr == todayStr -> {
                // Streak tidak bertambah
            }

            isYesterday(lastDateStr, todayStr) -> {
                if (hasCompletedToday) {
                    newStreak = oldStreak + 1
                    shouldUpdate = true
                    streakIncreased = true
                }
            }

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


            val newStreakDays = if (newStreak > oldStreak) {
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

            val newState = StreakState(
                currentStreak = newStreak,
                lastCompletionDate = if (newStreak > 0) todayStr else null,
                streakDays = newStreakDays
            )

            // GANTI: Simpan status streak ke TaskRepository (remote)
            TaskRepository.saveCurrentUserStreakState(newState)
        }

        return@withContext if (streakIncreased) newStreak else oldStreak
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

    private fun isYesterday(lastDateStr: String?, todayStr: String): Boolean {
        if (lastDateStr == null) return false
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = sdf.format(yesterday.time)
        return lastDateStr == yesterdayStr
    }

    // Ganti fungsi addNewTaskToUI di TaskActivity.kt dengan yang ini:
    private fun addNewTaskToUI(task: Task) {
        val context = this
        val oldStreak = 0 // Placeholder

        // 1. Muat layout item tugas
        val mainContainer = LayoutInflater.from(context).inflate(R.layout.list_item_task, tasksContainer, false) as LinearLayout
        mainContainer.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(16.dp, 0, 16.dp, 16.dp)
        }

        // 2. Dapatkan referensi views dari layout item
        val taskItem = mainContainer.findViewById<LinearLayout>(R.id.taskItem)
        val checklistBox = mainContainer.findViewById<View>(R.id.checklistBox)
        val taskTitle = mainContainer.findViewById<TextView>(R.id.taskTitle)
        val taskTime = mainContainer.findViewById<TextView>(R.id.taskTime)
        val taskCategoryXml = mainContainer.findViewById<TextView>(R.id.taskCategory)
        val exclamationIcon = mainContainer.findViewById<ImageView>(R.id.exclamationIcon)
        val arrowRight = mainContainer.findViewById<ImageView>(R.id.arrowRight)
        val actionButtonsContainer = mainContainer.findViewById<LinearLayout>(R.id.actionButtonsContainer)

        val btnFlowTimer = mainContainer.findViewById<LinearLayout>(R.id.btnFlowTimer)
        val btnEdit = mainContainer.findViewById<LinearLayout>(R.id.btnEdit)
        val btnDelete = mainContainer.findViewById<LinearLayout>(R.id.btnDelete)

        // 3. Mengisi data
        taskTitle.text = task.title

        // Ambil semua data yang mungkin ditampilkan
        val timeText = task.time.trim()
        val categoryText = task.category.trim()
        val detailsText = task.details.trim()

        // Cek apakah time hanya berisi Flow Timer
        val isFlowTimerOnly = task.flowDurationMillis > 0L && timeText.contains("(Flow)")
        val hasValidTime = timeText.isNotEmpty() && !isFlowTimerOnly

        // LOGIKA TAMPILAN: Location, Time, dan Details masing-masing di baris terpisah
        // Gunakan LinearLayout vertical untuk menampung semua field

        // Hapus semua child views dari container yang ada (jika perlu)
        val infoContainer = mainContainer.findViewById<LinearLayout>(R.id.taskItem)
            ?.findViewById<LinearLayout>(android.R.id.content) // Ini placeholder, sebenarnya kita akan modifikasi taskTime & taskCategory

        // Karena kita hanya punya 2 TextView (taskTime dan taskCategory),
        // kita akan gunakan keduanya untuk menampilkan maksimal 2 baris pertama
        // Dan tambahkan TextView baru untuk baris ketiga jika diperlukan

        // Strategi: Tampilkan di baris terpisah sebisa mungkin
        val displayLines = mutableListOf<String>()

        if (categoryText.isNotEmpty()) displayLines.add(categoryText)
        if (hasValidTime) displayLines.add(timeText)
        if (detailsText.isNotEmpty()) displayLines.add(detailsText)

        when (displayLines.size) {
            0 -> {
                taskTime.visibility = View.GONE
                taskCategoryXml.visibility = View.GONE
            }
            1 -> {
                taskTime.text = displayLines[0]
                taskTime.visibility = View.VISIBLE
                taskCategoryXml.visibility = View.GONE
            }
            2 -> {
                taskTime.text = displayLines[0]
                taskTime.visibility = View.VISIBLE
                taskCategoryXml.text = displayLines[1]
                taskCategoryXml.visibility = View.VISIBLE
            }
            3 -> {
                // Semua ada: Location, Time, Details
                taskTime.text = displayLines[0] // Location
                taskTime.visibility = View.VISIBLE
                // Gabungkan Time dan Details di baris 2 karena kita cuma punya 2 TextView
                taskCategoryXml.text = "${displayLines[1]}\n${displayLines[2]}"
                taskCategoryXml.visibility = View.VISIBLE
                taskCategoryXml.maxLines = 2
                taskCategoryXml.ellipsize = android.text.TextUtils.TruncateAt.END
            }
        }

        // 4. Logika prioritas
        if (task.priority != "None") {
            val colorResId = priorityColorMap[task.priority] ?: R.color.dark_blue
            val colorInt = ContextCompat.getColor(context, colorResId)
            exclamationIcon.visibility = View.VISIBLE
            exclamationIcon.setColorFilter(colorInt)
            exclamationIcon.contentDescription = "${task.priority} Priority"
        } else {
            exclamationIcon.visibility = View.GONE
        }

        // Flow Timer button state
        checklistBox.setBackgroundResource(R.drawable.bg_checklist)
        if (task.flowDurationMillis <= 0L) {
            btnFlowTimer.isEnabled = false
            btnFlowTimer.alpha = 0.5f
        } else {
            btnFlowTimer.isEnabled = true
            btnFlowTimer.alpha = 1.0f
        }

        // 5. Setup Listeners
        checklistBox.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val oldStreak = TaskRepository.getCurrentUserStreakState().currentStreak
                val success = TaskRepository.completeTask(task.id)
                withContext(Dispatchers.Main) {
                    if (success) {
                        val newStreak = updateStreakOnTaskComplete()
                        showConfirmationDialogWithStreakCheck(task.title, "selesai", newStreak, oldStreak)

                        val slideRight = AnimationUtils.loadAnimation(context, R.anim.slide_out_right)
                        mainContainer.startAnimation(slideRight)

                        Handler(Looper.getMainLooper()).postDelayed({
                            tasksContainer.removeView(mainContainer)
                            loadAllContent()
                        }, 300)
                    } else {
                        Toast.makeText(context, "Failed to mark task as complete.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnFlowTimer.setOnClickListener {
            val intent = Intent(context, FlowTimerActivity::class.java).apply {
                putExtra(FlowTimerActivity.EXTRA_TASK_NAME, task.title)
                putExtra(FlowTimerActivity.EXTRA_FLOW_DURATION, task.flowDurationMillis)
                putExtra(FlowTimerActivity.EXTRA_TASK_ID, task.id) // TAMBAHKAN INI
            }
            // Gunakan launcher agar bisa menerima result
            taskEditLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnEdit.setOnClickListener {
            val intent = Intent(context, EditTaskActivity::class.java).apply {
                putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id)
            }
            taskEditLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnDelete.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val success = TaskRepository.deleteTask(task.id)
                withContext(Dispatchers.Main) {
                    if (success) {
                        showConfirmationDialogWithStreakCheck(task.title, "dihapus", -1, -1)

                        val slideRight = AnimationUtils.loadAnimation(context, R.anim.slide_out_right)
                        mainContainer.startAnimation(slideRight)

                        Handler(Looper.getMainLooper()).postDelayed({
                            tasksContainer.removeView(mainContainer)
                            loadAllContent()
                        }, 300)
                    } else {
                        Toast.makeText(context, "Failed to delete task.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        taskItem.setOnClickListener {
            if (actionButtonsContainer.visibility == View.GONE) {
                actionButtonsContainer.visibility = View.VISIBLE
                arrowRight.rotation = 90f
            } else {
                actionButtonsContainer.visibility = View.GONE
                arrowRight.rotation = 0f
            }
        }

        tasksContainer.addView(mainContainer)
    }

    private fun updateEmptyState(taskCount: Int) {
        tvNoActivity.visibility = if (taskCount == 0) View.VISIBLE else View.GONE
    }

    private fun showConfirmationDialogWithStreakCheck(taskTitle: String, action: String, newStreak: Int, oldStreak: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_success, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val mainMessageTextView = (dialogView as ViewGroup).getChildAt(0) as? TextView

        val btnConfirm1 = dialogView.findViewById<TextView>(R.id.btnIgnore)
        val btnConfirm2 = dialogView.findViewById<TextView>(R.id.btnView)

        val message = if (action == "selesai") {
            "Congratulations! Task  '$taskTitle' successfully completed."
        } else {
            "Task  '$taskTitle' successfully deleted."
        }

        mainMessageTextView?.text = message
        mainMessageTextView?.setTextColor(Color.parseColor("#283F6D"))

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val dismissListener = View.OnClickListener {
            dialog.dismiss()

            if (action == "selesai" && newStreak > oldStreak) {
                showStreakSuccessDialog(newStreak)
            }
        }

        // Memastikan hanya satu tombol OK yang muncul
        btnConfirm2.visibility = View.GONE
        val buttonContainer = dialogView.getChildAt(2) as LinearLayout
        val verticalDivider = buttonContainer.getChildAt(1)
        verticalDivider.visibility = View.GONE

        btnConfirm1.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 2.0f).apply {
            gravity = Gravity.CENTER
        }

        btnConfirm1.text = "OK"
        btnConfirm1.setTextColor(Color.parseColor("#283F6D"))

        btnConfirm1.setOnClickListener(dismissListener)

        dialog.show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}