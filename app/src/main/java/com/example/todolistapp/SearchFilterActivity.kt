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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Perbaikan: Tambahkan extension property untuk konversi dp ke pixel Int
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    // Perbaikan: Tambahkan extension property untuk konversi sp ke pixel Float
    private val Int.sp: Float
        get() = (this * resources.displayMetrics.scaledDensity)

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
        return@runBlocking withContext(Dispatchers.IO) {
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

            return@withContext if (streakIncreased) newStreak else oldStreak
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

    private fun generateMonthChips() {
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
                textSize = 14.sp // Perbaikan: Gunakan extension property 'sp'
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
        // searchTasks sekarang hanya mengembalikan tugas "pending"
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
                // MENGGUNAKAN list_item_task.xml
                createTaskItemUsingLayout(taskResultsContainer, task)
            }
        }
    }

    /**
     * MENGGANTIKAN addNewTaskToUI DENGAN LOGIKA BERDASARKAN list_item_task.xml
     */
    private fun createTaskItemUsingLayout(container: LinearLayout, task: Task) {
        val context = this
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val oldStreak = sharedPrefs.getInt(KEY_STREAK, 0)

        // 1. Muat layout item tugas
        val mainContainer = LayoutInflater.from(context).inflate(R.layout.list_item_task, container, false) as LinearLayout
        mainContainer.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 8.dp) // Sesuaikan margin bawah
            // Hapus margin samping karena parent sudah ada padding
            marginStart = 0
            marginEnd = 0
        }

        // 2. Dapatkan referensi views dari layout item
        val taskItem = mainContainer.findViewById<LinearLayout>(R.id.taskItem)
        val checklistBox = mainContainer.findViewById<View>(R.id.checklistBox)
        val taskTitle = mainContainer.findViewById<TextView>(R.id.taskTitle)
        val taskTime = mainContainer.findViewById<TextView>(R.id.taskTime)
        val taskCategory = mainContainer.findViewById<TextView>(R.id.taskCategory)
        val exclamationIcon = mainContainer.findViewById<ImageView>(R.id.exclamationIcon)
        val arrowRight = mainContainer.findViewById<ImageView>(R.id.arrowRight)
        val actionButtonsContainer = mainContainer.findViewById<LinearLayout>(R.id.actionButtonsContainer)

        val btnFlowTimer = mainContainer.findViewById<LinearLayout>(R.id.btnFlowTimer)
        val btnEdit = mainContainer.findViewById<LinearLayout>(R.id.btnEdit)
        val btnDelete = mainContainer.findViewById<LinearLayout>(R.id.btnDelete)

        // 3. Mengisi data
        taskTitle.text = task.title

        // Menggabungkan Time & Category jika perlu
        val timeCategoryText = if (task.category.isNotEmpty()) {
            if (task.time.isNotEmpty()) "${task.time}\n${task.category}" else task.category
        } else {
            task.time
        }
        taskTime.text = timeCategoryText

        // Tambahkan info bulan di bawah waktu/kategori (khusus untuk SearchFilter)
        val dateText = TextView(context).apply {
            layoutParams = taskCategory.layoutParams
            // Menggunakan Calendar.MONTH dari dueDate Timestamp
            val monthIndex = task.dueDate.toDate().get(Calendar.MONTH)
            val dayOfMonth = task.dueDate.toDate().get(Calendar.DAY_OF_MONTH)
            text = "${dayOfMonth} ${monthNames[monthIndex]}"
            textSize = 12.sp // Perbaikan: Gunakan extension property 'sp'
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            gravity = Gravity.END
        }
        (taskTime.parent as LinearLayout).addView(dateText)
        (taskTime.parent as LinearLayout).removeView(taskCategory) // Hapus yang asli

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

        // 5. Setup Listeners
        checklistBox.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val success = TaskRepository.completeTask(task.id)
                if (success) {
                    val newStreak = updateStreakOnTaskComplete()
                    withContext(Dispatchers.Main) {
                        showConfirmationDialogWithStreakCheck(task.title, "selesai", newStreak, oldStreak)
                        performSearch(inputSearch.text.toString(), activeMonthFilter)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Gagal menandai tugas selesai.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnFlowTimer.setOnClickListener {
            val intent = Intent(context, FlowTimerActivity::class.java).apply {
                putExtra(FlowTimerActivity.EXTRA_TASK_NAME, task.title)
                putExtra(FlowTimerActivity.EXTRA_FLOW_DURATION, task.flowDurationMillis)
            }
            context.startActivity(intent)
        }

        btnEdit.setOnClickListener {
            val intent = Intent(context, EditTaskActivity::class.java).apply {
                putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id)
            }
            editTaskLauncher.launch(intent)
            context.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnDelete.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val success = TaskRepository.deleteTask(task.id)
                if (success) {
                    withContext(Dispatchers.Main) {
                        showConfirmationDialogWithStreakCheck(task.title, "dihapus", -1, -1)
                        performSearch(inputSearch.text.toString(), activeMonthFilter)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Gagal menghapus tugas.", Toast.LENGTH_SHORT).show()
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

        container.addView(mainContainer)
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
        }

        btnConfirm1.text = "OK"
        btnConfirm2.text = "Tutup"

        btnConfirm1.setTextColor(Color.parseColor("#283F6D"))
        btnConfirm2.setTextColor(Color.parseColor("#283F6D"))

        // Memastikan hanya satu tombol yang muncul (agar konsisten dengan TaskActivity)
        btnConfirm2.visibility = View.GONE
        val buttonContainer = dialogView.getChildAt(2) as LinearLayout
        val verticalDivider = buttonContainer.getChildAt(1)
        verticalDivider.visibility = View.GONE

        btnConfirm1.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 2.0f).apply {
            gravity = Gravity.CENTER
        }


        btnConfirm1.setOnClickListener(dismissListener)
        // btnConfirm2.setOnClickListener(dismissListener) // Dihapus karena tombol disembunyikan

        dialog.show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}