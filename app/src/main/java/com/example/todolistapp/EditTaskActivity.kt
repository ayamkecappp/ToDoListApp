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
import java.text.SimpleDateFormat // DITAMBAHKAN
import android.app.DatePickerDialog

class EditTaskActivity : AppCompatActivity() {

    private lateinit var inputPriority: EditText
    private lateinit var inputActivity: EditText
    private lateinit var inputTime: EditText
    private lateinit var inputLocation: EditText
    private lateinit var inputDate: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView
    private lateinit var tvEditFlowTimer: TextView

    private var currentTask: Task? = null
    private var taskIdToEdit: Long = -1L
    private var currentTaskDayStartMillis: Long = 0L

    // DITAMBAHKAN: Format tanggal untuk tampilan
    private val uiDateDisplayFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    private var currentSelectedPriority: String = ""
    private val priorities = arrayOf("None", "Low", "Medium", "High")

    // Konstanta untuk Flow Timer
    companion object {
        const val PREFS_NAME = "TimerPrefs"
        const val KEY_FLOW_TIMER_DURATION = "flow_timer_duration"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_RESCHEDULE_MODE = "EXTRA_RESCHEDULE_MODE"
    }

    private var flowTimerDurationMillis: Long = 0L

    // Konstanta untuk konversi waktu
    private val MILLIS_IN_HOUR = 60 * 60 * 1000L
    private val MILLIS_IN_MINUTE = 60 * 1000L
    private val MILLIS_IN_SECOND = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_task)

        // Inisialisasi Views
        inputActivity = findViewById(R.id.inputActivity)
        inputTime = findViewById(R.id.inputTime)
        inputLocation = findViewById(R.id.inputLocation)
        inputPriority = findViewById(R.id.inputPriority)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
        inputDate = findViewById(R.id.inputDate)
        tvEditFlowTimer = findViewById(R.id.tvAddFlowTimer)

        // Nonaktifkan fokus manual
        inputTime.isFocusable = false
        inputTime.isFocusableInTouchMode = false
        inputPriority.isFocusable = false
        inputPriority.isFocusableInTouchMode = false
        inputDate.isFocusable = false
        inputDate.isFocusableInTouchMode = false

        // 1. Load the task
        taskIdToEdit = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        currentTask = TaskRepository.getTaskById(taskIdToEdit)

        if (currentTask == null) {
            Toast.makeText(this, "Tugas tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Initialize Flow Timer state
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultDuration = 30 * MILLIS_IN_MINUTE
        flowTimerDurationMillis = sharedPrefs.getLong(KEY_FLOW_TIMER_DURATION, defaultDuration)

        // 3. Populate fields
        inputActivity.setText(currentTask!!.title)
        inputLocation.setText(currentTask!!.category)

        currentSelectedPriority = currentTask!!.priority
        inputPriority.setText(currentSelectedPriority)

        // --- Date Initialization ---
        val taskDayCalendar = Calendar.getInstance().apply { timeInMillis = currentTask!!.id }
        inputDate.setText(uiDateDisplayFormat.format(taskDayCalendar.time))
        currentTaskDayStartMillis = taskDayCalendar.timeInMillis
        // --- End Date Initialization ---

        // --- Flow Timer Check and Initialization ---
        val taskTime = currentTask!!.time
        if (taskTime.contains("(Flow)")) {
            val timeDisplayString = formatDurationToString(flowTimerDurationMillis)
            tvEditFlowTimer.text = "Flow Timer Set (${timeDisplayString})"
            inputTime.setText("")
            inputTime.tag = null
        } else {
            flowTimerDurationMillis = 0L
            inputTime.setText(taskTime)
            tvEditFlowTimer.text = "+ Add Flow Timer"
            if(currentTask!!.endTimeMillis > 0L) {
                inputTime.tag = currentTask!!.endTimeMillis
            }
        }
        // ------------------------------------------

        // Listener
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

        // Listener untuk Time Input (UPDATED: untuk mereset Flow Timer)
        inputTime.setOnClickListener {
            showTimeRangePicker()
        }

        // DITAMBAHKAN: Listener untuk Date Input
        inputDate.setOnClickListener {
            showDatePicker()
        }

        // Listener untuk Flow Timer
        tvEditFlowTimer.setOnClickListener {
            showFlowTimerDialog()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // --- NEW FUNCTION: showDatePicker (Dengan Custom Design) ---
    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTaskDayStartMillis }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Menggunakan tema bawaan Android yang bersih, yang hasilnya akan putih,
        // dan warna aksen akan diatur oleh tema aplikasi utama.
        val datePickerDialog = DatePickerDialog(
            this,
            // Menggunakan tema dialog yang bersih
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

                // Simpan tanggal baru untuk digunakan saat Save
                currentTaskDayStartMillis = newDayStartMillis

                // Sinkronisasi waktu berakhir (jika sudah diatur)
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
        // Mengubah background window menjadi putih untuk tampilan kustom yang bersih
        datePickerDialog.window?.setBackgroundDrawableResource(android.R.color.white)
        datePickerDialog.show()
    }
    // --- END NEW FUNCTION ---

    // --- NEW FUNCTION: showFlowTimerDialog ---
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

        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) // PERBAIKAN
        val defaultDuration = 30 * MILLIS_IN_MINUTE
        val currentDuration = if (flowTimerDurationMillis > 0L) flowTimerDurationMillis else sharedPrefs.getLong(KEY_FLOW_TIMER_DURATION, defaultDuration) // PERBAIKAN

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

            sharedPrefs.edit().putLong(KEY_FLOW_TIMER_DURATION, flowTimerDurationMillis).apply() // PERBAIKAN

            val timeDisplayString = formatDurationToString(flowTimerDurationMillis)

            tvEditFlowTimer.text = "Flow Timer Set (${timeDisplayString})"

            inputTime.setText("")
            inputTime.tag = null

            Toast.makeText(this, "Flow Timer berhasil disetel: ${timeDisplayString}.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
    // --- END NEW FUNCTION ---

    // --- NEW HELPER FUNCTION: formatDurationToString ---
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
    // --- END NEW HELPER ---

    // --- UPDATED FUNCTION: updateTask ---
    private fun updateTask(task: Task) {
        val originalTaskId = task.id

        val title = inputActivity.text.toString().trim()
        var time = inputTime.text.toString().trim()
        val location = inputLocation.text.toString().trim()
        val priority = currentSelectedPriority

        val taskEndTimeMillis: Long

        if (flowTimerDurationMillis > 0L) {
            // Flow Timer aktif
            taskEndTimeMillis = System.currentTimeMillis() + flowTimerDurationMillis

            val timeDisplay = formatDurationToString(flowTimerDurationMillis) + " (Flow)"
            time = timeDisplay
        } else {
            // Flow Timer tidak aktif
            taskEndTimeMillis = inputTime.tag as? Long ?: 0L
            if (time.isEmpty()) time = "Waktu tidak disetel"
        }

        if (title.isEmpty()) {
            Toast.makeText(this, "Nama Aktivitas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedTask = task.copy(
            id = originalTaskId,
            title = title,
            time = time,
            category = if (location.isEmpty()) "Uncategorized" else location,
            priority = priority,
            endTimeMillis = taskEndTimeMillis
        )
        val success = TaskRepository.updateTask(originalTaskId, updatedTask)

        if (success) {
            showConfirmationDialog(updatedTask.title)
        } else {
            Toast.makeText(this, "Gagal menyimpan pembaruan tugas.", Toast.LENGTH_SHORT).show()
        }
    }
    // --- END UPDATED FUNCTION ---

    // --- UPDATED FUNCTION: showTimeRangePicker (untuk mereset Flow Timer) ---
    private fun showTimeRangePicker() {
        // Reset Flow Timer ketika Time Picker digunakan
        flowTimerDurationMillis = 0L
        tvEditFlowTimer.text = "+ Add Flow Timer" // Reset teks tombol
        inputTime.setText("") // Clear inputTime agar tidak bentrok

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        var startTimeString = ""

        // 1. Time Picker untuk Waktu MULAI
        val startTimePicker = TimePickerDialog(
            this,
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar,
            { _, hourOfDay, minute ->
                startTimeString = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)

                // 2. Time Picker untuk Waktu BERAKHIR
                val endTimePicker = TimePickerDialog(
                    this,
                    android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar,
                    { _, endHourOfDay, endMinute ->
                        val endTimeString = String.format(Locale.getDefault(), "%02d:%02d", endHourOfDay, endMinute)

                        inputTime.setText("$startTimeString - $endTimeString")

                        // LOGIKA PENTING: MENGHITUNG END TIME MILLIS
                        val selectedDayCalendar = Calendar.getInstance().apply { timeInMillis = currentTask?.id ?: System.currentTimeMillis() }

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
    // --- END UPDATED FUNCTION ---

    private fun showConfirmationDialog(taskTitle: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_success, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val mainMessageTextView = dialogView.findViewById<TextView>(R.id.tvMessageTitle)
        val btnIgnore = dialogView.findViewById<TextView>(R.id.btnIgnore)
        val btnView = dialogView.findViewById<TextView>(R.id.btnView)

        mainMessageTextView.text = "Tugas '$taskTitle' berhasil diperbarui."
        mainMessageTextView.setTextColor(Color.parseColor("#283F6D"))

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnIgnore.text = "Kembali ke Task" // Ganti teks
        btnView.visibility = View.GONE // Sembunyikan tombol View jika tidak diperlukan

        btnIgnore.setOnClickListener {
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