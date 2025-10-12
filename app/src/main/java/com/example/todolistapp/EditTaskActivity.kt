package com.example.todolistapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.appcompat.app.AlertDialog
import android.graphics.Color
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.widget.NumberPicker
import android.graphics.drawable.ColorDrawable

class EditTaskActivity : AppCompatActivity() {

    private lateinit var inputPriority: EditText
    private lateinit var inputActivity: EditText
    private lateinit var inputTime: EditText
    private lateinit var inputLocation: EditText
    private lateinit var inputDetails: EditText
    private lateinit var inputDate: EditText
    private lateinit var tvAddFlowTimer: TextView

    private var taskIdToEdit: Long = -1L // ID ASLI dari task yang sedang diedit
    private var taskDateMillis: Long = System.currentTimeMillis() // Tanggal yang dipilih (bisa berubah)
    private var originalTaskDateMillis: Long = System.currentTimeMillis() // Tanggal ASLI task (tidak berubah)
    private var taskEndTimeMillis: Long = 0L // End Time asli


    companion object {
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_RESCHEDULE_MODE = "EXTRA_RESCHEDULE_MODE"
        const val DEFAULT_FLOW_TIMER_DURATION = 30 * 60 * 1000L // 30 menit default
    }

    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    private var currentSelectedPriority: String = "None"
    private val priorities = arrayOf("None", "Low", "Medium", "High")

