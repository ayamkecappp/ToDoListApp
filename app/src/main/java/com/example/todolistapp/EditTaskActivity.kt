package com.example.todolistapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import android.app.TimePickerDialog
import android.widget.NumberPicker
import android.graphics.drawable.ColorDrawable
import java.text.SimpleDateFormat
import android.app.DatePickerDialog
import android.widget.LinearLayout
import android.view.Gravity

class EditTaskActivity : AppCompatActivity() {

    private lateinit var inputPriority: EditText
    private lateinit var inputActivity: EditText
    private lateinit var inputTime: EditText
    private lateinit var inputLocation: EditText
    private lateinit var inputDate: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView
    private lateinit var tvEditFlowTimer: TextView
    private lateinit var tvTitle: TextView

    private var currentTask: Task? = null
    private var taskIdToEdit: Long = -1L
    private var currentTaskDayStartMillis: Long = 0L

    private val uiDateDisplayFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    private var currentSelectedPriority: String = ""
    private val priorities = arrayOf("None", "Low", "Medium", "High")

    private var isRescheduleMode: Boolean = false

    companion object {
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_RESCHEDULE_MODE = "EXTRA_RESCHEDULE_MODE"
        const val DEFAULT_FLOW_TIMER_DURATION = 30 * 60 * 1000L
    }

    private var taskSpecificFlowDuration: Long = 0L

