package com.example.todolistapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.HorizontalScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import java.text.SimpleDateFormat
import java.util.*

class TaskActivity : AppCompatActivity() {

    private val TAG = "TaskActivity"
    private lateinit var tasksContainer: LinearLayout
    private lateinit var tvNoActivity: TextView
    private lateinit var octoberText: TextView // Akan diubah menjadi tvMonthYear

    // Pastikan ini terikat ke HorizontalScrollView di layout (R.id.calendar_main)
    private lateinit var calendarMain: HorizontalScrollView
    private lateinit var dateItemsContainer: LinearLayout

    private lateinit var currentCalendar: Calendar
    private var selectedDate: Calendar = Calendar.getInstance()

    private val COLOR_ACTIVE_SELECTION = Color.parseColor("#283F6D")
    private val COLOR_DEFAULT_TEXT = Color.BLACK
    private val COLOR_SELECTED_TEXT = Color.WHITE
    private val CORNER_RADIUS_DP = 8
    private val ITEM_WIDTH_DP = 60

    // Key untuk Intent Extra (Harus sama dengan yang digunakan di AddTaskActivity)
    private val EXTRA_SELECTED_DATE_MILLIS = "EXTRA_SELECTED_DATE_MILLIS"

    private val addTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadTasksForSelectedDate()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task)

        // --- Inisialisasi Views ---
        tasksContainer = findViewById(R.id.tasksContainer)
        tvNoActivity = findViewById(R.id.tvNoActivity)

        // Asumsi ID untuk teks bulan adalah R.id.octoberText (sesuai kode sebelumnya)
        octoberText = findViewById(R.id.octoberText)

        // Inisialisasi HorizontalScrollView (Fix Scroll)
        calendarMain = findViewById(R.id.calendar_main)
        dateItemsContainer = findViewById(R.id.date_items_container)

        // --- Setup Kalender ---
        currentCalendar = selectedDate.clone() as Calendar
        currentCalendar.set(Calendar.DAY_OF_MONTH, 1)

        updateCalendar()

        // PERMINTAAN 1: Langsung scroll ke hari ini
        calendarMain.post {
            scrollToToday()
        }

        setDynamicMonthYear()
        handleIncomingTaskIntent(intent)

        // --- Listener ---

        // 1. Tombol search/filter: Membuka SearchFilterActivity
        val btnSearch = findViewById<ImageView?>(R.id.btn_search)
        btnSearch?.setOnClickListener {
            val intent = Intent(this, SearchFilterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // PERMINTAAN 2: Tombol New Reminder (Meneruskan tanggal yang dipilih)
        val btnNewReminder = findViewById<View>(R.id.reminderContainer)
        btnNewReminder?.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java).apply {
                // Tambahkan timestamp dari tanggal yang sedang dipilih
                putExtra(EXTRA_SELECTED_DATE_MILLIS, selectedDate.timeInMillis)
            }
            addTaskLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Tombol Calendar (Tidak berubah)
        val btnCalendar = findViewById<ImageView?>(R.id.btn_calendar)
        btnCalendar?.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } ?: Log.w(TAG, "btn_calendar tidak ditemukan di layout task.xml")

        // Bottom Navigation (Tidak berubah)
        val bottomNav = findViewById<BottomNavigationView?>(R.id.bottomNav)
        bottomNav?.selectedItemId = R.id.nav_tasks
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    true
                }
                R.id.nav_tasks -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    true
                }
                else -> false
            }
        }

        loadTasksForSelectedDate()
    }

    override fun onResume() {
        super.onResume()
        loadTasksForSelectedDate()
    }

    /**
     * Memuat tugas hanya untuk tanggal yang tersorot saat ini (selectedDate).
     */
    private fun loadTasksForSelectedDate() {
        tasksContainer.removeAllViews()

        val tasks = TaskRepository.getTasksByDate(selectedDate)

        for (task in tasks) {
            addNewTaskToUI(task.title, task.time, task.category)
        }
        updateEmptyState(tasks.size)
    }

    // --- Perbaikan Horizontal Scrolling (scrollToToday) ---
    private fun scrollToToday() {
        val today = Calendar.getInstance()

        // Periksa apakah bulan kalender yang ditampilkan adalah bulan saat ini
        if (today.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
            today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {

            val todayDayOfMonth = today.get(Calendar.DAY_OF_MONTH)
            // Indeks dimulai dari 0, Day of month dimulai dari 1
            val dayIndex = todayDayOfMonth - 1

            // Hitung posisi horizontal (sumbu X)
            val itemWidthPx = ITEM_WIDTH_DP.dp

            // Hitung offset agar hari yang dipilih berada di tengah layar
            val centerOffset = (calendarMain.width / 2) - (itemWidthPx / 2)
            val scrollPosition = (dayIndex * itemWidthPx) - centerOffset

            // Pastikan tidak scroll ke posisi negatif
            calendarMain.smoothScrollTo(scrollPosition.coerceAtLeast(0), 0)
        }
    }

    private fun setDynamicMonthYear() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))
        octoberText.text = sdf.format(currentCalendar.time)
    }

    private fun createRoundedBackground(color: Int): GradientDrawable {
        val cornerRadiusPx = CORNER_RADIUS_DP.dp.toFloat()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = cornerRadiusPx
        }
    }

    private fun updateCalendar() {
        dateItemsContainer.removeAllViews()

        // Clone kalender untuk iterasi agar currentCalendar tidak berubah
        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, i)

            val dayOfWeek = SimpleDateFormat("EEE", Locale("en", "US")).format(cal.time)
            val day = i

            val isSelected = (cal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                    cal.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH))

            val dayItemContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ITEM_WIDTH_DP.dp,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(0, 0, 8.dp, 0)
                }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = if (isSelected) createRoundedBackground(COLOR_ACTIVE_SELECTION) else createRoundedBackground(Color.WHITE)
                elevation = 2f
                setPadding(4.dp, 4.dp, 4.dp, 4.dp)
            }

            val dayOfWeekText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = dayOfWeek
                textSize = 12f
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
                setTextColor(if (isSelected) COLOR_SELECTED_TEXT else COLOR_DEFAULT_TEXT)
            }
            dayItemContainer.addView(dayOfWeekText)

            val dayNumberText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = day.toString()
                textSize = 18f
                val font = ResourcesCompat.getFont(context, R.font.lexend)
                typeface = android.graphics.Typeface.create(font, android.graphics.Typeface.BOLD)
                setTextColor(if (isSelected) COLOR_SELECTED_TEXT else COLOR_DEFAULT_TEXT)
            }
            dayItemContainer.addView(dayNumberText)

            // Logika klik untuk memilih tanggal dan memuat tugas
            dayItemContainer.setOnClickListener {
                val newSelectedDate = currentCalendar.clone() as Calendar
                newSelectedDate.set(Calendar.DAY_OF_MONTH, i)

                selectedDate = newSelectedDate

                loadTasksForSelectedDate()
                updateCalendar()
            }

            dateItemsContainer.addView(dayItemContainer)
        }
    }

    private fun handleIncomingTaskIntent(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("SHOULD_ADD_TASK", false)) {
            loadTasksForSelectedDate()
            intent.removeExtra("SHOULD_ADD_TASK")
        }
    }


    private fun addNewTaskToUI(title: String, time: String, category: String) {
        val context = this
        val marginPx = 16.dp

        val taskItem = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 64.dp).apply {
                setMargins(marginPx, 0, marginPx, marginPx)
            }
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_task, null)
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            elevation = 4f
            setPadding(12.dp, 12.dp, 12.dp, 12.dp)
        }

        val checklistBox = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(24.dp, 24.dp).apply {
                marginStart = 9.dp
            }
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_checklist, null)
        }
        taskItem.addView(checklistBox)

        val taskTitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                marginStart = 17.dp
            }
            text = title
            textSize = 16f
            setTextColor(Color.parseColor("#14142A"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        taskItem.addView(taskTitle)

        val taskTime = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 12.dp
            }
            text = time
            textSize = 14f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        taskItem.addView(taskTime)

        val taskCategory = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 12.dp
            }
            text = " | $category"
            textSize = 14f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        taskItem.addView(taskCategory)

        tasksContainer.addView(taskItem, 0)
    }

    private fun updateEmptyState(taskCount: Int) {
        tvNoActivity.visibility = if (taskCount == 0) View.VISIBLE else View.GONE
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}