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

class AddTaskActivity : AppCompatActivity() {

    private lateinit var inputPriority: EditText
    private lateinit var inputActivity: EditText
    private lateinit var inputTime: EditText
    private lateinit var inputLocation: EditText

    private var currentSelectedPriority: String = "None"
    private val priorities = arrayOf("None", "Low", "Medium", "High")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addtask)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Temukan semua input fields
        inputActivity = findViewById(R.id.inputActivity)
        inputTime = findViewById(R.id.inputTime)
        inputLocation = findViewById(R.id.inputLocation)
        inputPriority = findViewById(R.id.inputPriority)

        // SET KONDISI AWAL
        inputPriority.setText(currentSelectedPriority)

        // 1. Tombol kembali (Back)
        btnBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        // 2. Tombol Simpan (Save)
        btnSave.setOnClickListener {
            val title = inputActivity.text.toString().trim()
            val time = inputTime.text.toString().trim()
            val category = inputLocation.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Nama Aktivitas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Panggil dialog konfirmasi setelah validasi
            showConfirmationDialog(title, time, category)
        }

        // 3. Dropdown Priority - Panggil ListPopupWindow
        inputPriority.setOnClickListener {
            showPriorityDialog()
        }
    }

    override fun finish() {
        super.finish()
        // Transisi saat Activity ini ditutup
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Menampilkan dialog konfirmasi kustom setelah menekan Save.
     */
    private fun showConfirmationDialog(title: String, time: String, category: String) {
        // Inflate custom layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_success, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // ID diperbaiki di XML sebelumnya: btnIgnore -> Add more, btnView -> View
        val btnAddMore = dialogView.findViewById<TextView>(R.id.btnIgnore)
        val btnView = dialogView.findViewById<TextView>(R.id.btnView)

        // Helper untuk menyiapkan Intent hasil
        val createResultIntent = {
            Intent().apply {
                putExtra("EXTRA_TASK_TITLE", title)
                putExtra("EXTRA_TASK_TIME", if (time.isEmpty()) "Waktu tidak disetel" else time)
                putExtra("EXTRA_TASK_CATEGORY", if (category.isEmpty()) "Uncategorized" else category)
            }
        }

        // LOGIKA BARU: "Add more" (Simpan task, lalu reset form pada Activity ini)
        btnAddMore.setOnClickListener {
            // 1. Kirim data dan set RESULT_OK (agar TaskActivity menambahkan task)
            setResult(Activity.RESULT_OK, createResultIntent())

            // 2. Reset semua input field pada form ini
            inputActivity.setText("")
            inputTime.setText("")
            inputLocation.setText("")
            currentSelectedPriority = "None"
            inputPriority.setText(currentSelectedPriority)

            // 3. Tutup dialog. TETAP di Activity ini (tidak panggil finish()).
            dialog.dismiss()
        }

        // LOGIKA BARU: "View" (Simpan task, lalu kembali ke TaskActivity)
        btnView.setOnClickListener {
            // 1. Kirim data dan set RESULT_OK (agar TaskActivity menambahkan task)
            setResult(Activity.RESULT_OK, createResultIntent())

            // 2. Tutup dialog dan Activity saat ini (kembali ke TaskActivity)
            dialog.dismiss()
            finish() // Kembali ke TaskActivity
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