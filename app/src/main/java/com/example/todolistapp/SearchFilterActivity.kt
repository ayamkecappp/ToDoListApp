package com.example.todolistapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SearchFilterActivity : AppCompatActivity() {

    private lateinit var inputSearch: EditText
    // Menggunakan LinearLayout sesuai XML Anda, bukan RecyclerView
    private lateinit var taskResultsContainer: LinearLayout
    private lateinit var chipContainer: LinearLayout
    private lateinit var btnBack: ImageView

    private var activeMonthFilter: Int = -1 // -1 berarti "All"
    private val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_filter)

        // Inisialisasi Views
        inputSearch = findViewById(R.id.input_search)
        taskResultsContainer = findViewById(R.id.task_results_container_xml) // ID dari LinearLayout di XML Anda
        chipContainer = findViewById(R.id.chip_container)
        btnBack = findViewById(R.id.btn_back)

        btnBack.setOnClickListener {
            finish()
        }

        generateMonthChips()

        inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s.toString(), activeMonthFilter)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Tampilkan semua tugas saat pertama kali dibuka
        performSearch("", -1)
    }

    private fun generateMonthChips() {
        // ... (Fungsi ini tidak perlu diubah) ...
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
                ).apply { setMargins(0, 0, 8.dp, 0) }
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
        // ... (Fungsi ini tidak perlu diubah) ...
        activeMonthFilter = monthIndex
        for (i in 0 until chipContainer.childCount) {
            val chip = chipContainer.getChildAt(i) as TextView
            if (chip == selectedView) {
                chip.setBackgroundResource(R.drawable.chip_selected)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.chip_unselected)
                chip.setTextColor(Color.BLACK)
            }
        }
    }

    // --- FUNGSI PENCARIAN YANG DIPERBARUI UNTUK BEKERJA DENGAN LinearLayout ---
    private fun performSearch(query: String, monthFilter: Int) {
        lifecycleScope.launch {
            try {
                // Mengambil semua tugas dari semua status
                val allTasks = mutableListOf<Task>().apply {
                    addAll(TaskRepository.getTasksByStatus("pending"))
                    addAll(TaskRepository.getTasksByStatus("completed"))
                    addAll(TaskRepository.getTasksByStatus("missed"))
                }

                // Filter manual di aplikasi
                val filteredTasks = allTasks.filter { task ->
                    val matchesQuery = task.title.contains(query, ignoreCase = true)
                    val taskCalendar = Calendar.getInstance().apply { time = task.dueDate.toDate() }
                    val matchesMonth = if (monthFilter == -1) true else taskCalendar.get(Calendar.MONTH) == monthFilter
                    matchesQuery && matchesMonth
                }

                // Hapus semua hasil pencarian sebelumnya
                taskResultsContainer.removeAllViews()

                if (filteredTasks.isEmpty()) {
                    // Tampilkan pesan "tidak ada hasil" jika perlu
                    val noResultsView = TextView(this@SearchFilterActivity).apply {
                        text = "No tasks found"
                        gravity = Gravity.CENTER
                        setPadding(0, 50, 0, 0)
                    }
                    taskResultsContainer.addView(noResultsView)
                } else {
                    // Loop dan tambahkan setiap hasil ke LinearLayout
                    filteredTasks.forEach { task ->
                        val taskView = LayoutInflater.from(this@SearchFilterActivity)
                            .inflate(R.layout.item_task, taskResultsContainer, false)

                        // Bind data ke taskView (mirip seperti di adapter)
                        bindTaskData(taskView, task)

                        // Tambahkan view ke kontainer
                        taskResultsContainer.addView(taskView)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@SearchFilterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- FUNGSI BARU UNTUK BIND DATA DAN SETUP LISTENER SECARA MANUAL ---
    private fun bindTaskData(view: View, task: Task) {
        val title = view.findViewById<TextView>(R.id.tvTaskTitle)
        val time = view.findViewById<TextView>(R.id.tvTaskTime)
        val category = view.findViewById<TextView>(R.id.tvTaskCategory)
        val priorityIcon = view.findViewById<ImageView>(R.id.ivPriority)
        val actionButtonsContainer = view.findViewById<LinearLayout>(R.id.actionButtonsContainer)
        val arrowToggle = view.findViewById<ImageView>(R.id.ivArrowToggle)

        title.text = task.title
        category.text = task.category
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        time.text = sdf.format(task.dueDate.toDate())

        // Atur visibilitas prioritas
        val priorityColorMap = mapOf("Low" to R.color.low_priority, "Medium" to R.color.medium_priority, "High" to R.color.high_priority)
        if (task.priority != "None" && task.priority.isNotEmpty()) {
            priorityIcon.visibility = View.VISIBLE
            val colorResId = priorityColorMap[task.priority] ?: R.color.dark_blue
            priorityIcon.setColorFilter(ContextCompat.getColor(this, colorResId))
        } else {
            priorityIcon.visibility = View.GONE
        }

        // Setup listener untuk expand/collapse
        view.findViewById<View>(R.id.taskItemContainer).setOnClickListener {
            val isVisible = actionButtonsContainer.visibility == View.VISIBLE
            actionButtonsContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
            arrowToggle.rotation = if (isVisible) 0f else 180f
        }

        // Setup listener untuk tombol edit dan delete
        view.findViewById<View>(R.id.btnEdit).setOnClickListener {
            val intent = Intent(this, EditTaskActivity::class.java).apply {
                putExtra("TASK_ID", task.id)
            }
            startActivity(intent)
        }

        view.findViewById<View>(R.id.btnDelete).setOnClickListener {
            AlertDialog.Builder(this).setTitle("Hapus Tugas").setMessage("Hapus '${task.title}'?")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        TaskRepository.updateTaskStatus(task.id, "deleted")
                        Toast.makeText(this@SearchFilterActivity, "Tugas dihapus", Toast.LENGTH_SHORT).show()
                        performSearch(inputSearch.text.toString(), activeMonthFilter) // Refresh hasil
                    }
                }
                .setNegativeButton("Batal", null).show()
        }
    }

    // Helper untuk konversi dp ke px
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}