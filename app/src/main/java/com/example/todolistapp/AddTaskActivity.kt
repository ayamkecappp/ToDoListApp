package com.example.todolistapp

import android.content.Context
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
// Import yang diperlukan untuk Resource
import androidx.core.content.res.ResourcesCompat

class AddTaskActivity : AppCompatActivity() {

    private lateinit var inputPriority: EditText
    private var currentSelectedPriority: String = "None"
    private val priorities = arrayOf("None", "Low", "Medium", "High")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addtask)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)
        inputPriority = findViewById(R.id.inputPriority)

        // SET KONDISI AWAL
        inputPriority.setText(currentSelectedPriority)

        // 1. Tombol kembali (Back)
        btnBack.setOnClickListener {
            finish()
        }

        // 2. Tombol Simpan (Save)
        btnSave.setOnClickListener {
            val priority = inputPriority.text.toString()
            Toast.makeText(this, "Reminder disimpan dengan Prioritas: $priority", Toast.LENGTH_SHORT).show()
        }

        // 3. Dropdown Priority - Panggil ListPopupWindow
        inputPriority.setOnClickListener {
            showPriorityDialog()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Menampilkan ListPopupWindow dengan custom layout untuk Priority.
     */
    private fun showPriorityDialog() {
        // Mendapatkan drawable rounded corner yang baru
        val roundedBackground = ResourcesCompat.getDrawable(resources, R.drawable.bg_popup_rounded_12dp, theme)

        val listPopupWindow = ListPopupWindow(this).apply {
            anchorView = inputPriority

            // PERBAIKAN: Mengatur background ListPopupWindow menggunakan drawable rounded
            setBackgroundDrawable(roundedBackground)

            val adapter = PriorityAdapter(this@AddTaskActivity, priorities)
            setAdapter(adapter)

            // Tentukan lebar popup
            width = inputPriority.width
            isModal = true
            verticalOffset = 0
        }

        // Atur listener saat item dipilih
        listPopupWindow.setOnItemClickListener { parent, view, position, id ->
            val selectedPriority = priorities[position]

            currentSelectedPriority = selectedPriority
            inputPriority.setText(selectedPriority)

            Toast.makeText(this, "Prioritas diatur ke: $selectedPriority", Toast.LENGTH_SHORT).show()
            listPopupWindow.dismiss()
        }

        listPopupWindow.show()
    }

    /**
     * Adapter Kustom untuk ListPopupWindow
     */
    private inner class PriorityAdapter(context: Context, items: Array<String>) :
        ArrayAdapter<String>(context, 0, items) {

        private val inflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Menggunakan R.layout.list_item_priority
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