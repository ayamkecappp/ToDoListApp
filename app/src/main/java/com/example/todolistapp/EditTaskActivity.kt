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
import android.app.DatePickerDialog

class EditTaskActivity : AppCompatActivity() {

    private lateinit var inputPriority: EditText
    private lateinit var inputActivity: EditText
    private lateinit var inputTime: EditText
    private lateinit var inputLocation: EditText
    private lateinit var inputDate: EditText
    private lateinit var tvTitle: TextView // FIX: Tambahkan inisialisasi Title TextView

    private var taskIdToEdit: Long = -1L
    private var currentTaskDayStartMillis: Long = 0L
    private var currentTaskEndTimeMillis: Long = 0L

    private val uiDateDisplayFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    private var currentSelectedPriority: String = "None"
    private val priorities = arrayOf("None", "Low", "Medium", "High")

    companion object {
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_RESCHEDULE_MODE = "EXTRA_RESCHEDULE_MODE" // New constant
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_task)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)

        tvTitle = findViewById(R.id.tvNewReminderTitle) // Inisialisasi Title TextView
        inputActivity = findViewById(R.id.inputActivity)
        inputTime = findViewById(R.id.inputTime)
        inputLocation = findViewById(R.id.inputLocation)
        inputPriority = findViewById(R.id.inputPriority)
        inputDate = findViewById(R.id.inputDate)

        // 1. Cek Mode Reschedule dan ubah judul
        val isRescheduleMode = intent.getBooleanExtra(EXTRA_RESCHEDULE_MODE, false)
        if (isRescheduleMode) {
            tvTitle.text = "Reschedule Task"
        }

        // 2. Ambil Task ID dari Intent
        taskIdToEdit = intent.getLongExtra(EXTRA_TASK_ID, -1L)

        // 3. Muat Data Tugas yang Ada
        val existingTask = TaskRepository.getTaskById(taskIdToEdit)

        if (existingTask != null) {
            inputActivity.setText(existingTask.title)
            inputTime.setText(existingTask.time)
            inputLocation.setText(existingTask.category)
            inputPriority.setText(existingTask.priority)
            currentSelectedPriority = existingTask.priority

            currentTaskEndTimeMillis = existingTask.endTimeMillis
            inputTime.tag = existingTask.endTimeMillis

            val taskDayCalendar = Calendar.getInstance().apply { timeInMillis = existingTask.id }
            taskDayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            taskDayCalendar.set(Calendar.MINUTE, 0)
            taskDayCalendar.set(Calendar.SECOND, 0)
            taskDayCalendar.set(Calendar.MILLISECOND, 0)

            currentTaskDayStartMillis = taskDayCalendar.timeInMillis
            inputDate.setText(uiDateDisplayFormat.format(Date(currentTaskDayStartMillis)))

            Toast.makeText(this, "Tugas '${existingTask.title}' siap diedit.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error: Tugas tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnSave.text = "Update"

        btnBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        btnSave.setOnClickListener {
            val title = inputActivity.text.toString().trim()
            val newTimeDisplay = inputTime.text.toString().trim()
            val newLocation = inputLocation.text.toString().trim()
            val newPriority = currentSelectedPriority

            if (title.isEmpty()) {
                Toast.makeText(this, "Nama Aktivitas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalEndTimeMillis = inputTime.tag as? Long ?: currentTaskEndTimeMillis
            val newTaskId = currentTaskDayStartMillis

            val updatedTask = Task(
                id = newTaskId,
                title = title,
                time = if (newTimeDisplay.isEmpty()) "Waktu tidak disetel" else newTimeDisplay,
                category = if (newLocation.isEmpty()) "Uncategorized" else newLocation,
                priority = newPriority,
                endTimeMillis = finalEndTimeMillis,
                monthAdded = Calendar.getInstance().apply { timeInMillis = newTaskId }.get(Calendar.MONTH)
            )

            // PENTING: Panggil updateTask dengan taskIdToEdit (ID asli)
            val success = TaskRepository.updateTask(taskIdToEdit, updatedTask)

            if (success) {
                taskIdToEdit = newTaskId
                showConfirmationDialog(updatedTask)
            } else {
                Toast.makeText(this, "Gagal menyimpan pembaruan tugas.", Toast.LENGTH_SHORT).show()
            }
        }

        inputPriority.setOnClickListener {
            showPriorityDialog()
        }

        inputDate.setOnClickListener {
            showDatePicker()
        }
        inputDate.isFocusable = false
        inputDate.isFocusableInTouchMode = false


        inputTime.setOnClickListener {
            showTimeRangePicker()
        }
        inputTime.isFocusable = false
        inputTime.isFocusableInTouchMode = false
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTaskDayStartMillis }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
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

                if (currentTaskEndTimeMillis > 0L) {
                    val oldEndCalendar = Calendar.getInstance().apply { timeInMillis = currentTaskEndTimeMillis }

                    newCalendar.set(Calendar.HOUR_OF_DAY, oldEndCalendar.get(Calendar.HOUR_OF_DAY))
                    newCalendar.set(Calendar.MINUTE, oldEndCalendar.get(Calendar.MINUTE))
                    newCalendar.set(Calendar.SECOND, oldEndCalendar.get(Calendar.SECOND))
                    newCalendar.set(Calendar.MILLISECOND, oldEndCalendar.get(Calendar.MILLISECOND))

                    currentTaskEndTimeMillis = newCalendar.timeInMillis
                    inputTime.tag = currentTaskEndTimeMillis
                }

                currentTaskDayStartMillis = newDayStartMillis
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun showTimeRangePicker() {
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
                        endCalendarCheck.set(Calendar.SECOND, 0)
                        endCalendarCheck.set(Calendar.MILLISECOND, 0)

                        val startCalendarCheck = selectedDayCalendar.clone() as Calendar
                        startCalendarCheck.set(Calendar.HOUR_OF_DAY, startHourInt)
                        startCalendarCheck.set(Calendar.MINUTE, startMinuteInt)
                        startCalendarCheck.set(Calendar.SECOND, 0)
                        startCalendarCheck.set(Calendar.MILLISECOND, 0)

                        if (endCalendarCheck.timeInMillis <= startCalendarCheck.timeInMillis) {
                            selectedDayCalendar.add(Calendar.DAY_OF_MONTH, 1)
                        }

                        selectedDayCalendar.set(Calendar.HOUR_OF_DAY, endHourOfDay)
                        selectedDayCalendar.set(Calendar.MINUTE, endMinute)
                        selectedDayCalendar.set(Calendar.SECOND, 0)
                        selectedDayCalendar.set(Calendar.MILLISECOND, 0)

                        currentTaskEndTimeMillis = selectedDayCalendar.timeInMillis
                        inputTime.tag = currentTaskEndTimeMillis
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

        tvMessageTitle.text = "Success Update Reminder"
        tvMessageTitle.setTextColor(Color.parseColor("#283F6D"))

        val createResultIntent = {
            Intent().apply {
                putExtra("SHOULD_ADD_TASK", true)
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