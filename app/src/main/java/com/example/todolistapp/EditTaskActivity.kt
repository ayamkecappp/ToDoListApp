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
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import android.util.Log
import java.util.UUID
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditTaskActivity : AppCompatActivity() {

    private lateinit var inputPriority: EditText
    private lateinit var inputActivity: EditText
    private lateinit var inputTime: EditText
    private lateinit var inputLocation: EditText
    private lateinit var inputDetails: EditText
    private lateinit var inputDate: EditText
    private lateinit var tvAddFlowTimer: TextView
    private lateinit var btnSave: Button

    private var taskIdToEdit: String = "" // ID ASLI dari task yang sedang diedit (String)
    private var taskDateMillis: Long = System.currentTimeMillis() // Tanggal yang dipilih (bisa berubah)
    private var originalTaskDateMillis: Long = System.currentTimeMillis() // Tanggal ASLI task (dinormalisasi ke tengah malam, untuk perbandingan)
    private var taskEndTimeMillis: Long = 0L // End Time asli


    companion object {
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID" // Sekarang menampung String ID
        const val EXTRA_RESCHEDULE_MODE = "EXTRA_RESCHEDULE_MODE"
        const val DEFAULT_FLOW_TIMER_DURATION = 30 * 60 * 1000L // 30 menit default
    }

    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH)

    private var currentSelectedPriority: String = "None"
    private val priorities = arrayOf("None", "Low", "Medium", "High")

    private var flowTimerDurationMillis: Long = 0L
    private val MILLIS_IN_HOUR = 60 * 60 * 1000L
    private val MILLIS_IN_MINUTE = 60 * 1000L
    private val MILLIS_IN_SECOND = 1000L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_task)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)

        inputActivity = findViewById(R.id.inputActivity)
        inputTime = findViewById(R.id.inputTime)
        inputLocation = findViewById(R.id.inputLocation)
        inputPriority = findViewById(R.id.inputPriority)
        inputDetails = findViewById(R.id.inputDetails)
        inputDate = findViewById(R.id.inputDate)
        tvAddFlowTimer = findViewById(R.id.tvAddFlowTimer)

        // 1. Ambil Task ID dan Mode Reschedule dari Intent
        taskIdToEdit = intent.getStringExtra(EXTRA_TASK_ID) ?: ""
        val isRescheduleMode = intent.getBooleanExtra(EXTRA_RESCHEDULE_MODE, false)

        if (taskIdToEdit.isEmpty()) {
            Toast.makeText(this, "Error: Invalid Task ID.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Muat Data Tugas yang Ada (Menggunakan lifecycleScope)
        lifecycleScope.launch(Dispatchers.IO) {
            val existingTask = TaskRepository.getTaskById(taskIdToEdit)

            withContext(Dispatchers.Main) {
                if (existingTask != null) {

                    // --- NORMALISASI TANGGAL LAMA UNTUK PERBANDINGAN TANGGAL SAJA ---
                    val originalTaskCal = Calendar.getInstance().apply { timeInMillis = existingTask.dueDate.toDate().time }
                    originalTaskCal.set(Calendar.HOUR_OF_DAY, 0)
                    originalTaskCal.set(Calendar.MINUTE, 0)
                    originalTaskCal.set(Calendar.SECOND, 0)
                    originalTaskCal.set(Calendar.MILLISECOND, 0)

                    originalTaskDateMillis = originalTaskCal.timeInMillis // Tanggal dinormalisasi
                    taskDateMillis = originalTaskCal.timeInMillis // New date default juga dinormalisasi
                    // --------------------------------------------------------------------

                    // --- Mengisi Form ---
                    inputActivity.setText(existingTask.title)
                    inputLocation.setText(existingTask.category)
                    inputPriority.setText(existingTask.priority)
                    inputDetails.setText(existingTask.details)

                    currentSelectedPriority = existingTask.priority

                    taskEndTimeMillis = existingTask.endTimeMillis

                    // Set Data: Menggunakan tanggal asli (normalized date)
                    val initialDate = Date(taskDateMillis)
                    inputDate.setText(uiDateFormat.format(initialDate))

                    // Set Flow Timer State
                    // 1. Ambil durasi Flow Timer yang disimpan
                    val actualFlowDuration = existingTask.flowDurationMillis

                    // 2. Tentukan nilai flowTimerDurationMillis untuk state internal
                    if (actualFlowDuration > 0L) {
                        flowTimerDurationMillis = actualFlowDuration
                    } else {
                        flowTimerDurationMillis = DEFAULT_FLOW_TIMER_DURATION
                    }

                    // 3. Tentukan string tampilan Flow Timer berdasarkan flowTimerDurationMillis
                    val timeDisplayString = formatDurationToString(flowTimerDurationMillis)

                    // 4. Set tvAddFlowTimer (selalu mencerminkan flowDurationMillis yang dimiliki task)
                    if (actualFlowDuration > 0L) {
                        tvAddFlowTimer.text = "Flow Timer Set (${timeDisplayString})"
                    } else {
                        // Jika Flow Timer 0, tampilkan prompt dengan durasi default
                        tvAddFlowTimer.text = "+ Add Flow Timer (${formatDurationToString(DEFAULT_FLOW_TIMER_DURATION)})"
                    }

                    // 5. Tentukan isi inputTime dan tag
                    val taskTimeText = existingTask.time.trim()

                    if (existingTask.endTimeMillis > 0L) {
                        // Memiliki Range Time (tag = Long)
                        inputTime.tag = existingTask.endTimeMillis
                        inputTime.setText(taskTimeText)
                    } else if (actualFlowDuration > 0L) {
                        // Memiliki Flow Timer (tag = Boolean True)
                        inputTime.tag = true

                        // KOSONGKAN inputTime sesuai permintaan jika hanya Flow Timer yang disetel
                        if (taskTimeText.contains("(Flow)")) {
                            inputTime.setText("")
                        } else {
                            // Jika ada teks lain yang disimpan di field 'time', biarkan tetap ada
                            inputTime.setText(taskTimeText)
                        }
                    } else {
                        // Tidak ada waktu spesifik (tag = null)
                        inputTime.tag = null
                        inputTime.setText(taskTimeText)
                    }

                    // Set tombol dan judul untuk mode reschedule (jika ada)
                    if (isRescheduleMode) {
                        btnSave.text = "Restore & Reschedule"
                        findViewById<TextView>(R.id.tvNewReminderTitle)?.text = "Reschedule Task"

                        // Reset taskDateMillis ke hari ini untuk Reschedule default
                        val todayCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        taskDateMillis = todayCal.timeInMillis
                        inputDate.setText(uiDateFormat.format(Date(taskDateMillis)))

                        // Reset time info
                        inputTime.setText("")
                        inputTime.tag = null
                        flowTimerDurationMillis = DEFAULT_FLOW_TIMER_DURATION
                        tvAddFlowTimer.text = "+ Add Flow Timer (${formatDurationToString(DEFAULT_FLOW_TIMER_DURATION)})"
                    } else {
                        btnSave.text = "Update"
                    }

                    Toast.makeText(this@EditTaskActivity, "Task '${existingTask.title}' ready to edit.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@EditTaskActivity, "Error: Task not found.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        btnBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        btnSave.setOnClickListener {
            saveChanges(isRescheduleMode)
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

    private fun saveChanges(isRescheduleMode: Boolean) {
        val rawTitle = inputActivity.text.toString()
        val rawLocation = inputLocation.text.toString()
        val rawDetails = inputDetails.text.toString()
        val priority = currentSelectedPriority

        val title = InputValidator.sanitizeText(rawTitle)
        val location = InputValidator.sanitizeText(rawLocation)
        val details = InputValidator.sanitizeText(rawDetails)

        var time: String
        var newEndTimeMillis: Long = 0L
        var savedFlowDuration: Long = 0L

        val isTimeRangeSet = inputTime.tag is Long
        val isFlowTimerActive = inputTime.tag is Boolean && inputTime.tag as Boolean

        if (!InputValidator.isValidTaskTitle(title)) {
            inputActivity.error = "Nama aktivitas tidak boleh kosong atau terlalu panjang!"
            return
        }

        // Ambil teks dari kolom time
        val inputTimeText = inputTime.text.toString().trim()
        time = inputTimeText // Default: gunakan teks yang diinput user

        // Mulai calendar dengan tanggal yang telah dipilih (taskDateMillis)
        val selectedDayCalendar = Calendar.getInstance().apply { timeInMillis = taskDateMillis }

        if (isTimeRangeSet) {
            newEndTimeMillis = inputTime.tag as Long
            savedFlowDuration = flowTimerDurationMillis

            // *** LOGIKA UTAMA: Gabungkan tanggal baru dengan waktu lama ***
            val oldTimeCal = Calendar.getInstance().apply { timeInMillis = newEndTimeMillis }

            // Terapkan komponen waktu (jam, menit, dll.) dari waktu lama ke tanggal baru
            selectedDayCalendar.set(Calendar.HOUR_OF_DAY, oldTimeCal.get(Calendar.HOUR_OF_DAY))
            selectedDayCalendar.set(Calendar.MINUTE, oldTimeCal.get(Calendar.MINUTE))
            selectedDayCalendar.set(Calendar.SECOND, oldTimeCal.get(Calendar.SECOND))
            selectedDayCalendar.set(Calendar.MILLISECOND, oldTimeCal.get(Calendar.MILLISECOND))

            // Simpan waktu akhir yang telah digabungkan
            newEndTimeMillis = selectedDayCalendar.timeInMillis
            // *** END LOGIKA ***
        } else if (isFlowTimerActive && flowTimerDurationMillis > 0L) {
            // FIX: Jangan gunakan waktu akhir Flow Timer sebagai deadline Missed Task.
            newEndTimeMillis = 0L
            savedFlowDuration = flowTimerDurationMillis

            // PERBAIKAN: Hanya isi field time jika inputTime kosong.
            if (inputTimeText.isEmpty()) {
                time = formatDurationToString(flowTimerDurationMillis) + " (Flow)"
            } else {
                // Biarkan 'time' menggunakan inputTimeText
            }

            // FIX: Atur DueDate ke akhir hari (23:59:59)
            selectedDayCalendar.set(Calendar.HOUR_OF_DAY, 23)
            selectedDayCalendar.set(Calendar.MINUTE, 59)
            selectedDayCalendar.set(Calendar.SECOND, 59)
            selectedDayCalendar.set(Calendar.MILLISECOND, 999)
        } else {
            time = inputTimeText
            newEndTimeMillis = 0L
            savedFlowDuration = flowTimerDurationMillis
            // Set DueDate ke akhir hari (23:59:59) jika tidak ada waktu spesifik
            selectedDayCalendar.set(Calendar.HOUR_OF_DAY, 23)
            selectedDayCalendar.set(Calendar.MINUTE, 59)
            selectedDayCalendar.set(Calendar.SECOND, 59)
            selectedDayCalendar.set(Calendar.MILLISECOND, 999)
        }

        val dueDateTimestamp = Timestamp(selectedDayCalendar.time)

        // Periksa apakah tanggalnya berubah
        val isDateChanged = !isSameDay(taskDateMillis, originalTaskDateMillis)

        // Jika tanggal berubah ATAU mode reschedule, buat ID baru untuk "memindahkan" task
        val newTaskId = if (isRescheduleMode || isDateChanged) {
            UUID.randomUUID().toString()
        } else {
            taskIdToEdit // Pertahankan ID asli untuk update
        }

        // Buat objek Task baru
        val updatedTask = Task(
            id = newTaskId,
            title = title,
            time = time.ifEmpty { "" },
            category = location.ifEmpty { "" },
            priority = priority,
            endTimeMillis = newEndTimeMillis,
            flowDurationMillis = savedFlowDuration,
            dueDate = dueDateTimestamp,
            details = details,
            status = "pending"
        )

        btnSave.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // TaskRepository.updateTaskSync akan menangani penghapusan task lama jika ID berubah
                val success = TaskRepository.updateTaskSync(taskIdToEdit, updatedTask)

                withContext(Dispatchers.Main) {
                    if (success) {
                        val message = if (isDateChanged && !isRescheduleMode) {
                            "Task successfully moved to ${uiDateFormat.format(Date(taskDateMillis))}"
                        } else if (isRescheduleMode) {
                            "Task successfully rescheduled"
                        } else {
                            "Task successfully updated"
                        }
                        showConfirmationDialog(updatedTask, message)
                    } else {
                        Toast.makeText(this@EditTaskActivity, "Failed to save task updates.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditTaskActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true
                }
            }
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

                // taskDateMillis menyimpan tanggal BARU (dibersihkan dari waktu)
                taskDateMillis = calendar.timeInMillis
                inputDate.setText(uiDateFormat.format(calendar.time))

                if (!isSameDay(taskDateMillis, originalTaskDateMillis)) {
                    Toast.makeText(this, "Date changed. Task will be moved to the new date.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Flow Timer duration must be greater than 0.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            flowTimerDurationMillis = totalMillis

            // Set flag: Flow Timer adalah opsi waktu utama
            inputTime.tag = true

            val timeDisplayString = formatDurationToString(totalMillis)
            // FIX: Update teks tvAddFlowTimer dengan durasi Flow Timer yang baru
            tvAddFlowTimer.text = "Flow Timer Set (${timeDisplayString})"

            Toast.makeText(this, "Flow Timer successfully set: ${timeDisplayString}.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showTimeRangePicker() {
        inputTime.tag = null
        // FIX: Menggunakan nilai flowTimerDurationMillis yang sudah disetel user
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

                        // 1. Mulai dengan tanggal yang dipilih/terakhir disimpan (normalized)
                        val finalDueDateCalendar = Calendar.getInstance().apply { timeInMillis = taskDateMillis }

                        val startHourInt = startTimeString.substringBefore(":").toIntOrNull() ?: 0
                        val startMinuteInt = startTimeString.substringAfter(":").toIntOrNull() ?: 0

                        val endCalendarCheck = finalDueDateCalendar.clone() as Calendar
                        endCalendarCheck.set(Calendar.HOUR_OF_DAY, endHourOfDay)
                        endCalendarCheck.set(Calendar.MINUTE, endMinute)

                        val startCalendarCheck = finalDueDateCalendar.clone() as Calendar
                        startCalendarCheck.set(Calendar.HOUR_OF_DAY, startHourInt)
                        startCalendarCheck.set(Calendar.MINUTE, startMinuteInt)

                        if (endCalendarCheck.timeInMillis <= startCalendarCheck.timeInMillis) {
                            // Logika mendeteksi melampaui tengah malam, majukan tanggal
                            finalDueDateCalendar.add(Calendar.DAY_OF_MONTH, 1)

                            // *** PENTING: Update taskDateMillis dan tampilan inputDate jika roll-over
                            val midnightOfNewDate = finalDueDateCalendar.clone() as Calendar
                            midnightOfNewDate.set(Calendar.HOUR_OF_DAY, 0)
                            midnightOfNewDate.set(Calendar.MINUTE, 0)
                            midnightOfNewDate.set(Calendar.SECOND, 0)
                            midnightOfNewDate.set(Calendar.MILLISECOND, 0)

                            taskDateMillis = midnightOfNewDate.timeInMillis
                            inputDate.setText(uiDateFormat.format(midnightOfNewDate.time))
                            Toast.makeText(this, "End time is on the next day, date updated.", Toast.LENGTH_SHORT).show()
                            // ***
                        }

                        // 2. Set waktu akhir yang sebenarnya (dengan tanggal yang mungkin sudah maju)
                        finalDueDateCalendar.set(Calendar.HOUR_OF_DAY, endHourOfDay)
                        finalDueDateCalendar.set(Calendar.MINUTE, endMinute)
                        finalDueDateCalendar.set(Calendar.SECOND, 0)
                        finalDueDateCalendar.set(Calendar.MILLISECOND, 0)

                        inputTime.tag = finalDueDateCalendar.timeInMillis
                    },
                    currentHour,
                    currentMinute,
                    true
                )
                endTimePicker.setTitle("Select End Time")
                endTimePicker.show()
            },
            currentHour,
            currentMinute,
            true
        )
        startTimePicker.setTitle("Select Start Time")
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

        val createResultIntent = { Intent().apply { putExtra("SHOULD_REFRESH_TASK", true) } }

        btnClose.text = "Close"
        btnView.text = "View Task"

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

            Toast.makeText(this, "Priority set to: $selectedPriority", Toast.LENGTH_SHORT).show()
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