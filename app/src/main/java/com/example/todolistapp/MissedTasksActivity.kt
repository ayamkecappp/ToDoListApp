package com.example.todolistapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.content.Intent
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import android.view.View
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color
import android.view.animation.AnimationUtils

class MissedTasksActivity : AppCompatActivity() {

    // Deklarasi semua view yang akan kita kontrol
    private lateinit var contentContainer: ConstraintLayout
    private lateinit var tasksContainer: LinearLayout
    private lateinit var scrollView: androidx.core.widget.NestedScrollView
    private lateinit var emptyStateContainer: LinearLayout
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.missed_tasks)

        // Inisialisasi semua view dari XML
        contentContainer = findViewById(R.id.content_container)
        tasksContainer = findViewById(R.id.tasks_container)
        scrollView = findViewById(R.id.tasks_scroll_view)
        emptyStateContainer = findViewById(R.id.empty_state_container)

        findViewById<ImageView>(R.id.ivBackArrow).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
        }

        // Proses missed tasks sebelum memuat
        TaskRepository.processTasksForMissed()
        loadMissedTasks()
    }

    private fun loadMissedTasks() {
        tasksContainer.removeAllViews()
        val missedTasks = TaskRepository.getMissedTasks()

        if (missedTasks.isEmpty()) {
            // ---- KONDISI KOSONG ----
            // Sembunyikan background kartu dan tampilkan tampilan kosong
            scrollView.visibility = View.GONE
            emptyStateContainer.visibility = View.VISIBLE
        } else {
            // ---- KONDISI ADA TUGAS ----
            // Tampilkan background kartu dan sembunyikan tampilan kosong
            scrollView.visibility = View.VISIBLE
            emptyStateContainer.visibility = View.GONE

            // Kelompokkan tugas berdasarkan tanggal berakhir
            val groupedTasks = missedTasks.groupBy {
                Calendar.getInstance().apply { timeInMillis = it.endTimeMillis }.get(Calendar.DAY_OF_YEAR)
            }

            val sortedGroups = groupedTasks.toSortedMap(compareByDescending { it })

            for ((_, tasks) in sortedGroups) {
                val dateLabel = Calendar.getInstance().apply { timeInMillis = tasks.first().endTimeMillis }
                addDateHeader(dateLabel)

                for (task in tasks) {
                    createMissedTaskItem(task)
                }
            }

            // Jalankan animasi
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_bounce)
            contentContainer.startAnimation(slideDown)
        }
    }

    private fun addDateHeader(date: Calendar) {
        val dateText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(11.dp, 12.dp, 0, 0)
            }
            text = uiDateFormat.format(date.time)
            setTextAppearance(this@MissedTasksActivity, R.style.completedTasksLabel)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        tasksContainer.addView(dateText)
    }

    private fun createMissedTaskItem(task: Task) {
        val context = this

        val taskItemContainer = ConstraintLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 48.dp
            ).apply {
                setMargins(12.dp, 8.dp, 12.dp, 16.dp)
            }
            setBackgroundResource(R.drawable.rectangle_settings)
        }

        // TextView for Task Title
        val tvTaskTitle = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = 18.dp
            }
            text = task.title
            setTextAppearance(context, R.style.completedTasksContent)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        taskItemContainer.addView(tvTaskTitle)

        // ImageView for Reschedule Button Background (rectangle_5 - Dark Blue)
        val ivRescheduleBg = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                85.dp, 20.dp
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                marginEnd = 45.dp
            }
            setBackgroundResource(R.drawable.rectangle_5)
            contentDescription = "Reschedule Button"
            setOnClickListener {
                Toast.makeText(context, "Reschedule ${task.title} clicked", Toast.LENGTH_SHORT).show()
            }
        }
        taskItemContainer.addView(ivRescheduleBg)

        // TextView for Reschedule Text
        val tvRescheduleText = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ivRescheduleBg.id
                topToTop = ivRescheduleBg.id
                bottomToBottom = ivRescheduleBg.id
                marginStart = 13.dp
            }
            text = "Reschedule"
            setTextAppearance(context, R.style.deletedTasksLabel)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        taskItemContainer.addView(tvRescheduleText)

        tasksContainer.addView(taskItemContainer)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}