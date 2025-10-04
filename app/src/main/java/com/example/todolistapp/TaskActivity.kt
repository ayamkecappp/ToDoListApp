package com.example.todolistapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskActivity : AppCompatActivity() {

    private val TAG = "TaskActivity"
    private lateinit var tasksContainer: LinearLayout
    private lateinit var tvNoActivity: TextView
    private lateinit var octoberText: TextView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var calendarMain: HorizontalScrollView
    private lateinit var dateItemsContainer: LinearLayout

    // Calendar yang mewakili TANGGAL SAAT INI (Today)
    private lateinit var currentCalendar: Calendar
    // Calendar yang mewakili TANGGAL YANG DIPILIH (gunakan 'var' agar bisa diubah)
    private var selectedDate: Calendar = Calendar.getInstance()

    // Flag untuk memastikan scroll awal hanya terjadi sekali
    private var hasScrolledToToday = false

    private val COLOR_ACTIVE_SELECTION = Color.parseColor("#283F6D")
    private val COLOR_DEFAULT_TEXT = Color.BLACK
    private val COLOR_SELECTED_TEXT = Color.WHITE
    private val CORNER_RADIUS_DP = 16  // Corner radius untuk kelengkungan
    private val BORDER_WIDTH_DP = 2    // Ketebalan border
    private val BORDER_COLOR = Color.parseColor("#E0E0E0")  // Warna border abu-abu terang
    private val ITEM_WIDTH_DP = 68
    private val NUM_DAYS_TO_SHOW = 365

    private val EXTRA_SELECTED_DATE_MILLIS = "EXTRA_SELECTED_DATE_MILLIS"

    private val addTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadTasksForSelectedDate()
        }
    }

    // Map untuk memetakan Priority ke Resource Color ID
    private val priorityColorMap = mapOf(
        "Low" to R.color.low_priority,
        "Medium" to R.color.medium_priority,
        "High" to R.color.high_priority
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task)

        // --- Inisialisasi Views ---
        tasksContainer = findViewById(R.id.tasksContainer)
        tvNoActivity = findViewById(R.id.tvNoActivity)
        octoberText = findViewById(R.id.octoberText)
        calendarMain = findViewById(R.id.calendar_main)
        dateItemsContainer = findViewById(R.id.date_items_container)

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.itemIconTintList = null

        // Inisialisasi Calendar untuk hari ini - REALTIME
        currentCalendar = Calendar.getInstance()
        // Set selectedDate ke hari ini juga
        selectedDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // --- Setup Kalender & Navigasi ---
        generateYearlyCalendarViews()
        setDynamicMonthYear()
        handleIncomingTaskIntent(intent)

        // Scroll otomatis ke hari ini saat pertama kali membuka app
        calendarMain.post {
            if (!hasScrolledToToday) {
                scrollToSelectedDate(smooth = false)
                hasScrolledToToday = true
            }
        }

        // Listener Tombol
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

        // Bottom Navigation
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

        // PENTING: Proses missed tasks di awal
        TaskRepository.processTasksForMissed()
        loadTasksForSelectedDate()
    }

    override fun onStart() {
        super.onStart()
        bottomNav.selectedItemId = R.id.nav_tasks
    }

    override fun onResume() {
        super.onResume()

        // Update currentCalendar ke waktu REALTIME sekarang
        currentCalendar = Calendar.getInstance()

        TaskRepository.processTasksForMissed()
        loadTasksForSelectedDate()

        // Refresh tampilan kalender
        generateYearlyCalendarViews()
        setDynamicMonthYear()

        calendarMain.post {
            scrollToSelectedDate(smooth = true)
        }
    }

    // ===========================================
    // LOGIKA KALENDER
    // ===========================================

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

            // Tentukan apakah tanggal ini adalah tanggal yang dipilih
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

        // Atur warna latar dan teks
        val backgroundColor = if (isSelected) COLOR_ACTIVE_SELECTION else Color.WHITE
        val textColor = if (isSelected) COLOR_SELECTED_TEXT else COLOR_DEFAULT_TEXT

        val containerLayoutParams = LinearLayout.LayoutParams(
            ITEM_WIDTH_DP.dp,
            90.dp
        ).apply {
            setMargins(4.dp, 0, 4.dp, 0)
        }

        val container = LinearLayout(context).apply {
            layoutParams = containerLayoutParams
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            // PERBAIKAN: Background dengan border melengkung
            background = createRoundedBackgroundWithBorder(backgroundColor, isSelected)
            elevation = 4f
            setPadding(6.dp, 10.dp, 6.dp, 10.dp)
            tag = dayMillis
        }

        val textLayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // 1. Month (okt) - FONT DIPERKECIL
        val monthText = TextView(context).apply {
            text = month
            textSize = 10f  // Dikecilkan dari 11f
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setTextColor(textColor)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = textLayoutParams
        }
        container.addView(monthText)

        // 2. Day of Week (Sat/Sun) - FONT DIPERKECIL
        val dayOfWeekText = TextView(context).apply {
            text = dayOfWeek
            textSize = 12f  // Dikecilkan dari 14f
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setTextColor(textColor)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = textLayoutParams
        }
        container.addView(dayOfWeekText)

        // 3. Day of Month (4/5) - FONT DIPERKECIL
        val dayNumberText = TextView(context).apply {
            text = dayOfMonth.toString()
            textSize = 22f  // Dikecilkan dari 26f
            val font = ResourcesCompat.getFont(context, R.font.lexend)
            typeface = Typeface.create(font, Typeface.BOLD)
            setTextColor(textColor)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = textLayoutParams
        }
        container.addView(dayNumberText)

        container.setOnClickListener {
            val newSelectedMillis = it.tag as Long

            val newSelectedDate = selectedDate.clone() as Calendar
            newSelectedDate.timeInMillis = newSelectedMillis

            // Atur waktu ke 00:00:00
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

        // Hitung selisih hari dari startCal ke selectedDate
        val diffMillis = selectedDate.timeInMillis - startCal.timeInMillis
        val daysDifference = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        val itemWidthPx = ITEM_WIDTH_DP.dp + 8.dp

        // Hitung posisi scroll agar tanggal berada di tengah layar
        val centerOffset = (calendarMain.width / 2) - (ITEM_WIDTH_DP.dp / 2)
        val scrollPosition = (daysDifference * itemWidthPx) - centerOffset

        if (smooth) {
            calendarMain.smoothScrollTo(scrollPosition.coerceAtLeast(0), 0)
        } else {
            calendarMain.scrollTo(scrollPosition.coerceAtLeast(0), 0)
        }
    }

    // ===========================================
    // FUNGSI UMUM LAINNYA
    // ===========================================

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

    // PERBAIKAN: Background dengan border melengkung
    private fun createRoundedBackgroundWithBorder(fillColor: Int, isSelected: Boolean): GradientDrawable {
        val cornerRadiusPx = CORNER_RADIUS_DP.dp.toFloat()
        val borderWidthPx = BORDER_WIDTH_DP.dp

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            cornerRadius = cornerRadiusPx

            // Tambahkan border hanya untuk kotak yang tidak dipilih
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

    private fun addNewTaskToUI(task: Task) {
        val context = this
        val marginPx = 16.dp

        // --- Container Utama Vertikal ---
        val mainContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(marginPx, 0, marginPx, marginPx)
            }
            orientation = LinearLayout.VERTICAL
        }

        // --- 1. Item Tugas ---
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

        // Checklist/Status
        val checklistBox = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(24.dp, 24.dp).apply {
                marginEnd = 16.dp
            }
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_checklist, null)

            setOnClickListener {
                val success = TaskRepository.completeTask(task.id)
                if (success) {
                    showConfirmationDialog(task.title, "selesai")
                    (mainContainer.parent as? ViewGroup)?.removeView(mainContainer)
                    loadTasksForSelectedDate()
                } else {
                    Toast.makeText(context, "Gagal menandai tugas selesai.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        taskItem.addView(checklistBox)

        // Container Judul & Ikon Prioritas
        val titleAndPriorityContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Title
        val taskTitle = TextView(context).apply {
            text = task.title
            textSize = 16f
            setTextColor(Color.parseColor("#14142A"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        titleAndPriorityContainer.addView(taskTitle)

        // Ikon Prioritas
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

        // Container Detail (Waktu & Kategori)
        val detailWrapper = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
            ).apply {
                marginStart = 16.dp
            }
            orientation = LinearLayout.VERTICAL
        }

        // Waktu
        val taskTime = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = task.time
            textSize = 12f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            gravity = Gravity.END
        }
        detailWrapper.addView(taskTime)

        // Kategori
        val taskCategory = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = task.category
            textSize = 12f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            gravity = Gravity.END
        }
        detailWrapper.addView(taskCategory)

        taskItem.addView(titleAndPriorityContainer)
        taskItem.addView(detailWrapper)

        // Arrow
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

        // --- 2. Tombol Aksi ---
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
            }
            startActivity(intent)
        }

        val editButton = createActionButton(R.drawable.ic_edit, "Edit") {
            val intent = Intent(context, EditTaskActivity::class.java).apply {
                putExtra("EXTRA_TASK_ID", task.id)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val deleteButton = createActionButton(R.drawable.ic_trash, "Delete") {
            val success = TaskRepository.deleteTask(task.id)
            if (success) {
                showConfirmationDialog(task.title, "dihapus")
                loadTasksForSelectedDate()
            } else {
                Toast.makeText(context, "Gagal menghapus tugas.", Toast.LENGTH_SHORT).show()
            }
        }

        actionButtonsContainer.addView(flowTimerButton)
        actionButtonsContainer.addView(editButton)
        actionButtonsContainer.addView(deleteButton)

        mainContainer.addView(actionButtonsContainer)

        // --- 3. Logika Klik untuk Expand/Collapse ---
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