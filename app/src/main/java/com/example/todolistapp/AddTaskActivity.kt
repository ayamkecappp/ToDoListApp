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

class AddTaskActivity : AppCompatActivity() {

    private lateinit var inputPriority: EditText
    private lateinit var inputActivity: EditText
    private lateinit var inputTime: EditText
    private lateinit var inputLocation: EditText

    private var taskDateMillis: Long = System.currentTimeMillis()
    private val EXTRA_SELECTED_DATE_MILLIS = "EXTRA_SELECTED_DATE_MILLIS"
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    private var currentSelectedPriority: String = "None"
    private val priorities = arrayOf("None", "Low", "Medium", "High")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addtask)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)

        inputActivity = findViewById(R.id.inputActivity)
        inputTime = findViewById(R.id.inputTime)
        inputLocation = findViewById(R.id.inputLocation)
        inputPriority = findViewById(R.id.inputPriority)

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
            val time = inputTime.text.toString().trim()
            val location = inputLocation.text.toString().trim()
            val priority = currentSelectedPriority

            // --- Mengambil endTimeMillis dari tag EditText ---
            val taskEndTimeMillis = inputTime.tag as? Long ?: 0L
            // ----------------------------------------------------

            if (title.isEmpty()) {
                Toast.makeText(this, "Nama Aktivitas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newTask = Task(
                id = taskDateMillis,
                title = title,
                time = if (time.isEmpty()) "Waktu tidak disetel" else time,
                category = if (location.isEmpty()) "Uncategorized" else location,
                priority = priority,
                endTimeMillis = taskEndTimeMillis // Menyimpan endTimeMillis
            )
            TaskRepository.addTask(newTask)

            showConfirmationDialog(newTask)
        }

        inputPriority.setOnClickListener {
            showPriorityDialog()
        }

        // MODIFIKASI: Menggunakan Time Range Picker (24 jam)
        inputTime.setOnClickListener {
            showTimeRangePicker()
        }
        // Pastikan inputTime tidak fokus
        inputTime.isFocusable = false
        inputTime.isFocusableInTouchMode = false
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // FUNGSI TIME RANGE PICKER 24 JAM
    private fun showTimeRangePicker() {
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