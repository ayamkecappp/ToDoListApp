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

    // Konstanta untuk SharedPreferences
    companion object {
        const val PREFS_NAME = "TimerPrefs"
        const val KEY_FLOW_TIMER_DURATION = "flow_timer_duration"
    }

    // Konstanta untuk menyimpan durasi Flow Timer yang dipilih
    private var flowTimerDurationMillis: Long = 0L

    // Konstanta untuk konversi waktu
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

        // Dapatkan durasi Flow Timer yang terakhir disimpan (digunakan untuk pra-isi dialog)
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedDuration = sharedPrefs.getLong(KEY_FLOW_TIMER_DURATION, 0L)

        // PERBAIKAN LOGIKA DEFAULT: Jika storedDuration 0L, gunakan default 30 menit
        flowTimerDurationMillis = if (storedDuration > 0L) storedDuration else 30 * MILLIS_IN_MINUTE


        // 1. Periksa Intent untuk tanggal yang dipilih
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

            val taskEndTimeMillis: Long
            var isFlowTimerActive = false

            // 1. VALIDASI NAMA AKTIVITAS
            if (title.isEmpty()) {
                Toast.makeText(this, "Nama Aktivitas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. LOGIKA FLOW TIMER / TIME RANGE
            // Kita anggap Flow Timer aktif jika nilainya di Activity > 0L
            if (flowTimerDurationMillis > 0L) {
                // Flow Timer aktif
                isFlowTimerActive = true
                taskEndTimeMillis = System.currentTimeMillis() + flowTimerDurationMillis

                // Format durasi Flow Timer untuk disimpan di field waktu (untuk ditampilkan di TaskActivity)
                val durationHours = (flowTimerDurationMillis / MILLIS_IN_HOUR).toInt()
                val remainingAfterHours = flowTimerDurationMillis % MILLIS_IN_HOUR
                val durationMinutes = (remainingAfterHours / MILLIS_IN_MINUTE).toInt()
                val durationSeconds = ((remainingAfterHours % MILLIS_IN_MINUTE) / MILLIS_IN_SECOND).toInt()

                val timeDisplay = when {
                    durationHours > 0 && durationMinutes > 0 && durationSeconds > 0 -> "${durationHours}h ${durationMinutes}m ${durationSeconds}s (Flow)"
                    durationHours > 0 && durationMinutes > 0 -> "${durationHours}h ${durationMinutes}m (Flow)"
                    durationHours > 0 && durationSeconds > 0 -> "${durationHours}h ${durationSeconds}s (Flow)"
                    durationMinutes > 0 && durationSeconds > 0 -> "${durationMinutes}m ${durationSeconds}s (Flow)"
                    durationHours > 0 -> "${durationHours}h (Flow)"
                    durationMinutes > 0 -> "${durationMinutes}m (Flow)"
                    durationSeconds > 0 -> "${durationSeconds}s (Flow)"
                    else -> "Durasi Flow Timer Tidak Valid"
                }

                time = timeDisplay
            } else {
                // Flow Timer tidak aktif: Gunakan endTimeMillis dari TimePicker
                taskEndTimeMillis = inputTime.tag as? Long ?: 0L
            }

            // 3. VALIDASI WAKTU (WAJIB DIISI)
            if (!isFlowTimerActive && (time.isEmpty() || taskEndTimeMillis == 0L)) {
                Toast.makeText(this, "Waktu atau Durasi Flow Timer wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Perbaikan: Jika tidak ada Flow Timer, gunakan teks dari inputTime yang sudah diisi oleh TimePicker
            if (!isFlowTimerActive && time.isEmpty()) {
                time = inputTime.text.toString().trim()
            }


            val newTask = Task(
                id = taskDateMillis,
                title = title,
                time = time, // Gunakan waktu dari Time Picker atau Flow Timer
                category = if (location.isEmpty()) "Uncategorized" else location,
                priority = priority,
                endTimeMillis = taskEndTimeMillis // Menyimpan endTimeMillis (atau waktu selesai Flow Timer)
            )
            TaskRepository.addTask(newTask)

            showConfirmationDialog(newTask)
        }

        inputPriority.setOnClickListener {
            showPriorityDialog()
        }

        // Menggunakan Time Range Picker (24 jam)
        inputTime.setOnClickListener {
            showTimeRangePicker()
        }
        // Pastikan inputTime tidak fokus
        inputTime.isFocusable = false
        inputTime.isFocusableInTouchMode = false

        // Listener untuk Flow Timer
        tvAddFlowTimer.setOnClickListener {
            showFlowTimerDialog()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // FUNGSI BARU: DIALOG FLOW TIMER (UPDATED)
    private fun showFlowTimerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_flow_timer, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Dapatkan NumberPicker (ID harus sesuai dengan dialog_add_flow_timer.xml)
        val npHour = dialogView.findViewById<NumberPicker>(R.id.npHour)
        val npMinute = dialogView.findViewById<NumberPicker>(R.id.npMinute)
        val npSecond = dialogView.findViewById<NumberPicker>(R.id.npSecond)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<TextView>(R.id.btnSave)

        // Gunakan flowTimerDurationMillis yang ada di Activity sebagai currentDuration
        val currentDuration = flowTimerDurationMillis

        // Hitung nilai awal untuk NumberPicker
        var initialHours = 0
        var initialMinutes = 0
        var initialSeconds = 0

        if (currentDuration > 0L) {
            initialHours = (currentDuration / MILLIS_IN_HOUR).toInt()
            val remainingAfterHours = currentDuration % MILLIS_IN_HOUR
            initialMinutes = (remainingAfterHours / MILLIS_IN_MINUTE).toInt()
            initialSeconds = ((remainingAfterHours % MILLIS_IN_MINUTE) / MILLIS_IN_SECOND).toInt()
        }


        // Setup Number Picker Hour
        npHour.minValue = 0
        npHour.maxValue = 24
        npHour.value = initialHours

        // Setup Number Picker Minute
        npMinute.minValue = 0
        npMinute.maxValue = 59
        npMinute.value = initialMinutes

        // Setup Number Picker Second
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

            // Hitung total durasi dalam milidetik
            val totalMillis = (hours * MILLIS_IN_HOUR) + (minutes * MILLIS_IN_MINUTE) + (seconds * MILLIS_IN_SECOND)

            if (totalMillis <= 0L) {
                Toast.makeText(this, "Durasi Flow Timer harus lebih dari 0.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            flowTimerDurationMillis = totalMillis

            // Simpan durasi Flow Timer ke SharedPreferences (Global setting)
            val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) // PERBAIKAN: Langsung akses PREFS_NAME
            sharedPrefs.edit().putLong(KEY_FLOW_TIMER_DURATION, flowTimerDurationMillis).apply() // PERBAIKAN: Langsung akses KEY_FLOW_TIMER_DURATION

            // BARU: HANYA TAMPILKAN TOAST & UPDATE TEKS TOMBOL (UPDATED)
            val timeDisplayString = when {
                hours > 0 && minutes > 0 && seconds > 0 -> "${hours}h ${minutes}m ${seconds}s"
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 && seconds > 0 -> "${hours}h ${seconds}s"
                minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                seconds > 0 -> "${seconds}s"
                else -> "0s"
            }

            // Update teks tombol agar pengguna tahu durasi telah disetel
            tvAddFlowTimer.text = "Flow Timer Set (${timeDisplayString})"

            inputTime.setText("") // Clear inputTime agar tidak bentrok dengan Flow Timer
            inputTime.tag = null // Reset Time Picker tag

            Toast.makeText(this, "Flow Timer berhasil disetel: ${timeDisplayString}.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
    // END BARU

    // FUNGSI TIME RANGE PICKER 24 JAM (Diubah untuk mereset Flow Timer)
    private fun showTimeRangePicker() {
        // Reset Flow Timer ketika Time Picker digunakan
        flowTimerDurationMillis = 0L
        tvAddFlowTimer.text = "+ Add Flow Timer" // Reset teks tombol
        inputTime.setText("") // Clear input field

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        var startTimeString = ""

        // 1. Time Picker untuk Waktu MULAI
        val startTimePicker = TimePickerDialog(
            this,
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar,
            { _, hourOfDay, minute ->
                // Format waktu mulai (HH:mm)
                startTimeString = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)

                // 2. Time Picker untuk Waktu BERAKHIR (dipanggil setelah waktu mulai dipilih)
                val endTimePicker = TimePickerDialog(
                    this,
                    android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar,
                    { _, endHourOfDay, endMinute ->
                        // Format waktu berakhir (HH:mm)
                        val endTimeString = String.format(Locale.getDefault(), "%02d:%02d", endHourOfDay, endMinute)

                        // Update EditText dengan rentang waktu
                        inputTime.setText("$startTimeString - $endTimeString")

                        // --- LOGIKA PENTING: MENGHITUNG END TIME MILLIS ---
                        // Gunakan tanggal yang dipilih/default untuk menghitung waktu berakhir
                        val selectedDayCalendar = Calendar.getInstance().apply { timeInMillis = taskDateMillis }

                        // Mendapatkan waktu mulai dari string
                        val startHourInt = startTimeString.substringBefore(":").toIntOrNull() ?: 0
                        val startMinuteInt = startTimeString.substringAfter(":").toIntOrNull() ?: 0

                        val endCalendarCheck = selectedDayCalendar.clone() as Calendar
                        endCalendarCheck.set(Calendar.HOUR_OF_DAY, endHourOfDay)
                        endCalendarCheck.set(Calendar.MINUTE, endMinute)

                        val startCalendarCheck = selectedDayCalendar.clone() as Calendar
                        startCalendarCheck.set(Calendar.HOUR_OF_DAY, startHourInt)
                        startCalendarCheck.set(Calendar.MINUTE, startMinuteInt)

                        // Jika waktu berakhir lebih awal dari waktu mulai yang dikonversi ke milis (berarti melompat hari)
                        if (endCalendarCheck.timeInMillis <= startCalendarCheck.timeInMillis) {
                            selectedDayCalendar.add(Calendar.DAY_OF_MONTH, 1)
                        }

                        selectedDayCalendar.set(Calendar.HOUR_OF_DAY, endHourOfDay)
                        selectedDayCalendar.set(Calendar.MINUTE, endMinute)
                        selectedDayCalendar.set(Calendar.SECOND, 0)
                        selectedDayCalendar.set(Calendar.MILLISECOND, 0)

                        // Simpan endTimeMillis di tag EditText
                        inputTime.tag = selectedDayCalendar.timeInMillis
                        // ----------------------------------------------------
                    },
                    currentHour,
                    currentMinute,
                    true // is24HourView = true (Format 24 jam)
                )
                endTimePicker.setTitle("Pilih Waktu Berakhir")
                endTimePicker.show()
            },
            currentHour,
            currentMinute,
            true // is24HourView = true (Format 24 jam)
        )
        startTimePicker.setTitle("Pilih Waktu Mulai")
        startTimePicker.show()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()


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

            inputActivity.setText("")
            inputTime.setText("")
            inputLocation.setText("")
            currentSelectedPriority = "None"
            inputPriority.setText(currentSelectedPriority)
            inputTime.tag = null // Reset tag
            taskDateMillis = System.currentTimeMillis()
            flowTimerDurationMillis = 0L // Reset Flow Timer duration
            tvAddFlowTimer.text = "+ Add Flow Timer" // Reset teks tombol

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
}