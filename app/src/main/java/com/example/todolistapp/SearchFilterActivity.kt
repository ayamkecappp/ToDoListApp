package com.example.todolistapp

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import android.graphics.Color
import android.widget.HorizontalScrollView
import android.widget.ImageView

class SearchFilterActivity : AppCompatActivity() {

    private lateinit var inputSearch: EditText
    private lateinit var taskResultsContainer: LinearLayout
    private lateinit var chipContainer: LinearLayout
    private lateinit var btnBack: ImageView

    // Status filter bulan yang sedang aktif (-1 = All, 0=Jan, 11=Dec)
    private var activeMonthFilter: Int = -1
    private val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_filter)

        // --- INIALISASI VIEWS DARI XML ---
        inputSearch = findViewById(R.id.input_search)
        chipContainer = findViewById(R.id.chip_container)
        btnBack = findViewById(R.id.btn_back)

        // PERBAIKAN UTAMA: Menggunakan LinearLayout yang sudah ada di dalam ScrollView XML.
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
        // Hapus semua chip dinamis jika ada
        if (chipContainer.childCount > 1) {
            chipContainer.removeViews(1, chipContainer.childCount - 1)
        }

        // Set listener dan status awal untuk chip 'All'
        val chipAll = findViewById<TextView>(R.id.chip_all)
        chipAll.setOnClickListener {
            setActiveChip(-1, it)
            performSearch(inputSearch.text.toString(), -1)
        }
        // Set 'All' sebagai chip aktif default saat inisialisasi
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

                // OnClickListener ini adalah yang membuat chip bisa diklik
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

        val taskItem = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 64.dp).apply {
                setMargins(0, 0, 0, marginPx)
            }
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_task, null)
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            elevation = 4f
            setPadding(12.dp, 12.dp, 12.dp, 12.dp)
        }

        val checklistBox = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(24.dp, 24.dp).apply {
                marginStart = 9.dp
            }
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_checklist, null)
        }
        taskItem.addView(checklistBox)

        val taskTitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                marginStart = 17.dp
            }
            text = task.title
            textSize = 16f
            setTextColor(Color.parseColor("#14142A"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        taskItem.addView(taskTitle)

        // Menampilkan bulan tugas dibuat
        val taskMonth = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val monthIndex = task.monthAdded.coerceIn(0, 11)
            text = monthNames[monthIndex]
            textSize = 14f
            setTextColor(Color.parseColor("#283F6D"))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setPadding(0, 0, 12.dp, 0)
        }
        taskItem.addView(taskMonth)

        container.addView(taskItem)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // Definisi Int.dp
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}