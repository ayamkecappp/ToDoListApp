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
import android.graphics.drawable.ColorDrawable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTaskActivity : AppCompatActivity() {

    private lateinit var inputPriority: EditText
    private lateinit var inputActivity: EditText
    private lateinit var inputTime: EditText
    private lateinit var inputLocation: EditText

    // VARIABEL BARU: Untuk menyimpan timestamp tanggal yang akan digunakan
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

            // Tampilkan tanggal yang dipilih kepada pengguna
            Toast.makeText(this, "Aktivitas akan ditambahkan pada: ${uiDateFormat.format(selectedDate)}", Toast.LENGTH_LONG).show()
        } else {
            // Jika tidak ada tanggal yang dikirim, gunakan hari ini
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

            if (title.isEmpty()) {
                Toast.makeText(this, "Nama Aktivitas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // PERUBAHAN UTAMA: MENGGUNAKAN taskDateMillis SEBAGAI ID UNTUK MENYIMPAN TANGGAL
            val newTask = Task(
                id = taskDateMillis, // Menggunakan timestamp dari tanggal yang dipilih/default
                title = title,
                time = if (time.isEmpty()) "Waktu tidak disetel" else time,
                category = if (location.isEmpty()) "Uncategorized" else location,
                priority = priority
            )
            TaskRepository.addTask(newTask)

            showConfirmationDialog(newTask)
        }

        inputPriority.setOnClickListener {
            showPriorityDialog()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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

            inputActivity.setText("")
            inputTime.setText("")
            inputLocation.setText("")
            currentSelectedPriority = "None"
            inputPriority.setText(currentSelectedPriority)
            taskDateMillis = System.currentTimeMillis() // Reset ke hari ini untuk tugas berikutnya

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
            verticalOffset = 0
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