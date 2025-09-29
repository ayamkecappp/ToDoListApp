package com.example.todolistapp

import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import android.graphics.drawable.GradientDrawable
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity

class CalendarActivity : AppCompatActivity() {

    private lateinit var monthText: TextView
    private lateinit var calendarGrid: GridLayout
    private lateinit var currentCalendar: Calendar

    private var selectedDate: Calendar = Calendar.getInstance()
    private val COLOR_ACTIVE_SELECTION = Color.parseColor("#283F6D")
    private val COLOR_TODAY_HIGHLIGHT = Color.parseColor("#FFCC80")
    private val COLOR_DEFAULT_TEXT = Color.BLACK
    private val COLOR_SELECTED_TEXT = Color.WHITE

    private val CORNER_RADIUS_DP = 8

    // Key untuk Intent Extra
    private val EXTRA_SELECTED_DATE_MILLIS = "EXTRA_SELECTED_DATE_MILLIS"

    // Tambahkan ActivityResultLauncher untuk menerima data task
    private val addTaskFromCalendarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Ketika AddTaskActivity mengembalikan RESULT_OK (tugas ditambahkan)

            // 1. Luncurkan TaskActivity
            val taskIntent = Intent(this, TaskActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

                // 2. Teruskan data tugas yang diterima
                result.data?.let { data ->
                    // Salin semua extra dari Intent hasil (yang berisi data tugas)
                    putExtras(data.extras ?: Bundle())
                }

                // Tambahkan flag khusus agar TaskActivity tahu harus segera memproses task ini
                putExtra("SHOULD_ADD_TASK", true)
            }
            startActivity(taskIntent)
        }
        // Jika result CANCELED, biarkan CalendarActivity tetap aktif.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calendar)

        monthText = findViewById(R.id.month_text)
        calendarGrid = findViewById(R.id.calendar_grid)
        val arrowLeft = findViewById<ImageView>(R.id.arrow_left)
        val arrowRight = findViewById<ImageView>(R.id.arrow_right)

        val addReminderButton = findViewById<LinearLayout>(R.id.addReminderButton)
        val rootSwipeView = findViewById<LinearLayout>(R.id.calendar_container)

        currentCalendar = Calendar.getInstance()
        selectedDate = currentCalendar.clone() as Calendar
        updateCalendar()

        // tombol panah
        arrowLeft.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        arrowRight.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        // OnClickListener untuk tombol Add Reminder (Launch AddTaskActivity)
        // PERUBAHAN UTAMA: Meneruskan tanggal yang dipilih
        addReminderButton.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java).apply {
                // Teruskan timestamp dari selectedDate
                putExtra(EXTRA_SELECTED_DATE_MILLIS, selectedDate.timeInMillis)
            }
            addTaskFromCalendarLauncher.launch(intent) // Gunakan launcher
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Swipe gesture
        rootSwipeView.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeLeft() {
                currentCalendar.add(Calendar.MONTH, 1)
                updateCalendar()
            }

            override fun onSwipeRight() {
                currentCalendar.add(Calendar.MONTH, -1)
                updateCalendar()
            }
        })
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun createRoundedBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = CORNER_RADIUS_DP.dp.toFloat()
        }
    }


    private fun updateCalendar() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthText.text = sdf.format(currentCalendar.time)

        calendarGrid.removeAllViews()

        val tempCal = currentCalendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()

        val isCurrentMonthView = (today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH))

        val isSelectedMonthView = (selectedDate.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH))

        val totalCells = 42

        val marginDp = 4
        val marginPx = marginDp.dp

        for (i in 0 until totalCells) {

            val layoutParams = GridLayout.LayoutParams().apply {
                width = 48.dp
                height = 48.dp
                setMargins(marginPx, marginPx, marginPx, marginPx)
                columnSpec = GridLayout.spec(i % 7)
                rowSpec = GridLayout.spec(i / 7)
            }

            val dayView = TextView(this).apply {
                setLayoutParams(layoutParams)
                gravity = Gravity.CENTER
                textSize = 16f
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
            }

            if (i >= firstDayOfWeek && i < daysInMonth + firstDayOfWeek) {
                val dayNumber = i - firstDayOfWeek + 1
                dayView.text = dayNumber.toString()

                val isToday = isCurrentMonthView && today.get(Calendar.DAY_OF_MONTH) == dayNumber
                val isSelected = isSelectedMonthView && selectedDate.get(Calendar.DAY_OF_MONTH) == dayNumber

                // 1. Atur Tampilan Default (White background, Black text)
                dayView.setTextColor(COLOR_DEFAULT_TEXT)
                dayView.background = createRoundedBackground(Color.WHITE)

                // 2. Highlight Hari Ini (Background Kuning)
                if (isToday) {
                    dayView.background = createRoundedBackground(COLOR_TODAY_HIGHLIGHT)
                    dayView.setTextColor(COLOR_DEFAULT_TEXT)
                }

                // 3. Highlight Tanggal Aktif (Background Dark Blue, Text Putih)
                if (isSelected) {
                    dayView.background = createRoundedBackground(COLOR_ACTIVE_SELECTION)
                    dayView.setTextColor(COLOR_SELECTED_TEXT)
                }

                // Tambahkan OnClickListener
                dayView.setOnClickListener {
                    currentCalendar.set(Calendar.DAY_OF_MONTH, dayNumber)
                    // selectedDate diperbarui di sini, siap untuk diteruskan ke AddTaskActivity
                    selectedDate = currentCalendar.clone() as Calendar
                    updateCalendar()
                }

            } else {
                dayView.text = ""
                (dayView.layoutParams as? GridLayout.LayoutParams)?.setMargins(0, 0, 0, 0)
                dayView.background = null
                dayView.layoutParams.height = 0
            }

            calendarGrid.addView(dayView)
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}