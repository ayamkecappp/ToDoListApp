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

    // Key untuk Intent Extra
    private val EXTRA_SELECTED_DATE_MILLIS = "EXTRA_SELECTED_DATE_MILLIS"
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))


    // Tambahkan ActivityResultLauncher untuk menerima data task
    private val addTaskFromCalendarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Ketika AddTaskActivity mengembalikan RESULT_OK (tugas ditambahkan)

            // 1. Luncurkan TaskActivity
            val taskIntent = Intent(this, TaskActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

                // 2. Teruskan data tugas yang diterima
                result.data?.let { data ->
                    // Salin semua extra dari Intent hasil (yang berisi data tugas)
                    putExtras(data.extras ?: Bundle())
                }

                // Tambahkan flag khusus agar TaskActivity tahu harus segera memproses task ini
                putExtra("SHOULD_ADD_TASK", true)
            }
            startActivity(taskIntent)
        }
        // Jika result CANCELED, biarkan CalendarActivity tetap aktif.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calendar)

        monthText = findViewById(R.id.month_text)
        calendarGrid = findViewById(R.id.calendar_grid)
        val arrowLeft = findViewById<ImageView>(R.id.arrow_left)
        val arrowRight = findViewById<ImageView>(R.id.arrow_right)

        val addReminderButton = findViewById<LinearLayout>(R.id.addReminderButton)
        val rootSwipeView = findViewById<LinearLayout>(R.id.calendar_container)

        currentCalendar = Calendar.getInstance()
        selectedDate = currentCalendar.clone() as Calendar
        updateCalendar()

        // tombol panah
        arrowLeft.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        arrowRight.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        // OnClickListener untuk tombol Add Reminder (Launch AddTaskActivity)
        addReminderButton.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java).apply {
                // Teruskan timestamp dari selectedDate
                putExtra(EXTRA_SELECTED_DATE_MILLIS, selectedDate.timeInMillis)
            }
            addTaskFromCalendarLauncher.launch(intent) // Gunakan launcher
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Swipe gesture
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
        val size = 6.dp // Ukuran dot 6dp
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

    /**
     * MENAMPILKAN DIALOG DAFTAR TUGAS
     */
    private fun showTaskListDialog(date: Calendar, tasks: List<Task>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_task_list, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvDate = dialogView.findViewById<TextView>(R.id.tvDialogDate)
        val tasksContainer = dialogView.findViewById<LinearLayout>(R.id.llDialogTasksContainer)

        // Set title and date
        tvTitle.text = "Active Tasks"
        tvDate.text = uiDateFormat.format(date.time)

        if (tasks.isEmpty()) {
            val noTask = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8.dp, 0, 8.dp)
                }
                text = "Tidak ada tugas aktif di tanggal ini."
                textSize = 14f
                setTextColor(Color.parseColor("#98A8C8"))
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            tasksContainer.addView(noTask)
        } else {
            tasks.forEach { task ->
                createDialogTaskItem(tasksContainer, task)
            }
        }

        dialog.show()
    }

    /**
     * MEMBUAT ITEM TUGAS INDIVIDUAL DI DALAM DIALOG
     */
    private fun createDialogTaskItem(container: LinearLayout, task: Task) {
        val context = this
        val marginPx = 4.dp // Margin antar item

        val taskItem = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 50.dp
            ).apply {
                setMargins(0, 0, 0, marginPx)
            }
            background = createRoundedBackground(Color.parseColor("#E0ECFB")) // Warna latar untuk item di dialog
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(12.dp, 6.dp, 12.dp, 6.dp)
        }

        // Checklist/Status Placeholder
        val checklistBox = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(16.dp, 16.dp).apply {
                marginEnd = 12.dp
            }
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_checklist, null) // Gunakan drawable yang sudah ada
        }
        taskItem.addView(checklistBox)

        // Title and Priority Indicator
        val titleAndPriorityContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Title
        val taskTitle = TextView(context).apply {
            text = task.title
            textSize = 14f
            setTextColor(Color.parseColor("#14142A"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        titleAndPriorityContainer.addView(taskTitle)

        // Priority Icon (Exclamation mark)
        if (task.priority != "None" && task.priority != "Low") { // Hanya tampilkan untuk Medium/High
            val colorResId = when (task.priority) {
                "Medium" -> R.color.medium_priority
                "High" -> R.color.high_priority
                else -> R.color.dark_blue
            }
            val colorInt = ContextCompat.getColor(context, colorResId)

            val exclamationIcon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(14.dp, 14.dp).apply {
                    marginStart = 6.dp
                    gravity = Gravity.CENTER_VERTICAL
                }
                setImageResource(R.drawable.ic_exclamation_circle) // Menggunakan ic_exclamation_circle untuk prioritas
                contentDescription = "${task.priority} Priority"
                setColorFilter(colorInt)
            }
            titleAndPriorityContainer.addView(exclamationIcon)
        }

        taskItem.addView(titleAndPriorityContainer)

        // Time
        val taskTime = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 12.dp
            }
            // Tampilkan hanya waktu mulai jika itu rentang waktu
            val timeText = if (task.time.contains(" - ")) task.time.substringBefore(" -") else task.time
            text = timeText
            textSize = 12f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        taskItem.addView(taskTime)

        container.addView(taskItem)
    }


    private fun updateCalendar() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthText.text = sdf.format(currentCalendar.time)

        calendarGrid.removeAllViews()

        val tempCal = currentCalendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)

        // Ubah hari Minggu (1) menjadi 7 (sesuai standar grid 0=Senin, 6=Minggu), lalu geser.
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
        val cellHeight = 60.dp // Naikkan tinggi sel untuk memberi ruang pada titik
        val marginDp = 4
        val marginPx = marginDp.dp

        for (i in 0 until totalCells) {

            // Gunakan LinearLayout sebagai container untuk setiap sel
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
                setPadding(0, 4.dp, 0, 0) // Tambahkan padding atas untuk nomor hari
            }

            // TextView untuk Nomor Hari
            val dayView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER
                textSize = 16f
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
            }
            cellContainer.addView(dayView) // Tambahkan nomor hari terlebih dahulu

            // View untuk Titik Indikator Task (Dot)
            val dotView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 2.dp // Jarak antara nomor hari dan titik
                    width = 6.dp
                    height = 6.dp
                }
                visibility = View.GONE // Sembunyikan secara default
            }
            cellContainer.addView(dotView) // Tambahkan titik

            if (i >= firstDayOfWeek && i < daysInMonth + firstDayOfWeek) {
                val dayNumber = i - firstDayOfWeek + 1
                dayView.text = dayNumber.toString()

                // Tentukan tanggal yang sedang di proses
                val dateForCheck = currentCalendar.clone() as Calendar
                dateForCheck.set(Calendar.DAY_OF_MONTH, dayNumber)
                dateForCheck.set(Calendar.HOUR_OF_DAY, 0)
                dateForCheck.set(Calendar.MINUTE, 0)
                dateForCheck.set(Calendar.SECOND, 0)
                dateForCheck.set(Calendar.MILLISECOND, 0)


                // Cek apakah ada task pada tanggal ini
                TaskRepository.processTasksForMissed()
                val hasTasks = TaskRepository.hasTasksOnDate(dateForCheck)

                if (hasTasks) {
                    dotView.visibility = View.VISIBLE
                }

                val isToday = isCurrentMonthView && today.get(Calendar.DAY_OF_MONTH) == dayNumber
                val isSelected = isSelectedMonthView && selectedDate.get(Calendar.DAY_OF_MONTH) == dayNumber

                // 1. Atur Tampilan Default (White background, Black text)
                dayView.setTextColor(COLOR_DEFAULT_TEXT)
                cellContainer.background = createRoundedBackground(Color.WHITE)

                // 2. Highlight Hari Ini (Background Kuning)
                if (isToday) {
                    cellContainer.background = createRoundedBackground(COLOR_TODAY_HIGHLIGHT)
                    dayView.setTextColor(COLOR_DEFAULT_TEXT)
                }

                // 3. Highlight Tanggal Aktif (Background Dark Blue, Text Putih)
                if (isSelected) {
                    cellContainer.background = createRoundedBackground(COLOR_ACTIVE_SELECTION)
                    dayView.setTextColor(COLOR_SELECTED_TEXT)
                    if(hasTasks) {
                        dotView.background = createDotDrawable(Color.WHITE) // Ubah warna dot menjadi putih
                    }
                } else if(hasTasks) {
                    // Pastikan warna dot kembali ke default jika tidak dipilih, tetapi memiliki tugas
                    dotView.background = createDotDrawable(COLOR_ACTIVE_SELECTION)
                }


                // NEW: Tambahkan OnClickListener
                cellContainer.setOnClickListener {
                    val newSelectedDate = currentCalendar.clone() as Calendar
                    newSelectedDate.set(Calendar.DAY_OF_MONTH, dayNumber)
                    newSelectedDate.set(Calendar.HOUR_OF_DAY, 0)
                    newSelectedDate.set(Calendar.MINUTE, 0)
                    newSelectedDate.set(Calendar.SECOND, 0)
                    newSelectedDate.set(Calendar.MILLISECOND, 0)

                    // 1. Tampilkan Dialog jika ada task
                    val tasksOnDate = TaskRepository.getTasksByDate(newSelectedDate)
                    if (tasksOnDate.isNotEmpty()) {
                        showTaskListDialog(newSelectedDate, tasksOnDate)
                    }

                    // 2. Perbarui selectedDate (untuk highlight) dan render ulang
                    selectedDate = newSelectedDate
                    updateCalendar()
                }

                calendarGrid.addView(cellContainer) // Tambahkan container ke grid

            } else {
                // Sel kosong
                cellContainer.visibility = View.INVISIBLE
                // Ganti tinggi sel kosong menjadi 0 agar tidak mengganggu layout
                cellContainer.layoutParams.height = 0
                calendarGrid.addView(cellContainer)
            }
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}