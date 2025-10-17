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
import android.view.LayoutInflater
import android.widget.Toast
import android.app.Dialog
import android.view.Window
import android.animation.AnimatorListenerAdapter
import android.animation.Animator
import android.graphics.Typeface
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import androidx.core.content.ContextCompat
import android.content.Context

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

        runBlocking { TaskRepository.updateMissedTasks() }

        monthText = findViewById(R.id.month_text)
        calendarGrid = findViewById(R.id.calendar_grid)
        val arrowLeft = findViewById<ImageView>(R.id.arrow_left)
        val arrowRight = findViewById<ImageView>(R.id.arrow_right)

        val addReminderButton = findViewById<LinearLayout>(R.id.addReminderButton)
        val rootSwipeView = findViewById<LinearLayout>(R.id.calendar_container)

        currentCalendar = Calendar.getInstance()
        selectedDate = currentCalendar.clone() as Calendar
        updateCalendar()

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

        rootSwipeView.setOnTouchListener(object : OnSwipeTouchListener(this) {
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

    private fun updateStreakOnTaskComplete() {
        runBlocking {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())

            val currentStreak = prefs.getInt(KEY_STREAK, 0)
            val lastDateStr = prefs.getString(KEY_LAST_DATE, null)

            val completedToday = TaskRepository.getCompletedTasksByDate(Calendar.getInstance())
            val hasCompletedToday = completedToday.isNotEmpty()

            var newStreak = currentStreak
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
                }
                isYesterday(lastDateStr, todayStr) -> {
                    if (hasCompletedToday) {
                        newStreak = currentStreak + 1
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
            tasks.forEach { task ->
                createInteractiveTaskItem(tasksContainer, task, dialog)
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

        sheetContainer.translationX = sheetContainer.layoutParams.width.toFloat()
        sheetContainer.post {
            sheetContainer.animate()
                .translationX(0f)
                .setDuration(300)
                .start()
        }

        dialog.show()
    }


    private fun createInteractiveTaskItem(container: LinearLayout, task: Task, dialog: Dialog) {
        val context = this
        val lexendFont = ResourcesCompat.getFont(this, R.font.lexend)
        val marginPx = 16.dp

        val decorView = dialog.window?.decorView as? ViewGroup
        val overlayContainerView = decorView?.getChildAt(0) as? LinearLayout
        val sheetContainerView = overlayContainerView?.getChildAt(0) as? LinearLayout

        val mainContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 10.dp)
            }
            orientation = LinearLayout.VERTICAL
        }

        val taskItem = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                80.dp
            )
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_task, null)
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            elevation = 4f
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
        }

        // ✅ CHECKLIST BOX - LANGSUNG COMPLETE TASK
        val checklistBox = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(24.dp, 24.dp).apply {
                marginEnd = 16.dp
            }
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_checklist, null)

            setOnClickListener {
                GlobalScope.launch(Dispatchers.Main) {
                    val success = TaskRepository.completeTask(task.id)
                    if (success) {
                        updateStreakOnTaskComplete()
                        if (overlayContainerView != null && sheetContainerView != null) {
                            performCloseAnimation(dialog, overlayContainerView, sheetContainerView)
                        } else {
                            dialog.dismiss()
                        }
                        showConfirmationDialog(task.title, "selesai")
                        updateCalendar()
                    } else {
                        Toast.makeText(context, "Gagal menandai tugas selesai. Pastikan Anda sudah login.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        taskItem.addView(checklistBox)

        val titleAndDetailContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
            )
            orientation = LinearLayout.VERTICAL
        }

        val titleAndPriorityContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val taskTitle = TextView(context).apply {
            text = task.title
            textSize = 16f
            setTextColor(Color.parseColor("#14142A"))
            typeface = lexendFont
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
        titleAndDetailContainer.addView(titleAndPriorityContainer)

        val taskTimeCategory = TextView(context).apply {
            val timeText = task.time.ifEmpty { "" }
            val categoryText = task.category.ifEmpty { "" }

            text = if (timeText.isNotEmpty() && categoryText.isNotEmpty()) {
                "$timeText - $categoryText"
            } else if (timeText.isNotEmpty()) {
                timeText
            } else if (categoryText.isNotEmpty()) {
                categoryText
            } else {
                ""
            }

            textSize = 12f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = lexendFont
        }
        titleAndDetailContainer.addView(taskTimeCategory)

        taskItem.addView(titleAndDetailContainer)

        val arrowRight = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
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
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
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
                    typeface = lexendFont
                    gravity = Gravity.CENTER_HORIZONTAL
                })
            }
        }

        val flowTimerButton = createActionButton(R.drawable.ic_alarm, "Flow Timer") {
            if (overlayContainerView != null && sheetContainerView != null) {
                performCloseAnimation(dialog, overlayContainerView, sheetContainerView)
            } else {
                dialog.dismiss()
            }
            val intent = Intent(context, FlowTimerActivity::class.java).apply {
                putExtra(FlowTimerActivity.EXTRA_TASK_NAME, task.title)
                putExtra(FlowTimerActivity.EXTRA_FLOW_DURATION, task.flowDurationMillis)
            }
            startActivity(intent)
        }

        val editButton = createActionButton(R.drawable.ic_edit, "Edit") {
            if (overlayContainerView != null && sheetContainerView != null) {
                performCloseAnimation(dialog, overlayContainerView, sheetContainerView)
            } else {
                dialog.dismiss()
            }
            val intent = Intent(context, EditTaskActivity::class.java).apply {
                putExtra("EXTRA_TASK_ID", task.id)
            }
            editTaskLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val deleteButton = createActionButton(R.drawable.ic_trash, "Delete") {
            GlobalScope.launch(Dispatchers.Main) {
                val success = TaskRepository.deleteTask(task.id)
                if (success) {
                    if (overlayContainerView != null && sheetContainerView != null) {
                        performCloseAnimation(dialog, overlayContainerView, sheetContainerView)
                    } else {
                        dialog.dismiss()
                    }
                    showConfirmationDialog(task.title, "dihapus")
                    updateCalendar()
                } else {
                    Toast.makeText(context, "Gagal menghapus tugas. Pastikan Anda sudah login.", Toast.LENGTH_SHORT).show()
                }
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

        btnConfirm2.setOnClickListener(dismissListener)

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        updateCalendar()
    }

    private fun updateCalendar() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthText.text = sdf.format(currentCalendar.time)

        calendarGrid.removeAllViews()

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

        val lexendFont = ResourcesCompat.getFont(this, R.font.lexend)

        for (i in 0 until totalCells) {
            val cellContainer = LinearLayout(this).apply {
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

            val dayView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER
                textSize = 16f
                typeface = lexendFont
            }
            cellContainer.addView(dayView)

            val dotView = View(this).apply {
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

                // Menggunakan wrapper sinkron
                val hasTasks = TaskRepository.hasTasksOnDate(dateForCheck)

                if (hasTasks) {
                    dotView.visibility = View.VISIBLE
                }

                val isToday = isCurrentMonthView && today.get(Calendar.DAY_OF_MONTH) == dayNumber
                val isSelected = isSelectedMonthView && selectedDate.get(Calendar.DAY_OF_MONTH) == dayNumber

                dayView.setTextColor(COLOR_DEFAULT_TEXT)
                cellContainer.background = createRoundedBackground(Color.WHITE)

                if (isToday) {
                    cellContainer.background = createRoundedBackground(COLOR_TODAY_HIGHLIGHT)
                    dayView.setTextColor(COLOR_DEFAULT_TEXT)
                }

                if (isSelected) {
                    cellContainer.background = createRoundedBackground(COLOR_ACTIVE_SELECTION)
                    dayView.setTextColor(COLOR_SELECTED_TEXT)
                    if(hasTasks) {
                        dotView.background = createDotDrawable(Color.WHITE)
                    }
                } else if(hasTasks) {
                    dotView.background = createDotDrawable(COLOR_ACTIVE_SELECTION)
                }

                cellContainer.setOnClickListener {
                    val newSelectedDate = dateForCheck

                    GlobalScope.launch(Dispatchers.Main) {
                        val tasksOnDate = TaskRepository.getTasksByDateSync(newSelectedDate)

                        if (tasksOnDate.isNotEmpty()) {
                            showTaskRightSheet(newSelectedDate, tasksOnDate)
                        }

                        selectedDate = newSelectedDate.clone() as Calendar
                        updateCalendar()
                    }
                }

                calendarGrid.addView(cellContainer)

            } else {
                cellContainer.visibility = View.INVISIBLE
                cellContainer.layoutParams.height = 0
                calendarGrid.addView(cellContainer)
            }
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}