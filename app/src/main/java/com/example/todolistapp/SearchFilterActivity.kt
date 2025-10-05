package com.example.todolistapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle // DITAMBAHKAN: Memperbaiki Unresolved reference 'Bundle'
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

class SearchFilterActivity : AppCompatActivity() { // DIPERBAIKI

    private lateinit var inputSearch: EditText
    private lateinit var taskResultsContainer: LinearLayout
    private lateinit var chipContainer: LinearLayout
    private lateinit var btnBack: ImageView

    // Status filter bulan yang sedang aktif (-1 = All, 0=Jan, 11=Dec)
    private var activeMonthFilter: Int = -1
    private val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

    // Map untuk memetakan Priority ke Resource Color ID
    private val priorityColorMap = mapOf(
        "Low" to R.color.low_priority,
        "Medium" to R.color.medium_priority,
        "High" to R.color.high_priority
    )

    // Launcher untuk EditTaskActivity. Memastikan refresh saat kembali.
    private val editTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Setelah Edit/Update berhasil, refresh tampilan pencarian
            performSearch(inputSearch.text.toString(), activeMonthFilter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) { // DIPERBAIKI
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_filter)

        // --- INIALISASI VIEWS DARI XML ---
        inputSearch = findViewById(R.id.input_search)
        chipContainer = findViewById(R.id.chip_container)
        btnBack = findViewById(R.id.btn_back)
        taskResultsContainer = findViewById(R.id.task_results_container_xml)

        // Mengatur ulang padding agar list dimulai dengan benar di dalam ScrollView yang sudah di-constrain di XML.
        taskResultsContainer.setPadding(16.dp, 16.dp, 16.dp, 16.dp)


        // Listener untuk tombol Kembali (Arrow)
        btnBack.setOnClickListener {
            finish()
        }

        // 2. Generate Chips Bulan
        generateMonthChips()

