package com.example.todolistapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.HorizontalScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import androidx.core.content.ContextCompat

class TaskActivity : AppCompatActivity() {

    private val TAG = "TaskActivity"
    private lateinit var tasksContainer: LinearLayout
    private lateinit var tvNoActivity: TextView
    private lateinit var octoberText: TextView

    private lateinit var bottomNav: BottomNavigationView

    private lateinit var calendarMain: HorizontalScrollView
    private lateinit var dateItemsContainer: LinearLayout

    private lateinit var currentCalendar: Calendar
    private var selectedDate: Calendar = Calendar.getInstance()

    private val COLOR_ACTIVE_SELECTION = Color.parseColor("#283F6D")
    private val COLOR_DEFAULT_TEXT = Color.BLACK
    private val COLOR_SELECTED_TEXT = Color.WHITE
    private val CORNER_RADIUS_DP = 8
    private val ITEM_WIDTH_DP = 60

    private val EXTRA_SELECTED_DATE_MILLIS = "EXTRA_SELECTED_DATE_MILLIS"

    private val addTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadTasksForSelectedDate()
        }
    }

    // Map untuk memetakan Priority ke Resource Color ID
    private val priorityColorMap = mapOf(
        "Low" to R.color.low_priority,
        "Medium" to R.color.medium_priority,
        "High" to R.color.high_priority
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task)

        // --- Inisialisasi Views ---
        tasksContainer = findViewById(R.id.tasksContainer)
        tvNoActivity = findViewById(R.id.tvNoActivity)
        octoberText = findViewById(R.id.octoberText)
        calendarMain = findViewById(R.id.calendar_main)
        dateItemsContainer = findViewById(R.id.date_items_container)

        bottomNav = findViewById(R.id.bottomNav) as BottomNavigationView
        //Menghilangkan tint default
        bottomNav.itemIconTintList = null

        // --- Setup Kalender & Navigasi ---
        currentCalendar = selectedDate.clone() as Calendar
        currentCalendar.set(Calendar.DAY_OF_MONTH, 1)

        updateCalendar()
        calendarMain.post { scrollToToday() }
        setDynamicMonthYear()
        handleIncomingTaskIntent(intent)

        // Listener Tombol
        findViewById<ImageView?>(R.id.btn_search)?.setOnClickListener {
            startActivity(Intent(this, SearchFilterActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        findViewById<View>(R.id.reminderContainer)?.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java).apply {
                putExtra(EXTRA_SELECTED_DATE_MILLIS, selectedDate.timeInMillis)
            }
            addTaskLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        findViewById<ImageView?>(R.id.btn_calendar)?.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Bottom Navigation (Mempertahankan logika navigasi)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    true
                }
                R.id.nav_tasks -> true // Tidak melakukan apa-apa karena sudah di halaman ini.
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    true
                }
                else -> false
            }
        }



        // PENTING: Proses missed tasks di awal
        TaskRepository.processTasksForMissed()
        loadTasksForSelectedDate()
    }
    override fun onStart() {
        super.onStart()
        // PENTING: Set status item Task saat ini.
        // Memastikan ikon "Task" di bottom nav selalu aktif saat berada di halaman ini.
        bottomNav.selectedItemId = R.id.nav_tasks
    }

    override fun onResume() {
        super.onResume()
        // PENTING: Proses missed tasks setiap kali activity di-resume
        TaskRepository.processTasksForMissed()
        loadTasksForSelectedDate()
    }

    private fun loadTasksForSelectedDate() {
        tasksContainer.removeAllViews()
        // Memanggil processTasksForMissed() lagi untuk memastikan real-time check
        TaskRepository.processTasksForMissed()

        val tasks = TaskRepository.getTasksByDate(selectedDate)
        for (task in tasks) {
            addNewTaskToUI(task)
        }
        updateEmptyState(tasks.size)
    }

    private fun scrollToToday() {
        val today = Calendar.getInstance()
        if (today.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
            today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {

            val todayDayOfMonth = today.get(Calendar.DAY_OF_MONTH)
            val dayIndex = todayDayOfMonth - 1
            val itemWidthPx = ITEM_WIDTH_DP.dp
            val centerOffset = (calendarMain.width / 2) - (itemWidthPx / 2)
            val scrollPosition = (dayIndex * itemWidthPx) - centerOffset
            calendarMain.smoothScrollTo(scrollPosition.coerceAtLeast(0), 0)
        }
    }

    private fun setDynamicMonthYear() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))
        octoberText.text = sdf.format(currentCalendar.time)
    }

    private fun createRoundedBackground(color: Int): GradientDrawable {
        val cornerRadiusPx = CORNER_RADIUS_DP.dp.toFloat()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = cornerRadiusPx
        }
    }

    private fun updateCalendar() {
        dateItemsContainer.removeAllViews()

        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, i)

            val dayOfWeek = SimpleDateFormat("EEE", Locale("en", "US")).format(cal.time)
            val day = i

            val isSelected = (cal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                    cal.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH))

            val dayItemContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ITEM_WIDTH_DP.dp,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(0, 0, 8.dp, 0)
                }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = if (isSelected) createRoundedBackground(COLOR_ACTIVE_SELECTION) else createRoundedBackground(Color.WHITE)
                elevation = 2f
                setPadding(4.dp, 4.dp, 4.dp, 4.dp)
            }

            val dayOfWeekText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                text = dayOfWeek
                textSize = 12f
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
                setTextColor(if (isSelected) COLOR_SELECTED_TEXT else COLOR_DEFAULT_TEXT)
            }
            dayItemContainer.addView(dayOfWeekText)

            val dayNumberText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                text = day.toString()
                textSize = 18f
                val font = ResourcesCompat.getFont(context, R.font.lexend)
                typeface = android.graphics.Typeface.create(font, android.graphics.Typeface.BOLD)
                setTextColor(if (isSelected) COLOR_SELECTED_TEXT else COLOR_DEFAULT_TEXT)
            }
            dayItemContainer.addView(dayNumberText)

            dayItemContainer.setOnClickListener {
                val newSelectedDate = currentCalendar.clone() as Calendar
                newSelectedDate.set(Calendar.DAY_OF_MONTH, i)
                selectedDate = newSelectedDate
                loadTasksForSelectedDate()
                updateCalendar()
            }

            dateItemsContainer.addView(dayItemContainer)
        }
    }

    private fun handleIncomingTaskIntent(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("SHOULD_ADD_TASK", false)) {
            loadTasksForSelectedDate()
            intent.removeExtra("SHOULD_ADD_TASK")
        }
    }

    private fun addNewTaskToUI(task: Task) { // Menerima objek Task
        val context = this
        val marginPx = 16.dp

        // --- Container Utama Vertikal (Menampung Item Tugas + Tombol Aksi) ---
        val mainContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(marginPx, 0, marginPx, marginPx)
            }
            orientation = LinearLayout.VERTICAL
        }

        // --- 1. Item Tugas (Bagian yang dapat diklik untuk expand) ---
        val taskItem = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 80.dp)
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_task, null)
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            elevation = 4f
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
        }

        // Checklist/Status
        val checklistBox = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(24.dp, 24.dp).apply {
                marginEnd = 16.dp
            }
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_checklist, null)

            // LOGIKA BARU: KLIK CHECKLIST UNTUK MENANDAI SELESAI
            setOnClickListener {
                val success = TaskRepository.completeTask(task.id)
                if (success) {
                    showConfirmationDialog(task.title, "selesai")
                    // Hapus item tugas dari tampilan secara langsung
                    (mainContainer.parent as? ViewGroup)?.removeView(mainContainer)
                    // Muat ulang daftar untuk memperbarui status kosong
                    loadTasksForSelectedDate()
                } else {
                    Toast.makeText(context, "Gagal menandai tugas selesai.", Toast.LENGTH_SHORT).show()
                }
            }
            // END LOGIKA BARU
        }
        taskItem.addView(checklistBox)

        // Container Judul & Ikon Prioritas
        val titleAndPriorityContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Title (Laundry)
        val taskTitle = TextView(context).apply {
            text = task.title // Menggunakan properti dari objek Task
            textSize = 16f
            setTextColor(Color.parseColor("#14142A"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        titleAndPriorityContainer.addView(taskTitle)

        // Ikon Status/Prioritas (tanda seru) - Hanya tampil jika Priority bukan "None"
        if (task.priority != "None") {
            val colorResId = priorityColorMap[task.priority] ?: R.color.dark_blue
            val colorInt = ContextCompat.getColor(context, colorResId)

            val exclamationIcon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(16.dp, 16.dp).apply {
                    marginStart = 8.dp
                    gravity = Gravity.CENTER_VERTICAL
                }
                setImageResource(R.drawable.ic_missed) // Menggunakan ikon yang tersedia
                contentDescription = "${task.priority} Priority"

                // --- LOGIKA BARU UNTUK WARNA ---
                // Menerapkan tint warna berdasarkan level prioritas
                setColorFilter(colorInt)
                // --- END LOGIKA BARU ---
            }
            titleAndPriorityContainer.addView(exclamationIcon)
        }

        // Container Detail (Waktu & Kategori)
        val detailWrapper = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                // Memberikan ruang agar detail mepet ke kanan
                marginStart = 16.dp
            }
            orientation = LinearLayout.VERTICAL
        }

        // Waktu (12.00 - 13.00) - Rata Kanan
        val taskTime = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            text = task.time // Menggunakan properti dari objek Task
            textSize = 12f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            gravity = Gravity.END
        }
        detailWrapper.addView(taskTime)

        // Kategori (Home) - Rata Kanan
        val taskCategory = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            text = task.category // Menggunakan properti dari objek Task
            textSize = 12f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            gravity = Gravity.END
        }
        detailWrapper.addView(taskCategory)


        // Masukkan Title Container ke taskItem (di sebelah Checklist)
        taskItem.addView(titleAndPriorityContainer)
        // Masukkan Detail Wrapper (Waktu/Kategori)
        taskItem.addView(detailWrapper)


        // Arrow Right/Down
        var arrowRight = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = 8.dp
            }
            setImageResource(R.drawable.baseline_arrow_forward_ios_24)
            setColorFilter(Color.parseColor("#283F6D"))
            rotation = 0f // Rotasi awal 0f (panah ke kanan: >)
        }
        taskItem.addView(arrowRight)

        // Tambahkan taskItem ke mainContainer
        mainContainer.addView(taskItem)


        // --- 2. Tombol Aksi (Awalnya tersembunyi) ---
        val actionButtonsContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.GONE // Sembunyikan secara default
        }

        // Fungsi untuk membuat tombol aksi
        fun createActionButton(iconResId: Int, buttonText: String, onClick: () -> Unit): LinearLayout {
            return LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                    setMargins(4.dp, 0, 4.dp, 0)
                }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener { onClick() }

                // Background tombol individual menggunakan warna yang sama dengan list task (bg_task)
                background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_task, null)
                setPadding(8.dp, 12.dp, 8.dp, 12.dp)

                // Ikon
                addView(ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(32.dp, 32.dp)
                    setImageResource(iconResId)
                    setColorFilter(Color.parseColor("#283F6D"))
                })
                // Teks
                addView(TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    text = buttonText
                    textSize = 10f
                    setTextColor(Color.parseColor("#283F6D"))
                    typeface = ResourcesCompat.getFont(context, R.font.lexend)
                    gravity = Gravity.CENTER_HORIZONTAL
                })
            }
        }

        // Flow Timer Button
        val flowTimerButton = createActionButton(
            R.drawable.ic_alarm, "Flow Timer") {
            // LOGIKA BARU: BUKA FlowTimerActivity
            val intent = Intent(context, FlowTimerActivity::class.java).apply {
                putExtra(FlowTimerActivity.EXTRA_TASK_NAME, task.title)
            }
            startActivity(intent)
        }

        val editButton = createActionButton(
            R.drawable.ic_edit, "Edit") {
            // FIX: Buka EditTaskActivity untuk mengedit tugas yang dipilih
            val intent = Intent(context, EditTaskActivity::class.java).apply {
                putExtra("EXTRA_TASK_ID", task.id)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // LOGIKA DELETE
        val deleteButton = createActionButton(
            R.drawable.ic_trash, "Delete") {
            val success = TaskRepository.deleteTask(task.id)
            if (success) {
                showConfirmationDialog(task.title, "dihapus")
                // Muat ulang daftar tugas setelah penghapusan
                loadTasksForSelectedDate()
            } else {
                Toast.makeText(context, "Gagal menghapus tugas.", Toast.LENGTH_SHORT).show()
            }
        }

        actionButtonsContainer.addView(flowTimerButton)
        actionButtonsContainer.addView(editButton)
        actionButtonsContainer.addView(deleteButton)

        // Tambahkan container tombol aksi ke mainContainer
        mainContainer.addView(actionButtonsContainer)


        // --- 3. Logika Klik untuk Expand/Collapse ---
        taskItem.setOnClickListener {
            // Toggle visibilitas container tombol aksi
            if (actionButtonsContainer.visibility == View.GONE) {
                actionButtonsContainer.visibility = View.VISIBLE
                arrowRight.rotation = 90f // Rotasi menjadi 90f (panah ke bawah: V)
            } else {
                actionButtonsContainer.visibility = View.GONE
                arrowRight.rotation = 0f // Rotasi kembali ke 0f (panah ke kanan: >)
            }
        }

        tasksContainer.addView(mainContainer, 0)
    }

    private fun updateEmptyState(taskCount: Int) {
        tvNoActivity.visibility = if (taskCount == 0) View.VISIBLE else View.GONE
    }

    // FUNGSI KONFIRMASI (untuk Complete/Delete) yang menggunakan dialog_save_success.xml
    private fun showConfirmationDialog(taskTitle: String, action: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_success, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val mainMessageTextView = (dialogView as ViewGroup).getChildAt(0) as? TextView

        val btnConfirm1 = dialogView.findViewById<TextView>(R.id.btnIgnore)
        val btnConfirm2 = dialogView.findViewById<TextView>(R.id.btnView)

        val message: String
        if (action == "selesai") {
            message = "Selamat! Tugas '$taskTitle' berhasil diselesaikan."
        } else {
            message = "Tugas '$taskTitle' berhasil dihapus."
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

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}