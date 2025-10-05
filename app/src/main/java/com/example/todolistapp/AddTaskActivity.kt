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
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.appcompat.app.AlertDialog
import android.graphics.Color
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import android.content.DialogInterface
import android.app.TimePickerDialog
import android.widget.NumberPicker
import android.graphics.drawable.ColorDrawable

class AddTaskActivity : AppCompatActivity() {

    private lateinit var inputPriority: EditText
    private lateinit var inputActivity: EditText
    private lateinit var inputTime: EditText
    private lateinit var inputLocation: EditText
    private lateinit var tvAddFlowTimer: TextView

    private var taskDateMillis: Long = System.currentTimeMillis()
    private val EXTRA_SELECTED_DATE_MILLIS = "EXTRA_SELECTED_DATE_MILLIS"
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    private var currentSelectedPriority: String = "None"
    private val priorities = arrayOf("None", "Low", "Medium", "High")

    companion object {
        const val DEFAULT_FLOW_TIMER_DURATION = 30 * 60 * 1000L // 30 menit default
    }

    // ✅ Durasi Flow Timer SPESIFIK untuk task yang akan dibuat
    private var flowTimerDurationMillis: Long = DEFAULT_FLOW_TIMER_DURATION

    private val MILLIS_IN_HOUR = 60 * 60 * 1000L
    private val MILLIS_IN_MINUTE = 60 * 1000L
    private val MILLIS_IN_SECOND = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addtask)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)

        inputActivity = findViewById(R.id.inputActivity)
        inputTime = findViewById(R.id.inputTime)
        inputLocation = findViewById(R.id.inputLocation)
        inputPriority = findViewById(R.id.inputPriority)
        tvAddFlowTimer = findViewById(R.id.tvAddFlowTimer)

        // ✅ KONDISI AWAL SELALU: "+ Add Flow Timer (30m)"
        flowTimerDurationMillis = DEFAULT_FLOW_TIMER_DURATION
        val defaultTimeDisplayString = formatDurationToString(DEFAULT_FLOW_TIMER_DURATION)
        tvAddFlowTimer.text = "+ Add Flow Timer (${defaultTimeDisplayString})"

        // Pastikan tag awal null
        inputTime.tag = null

        val selectedMillis = intent.getLongExtra(EXTRA_SELECTED_DATE_MILLIS, -1L)
        if (selectedMillis != -1L) {
            taskDateMillis = selectedMillis
            val selectedDate = Date(taskDateMillis)
            Toast.makeText(this, "Aktivitas akan ditambahkan pada: ${uiDateFormat.format(selectedDate)}", Toast.LENGTH_LONG).show()
        } else {
            taskDateMillis = System.currentTimeMillis()
            val todayDate = Date(taskDateMillis)
            Toast.makeText(this, "Aktivitas akan ditambahkan pada hari ini: ${uiDateFormat.format(todayDate)}", Toast.LENGTH_SHORT).show()
        }

        inputPriority.setText(currentSelectedPriority)

        btnBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        btnSave.setOnClickListener {
            val title = inputActivity.text.toString().trim()
            var time = inputTime.text.toString().trim()
            val location = inputLocation.text.toString().trim()
            val priority = currentSelectedPriority

            var taskEndTimeMillis: Long = 0L

            // Cek Tag: Boolean untuk Flow Timer, Long untuk Time Range
            val isTimeRangeSet = inputTime.tag is Long
            val isFlowTimerActive = inputTime.tag is Boolean && inputTime.tag as Boolean

            var savedFlowDuration: Long = 0L

            // VALIDASI NAMA AKTIVITAS
            if (title.isEmpty()) {
                Toast.makeText(this, "Nama Aktivitas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // LOGIKA FLOW TIMER / TIME RANGE
            if (isTimeRangeSet) {
                // Time Range diutamakan
                taskEndTimeMillis = inputTime.tag as Long
                time = inputTime.text.toString().trim()
                // Simpan durasi flow spesifik task (untuk next edit)
                savedFlowDuration = flowTimerDurationMillis
            } else if (isFlowTimerActive && flowTimerDurationMillis > 0L) {
                // Flow Timer disetel
                taskEndTimeMillis = System.currentTimeMillis() + flowTimerDurationMillis
                val timeDisplay = formatDurationToString(flowTimerDurationMillis) + " (Flow)"
                time = timeDisplay
                savedFlowDuration = flowTimerDurationMillis
            } else {
                // Tidak ada waktu yang disetel
                time = ""
                taskEndTimeMillis = 0L
                // Simpan durasi flow spesifik task (untuk next edit)
                savedFlowDuration = flowTimerDurationMillis
            }

            // MEMBUAT TASK BARU
            val newTask = Task(
                id = taskDateMillis,
                title = title,
                time = time,
                category = location.ifEmpty { "" },
                priority = priority,
                endTimeMillis = taskEndTimeMillis,
                flowDurationMillis = savedFlowDuration // ✅ SIMPAN DURASI SPESIFIK
            )
            TaskRepository.addTask(newTask)

            showConfirmationDialog(newTask)
        }

        inputPriority.setOnClickListener {
            showPriorityDialog()
        }

        inputTime.setOnClickListener {
            showTimeRangePicker()
        }
        inputTime.isFocusable = false
        inputTime.isFocusableInTouchMode = false

        tvAddFlowTimer.setOnClickListener {
            showFlowTimerDialog()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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

        // ✅ GUNAKAN DURASI TASK INI (State yang tersimpan)
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

            // ✅ SIMPAN DURASI HANYA UNTUK TASK INI
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
        // ✅ RESET TAG KE NULL (membatalkan Flow Timer/Time Range lama)
        inputTime.tag = null

        // ✅ UPDATE TAMPILAN (tampilkan durasi current task yang tersimpan)
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

                        // Tag disetel ke Long untuk Time Range
                        inputTime.tag = selectedDayCalendar.timeInMillis as Long
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

    private fun showConfirmationDialog(newTask: Task) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_success, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnAddMore = dialogView.findViewById<TextView>(R.id.btnIgnore)
        val btnView = dialogView.findViewById<TextView>(R.id.btnView)

        val createResultIntent = {
            Intent().apply {
                putExtra("EXTRA_TASK_TITLE", newTask.title)
                putExtra("EXTRA_TASK_TIME", newTask.time)
                putExtra("EXTRA_TASK_CATEGORY", newTask.category)
            }
        }

        btnAddMore.setOnClickListener {
            setResult(Activity.RESULT_OK, createResultIntent())

            // ✅ RESET FORM
            inputActivity.setText("")
            inputTime.setText("")
            inputLocation.setText("")
            currentSelectedPriority = "None"
            inputPriority.setText(currentSelectedPriority)
            inputTime.tag = null

            // ✅ RESET DURASI KE DEFAULT 30m
            flowTimerDurationMillis = DEFAULT_FLOW_TIMER_DURATION
            val defaultTimeDisplayString = formatDurationToString(DEFAULT_FLOW_TIMER_DURATION)
            tvAddFlowTimer.text = "+ Add Flow Timer (${defaultTimeDisplayString})"

            taskDateMillis = System.currentTimeMillis()

            dialog.dismiss()
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

            val adapter = PriorityAdapter(this@AddTaskActivity, priorities)
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
}