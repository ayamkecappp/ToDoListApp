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
import java.text.SimpleDateFormat
import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.graphics.drawable.ColorDrawable
import android.widget.NumberPicker // NEW

class EditTaskActivity : AppCompatActivity() {

    private lateinit var inputPriority: EditText
    private lateinit var inputActivity: EditText
    private lateinit var inputTime: EditText
    private lateinit var inputLocation: EditText
    private lateinit var inputDate: EditText
    private lateinit var tvAddFlowTimer: TextView // NEW

    private var taskIdToEdit: Long = -1L
    private var taskSourceList: String? = null
    private var isRescheduleMode: Boolean = false

    // Menggunakan satu Calendar object untuk menyimpan tanggal dan waktu yang dipilih
    private var selectedDateTime: Calendar = Calendar.getInstance()

    companion object {
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TYPE = "EXTRA_TASK_TYPE" // "missed", "deleted", atau Status.ACTIVE.name
        // Konstanta Flow Timer (Replikasi dari AddTaskActivity)
        const val PREFS_NAME = "TimerPrefs"
        const val KEY_FLOW_TIMER_DURATION = "flow_timer_duration"
    }

    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))
    private val timeDisplayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var currentSelectedPriority: String = "None"
    private val priorities = arrayOf("None", "Low", "Medium", "High")

    private var flowTimerDurationMillis: Long = 0L // NEW

    // Konstanta untuk konversi waktu (Replikasi dari AddTaskActivity)
    private val MILLIS_IN_HOUR = 60 * 60 * 1000L
    private val MILLIS_IN_MINUTE = 60 * 1000L
    private val MILLIS_IN_SECOND = 1000L // NEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_task)

        val tvTitle = findViewById<TextView>(R.id.tvNewReminderTitle)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)

        inputActivity = findViewById(R.id.inputActivity)
        inputTime = findViewById(R.id.inputTime)
        inputLocation = findViewById(R.id.inputLocation)
        inputPriority = findViewById(R.id.inputPriority)
        inputDate = findViewById(R.id.inputDate)
        tvAddFlowTimer = findViewById(R.id.tvAddFlowTimer) // NEW

        // 1. Ambil Task ID dan Tipe dari Intent
        taskIdToEdit = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        taskSourceList = intent.getStringExtra(EXTRA_TASK_TYPE)

        isRescheduleMode = taskSourceList == "missed" || taskSourceList == "deleted"

        // 2. Muat Data Tugas yang Ada
        val existingTask = TaskRepository.findTaskInAnyList(taskIdToEdit)

        // NEW: Load Flow Timer duration dari SharedPreferences
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        flowTimerDurationMillis = sharedPrefs.getLong(KEY_FLOW_TIMER_DURATION, 0L)

        if (existingTask != null) {
            // Logika inisialisasi selectedDateTime
            selectedDateTime.timeInMillis = existingTask.id
            if (existingTask.endTimeMillis > 0L) {
                val endTimeCal = Calendar.getInstance().apply { timeInMillis = existingTask.endTimeMillis }
                selectedDateTime.set(Calendar.HOUR_OF_DAY, endTimeCal.get(Calendar.HOUR_OF_DAY))
                selectedDateTime.set(Calendar.MINUTE, endTimeCal.get(Calendar.MINUTE))
            } else {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, 12)
                selectedDateTime.set(Calendar.MINUTE, 0)
            }


            // Isi form dengan data tugas yang ada
            inputActivity.setText(existingTask.title.replace("Reschedule: ", ""))
            inputTime.setText(existingTask.time)
            inputLocation.setText(existingTask.category)
            inputPriority.setText(existingTask.priority)
            currentSelectedPriority = existingTask.priority
            inputDate.setText(uiDateFormat.format(selectedDateTime.time)) // Tampilkan tanggal

            // NEW: Cek apakah tugas yang diedit adalah Flow Timer
            if (existingTask.time.contains("(Flow)")) {
                // Parse durasi dari string jika perlu, atau andalkan flowTimerDurationMillis yang sudah di-load
                // Karena kita hanya menggunakan flowTimerDurationMillis untuk dialog set, kita biarkan saja.
                val timeParts = existingTask.time.split(" ")
                val durationString = timeParts.joinToString(" ").removeSuffix(" (Flow)")
                tvAddFlowTimer.text = "Flow Timer Set (${durationString})"
            }

            if (isRescheduleMode) {
                tvTitle.text = "Reschedule Task"
                btnSave.text = "Reschedule"
            } else {
                tvTitle.text = "Edit Reminder"
                btnSave.text = "Update"
            }
        } else {
            Toast.makeText(this, "Error: Tugas tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        // 3. Setup Listeners
        btnBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        btnSave.setOnClickListener {
            handleSave(existingTask)
        }

        inputPriority.setOnClickListener {
            showPriorityDialog()
        }

        inputTime.setOnClickListener {
            showTimeRangePicker()
        }
        inputTime.isFocusable = false
        inputTime.isFocusableInTouchMode = false

        // Listener untuk Date Picker
        inputDate.setOnClickListener {
            showDatePicker()
        }
        inputDate.isFocusable = false
        inputDate.isFocusableInTouchMode = false

        // Listener untuk Flow Timer // NEW
        tvAddFlowTimer.setOnClickListener {
            showFlowTimerDialog()
        }
    }

    private fun handleSave(existingTask: Task) {
        val title = inputActivity.text.toString().trim()
        var timeDisplay = inputTime.text.toString().trim() // Changed to var
        val location = inputLocation.text.toString().trim()
        val priority = currentSelectedPriority

        if (title.isEmpty()) {
            Toast.makeText(this, "Nama Aktivitas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        // Atur Status Baru: ACTIVE jika di-edit/reschedule, atau tetap sama
        val newStatus = if (isRescheduleMode) Task.Status.ACTIVE else existingTask.status
        val processedTitle = if (isRescheduleMode && !title.startsWith("Reschedule: ")) "Reschedule: $title" else title

        val newEndTimeMillis: Long

        // NEW LOGIC FOR FLOW TIMER
        if (flowTimerDurationMillis > 0L) {
            newEndTimeMillis = System.currentTimeMillis() + flowTimerDurationMillis

            val durationHours = (flowTimerDurationMillis / MILLIS_IN_HOUR).toInt()
            val remainingAfterHours = flowTimerDurationMillis % MILLIS_IN_HOUR
            val durationMinutes = (remainingAfterHours / MILLIS_IN_MINUTE).toInt()
            val durationSeconds = ((remainingAfterHours % MILLIS_IN_MINUTE) / MILLIS_IN_SECOND).toInt()

            val flowTimeDisplay = when {
                durationHours > 0 -> "${durationHours}h ${durationMinutes}m ${durationSeconds}s (Flow)"
                durationMinutes > 0 -> "${durationMinutes}m ${durationSeconds}s (Flow)"
                durationSeconds > 0 -> "${durationSeconds}s (Flow)"
                else -> "Durasi Flow Timer Tidak Valid"
            }
            timeDisplay = flowTimeDisplay
        } else {
            // Flow Timer is not active: use selectedDateTime (from TimePicker/Date Picker)
            newEndTimeMillis = selectedDateTime.timeInMillis
        }
        // END NEW LOGIC

        // 2. Buat objek Task baru dengan ID lama dan data yang diperbarui
        val updatedTask = existingTask.copy(
            id = existingTask.id, // ID tetap (kunci unik)
            title = processedTitle,
            time = if (timeDisplay.isEmpty()) "Waktu tidak disetel" else timeDisplay,
            category = if (location.isEmpty()) "Uncategorized" else location,
            priority = priority,
            endTimeMillis = newEndTimeMillis,
            monthAdded = selectedDateTime.get(Calendar.MONTH), // Perbarui bulan
            status = newStatus // PENTING: Pindahkan ke daftar aktif jika Reschedule
        )

        // Panggil fungsi update di Repository
        val success = TaskRepository.updateTask(updatedTask)

        if (success) {
            showConfirmationDialog(updatedTask)
        } else {
            Toast.makeText(this, if (isRescheduleMode) "Gagal me-reschedule tugas." else "Gagal menyimpan pembaruan tugas.", Toast.LENGTH_SHORT).show()
        }
    }

    // NEW: showFlowTimerDialog (Replikasi dari AddTaskActivity.kt)
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

        // Dapatkan durasi terakhir dari SharedPreferences
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentDuration = sharedPrefs.getLong(KEY_FLOW_TIMER_DURATION, 30 * MILLIS_IN_MINUTE)

        // Hitung nilai awal untuk NumberPicker
        var initialHours = 0
        var initialMinutes = 0
        var initialSeconds = 0

        if (currentDuration > 0L) {
            initialHours = (currentDuration / MILLIS_IN_HOUR).toInt()
            val remainingMillisAfterHours = currentDuration % MILLIS_IN_HOUR
            initialMinutes = (remainingMillisAfterHours / MILLIS_IN_MINUTE).toInt()
            initialSeconds = ((remainingMillisAfterHours % MILLIS_IN_MINUTE) / MILLIS_IN_SECOND).toInt()
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

            // Simpan durasi Flow Timer ke SharedPreferences
            sharedPrefs.edit().putLong(KEY_FLOW_TIMER_DURATION, flowTimerDurationMillis).apply()

            // HANYA TAMPILKAN TOAST & UPDATE TEKS TOMBOL
            val timeDisplayString = when {
                hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m ${seconds}s"
                seconds > 0 -> "${seconds}s"
                else -> "0s"
            }

            // Update teks tombol agar pengguna tahu durasi telah disetel
            tvAddFlowTimer.text = "Flow Timer Set (${timeDisplayString})"

            // Clear inputTime & tag karena menggunakan Flow Timer
            inputTime.setText("")

            Toast.makeText(this, "Flow Timer berhasil disetel: ${timeDisplayString}.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
    // END NEW: showFlowTimerDialog

    private fun showDatePicker() {
        val year = selectedDateTime.get(Calendar.YEAR)
        val month = selectedDateTime.get(Calendar.MONTH)
        val day = selectedDateTime.get(Calendar.DAY_OF_MONTH)

        // Gunakan DatePickerDialog (Standar Android Dialog)
        val datePicker = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Perbarui HANYA tanggal di selectedDateTime
                selectedDateTime.set(selectedYear, selectedMonth, selectedDay)
                inputDate.setText(uiDateFormat.format(selectedDateTime.time))
            },
            year, month, day)

        datePicker.show()
    }


    private fun showTimeRangePicker() {
        // Reset Flow Timer ketika Time Picker digunakan
        flowTimerDurationMillis = 0L
        tvAddFlowTimer.text = "+ Add Flow Timer" // Reset teks tombol
        inputTime.setText("") // Clear input field

        val calendar = selectedDateTime.clone() as Calendar
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        var startTimeString = ""

        // 1. Time Picker untuk Waktu MULAI
        val startTimePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                startTimeString = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)

                // 2. Time Picker untuk Waktu BERAKHIR
                val endTimePicker = TimePickerDialog(
                    this,
                    { _, endHourOfDay, endMinute ->
                        val endTimeString = String.format(Locale.getDefault(), "%02d:%02d", endHourOfDay, endMinute)

                        // Perbarui EditText
                        inputTime.setText("$startTimeString - $endTimeString")

                        // Periksa apakah waktu berakhir melompat hari
                        val startCal = selectedDateTime.clone() as Calendar
                        startCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        startCal.set(Calendar.MINUTE, minute)

                        val endCalCheck = selectedDateTime.clone() as Calendar
                        endCalCheck.set(Calendar.HOUR_OF_DAY, endHourOfDay)
                        endCalCheck.set(Calendar.MINUTE, endMinute)

                        // Jika waktu berakhir lebih awal/sama dengan waktu mulai, tambahkan satu hari ke tanggal
                        if (endCalCheck.timeInMillis <= startCal.timeInMillis) {
                            selectedDateTime.add(Calendar.DAY_OF_MONTH, 1)
                            inputDate.setText(uiDateFormat.format(selectedDateTime.time)) // Update display tanggal
                        }

                        // Set waktu BERAKHIR di selectedDateTime
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, endHourOfDay)
                        selectedDateTime.set(Calendar.MINUTE, endMinute)
                        selectedDateTime.set(Calendar.SECOND, 0)
                        selectedDateTime.set(Calendar.MILLISECOND, 0)
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


    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun showConfirmationDialog(task: Task) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_success, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvMessageTitle = dialogView.findViewById<TextView>(R.id.tvMessageTitle)
        val btnClose = dialogView.findViewById<TextView>(R.id.btnIgnore)
        val btnView = dialogView.findViewById<TextView>(R.id.btnView)

        tvMessageTitle.text = if (isRescheduleMode) "Success Reschedule Task" else "Success Update Reminder"
        tvMessageTitle.setTextColor(Color.parseColor("#283F6D"))

        val createResultIntent = {
            Intent().apply {
                putExtra(EXTRA_TASK_ID, task.id)
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