    private val MILLIS_IN_HOUR = 60 * 60 * 1000L
    private val MILLIS_IN_MINUTE = 60 * 1000L
    private val MILLIS_IN_SECOND = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_task)

        // PENTING: Inisialisasi TaskRepository untuk load data tersimpan
        TaskRepository.initialize(applicationContext)

        inputActivity = findViewById(R.id.inputActivity)
        inputTime = findViewById(R.id.inputTime)
        inputLocation = findViewById(R.id.inputLocation)
        inputPriority = findViewById(R.id.inputPriority)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
        inputDate = findViewById(R.id.inputDate)
        tvEditFlowTimer = findViewById(R.id.tvAddFlowTimer)
        tvTitle = findViewById(R.id.tvNewReminderTitle)

        isRescheduleMode = intent.getBooleanExtra(EXTRA_RESCHEDULE_MODE, false)

        if (isRescheduleMode) {
            tvTitle.text = "Reschedule Task"
            btnSave.text = "Reschedule"
        } else {
            tvTitle.text = "Edit Task"
        }

        inputTime.isFocusable = false
        inputTime.isFocusableInTouchMode = false
        inputPriority.isFocusable = false
        inputPriority.isFocusableInTouchMode = false
        inputDate.isFocusable = false
        inputDate.isFocusableInTouchMode = false

        taskIdToEdit = intent.getLongExtra(EXTRA_TASK_ID, -1L)

        // PERBAIKAN: Load task dari repository setelah initialize
        currentTask = TaskRepository.getTaskById(taskIdToEdit)

        if (currentTask == null) {
            Toast.makeText(this, "Tugas tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Ambil durasi dari task ini
        taskSpecificFlowDuration = if (currentTask!!.flowDurationMillis > 0L) {
            currentTask!!.flowDurationMillis
        } else {
            DEFAULT_FLOW_TIMER_DURATION
        }

        inputActivity.setText(currentTask!!.title)
        inputLocation.setText(currentTask!!.category)

        currentSelectedPriority = currentTask!!.priority
        inputPriority.setText(currentSelectedPriority)

        val taskDayCalendar = Calendar.getInstance().apply { timeInMillis = currentTask!!.id }
        inputDate.setText(uiDateDisplayFormat.format(taskDayCalendar.time))
        currentTaskDayStartMillis = taskDayCalendar.timeInMillis

        val taskTime = currentTask!!.time

        // Logika penyetelan input time tag
        if (taskTime.contains("(Flow)")) {
            // Task menggunakan Flow Timer
            val timeDisplayString = formatDurationToString(taskSpecificFlowDuration)
            tvEditFlowTimer.text = "Flow Timer Set (${timeDisplayString})"
            inputTime.setText("")
            inputTime.tag = true
        } else {
            // Task menggunakan Time Range atau tidak ada waktu
            inputTime.setText(taskTime)

            val timeDisplayString = formatDurationToString(taskSpecificFlowDuration)
            tvEditFlowTimer.text = "+ Add Flow Timer (${timeDisplayString})"

            if(currentTask!!.endTimeMillis > 0L) {
                inputTime.tag = currentTask!!.endTimeMillis
            } else {
                inputTime.tag = null
            }
        }

        btnBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        btnSave.setOnClickListener {
            updateTask(currentTask!!)
        }

        inputPriority.setOnClickListener {
            showPriorityDialog()
        }

        inputTime.setOnClickListener {
            showTimeRangePicker()
        }

        inputDate.setOnClickListener {
            showDatePicker()
        }

        tvEditFlowTimer.setOnClickListener {
            showFlowTimerDialog()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTaskDayStartMillis }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar,
            { _, selectedYear, selectedMonth, selectedDay ->
                val newCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val newDayStartMillis = newCalendar.timeInMillis

                inputDate.setText(uiDateDisplayFormat.format(Date(newDayStartMillis)))
                currentTaskDayStartMillis = newDayStartMillis

                // Jika Time Range aktif, update endTimeMillis-nya
                val existingEndTimeMillis = inputTime.tag as? Long ?: 0L
                if (existingEndTimeMillis > 0L) {
                    val existingEndTimeCal = Calendar.getInstance().apply { timeInMillis = existingEndTimeMillis }

                    newCalendar.set(Calendar.HOUR_OF_DAY, existingEndTimeCal.get(Calendar.HOUR_OF_DAY))
                    newCalendar.set(Calendar.MINUTE, existingEndTimeCal.get(Calendar.MINUTE))

                    inputTime.tag = newCalendar.timeInMillis
                }
            },
            year, month, day
        )
        datePickerDialog.window?.setBackgroundDrawableResource(android.R.color.white)
        datePickerDialog.show()
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

        val initialDuration = taskSpecificFlowDuration

        var initialHours = 0
        var initialMinutes = 0
        var initialSeconds = 0

        if (initialDuration > 0L) {
            initialHours = (initialDuration / MILLIS_IN_HOUR).toInt()
            val remainingAfterHours = initialDuration % MILLIS_IN_HOUR
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

            taskSpecificFlowDuration = totalMillis

            val timeDisplayString = formatDurationToString(totalMillis)

            tvEditFlowTimer.text = "Flow Timer Set (${timeDisplayString})"

            inputTime.setText("")
            inputTime.tag = true

            Toast.makeText(this, "Durasi Flow Timer diatur ke: ${timeDisplayString}.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
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

    private fun updateTask(task: Task) {
        val originalTaskId = task.id

        val title = inputActivity.text.toString().trim()
        var time = inputTime.text.toString().trim()
        val location = inputLocation.text.toString().trim()
        val priority = currentSelectedPriority

        var taskEndTimeMillis: Long = 0L

        // Cek Tag: Boolean untuk Flow Timer, Long untuk Time Range
        val isFlowTimerActive = inputTime.tag is Boolean && inputTime.tag as Boolean
        val isTimeRangeActive = inputTime.tag is Long

        var savedFlowDuration: Long = 0L

        val newTaskId = currentTaskDayStartMillis

        if (isFlowTimerActive) {
            // Flow Timer aktif
            taskEndTimeMillis = System.currentTimeMillis() + taskSpecificFlowDuration
            val timeDisplay = formatDurationToString(taskSpecificFlowDuration) + " (Flow)"
            time = timeDisplay
            savedFlowDuration = taskSpecificFlowDuration
        } else if(isTimeRangeActive) {
            // Time Range aktif
            taskEndTimeMillis = inputTime.tag as Long
            time = inputTime.text.toString().trim()
            savedFlowDuration = taskSpecificFlowDuration
        } else {
            // Tidak ada waktu
            time = ""
            taskEndTimeMillis = 0L
            savedFlowDuration = taskSpecificFlowDuration
        }

        if (title.isEmpty()) {
            Toast.makeText(this, "Nama Aktivitas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        // PERBAIKAN: Hitung monthAdded berdasarkan newTaskId (tanggal baru)
        val monthAdded = Calendar.getInstance().apply {
            timeInMillis = newTaskId
        }.get(Calendar.MONTH)

        val updatedTask = task.copy(
            id = newTaskId,
            title = title,
            time = time,
            category = location.ifEmpty { "" },
            priority = priority,
            endTimeMillis = taskEndTimeMillis,
            monthAdded = monthAdded, // Update monthAdded
            flowDurationMillis = savedFlowDuration,
            actionDateMillis = null // Reset actionDateMillis untuk task yang di-edit
        )

        val success = TaskRepository.updateTask(originalTaskId, updatedTask)

        if (success) {
            val action = if (isRescheduleMode) "di-reschedule" else "diperbarui"
            showConfirmationDialog(updatedTask.title, action)
        } else {
            Toast.makeText(this, "Gagal menyimpan pembaruan tugas.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimeRangePicker() {
        // Reset tag ke null
        inputTime.tag = null

        val timeDisplayString = formatDurationToString(taskSpecificFlowDuration)
        tvEditFlowTimer.text = "+ Add Flow Timer (${timeDisplayString})"

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

                        val selectedDayCalendar = Calendar.getInstance().apply { timeInMillis = currentTaskDayStartMillis }

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

    private fun showConfirmationDialog(taskTitle: String, action: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_success, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val mainMessageTextView = dialogView.findViewById<TextView>(R.id.tvMessageTitle)
        val btnIgnore = dialogView.findViewById<TextView>(R.id.btnIgnore)
        val btnView = dialogView.findViewById<TextView>(R.id.btnView)

        val dialogViewRoot = dialogView as ViewGroup
        val buttonContainer = dialogViewRoot.getChildAt(2) as LinearLayout
        val verticalDivider = buttonContainer.getChildAt(1)

        val message = "Tugas '$taskTitle' berhasil $action."

        mainMessageTextView.text = message
        mainMessageTextView.setTextColor(Color.parseColor("#283F6D"))

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnIgnore.visibility = View.GONE
        verticalDivider.visibility = View.GONE

        btnView.text = "OK"

        val viewParams = btnView.layoutParams as LinearLayout.LayoutParams
        viewParams.width = 0
        viewParams.weight = 2.0f
        btnView.layoutParams = viewParams
        btnView.gravity = Gravity.CENTER

        btnView.setOnClickListener {
            setResult(Activity.RESULT_OK)
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

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}