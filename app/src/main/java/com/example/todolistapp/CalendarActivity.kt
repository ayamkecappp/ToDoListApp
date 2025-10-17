package com.example.todolistapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    // Deklarasi view sesuai dengan ID di calendar.xml Anda
    private lateinit var monthText: TextView
    private lateinit var calendarGrid: GridLayout
    private lateinit var currentCalendar: Calendar
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calendar)

        // Inisialisasi Views
        monthText = findViewById(R.id.month_text)
        calendarGrid = findViewById(R.id.calendar_grid)
        val arrowLeft = findViewById<ImageView>(R.id.arrow_left)
        val arrowRight = findViewById<ImageView>(R.id.arrow_right)
        val addReminderButton = findViewById<View>(R.id.addReminderButton)

        currentCalendar = Calendar.getInstance()
        selectedDate = Calendar.getInstance()

        updateCalendar()

        // Setup Listeners
        arrowLeft.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }
        arrowRight.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }
        addReminderButton.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateCalendar()
    }

    private fun updateCalendar() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthText.text = sdf.format(currentCalendar.time)
        calendarGrid.removeAllViews()
        val monthCalendar = currentCalendar.clone() as Calendar
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfMonth = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        lifecycleScope.launch {
            for (i in 0 until 42) {
                val cellView = layoutInflater.inflate(R.layout.item_calendar_day, calendarGrid, false)
                val dayText = cellView.findViewById<TextView>(R.id.tv_day)
                val taskIndicator = cellView.findViewById<View>(R.id.task_indicator)
                if (i >= firstDayOfMonth && i < firstDayOfMonth + daysInMonth) {
                    val day = i - firstDayOfMonth + 1
                    dayText.text = day.toString()
                    val cellDate = currentCalendar.clone() as Calendar
                    cellDate.set(Calendar.DAY_OF_MONTH, day)
                    val tasksOnDate = TaskRepository.getTasksByDate(cellDate)
                    taskIndicator.visibility = if (tasksOnDate.isNotEmpty()) View.VISIBLE else View.INVISIBLE
                    cellView.setOnClickListener {
                        selectedDate = cellDate
                        if (tasksOnDate.isNotEmpty()) {
                            showTasksDialog(selectedDate, tasksOnDate)
                        } else {
                            Toast.makeText(this@CalendarActivity, "Tidak ada tugas di tanggal ini", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    dayText.text = ""
                    taskIndicator.visibility = View.INVISIBLE
                }
                calendarGrid.addView(cellView)
            }
        }
    }

    // --- FUNGSI INI TELAH DIPERBAIKI TOTAL ---
    private fun showTasksDialog(date: Calendar, tasks: List<Task>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_task_list, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvDialogDate = dialogView.findViewById<TextView>(R.id.tvDialogDate)
        // Menggunakan ID llDialogTasksContainer dari XML Anda
        val tasksContainer = dialogView.findViewById<LinearLayout>(R.id.llDialogTasksContainer)

        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        tvDialogDate.text = sdf.format(date.time)

        // Hapus semua view lama sebelum menambahkan yang baru
        tasksContainer.removeAllViews()

        // Peta untuk warna prioritas
        val priorityColorMap = mapOf(
            "Low" to R.color.low_priority,
            "Medium" to R.color.medium_priority,
            "High" to R.color.high_priority
        )

        // Loop untuk setiap tugas dan buat tampilannya secara manual
        for (task in tasks) {
            // Inflate layout item_task untuk setiap tugas
            val taskItemView = layoutInflater.inflate(R.layout.item_task, tasksContainer, false)

            // Dapatkan semua view dari item_task
            val title = taskItemView.findViewById<TextView>(R.id.tvTaskTitle)
            val time = taskItemView.findViewById<TextView>(R.id.tvTaskTime)
            val category = taskItemView.findViewById<TextView>(R.id.tvTaskCategory)
            val checkbox = taskItemView.findViewById<CheckBox>(R.id.checkboxTask)
            val priorityIcon = taskItemView.findViewById<ImageView>(R.id.ivPriority)

            // Sembunyikan tombol expand dan kontainer aksi karena tidak digunakan di dialog ini
            taskItemView.findViewById<View>(R.id.ivArrowToggle).visibility = View.GONE
            taskItemView.findViewById<View>(R.id.actionButtonsContainer).visibility = View.GONE

            // Isi data ke dalam view
            title.text = task.title
            category.text = task.category
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            time.text = timeFormat.format(task.dueDate.toDate())
            checkbox.isChecked = task.status == "completed"

            // Atur prioritas
            if (task.priority != "None" && task.priority.isNotEmpty()) {
                priorityIcon.visibility = View.VISIBLE
                val colorResId = priorityColorMap[task.priority] ?: R.color.dark_blue
                priorityIcon.setColorFilter(ContextCompat.getColor(this, colorResId))
            } else {
                priorityIcon.visibility = View.GONE
            }

            // Tambahkan view tugas ini ke dalam LinearLayout di dalam dialog
            tasksContainer.addView(taskItemView)
        }

        dialog.show()
    }
}