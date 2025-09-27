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
import android.widget.HorizontalScrollView // Diperlukan untuk mengakses HorizontalScrollView
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
    private lateinit var octoberText: TextView

    // VARIABEL KALENDER
    private lateinit var calendarMain: HorizontalScrollView // Tambah inisialisasi HorizontalScrollView
    private lateinit var dateItemsContainer: LinearLayout
    private lateinit var currentCalendar: Calendar
    private var selectedDate: Calendar = Calendar.getInstance()
    private val COLOR_ACTIVE_SELECTION = Color.parseColor("#283F6D")
    private val COLOR_DEFAULT_TEXT = Color.BLACK
    private val COLOR_SELECTED_TEXT = Color.WHITE
    private val CORNER_RADIUS_DP = 8
    private val ITEM_WIDTH_DP = 68 // Lebar total item (60dp + 8dp margin)

    // Activity Result Launcher... (Kode tidak berubah)
    private val addTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val title = data.getStringExtra("EXTRA_TASK_TITLE") ?: "New Activity"
                val time = data.getStringExtra("EXTRA_TASK_TIME") ?: "00.00 - 00.00"
                val category = data.getStringExtra("EXTRA_TASK_CATEGORY") ?: "Other"

                addNewTaskToUI(title, time, category)
                updateEmptyState()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task)

        tasksContainer = findViewById(R.id.tasksContainer)
        tvNoActivity = findViewById(R.id.tvNoActivity)
        octoberText = findViewById(R.id.octoberText)

        // Inisialisasi HorizontalScrollView dan LinearLayout kalender
        calendarMain = findViewById(R.id.calendar_main) // NEW
        dateItemsContainer = findViewById(R.id.date_items_container)

        currentCalendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        selectedDate = Calendar.getInstance()

        // Panggil fungsi generate calendar (1 bulan penuh)
        updateCalendar()

        // NEW: Gulir ke tanggal hari ini setelah kalender dimuat
        calendarMain.post {
            scrollToToday()
        }

        // Panggil fungsi untuk mengatur Bulan dan Tahun
        setDynamicMonthYear()

        // TANGANI DATA TUGAS YANG MUNGKIN DATANG DARI INTENT
        handleIncomingTaskIntent(intent)

        // OnClickListener untuk membuat Reminder baru
        val btnNewReminder = findViewById<View>(R.id.reminderContainer)
        btnNewReminder?.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            addTaskLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Tombol kalender kembali ke bulan saat ini
        val btnCalendar = findViewById<ImageView?>(R.id.btn_calendar)
        btnCalendar?.setOnClickListener {
            currentCalendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }
            selectedDate = Calendar.getInstance()
            updateCalendar()
            setDynamicMonthYear()
            // Gulir ulang ke hari ini setelah pembaruan
            calendarMain.post {
                scrollToToday()
            }
        } ?: Log.w(TAG, "btn_calendar tidak ditemukan di layout task.xml")

        val bottomNav = findViewById<BottomNavigationView?>(R.id.bottomNav)
        if (bottomNav == null) {
            Log.w(TAG, "BottomNavigationView (id=bottomNav) tidak ditemukan di layout task.xml")
            return
        }

        bottomNav.selectedItemId = R.id.nav_tasks

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    true
                }
                R.id.nav_tasks -> {
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        updateEmptyState()
    }

    // Fungsi untuk menghitung dan menggulir ke tanggal hari ini/tanggal terpilih
    private fun scrollToToday() {
        val today = Calendar.getInstance()

        // Hanya gulir jika bulan yang dilihat adalah bulan saat ini
        if (today.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
            today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {

            val todayDayOfMonth = today.get(Calendar.DAY_OF_MONTH)
            val dayIndex = todayDayOfMonth - 1 // Indeks array dimulai dari 0

            // Hitung posisi scroll: (Indeks Hari * Lebar Item DP)
            // Kurangi setengah lebar HorizontalScrollView agar hari ini berada di tengah (opsional, tapi lebih baik)
            val offset = (dayIndex * ITEM_WIDTH_DP).dp - (calendarMain.width / 2) + (ITEM_WIDTH_DP.dp / 2)

            calendarMain.smoothScrollTo(offset, 0)
        }
    }

    // Fungsi untuk mengatur Bulan dan Tahun di octoberText
    private fun setDynamicMonthYear() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        octoberText.text = sdf.format(currentCalendar.time)
    }

    // Fungsi untuk membuat rounded background
    private fun createRoundedBackground(color: Int): GradientDrawable {
        val cornerRadiusPx = CORNER_RADIUS_DP.dp.toFloat()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = cornerRadiusPx
        }
    }

    // FUNGSI UTAMA KALENDER (1 Bulan Penuh)
    private fun updateCalendar() {
        dateItemsContainer.removeAllViews()

        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, i)

            val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
            val day = i

            val isSelected = (cal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                    cal.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH))

            // Container untuk setiap item hari
            val dayItemContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    60.dp,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(0, 0, 8.dp, 0) // Margin 8dp ditambahkan ke lebar item
                }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = if (isSelected) createRoundedBackground(COLOR_ACTIVE_SELECTION) else createRoundedBackground(Color.WHITE)
                elevation = 2f
                setPadding(4.dp, 4.dp, 4.dp, 4.dp)
            }

            // Text Hari (Contoh: Mon)
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

            // Text Tanggal (Contoh: 27)
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

            // Tambahkan OnClickListener
            dayItemContainer.setOnClickListener {
                val newSelectedDate = currentCalendar.clone() as Calendar
                newSelectedDate.set(Calendar.DAY_OF_MONTH, i)

                selectedDate = newSelectedDate
                setDynamicMonthYear()
                updateCalendar()
                // TODO: Muat tugas berdasarkan tanggal yang dipilih
            }

            dateItemsContainer.addView(dayItemContainer)
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingTaskIntent(intent)
    }

    private fun handleIncomingTaskIntent(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("SHOULD_ADD_TASK", false)) {
            val title = intent.getStringExtra("EXTRA_TASK_TITLE") ?: "New Activity"
            val time = intent.getStringExtra("EXTRA_TASK_TIME") ?: "00.00 - 00.00"
            val category = intent.getStringExtra("EXTRA_TASK_CATEGORY") ?: "Other"

            addNewTaskToUI(title, time, category)
            updateEmptyState()

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
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 17.dp
            }
            text = title
            textSize = 16f
            setTextColor(Color.parseColor("#14142A"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        taskItem.addView(taskTitle)

        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1.0f)
        }
        taskItem.addView(spacer)


        val taskTime = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 35.dp
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
                marginStart = 28.dp
            }
            text = category
            textSize = 14f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        taskItem.addView(taskCategory)

        tasksContainer.addView(taskItem, 0)
    }

    private fun updateEmptyState() {
        val taskCount = tasksContainer.childCount - (if (tasksContainer.indexOfChild(tvNoActivity) != -1) 1 else 0)

        if (taskCount == 0) {
            tvNoActivity.visibility = View.VISIBLE
        } else {
            tvNoActivity.visibility = View.GONE
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}