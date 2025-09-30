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

class MissedTasksActivity : AppCompatActivity() {

    private lateinit var tasksContainer: LinearLayout
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.missed_tasks)

        val ivBackArrow = findViewById<ImageView>(R.id.ivBackArrow)
        ivBackArrow.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
        }

        // --- Inisialisasi dan Muat Data ---
        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.bgCompletedTasks)
        val innerConstraintLayout = scrollView.getChildAt(0) as ConstraintLayout

        innerConstraintLayout.removeAllViews()

        // Buat LinearLayout baru untuk menampung daftar tugas yang terlewat
        tasksContainer = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        innerConstraintLayout.addView(tasksContainer)

        // PENTING: Proses missed tasks sebelum memuat
        TaskRepository.processTasksForMissed()
        loadMissedTasks()
    }

    private fun loadMissedTasks() {
        tasksContainer.removeAllViews()
        val missedTasks = TaskRepository.getMissedTasks()

        if (missedTasks.isEmpty()) {
            val noTasksText = TextView(this).apply {
                text = "Tidak ada tugas yang terlewat."
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                setPadding(0, 50.dp, 0, 50.dp)
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
            }
            tasksContainer.addView(noTasksText)
        } else {
            // Kelompokkan tugas berdasarkan tanggal berakhir (Day of Year dari endTimeMillis)
            val groupedTasks = missedTasks.groupBy {
                Calendar.getInstance().apply { timeInMillis = it.endTimeMillis }.get(Calendar.DAY_OF_YEAR)
            }

            // Urutkan grup berdasarkan tanggal (terbaru di atas)
            val sortedGroups = groupedTasks.toSortedMap(compareByDescending { it })

            for ((_, tasks) in sortedGroups) {
                // Gunakan endTimeMillis untuk header tanggal
                val dateLabel = Calendar.getInstance().apply { timeInMillis = tasks.first().endTimeMillis }
                addDateHeader(dateLabel)

                // Tambahkan tugas
                for (task in tasks) {
                    createMissedTaskItem(task)
                }
            }
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