// main/java/com/example/todolistapp/CalendarActivity.kt
package com.example.todolistapp

import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import android.graphics.drawable.GradientDrawable
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.graphics.Typeface
import android.widget.Toast
import android.app.Dialog
import android.view.Window
import android.content.Context
import android.animation.AnimatorListenerAdapter
import android.animation.Animator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class CalendarActivity : AppCompatActivity() {

    private lateinit var monthText: TextView
    private lateinit var calendarGrid: GridLayout
    private lateinit var currentCalendar: Calendar

    private var selectedDate: Calendar = Calendar.getInstance()
    private val COLOR_ACTIVE_SELECTION = Color.parseColor("#283F6D")
    private val COLOR_TODAY_HIGHLIGHT = Color.parseColor("#FFCC80")
    private val COLOR_DEFAULT_TEXT = Color.BLACK
    private val COLOR_SELECTED_TEXT = Color.WHITE

    private val CORNER_RADIUS_DP = 8

    private val EXTRA_SELECTED_DATE_MILLIS = "EXTRA_SELECTED_DATE_MILLIS"
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val priorityColorMap = mapOf(
        "Low" to R.color.low_priority,
        "Medium" to R.color.medium_priority,
        "High" to R.color.high_priority
    )

    private val PREFS_NAME = "TimyTimePrefs"
    private val KEY_STREAK = "current_streak"
    private val KEY_LAST_DATE = "last_completion_date"
    private val KEY_STREAK_DAYS = "streak_days"

    private val editTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateCalendar()
        }
    }

    private val addTaskFromCalendarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val taskIntent = Intent(this, TaskActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                putExtra("SHOULD_REFRESH_TASK", true)
            }
            startActivity(taskIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calendar)

        monthText = findViewById(R.id.month_text)
        calendarGrid = findViewById(R.id.calendar_grid)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val arrowLeft = findViewById<ImageView>(R.id.arrow_left)
        val arrowRight = findViewById<ImageView>(R.id.arrow_right)

        val addReminderButton = findViewById<LinearLayout>(R.id.addReminderButton)
        val rootSwipeView = findViewById<LinearLayout>(R.id.calendar_container)

        currentCalendar = Calendar.getInstance()
        selectedDate = currentCalendar.clone() as Calendar

        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                TaskRepository.updateMissedTasks()
            }
            updateCalendar()
        }

        btnBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        arrowLeft.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        arrowRight.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        addReminderButton.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java).apply {
                val selectedDayStart = selectedDate.clone() as Calendar
                selectedDayStart.set(Calendar.HOUR_OF_DAY, 12)
                putExtra(EXTRA_SELECTED_DATE_MILLIS, selectedDayStart.timeInMillis)
            }
            addTaskFromCalendarLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        rootSwipeView.setOnTouchListener(object : OnSwipeTouchListener(this@CalendarActivity) {
            override fun onSwipeLeft() {
                currentCalendar.add(Calendar.MONTH, 1)
                updateCalendar()
            }

            override fun onSwipeRight() {
                currentCalendar.add(Calendar.MONTH, -1)
                updateCalendar()
            }
        })
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun createDotDrawable(color: Int): GradientDrawable {
        val size = 6.dp
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setSize(size, size)
        }
    }

    private fun createRoundedBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = CORNER_RADIUS_DP.dp.toFloat()
        }
    }

    private suspend fun updateStreakOnTaskComplete() = withContext(Dispatchers.IO) {
        val context = this@CalendarActivity
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        val currentStreak = prefs.getInt(KEY_STREAK, 0)
        val lastDateStr = prefs.getString(KEY_LAST_DATE, null)

        val completedToday = TaskRepository.getCompletedTasksByDate(Calendar.getInstance())
        val hasCompletedToday = completedToday.isNotEmpty()

        var newStreak = currentStreak
        var shouldUpdate = false

        when {
            lastDateStr == null -> {
                if (hasCompletedToday) {
                    newStreak = 1
                    shouldUpdate = true
                }
            }
            lastDateStr == todayStr -> {
            }
            isYesterday(lastDateStr, todayStr) -> {
                if (hasCompletedToday) {
                    newStreak = currentStreak + 1
                    shouldUpdate = true
                }
            }
            else -> {
                if (hasCompletedToday) {
                    newStreak = 1
                    shouldUpdate = true
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

    private fun isYesterday(lastDateStr: String?, todayStr: String): Boolean {
        if (lastDateStr == null) return false
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = sdf.format(yesterday.time)
        return lastDateStr == yesterdayStr
    }

    private fun performCloseAnimation(dialog: Dialog, overlay: LinearLayout, sheet: LinearLayout) {
        sheet.animate()
            .translationX(sheet.width.toFloat())
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dialog.dismiss()
                }
            })
            .start()

        overlay.animate()
            .alpha(0f)
            .setDuration(250)
            .start()
    }

    private fun showTaskRightSheet(date: Calendar, tasks: List<Task>) {
        val lexendFont = ResourcesCompat.getFont(this, R.font.lexend)
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val overlayContainer = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#80000000"))
            gravity = Gravity.END
            alpha = 0f
        }

        val sheetContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            elevation = 8f
            setOnClickListener { }
        }

        overlayContainer.addView(sheetContainer)

        val headerContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
            setBackgroundColor(Color.WHITE)
        }

        val ivCloseButton = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(36.dp, 36.dp)
            setImageResource(R.drawable.ic_arrow_back)
            setColorFilter(Color.parseColor("#14142A"))
            setPadding(6.dp, 6.dp, 6.dp, 6.dp)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                performCloseAnimation(dialog, overlayContainer, sheetContainer)
            }
        }
        headerContainer.addView(ivCloseButton)

        val tvDrawerTitle = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
            ).apply {
                marginStart = 12.dp
            }
            text = "Tasks on ${uiDateFormat.format(date.time)}"
            textSize = 18f
            setTextColor(Color.parseColor("#14142A"))
            typeface = Typeface.create(lexendFont, Typeface.BOLD)
        }
        headerContainer.addView(tvDrawerTitle)

        sheetContainer.addView(headerContainer)

        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        val tasksContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 8.dp, 16.dp, 16.dp)
        }

        if (tasks.isEmpty()) {
            val noTask = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 50.dp, 0, 50.dp)
                }
                text = "Tidak ada tugas aktif di tanggal ini."
                textSize = 14f
                setTextColor(Color.parseColor("#98A8C8"))
                typeface = lexendFont
                gravity = Gravity.CENTER_HORIZONTAL
            }
            tasksContainer.addView(noTask)
        } else {
            // Menggunakan layout list_item_task.xml yang baru
            tasks.forEach { task ->
                createTaskItemUsingLayout(tasksContainer, task, dialog)
            }
        }

        scrollView.addView(tasksContainer)
        sheetContainer.addView(scrollView)

        dialog.setContentView(overlayContainer)

        overlayContainer.setOnClickListener {
            performCloseAnimation(dialog, overlayContainer, sheetContainer)
        }

        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        overlayContainer.animate().alpha(1f).setDuration(250).start()
        sheetContainer.translationX = sheetContainer.layoutParams.width.toFloat()
        sheetContainer.post {
            sheetContainer.animate()
                .translationX(0f)
                .setDuration(300)
                .start()
        }

        dialog.show()
    }

    /**
     * Membuat item tugas menggunakan list_item_task.xml
     */
    private fun createTaskItemUsingLayout(container: LinearLayout, task: Task, dialog: Dialog) {
        val context = this@CalendarActivity
        val lexendFont = ResourcesCompat.getFont(context, R.font.lexend)

        val decorView = dialog.window?.decorView as? ViewGroup
        val overlayContainerView = decorView?.getChildAt(0) as? LinearLayout
        val sheetContainerView = overlayContainerView?.getChildAt(0) as? LinearLayout

        // 1. Muat layout item tugas
        val mainContainer = LayoutInflater.from(context).inflate(R.layout.list_item_task, container, false) as LinearLayout
        mainContainer.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 10.dp)
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

        val timeText = task.time.ifEmpty { "" }
        val categoryText = task.category.ifEmpty { "" }

        if (categoryText.isNotEmpty() && timeText.isNotEmpty()) {
            taskTime.text = timeText
            taskCategoryXml.text = categoryText
            taskCategoryXml.visibility = View.VISIBLE
        } else if (timeText.isNotEmpty()) {
            taskTime.text = timeText
            taskCategoryXml.visibility = View.GONE
        } else if (categoryText.isNotEmpty()) {
            taskTime.text = categoryText
            taskCategoryXml.visibility = View.GONE
        } else {
            taskTime.visibility = View.GONE
            taskCategoryXml.visibility = View.GONE
        }


        // 4. Logika prioritas & status
        if (task.priority != "None") {
            val colorResId = priorityColorMap[task.priority] ?: R.color.dark_blue
            val colorInt = ContextCompat.getColor(context, colorResId)
            exclamationIcon.visibility = View.VISIBLE
            exclamationIcon.setColorFilter(colorInt)
            exclamationIcon.contentDescription = "${task.priority} Priority"
        } else {
            exclamationIcon.visibility = View.GONE
        }

        // Atur tampilan checklist (asumsi task di sini adalah pending)
        checklistBox.setBackgroundResource(R.drawable.bg_checklist)

        // Nonaktifkan flow timer jika durasinya 0
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
                val success = TaskRepository.completeTask(task.id)
                if (success) {
                    updateStreakOnTaskComplete()
                    withContext(Dispatchers.Main) {
                        if (overlayContainerView != null && sheetContainerView != null) {
                            performCloseAnimation(dialog, overlayContainerView, sheetContainerView)
                        } else {
                            dialog.dismiss()
                        }
                        showConfirmationDialog(task.title, "selesai")
                        updateCalendar() // Muat ulang kalender setelah selesai
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Gagal menandai tugas selesai. Pastikan Anda sudah login.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnFlowTimer.setOnClickListener {
            if (task.flowDurationMillis <= 0L) {
                Toast.makeText(context, "Tidak ada Flow Timer disetel untuk tugas ini.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (overlayContainerView != null && sheetContainerView != null) {
                performCloseAnimation(dialog, overlayContainerView, sheetContainerView)
            } else {
                dialog.dismiss()
            }
            val intent = Intent(context, FlowTimerActivity::class.java).apply {
                putExtra(FlowTimerActivity.EXTRA_TASK_NAME, task.title)
                putExtra(FlowTimerActivity.EXTRA_FLOW_DURATION, task.flowDurationMillis)
            }
            context.startActivity(intent)
        }

        btnEdit.setOnClickListener {
            if (overlayContainerView != null && sheetContainerView != null) {
                performCloseAnimation(dialog, overlayContainerView, sheetContainerView)
            } else {
                dialog.dismiss()
            }
            val intent = Intent(context, EditTaskActivity::class.java).apply {
                putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id)
            }
            editTaskLauncher.launch(intent)
            context.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnDelete.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val success = TaskRepository.deleteTask(task.id)
                withContext(Dispatchers.Main) {
                    if (success) {
                        if (overlayContainerView != null && sheetContainerView != null) {
                            performCloseAnimation(dialog, overlayContainerView, sheetContainerView)
                        } else {
                            dialog.dismiss()
                        }
                        showConfirmationDialog(task.title, "dihapus")
                        updateCalendar() // Muat ulang kalender setelah dihapus
                    } else {
                        Toast.makeText(context, "Gagal menghapus tugas. Pastikan Anda sudah login.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        taskItem.setOnClickListener {
            // Toggle visibility of actionButtonsContainer
            if (actionButtonsContainer.visibility == View.GONE) {
                actionButtonsContainer.visibility = View.VISIBLE
                arrowRight.rotation = 90f
            } else {
                actionButtonsContainer.visibility = View.GONE
                arrowRight.rotation = 0f
            }
        }

        container.addView(mainContainer)
    }

    private fun showConfirmationDialog(taskTitle: String, action: String) {
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
        }

        val buttonContainer = dialogView.getChildAt(2) as LinearLayout
        val verticalDivider = buttonContainer.getChildAt(1)

        btnConfirm1.visibility = View.GONE
        verticalDivider.visibility = View.GONE

        btnConfirm2.text = "OK"

        val viewParams = btnConfirm2.layoutParams as LinearLayout.LayoutParams
        viewParams.width = 0
        viewParams.weight = 2.0f
        btnConfirm2.layoutParams = viewParams
        btnConfirm2.gravity = Gravity.CENTER
        btnConfirm2.setTextColor(Color.parseColor("#283F6D"))

        btnConfirm2.setOnClickListener(dismissListener)

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        updateCalendar()
    }

    private fun updateCalendar() {
        lifecycleScope.launch(Dispatchers.Main) {
            val activityContext = this@CalendarActivity
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            monthText.text = sdf.format(currentCalendar.time)

            calendarGrid.removeAllViews()

            // Perbaikan: Pastikan updateMissedTasks dijalankan sebelum fetching data
            withContext(Dispatchers.IO) {
                TaskRepository.updateMissedTasks()
            }

            val allTasksInDateRange = withContext(Dispatchers.IO) {
                TaskRepository.getTasksInDateRangeForCalendar(currentCalendar)
            }

            val hasTaskMap = allTasksInDateRange.map {
                dateOnlyFormat.format(it.dueDate.toDate())
            }.toSet()

            val tasksByDateMap = allTasksInDateRange.groupBy {
                dateOnlyFormat.format(it.dueDate.toDate())
            }

            val tempCal = currentCalendar.clone() as Calendar
            tempCal.set(Calendar.DAY_OF_MONTH, 1)

            val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
            val firstDayOfWeek = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2

            val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val today = Calendar.getInstance()

            val isCurrentMonthView = (today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                    today.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH))

            val isSelectedMonthView = (selectedDate.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                    selectedDate.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH))

            val totalCells = 42

            val cellWidth = 48.dp
            val cellHeight = 60.dp
            val marginDp = 4
            val marginPx = marginDp.dp

            val lexendFont = ResourcesCompat.getFont(activityContext, R.font.lexend)

            for (i in 0 until totalCells) {
                val cellContainer = LinearLayout(activityContext).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = cellWidth
                        height = cellHeight
                        setMargins(marginPx, marginPx, marginPx, marginPx)
                        columnSpec = GridLayout.spec(i % 7)
                        rowSpec = GridLayout.spec(i / 7)
                    }
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    setPadding(0, 4.dp, 0, 0)
                }

                val dayView = TextView(activityContext).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    gravity = Gravity.CENTER
                    textSize = 16f
                    typeface = lexendFont
                }
                cellContainer.addView(dayView)

                val dotView = View(activityContext).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 2.dp
                        width = 6.dp
                        height = 6.dp
                    }
                    visibility = View.GONE
                }
                cellContainer.addView(dotView)

                if (i >= firstDayOfWeek && i < daysInMonth + firstDayOfWeek) {
                    val dayNumber = i - firstDayOfWeek + 1
                    dayView.text = dayNumber.toString()

                    val dateForCheck = currentCalendar.clone() as Calendar
                    dateForCheck.set(Calendar.DAY_OF_MONTH, dayNumber)
                    dateForCheck.set(Calendar.HOUR_OF_DAY, 0)
                    dateForCheck.set(Calendar.MINUTE, 0)
                    dateForCheck.set(Calendar.SECOND, 0)
                    dateForCheck.set(Calendar.MILLISECOND, 0)

                    val dateKey = dateOnlyFormat.format(dateForCheck.time)
                    val hasTasks = hasTaskMap.contains(dateKey)

                    val isSelected = isSelectedMonthView && selectedDate.get(Calendar.DAY_OF_MONTH) == dayNumber

                    cellContainer.visibility = View.VISIBLE
                    cellContainer.layoutParams.height = cellHeight

                    if (hasTasks) {
                        dotView.visibility = View.VISIBLE
                    } else {
                        dotView.visibility = View.GONE
                    }

                    val isToday = isCurrentMonthView && today.get(Calendar.DAY_OF_MONTH) == dayNumber

                    dayView.setTextColor(COLOR_DEFAULT_TEXT)
                    cellContainer.background = createRoundedBackground(Color.WHITE)

                    if (isToday) {
                        cellContainer.background = createRoundedBackground(COLOR_TODAY_HIGHLIGHT)
                        dayView.setTextColor(COLOR_DEFAULT_TEXT)
                    }

                    if (isSelected) {
                        cellContainer.background = createRoundedBackground(COLOR_ACTIVE_SELECTION)
                        dayView.setTextColor(COLOR_SELECTED_TEXT)
                        if (hasTasks) {
                            dotView.background = createDotDrawable(Color.WHITE)
                        }
                    } else if (hasTasks) {
                        dotView.background = createDotDrawable(COLOR_ACTIVE_SELECTION)
                    }

                    cellContainer.setOnClickListener {
                        val newSelectedDate = dateForCheck

                        val tasksOnDate = tasksByDateMap[dateKey] ?: emptyList()

                        if (tasksOnDate.isNotEmpty()) {
                            showTaskRightSheet(newSelectedDate, tasksOnDate)
                        }

                        selectedDate = newSelectedDate.clone() as Calendar
                        updateCalendar()
                    }

                    calendarGrid.addView(cellContainer)

                } else {
                    cellContainer.visibility = View.INVISIBLE
                    cellContainer.layoutParams.height = 0
                    calendarGrid.addView(cellContainer)
                }
            }
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}