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
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import android.widget.Toast
import androidx.core.content.ContextCompat

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

    // PERBAIKAN: Deklarasi uiDateFormat
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    // Key untuk Intent Extra
    private val EXTRA_SELECTED_DATE_MILLIS = "EXTRA_SELECTED_DATE_MILLIS"

    // Tambahkan ActivityResultLauncher untuk menerima data task
    private val addTaskFromCalendarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Ketika AddTaskActivity mengembalikan RESULT_OK (tugas ditambahkan)
            updateCalendar() // Muat ulang kalender untuk menampilkan dot task

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

    // Map untuk memetakan Priority ke Resource Color ID
    private val priorityColorMap = mapOf(
        "Low" to R.color.low_priority,
        "Medium" to R.color.medium_priority,
        "High" to R.color.high_priority
    )

    // Launcher untuk EditTaskActivity dari Dialog
    private val editTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Jika Edit/Reschedule berhasil, refresh kalender
            updateCalendar()
            Toast.makeText(this, "Tugas berhasil diperbarui.", Toast.LENGTH_SHORT).show()
        }
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
        // PERUBAHAN UTAMA: Meneruskan tanggal yang dipilih
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

    override fun onResume() {
        super.onResume()
        TaskRepository.processTasksForMissed() // Pastikan missed tasks diproses
        updateCalendar() // Refresh UI jika ada perubahan data
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun createRoundedBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = CORNER_RADIUS_DP.dp.toFloat()
        }
    }


    private fun updateCalendar() {
        // Ambil semua tanggal task aktif (hanya tgl, tanpa jam/menit/detik)
        val activeTaskDates = TaskRepository.getAllActiveTaskDates()

        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthText.text = sdf.format(currentCalendar.time)

        calendarGrid.removeAllViews()

        val tempCal = currentCalendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()

        val isCurrentMonthView = (today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH))

        val isSelectedMonthView = (selectedDate.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH))

        val totalCells = 42

        val marginDp = 4
        val marginPx = marginDp.dp

        for (i in 0 until totalCells) {

            // --- CONTAINER UTAMA PER TANGGAL (TextView & Dot) ---
            val dayContainer = LinearLayout(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 48.dp
                    height = 48.dp
                    setMargins(marginPx, marginPx, marginPx, marginPx)
                    columnSpec = GridLayout.spec(i % 7)
                    rowSpec = GridLayout.spec(i / 7)
                }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                background = null // Default background
            }

            val dayView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                    setMargins(0, 0, 0, 2.dp)
                }
                width = 30.dp
                height = 30.dp
                gravity = Gravity.CENTER
                textSize = 16f
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
                tag = null // Gunakan tag untuk menyimpan tanggal saat ini
            }
            dayContainer.addView(dayView)

            val taskDot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(6.dp, 6.dp).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(COLOR_ACTIVE_SELECTION)
                }
                visibility = View.GONE
            }
            dayContainer.addView(taskDot)


            if (i >= firstDayOfWeek && i < daysInMonth + firstDayOfWeek) {
                val dayNumber = i - firstDayOfWeek + 1
                dayView.text = dayNumber.toString()

                // BARU: Buat Calendar object untuk hari ini
                val dayCal = currentCalendar.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                dayCal.set(Calendar.HOUR_OF_DAY, 0)
                dayCal.set(Calendar.MINUTE, 0)
                dayCal.set(Calendar.SECOND, 0)
                dayCal.set(Calendar.MILLISECOND, 0)

                // PERBAIKAN COMPILATION ERROR: Menggunakan variabel 'dayCal' yang benar
                dayView.tag = dayCal.timeInMillis


                val isToday = isCurrentMonthView && today.get(Calendar.DAY_OF_MONTH) == dayNumber
                val isSelected = isSelectedMonthView && selectedDate.get(Calendar.DAY_OF_MONTH) == dayNumber

                // BARU: Cek apakah ada task di tanggal ini (untuk dot)
                val hasTask = activeTaskDates.contains(dayCal.timeInMillis)

                // 1. Atur Tampilan Default (White background, Black text)
                dayView.setTextColor(COLOR_DEFAULT_TEXT)
                dayContainer.background = createRoundedBackground(Color.WHITE)

                // 2. Highlight Hari Ini (Background Kuning)
                if (isToday) {
                    dayView.background = createRoundedBackground(COLOR_TODAY_HIGHLIGHT)
                    dayView.setTextColor(COLOR_DEFAULT_TEXT)
                }

                // 3. Highlight Tanggal Aktif (Background Dark Blue, Text Putih)
                if (isSelected) {
                    dayView.background = createRoundedBackground(COLOR_ACTIVE_SELECTION)
                    dayView.setTextColor(COLOR_SELECTED_TEXT)
                }

                // BARU: Tambahkan dot jika ada task
                if (hasTask) {
                    taskDot.visibility = View.VISIBLE
                    // MODIFIED: Ganti warna dot menjadi putih jika terpilih, jangan sembunyikan
                    if (isSelected) {
                        (taskDot.background as GradientDrawable).setColor(Color.WHITE)
                    } else {
                        (taskDot.background as GradientDrawable).setColor(Color.RED) // Pastikan warna default merah
                    }
                }

                // Tambahkan OnClickListener
                dayContainer.setOnClickListener {
                    val newSelectedDateCal = currentCalendar.clone() as Calendar
                    newSelectedDateCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                    selectedDate = newSelectedDateCal.clone() as Calendar

                    updateCalendar() // Perbarui UI kalender

                    // Jika ada tugas, tampilkan dialog tugas
                    if (hasTask) {
                        showTasksDialog(selectedDate)
                    } else {
                        Toast.makeText(this, "Tidak ada tugas aktif di ${dayView.text}", Toast.LENGTH_SHORT).show()
                    }
                }

                // PENTING: Untuk memastikan tampilan tombol Add Reminder selalu sesuai dengan selectedDate
                if (isSelected) {
                    val addReminderButton = findViewById<LinearLayout>(R.id.addReminderButton)
                    val tvAddReminder = addReminderButton.findViewById<TextView>(R.id.tvAddReminder)
                    tvAddReminder.text = "Add Reminder for ${dayView.text}"
                }

            } else {
                dayContainer.layoutParams.height = 0
            }

            calendarGrid.addView(dayContainer)
        }
    }

    private fun showTasksDialog(date: Calendar) {
        val tasks = TaskRepository.getTasksByDate(date)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_task_list, null)

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvDialogDate = dialogView.findViewById<TextView>(R.id.tvDialogDate)
        val tasksContainer = dialogView.findViewById<LinearLayout>(R.id.llDialogTasksContainer)

        tvDialogTitle.text = "Tasks"
        tvDialogDate.text = uiDateFormat.format(date.time)

        tasksContainer.removeAllViews()

        if (tasks.isEmpty()) {
            tasksContainer.addView(TextView(this).apply {
                text = "Tidak ada tugas aktif di tanggal ini."
                textSize = 16f
                setTextColor(Color.GRAY)
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
                gravity = Gravity.CENTER
                setPadding(0, 20.dp, 0, 20.dp)
            })
        } else {
            tasks.forEach { task ->
                createTaskItemInDialog(tasksContainer, task, dialog)
            }
        }

        // Tombol Close
        val btnClose = TextView(this).apply {
            text = "Close"
            textSize = 16f
            setTextColor(COLOR_ACTIVE_SELECTION)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            gravity = Gravity.CENTER
            setPadding(0, 12.dp, 0, 12.dp)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 50.dp).apply {
                setMargins(0, 8.dp, 0, 0)
                background = ContextCompat.getDrawable(context, R.drawable.bg_bottom_right_rounded)
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
            }
        }
        (tasksContainer.parent.parent as ViewGroup).addView(btnClose)


        dialog.show()
    }

    // BARU: Membuat UI item tugas di dalam dialog
    private fun createTaskItemInDialog(container: LinearLayout, task: Task, dialog: AlertDialog) {
        val context = this

        val itemContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 4.dp, 0, 4.dp)
            }
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.rectangle_settings) // Background putih
        }

        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12.dp, 8.dp, 12.dp, 8.dp)
        }

        // Judul & Priority
        val titleLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            gravity = Gravity.CENTER_VERTICAL
        }
        val tvTitle = TextView(context).apply {
            text = task.title
            textSize = 16f
            setTextColor(Color.parseColor("#14142A"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        titleLayout.addView(tvTitle)

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
            titleLayout.addView(exclamationIcon)
        }

        headerLayout.addView(titleLayout)

        // Waktu
        val tvTime = TextView(context).apply {
            text = task.time
            textSize = 12f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            gravity = Gravity.END
            setPadding(12.dp, 0, 0, 0)
        }
        headerLayout.addView(tvTime)

        itemContainer.addView(headerLayout)

        // Aksi (Tersembunyi)
        val actionContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            weightSum = 4f
            setPadding(16.dp, 8.dp, 16.dp, 8.dp)
        }

        fun createDropdownButton(iconResId: Int, buttonText: String, weight: Float, onClick: () -> Unit): LinearLayout {
            return LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight).apply {
                    setMargins(4.dp, 0, 4.dp, 0)
                }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener { onClick() }

                background = createRoundedBackground(Color.parseColor("#E0ECFB")) // Warna latar belakang aksi
                setPadding(8.dp, 8.dp, 8.dp, 8.dp)

                addView(ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(24.dp, 24.dp)
                    setImageResource(iconResId)
                    setColorFilter(Color.parseColor("#283F6D"))
                })
                addView(TextView(context).apply {
                    text = buttonText
                    textSize = 10f
                    setTextColor(Color.parseColor("#283F6D"))
                    typeface = ResourcesCompat.getFont(context, R.font.lexend)
                    gravity = Gravity.CENTER_HORIZONTAL
                })
            }
        }

        // Complete Button
        val completeBtn = createDropdownButton(R.drawable.ic_check, "Complete", 1.0f) {
            if (TaskRepository.completeTask(task.id)) {
                Toast.makeText(context, "${task.title} Completed!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                updateCalendar() // Refresh UI
            }
        }

        // Flow Timer Button
        val flowTimerBtn = createDropdownButton(R.drawable.ic_alarm, "Flow Timer", 1.0f) {
            val intent = Intent(context, FlowTimerActivity::class.java).apply {
                putExtra(FlowTimerActivity.EXTRA_TASK_NAME, task.title)
            }
            context.startActivity(intent)
            dialog.dismiss()
        }

        // Edit Button
        val editBtn = createDropdownButton(R.drawable.ic_edit, "Edit", 1.0f) {
            val intent = Intent(context, EditTaskActivity::class.java).apply {
                putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id)
                putExtra(EditTaskActivity.EXTRA_TASK_TYPE, Task.Status.ACTIVE.name) // Menandakan edit task aktif
            }
            editTaskLauncher.launch(intent)
            dialog.dismiss()
        }

        // Delete Button
        val deleteBtn = createDropdownButton(R.drawable.ic_trash, "Delete", 1.0f) {
            if (TaskRepository.deleteTask(task.id)) {
                Toast.makeText(context, "${task.title} Deleted", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                updateCalendar() // Refresh UI
            }
        }

        actionContainer.addView(completeBtn)
        actionContainer.addView(flowTimerBtn)
        actionContainer.addView(editBtn)
        actionContainer.addView(deleteBtn)

        itemContainer.addView(actionContainer) // Tambahkan container aksi ke itemContainer

        // --- TOGGLE LOGIC ---
        headerLayout.setOnClickListener {
            val isExpanded = actionContainer.visibility == View.VISIBLE
            actionContainer.visibility = if (isExpanded) View.GONE else View.VISIBLE
        }

        container.addView(itemContainer)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}