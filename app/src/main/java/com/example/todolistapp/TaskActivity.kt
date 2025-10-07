package com.example.todolistapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.hypot
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
import android.os.Looper
import android.view.animation.AnimationUtils

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

    private val COLOR_ACTIVE_SELECTION = Color.parseColor("#283F6D")
    private val COLOR_DEFAULT_TEXT = Color.BLACK
    private val COLOR_SELECTED_TEXT = Color.WHITE
    private val COLOR_DOT_INDICATOR = Color.parseColor("#283F6D")
    private val CORNER_RADIUS_DP = 20
    private val BORDER_WIDTH_DP = 2
    private val BORDER_COLOR = Color.parseColor("#E0E0E0")
    private val ITEM_WIDTH_DP = 68
    private val NUM_DAYS_TO_SHOW = 365

    private val EXTRA_SELECTED_DATE_MILLIS = "EXTRA_SELECTED_DATE_MILLIS"

    // Streak SharedPreferences
    private val PREFS_NAME = "TimyTimePrefs"
    private val KEY_STREAK = "current_streak"
    private val KEY_LAST_DATE = "last_completion_date"
    private val KEY_STREAK_DAYS = "streak_days"

    companion object {
        const val RESULT_TASK_DELETED = 101
    }

    private val taskEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || result.resultCode == RESULT_TASK_DELETED) {
            TaskRepository.processTasksForMissed()
            loadTasksForSelectedDate()
            // Update kalender untuk refresh titik indikator
            generateYearlyCalendarViews()
        }
    }

    private val addTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadTasksForSelectedDate()
            // Update kalender untuk menampilkan titik di tanggal baru
            generateYearlyCalendarViews()
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

        // PENTING: Inisialisasi TaskRepository untuk load data tersimpan
        TaskRepository.initialize(applicationContext)

        tasksContainer = findViewById(R.id.tasksContainer)
        tvNoActivity = findViewById(R.id.tvNoActivity)
        octoberText = findViewById(R.id.octoberText)
        calendarMain = findViewById(R.id.calendar_main)
        dateItemsContainer = findViewById(R.id.date_items_container)

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.itemIconTintList = null

        currentCalendar = Calendar.getInstance()
        selectedDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        generateYearlyCalendarViews()
        setDynamicMonthYear()
        handleIncomingTaskIntent(intent)

        calendarMain.post {
            if (!hasScrolledToToday) {
                scrollToSelectedDate(smooth = false)
                hasScrolledToToday = true
            }
        }

        findViewById<ImageView?>(R.id.btn_search)?.setOnClickListener {
            startActivity(Intent(this, SearchFilterActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        findViewById<View>(R.id.reminderContainer)?.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java).apply {
                val selectedDateCopy = selectedDate.clone() as Calendar
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
                    startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    true
                }
                R.id.nav_tasks -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    true
                }
                else -> false
            }
        }

        TaskRepository.processTasksForMissed()
        loadTasksForSelectedDate()
    }

    override fun onStart() {
        super.onStart()
        bottomNav.selectedItemId = R.id.nav_tasks
    }

    override fun onResume() {
        super.onResume()

        currentCalendar = Calendar.getInstance()

        TaskRepository.processTasksForMissed()
        loadTasksForSelectedDate()

        generateYearlyCalendarViews()
        setDynamicMonthYear()

        calendarMain.post {
            scrollToSelectedDate(smooth = true)
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

    private fun generateYearlyCalendarViews() {
        dateItemsContainer.removeAllViews()

        val startCal = Calendar.getInstance()
        val tempStartCal = startCal.clone() as Calendar
        tempStartCal.add(Calendar.DAY_OF_YEAR, -(NUM_DAYS_TO_SHOW / 2))

        for (i in 0 until NUM_DAYS_TO_SHOW) {
            val cal = tempStartCal.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, i)

            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val month = SimpleDateFormat("MMM", Locale("in", "ID")).format(cal.time).lowercase()
            val dayOfWeek = SimpleDateFormat("EEE", Locale("en", "US")).format(cal.time)

            val isSelected = (cal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR))

            val dayItemContainer = createDateItemView(month, dayOfWeek, dayOfMonth, isSelected, cal.timeInMillis)
            dateItemsContainer.addView(dayItemContainer)
        }
    }

    private fun createDateItemView(
        month: String,
        dayOfWeek: String,
        dayOfMonth: Int,
        isSelected: Boolean,
        dayMillis: Long
    ): View {
        val context = this

        val dateForCheck = Calendar.getInstance().apply { timeInMillis = dayMillis }
        dateForCheck.set(Calendar.HOUR_OF_DAY, 0)
        dateForCheck.set(Calendar.MINUTE, 0)
        dateForCheck.set(Calendar.SECOND, 0)
        dateForCheck.set(Calendar.MILLISECOND, 0)

        TaskRepository.processTasksForMissed()
        val hasTasks = TaskRepository.hasTasksOnDate(dateForCheck)

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

        container.setOnClickListener {
            val newSelectedMillis = it.tag as Long

            val newSelectedDate = selectedDate.clone() as Calendar
            newSelectedDate.timeInMillis = newSelectedMillis

            newSelectedDate.set(Calendar.HOUR_OF_DAY, 0)
            newSelectedDate.set(Calendar.MINUTE, 0)
            newSelectedDate.set(Calendar.SECOND, 0)
            newSelectedDate.set(Calendar.MILLISECOND, 0)

            this@TaskActivity.selectedDate = newSelectedDate

            generateYearlyCalendarViews()
            setDynamicMonthYear()
            loadTasksForSelectedDate()
            scrollToSelectedDate(smooth = true)
        }

        return container
    }

    private fun scrollToSelectedDate(smooth: Boolean = false) {
        val startCal = Calendar.getInstance().clone() as Calendar
        startCal.add(Calendar.DAY_OF_YEAR, -(NUM_DAYS_TO_SHOW / 2))
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val diffMillis = selectedDate.timeInMillis - startCal.timeInMillis
        val daysDifference = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        val itemWidthPx = ITEM_WIDTH_DP.dp + 8.dp

        val centerOffset = (calendarMain.width / 2) - (ITEM_WIDTH_DP.dp / 2)
        val scrollPosition = (daysDifference * itemWidthPx) - centerOffset

        if (smooth) {
            calendarMain.smoothScrollTo(scrollPosition.coerceAtLeast(0), 0)
        } else {
            calendarMain.scrollTo(scrollPosition.coerceAtLeast(0), 0)
        }
    }

    private fun loadTasksForSelectedDate() {
        tasksContainer.removeAllViews()
        TaskRepository.processTasksForMissed()

        val selectedDateLocal = selectedDate.clone() as Calendar
        selectedDateLocal.set(Calendar.HOUR_OF_DAY, 0)
        selectedDateLocal.set(Calendar.MINUTE, 0)
        selectedDateLocal.set(Calendar.SECOND, 0)
        selectedDateLocal.set(Calendar.MILLISECOND, 0)

        val tasks = TaskRepository.getTasksByDate(selectedDateLocal)
        for (task in tasks) {
            addNewTaskToUI(task)
        }
        updateEmptyState(tasks.size)
    }

    private fun setDynamicMonthYear() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))
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

    private fun createRoundedBackground(color: Int): GradientDrawable {
        val cornerRadiusPx = CORNER_RADIUS_DP.dp.toFloat()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = cornerRadiusPx
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

        if (intent != null && intent.getBooleanExtra("SHOULD_ADD_TASK", false)) {
            loadTasksForSelectedDate()
            intent.removeExtra("SHOULD_ADD_TASK")
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
            tvStreakMessage.setTextColor(Color.parseColor("#FFC107"))

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

    private fun updateStreakOnTaskComplete(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val todayCalendar = Calendar.getInstance()

        val oldStreak = prefs.getInt(KEY_STREAK, 0)
        val lastDateStr = prefs.getString(KEY_LAST_DATE, null)

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
                    newStreak = 0
                    shouldUpdate = true
                }
            }
        }

        if (shouldUpdate) {
            val streakDays = prefs.getString(KEY_STREAK_DAYS, "") ?: ""
            val currentDay = getCurrentDayOfWeek()
            val existingDays = streakDays.split(",").mapNotNull { it.toIntOrNull() }

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

            prefs.edit().apply {
                putInt(KEY_STREAK, newStreak)
                putString(KEY_LAST_DATE, if (newStreak > 0) todayStr else null)
                putString(KEY_STREAK_DAYS, newStreakDays)
                apply()
            }
        }

        return if (streakIncreased) newStreak else oldStreak
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

    private fun addNewTaskToUI(task: Task) {
        val context = this
        val marginPx = 16.dp
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val oldStreak = sharedPrefs.getInt(KEY_STREAK, 0)

        val mainContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(marginPx, 0, marginPx, marginPx)
            }
            orientation = LinearLayout.VERTICAL
        }

        val taskItem = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 80.dp
            )
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_task, null)
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            elevation = 4f
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
        }

        val checklistBox = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(24.dp, 24.dp).apply {
                marginEnd = 16.dp
            }
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_checklist, null)

            setOnClickListener {
                val success = TaskRepository.completeTask(task.id)
                if (success) {
                    val newStreak = updateStreakOnTaskComplete()
                    showConfirmationDialogWithStreakCheck(task.title, "selesai", newStreak, oldStreak)

                    val slideRight = AnimationUtils.loadAnimation(context, R.anim.slide_out_right)
                    mainContainer.startAnimation(slideRight)

                    Handler(Looper.getMainLooper()).postDelayed({
                        tasksContainer.removeView(mainContainer)
                        loadTasksForSelectedDate()
                        // Update kalender untuk menghapus titik jika tidak ada task
                        generateYearlyCalendarViews()
                    }, 300)
                } else {
                    Toast.makeText(context, "Gagal menandai tugas selesai.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        taskItem.addView(checklistBox)

        val titleAndPriorityContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val taskTitle = TextView(context).apply {
            text = task.title
            textSize = 16f
            setTextColor(Color.parseColor("#14142A"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        titleAndPriorityContainer.addView(taskTitle)

        if (task.priority != "None") {
            val colorResId = priorityColorMap[task.priority] ?: R.color.dark_blue
            val colorInt = ContextCompat.getColor(context, colorResId)

            val exclamationIcon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(16.dp, 16.dp).apply {
                    marginStart = 8.dp
                    gravity = Gravity.CENTER_VERTICAL
                }
                setImageResource(R.drawable.ic_missed)
                contentDescription = "${task.priority} Priority"
                setColorFilter(colorInt)
            }
            titleAndPriorityContainer.addView(exclamationIcon)
        }

        val detailWrapper = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
            ).apply {
                marginStart = 16.dp
            }
            orientation = LinearLayout.VERTICAL
        }

        val taskTime = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = task.time.ifEmpty { "" }
            textSize = 12f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            gravity = Gravity.END
        }
        detailWrapper.addView(taskTime)

        val taskCategory = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = task.category.ifEmpty { "" }
            textSize = 12f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            gravity = Gravity.END
        }
        detailWrapper.addView(taskCategory)

        taskItem.addView(titleAndPriorityContainer)
        taskItem.addView(detailWrapper)

        val arrowRight = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = 8.dp
            }
            setImageResource(R.drawable.baseline_arrow_forward_ios_24)
            setColorFilter(Color.parseColor("#283F6D"))
            rotation = 0f
        }
        taskItem.addView(arrowRight)

        mainContainer.addView(taskItem)

        val actionButtonsContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.GONE
        }

        fun createActionButton(iconResId: Int, buttonText: String, onClick: () -> Unit): LinearLayout {
            return LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
                ).apply {
                    setMargins(4.dp, 0, 4.dp, 0)
                }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener { onClick() }

                background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_task, null)
                setPadding(8.dp, 12.dp, 8.dp, 12.dp)

                addView(ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(32.dp, 32.dp)
                    setImageResource(iconResId)
                    setColorFilter(Color.parseColor("#283F6D"))
                })

                addView(TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    text = buttonText
                    textSize = 10f
                    setTextColor(Color.parseColor("#283F6D"))
                    typeface = ResourcesCompat.getFont(context, R.font.lexend)
                    gravity = Gravity.CENTER_HORIZONTAL
                })
            }
        }

        val flowTimerButton = createActionButton(R.drawable.ic_alarm, "Flow Timer") {
            val intent = Intent(context, FlowTimerActivity::class.java).apply {
                putExtra(FlowTimerActivity.EXTRA_TASK_NAME, task.title)
                putExtra(FlowTimerActivity.EXTRA_FLOW_DURATION, task.flowDurationMillis)
            }
            startActivity(intent)
        }

        val editButton = createActionButton(R.drawable.ic_edit, "Edit") {
            val intent = Intent(context, EditTaskActivity::class.java).apply {
                putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id)
            }
            taskEditLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val deleteButton = createActionButton(R.drawable.ic_trash, "Delete") {
            val success = TaskRepository.deleteTask(task.id)
            if (success) {
                showConfirmationDialogWithStreakCheck(task.title, "dihapus", -1, -1)

                val slideRight = AnimationUtils.loadAnimation(context, R.anim.slide_out_right)
                mainContainer.startAnimation(slideRight)

                Handler(Looper.getMainLooper()).postDelayed({
                    tasksContainer.removeView(mainContainer)
                    loadTasksForSelectedDate()
                    // Update kalender untuk menghapus titik jika tidak ada task
                    generateYearlyCalendarViews()
                }, 300)
            } else {
                Toast.makeText(context, "Gagal menghapus tugas.", Toast.LENGTH_SHORT).show()
            }
        }

        actionButtonsContainer.addView(flowTimerButton)
        actionButtonsContainer.addView(editButton)
        actionButtonsContainer.addView(deleteButton)

        mainContainer.addView(actionButtonsContainer)

        taskItem.setOnClickListener {
            if (actionButtonsContainer.visibility == View.GONE) {
                actionButtonsContainer.visibility = View.VISIBLE
                arrowRight.rotation = 90f
            } else {
                actionButtonsContainer.visibility = View.GONE
                arrowRight.rotation = 0f
            }
        }

        tasksContainer.addView(mainContainer, 0)
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
            "Selamat! Tugas '$taskTitle' berhasil diselesaikan."
        } else {
            "Tugas '$taskTitle' berhasil dihapus."
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

        btnConfirm1.text = "OK"
        btnConfirm2.text = "Tutup"

        btnConfirm1.setTextColor(Color.parseColor("#283F6D"))
        btnConfirm2.setTextColor(Color.parseColor("#283F6D"))

        btnConfirm1.setOnClickListener(dismissListener)
        btnConfirm2.setOnClickListener(dismissListener)

        dialog.show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}