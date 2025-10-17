package com.example.todolistapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import android.util.Log

class SearchFilterActivity : AppCompatActivity() {

    private lateinit var inputSearch: EditText
    private lateinit var taskResultsContainer: LinearLayout
    private lateinit var chipContainer: LinearLayout
    private lateinit var btnBack: ImageView

    private var activeMonthFilter: Int = -1
    private val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

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
            performSearch(inputSearch.text.toString(), activeMonthFilter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_filter)

        inputSearch = findViewById(R.id.input_search)
        chipContainer = findViewById(R.id.chip_container)
        btnBack = findViewById(R.id.btn_back)
        taskResultsContainer = findViewById(R.id.task_results_container_xml)

        taskResultsContainer.setPadding(16.dp, 16.dp, 16.dp, 16.dp)

        btnBack.setOnClickListener {
            finish()
        }

        generateMonthChips()

        inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                performSearch(s.toString(), activeMonthFilter)
            }
            override fun afterTextChanged(s: Editable) {}
        })

        inputSearch.setOnTouchListener(View.OnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableEnd = inputSearch.compoundDrawables[2]
                if (drawableEnd != null) {
                    val isClicked = event.rawX >= (inputSearch.right - drawableEnd.bounds.width() - inputSearch.paddingEnd)
                    if (isClicked) {
                        inputSearch.setText("")
                        performSearch("", activeMonthFilter)
                        return@OnTouchListener true
                    }
                }
            }
            false
        })

        performSearch("", activeMonthFilter)
    }

    private fun updateStreakOnTaskComplete(): Int = runBlocking {
        // ... (Logika streak sama dengan di TaskActivity, menggunakan runBlocking)
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

        return@runBlocking if (streakIncreased) newStreak else oldStreak
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

    private fun generateMonthChips() {
        // ... (Logic sama)
        if (chipContainer.childCount > 1) {
            chipContainer.removeViews(1, chipContainer.childCount - 1)
        }

        val chipAll = findViewById<TextView>(R.id.chip_all)
        chipAll.setOnClickListener {
            setActiveChip(-1, it)
            performSearch(inputSearch.text.toString(), -1)
        }
        setActiveChip(-1, chipAll)

        for (monthIndex in 0 until 12) {
            val chip = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, 32.dp
                ).apply {
                    setMargins(0, 0, 8.dp, 0)
                }
                text = " ${monthNames[monthIndex]} "
                setPaddingRelative(12.dp, 0, 12.dp, 0)
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(Color.BLACK)
                background = ResourcesCompat.getDrawable(resources, R.drawable.chip_unselected, null)
                tag = monthIndex

                setOnClickListener {
                    setActiveChip(monthIndex, it)
                    performSearch(inputSearch.text.toString(), monthIndex)
                }
            }
            chipContainer.addView(chip)
        }
    }

    private fun setActiveChip(monthIndex: Int, selectedView: View) {
        // ... (Logic sama)
        activeMonthFilter = monthIndex

        for (i in 0 until chipContainer.childCount) {
            val chip = chipContainer.getChildAt(i) as TextView
            val isSelected = chip == selectedView

            if (isSelected) {
                chip.setBackgroundResource(R.drawable.chip_selected)
                chip.setTextColor(Color.WHITE)
                if (chip.id == R.id.chip_all) {
                    chip.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
            } else {
                chip.setBackgroundResource(R.drawable.chip_unselected)
                chip.setTextColor(Color.BLACK)
            }
        }
    }

    // Menggunakan wrapper sinkron
    private fun performSearch(query: String, monthFilter: Int) {
        // Menggunakan wrapper sinkron
        val filteredTasks = TaskRepository.searchTasks(query, monthFilter)
        taskResultsContainer.removeAllViews()

        if (filteredTasks.isEmpty()) {
            val filterName = if (monthFilter == -1) "Semua Bulan" else monthNames[monthFilter]

            val noResults = TextView(this).apply {
                text = "Tidak ada hasil ditemukan untuk filter:\n'${query.ifEmpty { "Semua Tugas" }}' di bulan $filterName"
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, 50.dp, 0, 50.dp)
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
            }
            taskResultsContainer.addView(noResults)
        } else {
            for (task in filteredTasks) {
                addNewTaskToUI(taskResultsContainer, task)
            }
        }
    }

    // Menggunakan Task.id (String)
    private fun addNewTaskToUI(container: LinearLayout, task: Task) {
        val context = this
        val marginPx = 8.dp
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val oldStreak = sharedPrefs.getInt(KEY_STREAK, 0)

        // ... (Layout initialization sama)
        val mainContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, marginPx)
            }
            orientation = LinearLayout.VERTICAL
        }

        val taskItem = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 64.dp
            )
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_task, null)
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            elevation = 4f
            setPadding(12.dp, 12.dp, 12.dp, 12.dp)
        }

        // ✅ CHECKLIST BOX - LANGSUNG COMPLETE TASK
        val checklistBox = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(24.dp, 24.dp).apply {
                marginEnd = 16.dp
                marginStart = 4.dp
            }
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_checklist, null)

            setOnClickListener {
                GlobalScope.launch(Dispatchers.Main) {
                    // ID sekarang String
                    val success = TaskRepository.completeTask(task.id)
                    if (success) {
                        val newStreak = updateStreakOnTaskComplete()
                        showConfirmationDialogWithStreakCheck(task.title, "selesai", newStreak, oldStreak)
                        performSearch(inputSearch.text.toString(), activeMonthFilter)
                    } else {
                        Toast.makeText(context, "Gagal menandai tugas selesai. Pastikan Anda sudah login.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        taskItem.addView(checklistBox)

        // ... (Layout Title & Priority sama)
        val titleAndPriorityContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
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
        taskItem.addView(titleAndPriorityContainer)

        val taskMonth = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            // Menggunakan Calendar.MONTH dari dueDate Timestamp
            val monthIndex = task.dueDate.toDate().get(Calendar.MONTH)
            text = monthNames[monthIndex]
            textSize = 14f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setPadding(0, 0, 12.dp, 0)
            gravity = Gravity.CENTER_VERTICAL
        }
        taskItem.addView(taskMonth)

        // ... (Layout Arrow sama)
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
            // ... (Logic createActionButton sama)
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
                putExtra("EXTRA_TASK_ID", task.id) // ID sekarang String
            }
            editTaskLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val deleteButton = createActionButton(R.drawable.ic_trash, "Delete") {
            GlobalScope.launch(Dispatchers.Main) {
                val success = TaskRepository.deleteTask(task.id) // ID sekarang String
                if (success) {
                    showConfirmationDialogWithStreakCheck(task.title, "dihapus", -1, -1)
                    performSearch(inputSearch.text.toString(), activeMonthFilter)
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

    private fun showConfirmationDialogWithStreakCheck(taskTitle: String, action: String, newStreak: Int, oldStreak: Int) {
        // ... (Logic dialog sama)
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
                // Logic untuk menampilkan streak dialog
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