        // 3. Listener Pencarian Teks (Memicu pencarian saat mengetik)
        inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                performSearch(s.toString(), activeMonthFilter)
            }
            override fun afterTextChanged(s: Editable) {}
        })

        // 4. Listener Tombol Batal/X (Memicu penghapusan teks dan pencarian ulang)
        inputSearch.setOnTouchListener(View.OnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                // Compound Drawable index 2 adalah drawableEnd (ikon Clear/X)
                val drawableEnd = inputSearch.compoundDrawables[2]
                if (drawableEnd != null) {
                    // Hitung area klik pada ikon Clear
                    val isClicked = event.rawX >= (inputSearch.right - drawableEnd.bounds.width() - inputSearch.paddingEnd)
                    if (isClicked) {
                        inputSearch.setText("")
                        // Memicu pencarian ulang setelah teks dihapus
                        performSearch("", activeMonthFilter)
                        return@OnTouchListener true
                    }
                }
            }
            false
        })

        // Muat semua tugas saat pertama kali dibuka (filter bulan = All)
        performSearch("", activeMonthFilter)
    }

    // --- LOGIKA CHIPS ---

    private fun generateMonthChips() {
        if (chipContainer.childCount > 1) {
            chipContainer.removeViews(1, chipContainer.childCount - 1)
        }

        val chipAll = findViewById<TextView>(R.id.chip_all)
        chipAll.setOnClickListener {
            setActiveChip(-1, it)
            performSearch(inputSearch.text.toString(), -1)
        }
        setActiveChip(-1, chipAll)

        for (monthIndex in 0 until 12) {
            val chip = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, 32.dp
                ).apply {
                    setMargins(0, 0, 8.dp, 0)
                }
                text = " ${monthNames[monthIndex]} "
                setPaddingRelative(12.dp, 0, 12.dp, 0)
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(Color.BLACK)
                background = ResourcesCompat.getDrawable(resources, R.drawable.chip_unselected, null)
                tag = monthIndex

                setOnClickListener {
                    setActiveChip(monthIndex, it)
                    performSearch(inputSearch.text.toString(), monthIndex)
                }
            }
            chipContainer.addView(chip)
        }
    }

    private fun setActiveChip(monthIndex: Int, selectedView: View) {
        activeMonthFilter = monthIndex

        for (i in 0 until chipContainer.childCount) {
            val chip = chipContainer.getChildAt(i) as TextView
            val isSelected = chip == selectedView

            if (isSelected) {
                chip.setBackgroundResource(R.drawable.chip_selected)
                chip.setTextColor(Color.WHITE)
                if (chip.id == R.id.chip_all) {
                    chip.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
            } else {
                chip.setBackgroundResource(R.drawable.chip_unselected)
                chip.setTextColor(Color.BLACK)
            }
        }
    }

    // --- LOGIKA PENCARIAN & TAMPILAN ---

    private fun performSearch(query: String, monthFilter: Int) {
        val filteredTasks = TaskRepository.searchTasks(query, monthFilter)
        taskResultsContainer.removeAllViews()

        if (filteredTasks.isEmpty()) {
            val filterName = if (monthFilter == -1) "Semua Bulan" else monthNames[monthFilter]

            val noResults = TextView(this).apply {
                text = "Tidak ada hasil ditemukan untuk filter:\n'${query.ifEmpty { "Semua Tugas" }}' di bulan $filterName"
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, 50.dp, 0, 50.dp)
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
            }
            taskResultsContainer.addView(noResults)
        } else {
            for (task in filteredTasks) {
                addNewTaskToUI(taskResultsContainer, task)
            }
        }
    }

    private fun addNewTaskToUI(container: LinearLayout, task: Task) {
        val context = this
        val marginPx = 8.dp

        // --- Container Utama Vertikal ---
        val mainContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, marginPx)
            }
            orientation = LinearLayout.VERTICAL
        }

        // --- 1. Item Tugas (Header) ---
        val taskItem = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 64.dp
            )
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_task, null)
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            elevation = 4f
            setPadding(12.dp, 12.dp, 12.dp, 12.dp)
        }

        // 1.1 Checklist/Status
        val checklistBox = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(24.dp, 24.dp).apply {
                marginEnd = 16.dp
                marginStart = 4.dp // Padding kiri untuk visual
            }
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_checklist, null)

            // Overriden by the Complete button in the expanded view for consistency
        }
        taskItem.addView(checklistBox)


        // 1.2 Container Judul & Ikon Prioritas
        val titleAndPriorityContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Title
        val taskTitle = TextView(context).apply {
            text = task.title
            textSize = 16f
            setTextColor(Color.parseColor("#14142A"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        titleAndPriorityContainer.addView(taskTitle)

        // Ikon Prioritas
        if (task.priority != "None") {
            val colorResId = priorityColorMap[task.priority] ?: R.color.dark_blue
            val colorInt = ContextCompat.getColor(context, colorResId)

            val exclamationIcon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(16.dp, 16.dp).apply {
                    marginStart = 8.dp
                    gravity = Gravity.CENTER_VERTICAL
                }
                setImageResource(R.drawable.ic_missed) // Menggunakan ic_missed sebagai placeholder ikon
                contentDescription = "${task.priority} Priority"
                setColorFilter(colorInt)
            }
            titleAndPriorityContainer.addView(exclamationIcon)
        }
        taskItem.addView(titleAndPriorityContainer)

        // 1.3 Month Tag and Arrow
        val taskMonth = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val monthIndex = task.monthAdded.coerceIn(0, 11)
            text = monthNames[monthIndex]
            textSize = 14f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setPadding(0, 0, 12.dp, 0)
            gravity = Gravity.CENTER_VERTICAL
        }
        taskItem.addView(taskMonth)

        // Arrow
        val arrowRight = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = 8.dp
            }
            setImageResource(R.drawable.baseline_arrow_forward_ios_24)
            setColorFilter(Color.parseColor("#283F6D"))
            rotation = 0f
        }
        taskItem.addView(arrowRight)

        mainContainer.addView(taskItem)


        // --- 2. Tombol Aksi (Expanded Area) ---
        val actionButtonsContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.GONE // Hidden by default
        }

        // Helper function for creating action buttons
        fun createActionButton(iconResId: Int, buttonText: String, onClick: () -> Unit): LinearLayout {
            return LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
                ).apply {
                    setMargins(4.dp, 0, 4.dp, 0)
                }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener { onClick() }

                background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_task, null)
                setPadding(8.dp, 12.dp, 8.dp, 12.dp)

                addView(ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(32.dp, 32.dp)
                    setImageResource(iconResId)
                    setColorFilter(Color.parseColor("#283F6D"))
                })

                addView(TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    text = buttonText
                    textSize = 10f
                    setTextColor(Color.parseColor("#283F6D"))
                    typeface = ResourcesCompat.getFont(context, R.font.lexend)
                    gravity = Gravity.CENTER_HORIZONTAL
                })
            }
        }

        // **ACTION BUTTONS**

        // Complete Button
        val completeButton = createActionButton(R.drawable.ic_completed, "Complete") {
            val success = TaskRepository.completeTask(task.id)
            if (success) {
                showConfirmationDialog(task.title, "selesai")
                // Setelah complete, refresh list
                performSearch(inputSearch.text.toString(), activeMonthFilter)
            } else {
                Toast.makeText(context, "Gagal menyelesaikan tugas.", Toast.LENGTH_SHORT).show()
            }
        }

        // Flow Timer Button
        val flowTimerButton = createActionButton(R.drawable.ic_alarm, "Flow Timer") {
            val intent = Intent(context, FlowTimerActivity::class.java).apply {
                putExtra(FlowTimerActivity.EXTRA_TASK_NAME, task.title)
            }
            startActivity(intent)
        }

        // Edit Button
        val editButton = createActionButton(R.drawable.ic_edit, "Edit") {
            val intent = Intent(context, EditTaskActivity::class.java).apply {
                putExtra("EXTRA_TASK_ID", task.id)
            }
            editTaskLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Delete Button
        val deleteButton = createActionButton(R.drawable.ic_trash, "Delete") {
            val success = TaskRepository.deleteTask(task.id)
            if (success) {
                showConfirmationDialog(task.title, "dihapus")
                // Setelah delete, refresh list
                performSearch(inputSearch.text.toString(), activeMonthFilter)
            } else {
                Toast.makeText(context, "Gagal menghapus tugas.", Toast.LENGTH_SHORT).show()
            }
        }

        actionButtonsContainer.addView(completeButton)
        actionButtonsContainer.addView(flowTimerButton)
        actionButtonsContainer.addView(editButton)
        actionButtonsContainer.addView(deleteButton)

        mainContainer.addView(actionButtonsContainer)

        // --- 3. Logika Klik untuk Expand/Collapse ---
        taskItem.setOnClickListener {
            if (actionButtonsContainer.visibility == View.GONE) {
                actionButtonsContainer.visibility = View.VISIBLE
                arrowRight.rotation = 90f
            } else {
                actionButtonsContainer.visibility = View.GONE
                arrowRight.rotation = 0f
            }
        }

        container.addView(mainContainer)
    }

    // NEW: Function to show confirmation dialog (Copied from TaskActivity.kt)
    private fun showConfirmationDialog(taskTitle: String, action: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_success, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val mainMessageTextView = (dialogView as ViewGroup).getChildAt(0) as? TextView

        val btnConfirm1 = dialogView.findViewById<TextView>(R.id.btnIgnore)
        val btnConfirm2 = dialogView.findViewById<TextView>(R.id.btnView)

        val message = if (action == "selesai") {
            "Selamat! Tugas '$taskTitle' berhasil diselesaikan."
        } else {
            "Tugas '$taskTitle' berhasil dihapus."
        }

        mainMessageTextView?.text = message
        mainMessageTextView?.setTextColor(Color.parseColor("#283F6D"))

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val dismissListener = View.OnClickListener {
            dialog.dismiss()
        }

        btnConfirm1.text = "OK"
        btnConfirm2.text = "Tutup"

        btnConfirm1.setTextColor(Color.parseColor("#283F6D"))
        btnConfirm2.setTextColor(Color.parseColor("#283F6D"))

        btnConfirm1.setOnClickListener(dismissListener)
        btnConfirm2.setOnClickListener(dismissListener)

        dialog.show()
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // Definisi Int.dp
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}