    // Flow Timer Properties
    private var flowTimerDurationMillis: Long = 0L
    private val MILLIS_IN_HOUR = 60 * 60 * 1000L
    private val MILLIS_IN_MINUTE = 60 * 1000L
    private val MILLIS_IN_SECOND = 1000L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_task)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)

        inputActivity = findViewById(R.id.inputActivity)
        inputTime = findViewById(R.id.inputTime)
        inputLocation = findViewById(R.id.inputLocation)
        inputPriority = findViewById(R.id.inputPriority)
        inputDetails = findViewById(R.id.inputDetails)
        inputDate = findViewById(R.id.inputDate)
        tvAddFlowTimer = findViewById(R.id.tvAddFlowTimer)

        // 1. Ambil Task ID dan Mode Reschedule dari Intent
        taskIdToEdit = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val isRescheduleMode = intent.getBooleanExtra(EXTRA_RESCHEDULE_MODE, false)

        // 2. Muat Data Tugas yang Ada menggunakan ID yang unik
        val existingTask = TaskRepository.getTaskById(taskIdToEdit)

        if (existingTask != null) {
            // Isi form dengan data tugas yang ada
            inputActivity.setText(existingTask.title)
            inputTime.setText(existingTask.time)
            inputLocation.setText(existingTask.category)
            inputPriority.setText(existingTask.priority)
            inputDetails.setText(existingTask.details)

            currentSelectedPriority = existingTask.priority

            // Simpan ID asli dan tanggal asli
            originalTaskDateMillis = existingTask.id // Tanggal ASLI task (tidak berubah)
            taskDateMillis = existingTask.id // Tanggal yang bisa diubah user
            taskEndTimeMillis = existingTask.endTimeMillis

            // Set Data: Menggunakan tanggal asli
            val initialDate = Date(taskDateMillis)
            inputDate.setText(uiDateFormat.format(initialDate))

            // Set Flow Timer State
            flowTimerDurationMillis = existingTask.flowDurationMillis.coerceAtLeast(DEFAULT_FLOW_TIMER_DURATION)
            val timeDisplayString = formatDurationToString(flowTimerDurationMillis)

            // Cek apakah ini Flow Timer atau Time Range atau None
            if (existingTask.time.contains("(Flow)")) {
                tvAddFlowTimer.text = "Flow Timer Set (${timeDisplayString})"
                inputTime.tag = true // Flow Timer aktif
            } else if (existingTask.endTimeMillis > 0L && existingTask.time.isNotEmpty()) {
                inputTime.tag = existingTask.endTimeMillis // Time Range aktif
                tvAddFlowTimer.text = "+ Add Flow Timer (${timeDisplayString})"
            } else {
                inputTime.tag = null // Tidak ada waktu aktif
                tvAddFlowTimer.text = "+ Add Flow Timer (${timeDisplayString})"
            }

            // Set tombol dan judul untuk mode reschedule (jika ada)
            if (isRescheduleMode) {
                btnSave.text = "Restore & Reschedule"
                findViewById<TextView>(R.id.tvNewReminderTitle)?.text = "Reschedule Task"

                // Reset taskDateMillis ke hari ini untuk Reschedule default
                taskDateMillis = System.currentTimeMillis()
                inputDate.setText(uiDateFormat.format(Date(taskDateMillis)))

                // Reset time info
                inputTime.setText("")
                inputTime.tag = null
                flowTimerDurationMillis = DEFAULT_FLOW_TIMER_DURATION
                tvAddFlowTimer.text = "+ Add Flow Timer (${formatDurationToString(DEFAULT_FLOW_TIMER_DURATION)})"
            } else {
                btnSave.text = "Update"
            }

            Toast.makeText(this, "Tugas '${existingTask.title}' siap diedit.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error: Tugas tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        btnSave.setOnClickListener {
            val title = inputActivity.text.toString().trim()
            val location = inputLocation.text.toString().trim()
            val priority = currentSelectedPriority
            val details = inputDetails.text.toString().trim()

            var time: String
            var newEndTimeMillis: Long = 0L
            var savedFlowDuration: Long = 0L

            // Ambil endTimeMillis dari tag (jika diubah oleh Time Picker) atau Flow Timer state
            val isTimeRangeSet = inputTime.tag is Long
            val isFlowTimerActive = inputTime.tag is Boolean && inputTime.tag as Boolean

            if (title.isEmpty()) {
                Toast.makeText(this, "Nama Aktivitas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isTimeRangeSet) {
                newEndTimeMillis = inputTime.tag as Long
                time = inputTime.text.toString().trim()
                savedFlowDuration = flowTimerDurationMillis
            } else if (isFlowTimerActive && flowTimerDurationMillis > 0L) {
                newEndTimeMillis = 0L // Biarkan 0L agar tidak bentrok dengan Flow Timer visual
                val timeDisplay = formatDurationToString(flowTimerDurationMillis) + " (Flow)"
                time = timeDisplay
                savedFlowDuration = flowTimerDurationMillis
            } else {
                time = ""
                newEndTimeMillis = 0L
                savedFlowDuration = flowTimerDurationMillis
            }

            // Dapatkan bulan yang benar berdasarkan taskDateMillis yang baru
            val calendarForNewDate = Calendar.getInstance().apply { timeInMillis = taskDateMillis }
            val updatedMonthAdded = calendarForNewDate.get(Calendar.MONTH)

            // Cek apakah tanggal berubah
            val isDateChanged = !isSameDay(taskDateMillis, originalTaskDateMillis)

            // Tentukan ID untuk task yang akan disimpan
            val newTaskId = if (isRescheduleMode || isDateChanged) {
                // Jika mode reschedule atau tanggal berubah, gunakan tanggal baru sebagai ID
                calendarForNewDate.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            } else {
                // Jika tanggal tidak berubah, gunakan ID asli
                taskIdToEdit
            }

            // Buat objek Task baru
            val updatedTask = Task(
                id = newTaskId,
                title = title,
                time = time.ifEmpty { "" },
                category = location.ifEmpty { "" },
                priority = priority,
                endTimeMillis = newEndTimeMillis,
                monthAdded = updatedMonthAdded,
                flowDurationMillis = savedFlowDuration,
                details = details,
                actionDateMillis = null
            )

            val success = if (isRescheduleMode || isDateChanged) {
                // Hapus task lama dan buat task baru di tanggal yang baru
                TaskRepository.deleteTask(taskIdToEdit)
                TaskRepository.addTask(updatedTask)
                true
            } else {
                // Update task yang ada dengan ID yang sama
                TaskRepository.updateTask(taskIdToEdit, updatedTask)
            }

            if (success) {
                val message = if (isDateChanged && !isRescheduleMode) {
                    "Task berhasil dipindahkan ke ${uiDateFormat.format(Date(taskDateMillis))}"
                } else if (isRescheduleMode) {
                    "Task berhasil dijadwalkan ulang"
                } else {
                    "Task berhasil diperbarui"
                }
                showConfirmationDialog(updatedTask, message)
            } else {
                Toast.makeText(this, "Gagal menyimpan pembaruan tugas.", Toast.LENGTH_SHORT).show()
            }
        }

        inputPriority.setOnClickListener {
            showPriorityDialog()
        }

        inputTime.setOnClickListener {
            showTimeRangePicker()
        }
        inputTime.isFocusable = false
        inputTime.isFocusableInTouchMode = false

        inputDate.setOnClickListener {
            showDatePicker()
        }
        inputDate.isFocusable = false
        inputDate.isFocusableInTouchMode = false

        tvAddFlowTimer.setOnClickListener {
            showFlowTimerDialog()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = taskDateMillis }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                taskDateMillis = calendar.timeInMillis
                inputDate.setText(uiDateFormat.format(calendar.time))

                // Beri notifikasi jika tanggal berubah
                if (!isSameDay(taskDateMillis, originalTaskDateMillis)) {
                    Toast.makeText(this, "Tanggal diubah. Task akan dipindahkan ke tanggal baru.", Toast.LENGTH_SHORT).show()
                }
            }, year, month, day)

        datePicker.show()
    }

    private fun showFlowTimerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_flow_timer, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val npHour = dialogView.findViewById<NumberPicker>(R.id.npHour)
        val npMinute = dialogView.findViewById<NumberPicker>(R.id.npMinute)
        val npSecond = dialogView.findViewById<NumberPicker>(R.id.npSecond)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<TextView>(R.id.btnSave)

        // Gunakan durasi yang tersimpan untuk pre-fill
        val currentDuration = flowTimerDurationMillis

        var initialHours = 0
        var initialMinutes = 0
        var initialSeconds = 0

        if (currentDuration > 0L) {
            initialHours = (currentDuration / MILLIS_IN_HOUR).toInt()
            val remainingAfterHours = currentDuration % MILLIS_IN_HOUR
            initialMinutes = (remainingAfterHours / MILLIS_IN_MINUTE).toInt()
            initialSeconds = ((remainingAfterHours % MILLIS_IN_MINUTE) / MILLIS_IN_SECOND).toInt()
        }

        npHour.minValue = 0
        npHour.maxValue = 24
        npHour.value = initialHours

        npMinute.minValue = 0
        npMinute.maxValue = 59
        npMinute.value = initialMinutes

        npSecond.minValue = 0
        npSecond.maxValue = 59
        npSecond.value = initialSeconds

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val hours = npHour.value
            val minutes = npMinute.value
            val seconds = npSecond.value

            val totalMillis = (hours * MILLIS_IN_HOUR) + (minutes * MILLIS_IN_MINUTE) + (seconds * MILLIS_IN_SECOND)

            if (totalMillis <= 0L) {
                Toast.makeText(this, "Durasi Flow Timer harus lebih dari 0.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            flowTimerDurationMillis = totalMillis

            // Tag disetel ke Boolean true untuk menandakan Flow Timer aktif
            inputTime.tag = true

            val timeDisplayString = formatDurationToString(totalMillis)
            tvAddFlowTimer.text = "Flow Timer Set (${timeDisplayString})"

            inputTime.setText("")

            Toast.makeText(this, "Flow Timer berhasil disetel: ${timeDisplayString}.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showTimeRangePicker() {
        // Reset Flow Timer status
        inputTime.tag = null
        val timeDisplayString = formatDurationToString(flowTimerDurationMillis)
        tvAddFlowTimer.text = "+ Add Flow Timer (${timeDisplayString})"
        inputTime.setText("")

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        var startTimeString = ""

        val startTimePicker = TimePickerDialog(
            this,
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar,
            { _, hourOfDay, minute ->
                startTimeString = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)

                val endTimePicker = TimePickerDialog(
                    this,
                    android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar,
                    { _, endHourOfDay, endMinute ->
                        val endTimeString = String.format(Locale.getDefault(), "%02d:%02d", endHourOfDay, endMinute)
                        inputTime.setText("$startTimeString - $endTimeString")

                        // Gunakan tanggal yang dipilih sebagai basis
                        val selectedDayCalendar = Calendar.getInstance().apply { timeInMillis = taskDateMillis }
                        val startHourInt = startTimeString.substringBefore(":").toIntOrNull() ?: 0
                        val startMinuteInt = startTimeString.substringAfter(":").toIntOrNull() ?: 0

                        val endCalendarCheck = selectedDayCalendar.clone() as Calendar
                        endCalendarCheck.set(Calendar.HOUR_OF_DAY, endHourOfDay)
                        endCalendarCheck.set(Calendar.MINUTE, endMinute)

                        val startCalendarCheck = selectedDayCalendar.clone() as Calendar
                        startCalendarCheck.set(Calendar.HOUR_OF_DAY, startHourInt)
                        startCalendarCheck.set(Calendar.MINUTE, startMinuteInt)

                        if (endCalendarCheck.timeInMillis <= startCalendarCheck.timeInMillis) {
                            selectedDayCalendar.add(Calendar.DAY_OF_MONTH, 1)
                        }

                        selectedDayCalendar.set(Calendar.HOUR_OF_DAY, endHourOfDay)
                        selectedDayCalendar.set(Calendar.MINUTE, endMinute)
                        selectedDayCalendar.set(Calendar.SECOND, 0)
                        selectedDayCalendar.set(Calendar.MILLISECOND, 0)

                        // Simpan newEndTimeMillis
                        inputTime.tag = selectedDayCalendar.timeInMillis
                    },
                    currentHour,
                    currentMinute,
                    true
                )
                endTimePicker.setTitle("Pilih Waktu Berakhir")
                endTimePicker.show()
            },
            currentHour,
            currentMinute,
            true
        )
        startTimePicker.setTitle("Pilih Waktu Mulai")
        startTimePicker.show()
    }

    private fun formatDurationToString(millis: Long): String {
        val durationHours = (millis / MILLIS_IN_HOUR).toInt()
        val remainingAfterHours = millis % MILLIS_IN_HOUR
        val durationMinutes = (remainingAfterHours / MILLIS_IN_MINUTE).toInt()
        val durationSeconds = ((remainingAfterHours % MILLIS_IN_MINUTE) / MILLIS_IN_SECOND).toInt()

        return when {
            durationHours > 0 && durationMinutes > 0 && durationSeconds > 0 -> "${durationHours}h ${durationMinutes}m ${durationSeconds}s"
            durationHours > 0 && durationMinutes > 0 -> "${durationHours}h ${durationMinutes}m"
            durationHours > 0 && durationSeconds > 0 -> "${durationHours}h ${durationSeconds}s"
            durationMinutes > 0 && durationSeconds > 0 -> "${durationMinutes}m ${durationSeconds}s"
            durationHours > 0 -> "${durationHours}h"
            durationMinutes > 0 -> "${durationMinutes}m"
            durationSeconds > 0 -> "${durationSeconds}s"
            else -> "0s"
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun showConfirmationDialog(task: Task, message: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_success, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvMessageTitle = dialogView.findViewById<TextView>(R.id.tvMessageTitle)
        val btnClose = dialogView.findViewById<TextView>(R.id.btnIgnore)
        val btnView = dialogView.findViewById<TextView>(R.id.btnView)

        tvMessageTitle.text = message
        tvMessageTitle.setTextColor(Color.parseColor("#283F6D"))

        val createResultIntent = {
            Intent().apply {
                putExtra("EXTRA_TASK_TITLE", task.title)
                putExtra("EXTRA_TASK_TIME", task.time)
                putExtra("EXTRA_TASK_CATEGORY", task.category)
            }
        }

        btnClose.text = "Tutup"
        btnView.text = "Lihat Tugas"

        btnClose.setOnClickListener {
            setResult(Activity.RESULT_OK, createResultIntent())
            dialog.dismiss()
            finish()
        }

        btnView.setOnClickListener {
            setResult(Activity.RESULT_OK, createResultIntent())

            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun showPriorityDialog() {
        val listPopupWindow = ListPopupWindow(this).apply {
            anchorView = inputPriority

            val adapter = PriorityAdapter(this@EditTaskActivity, priorities)
            setAdapter(adapter)

            width = inputPriority.width
            isModal = true
            setBackgroundDrawable(ResourcesCompat.getDrawable(resources, R.drawable.bg_popup_rounded_12dp, theme))
        }

        listPopupWindow.setOnItemClickListener { parent, view, position, id ->
            val selectedPriority = priorities[position]

            currentSelectedPriority = selectedPriority
            inputPriority.setText(selectedPriority)

            Toast.makeText(this, "Prioritas diatur ke: $selectedPriority", Toast.LENGTH_SHORT).show()
            listPopupWindow.dismiss()
        }

        listPopupWindow.show()
    }

    private inner class PriorityAdapter(context: Context, items: Array<String>) :
        ArrayAdapter<String>(context, 0, items) {

        private val inflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.list_item_priority, parent, false)
            val item = getItem(position)!!

            val tvOption = view.findViewById<TextView>(R.id.tvPriorityOption)
            val ivCheckmark = view.findViewById<ImageView>(R.id.ivCheckmark)

            tvOption.text = item

            if (item == currentSelectedPriority) {
                ivCheckmark.visibility = View.VISIBLE
            } else {
                ivCheckmark.visibility = View.INVISIBLE
            }

            return view
        }
    }
}