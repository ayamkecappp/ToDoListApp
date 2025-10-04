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
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color
import android.graphics.Paint
// HAPUS import FrameLayout karena tidak dipakai
import android.view.animation.AnimationUtils


class CompletedTasksActivity : AppCompatActivity() {

    // Deklarasi semua view yang akan kita kontrol
    private lateinit var contentContainer: ConstraintLayout
    private lateinit var tasksContainer: LinearLayout
    private lateinit var scrollView: androidx.core.widget.NestedScrollView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var ivTimyTasks: ImageView
    // HAPUS baris ini: private lateinit var animationWrapper: FrameLayout
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.completed_tasks)

        // Inisialisasi semua view dari XML
        contentContainer = findViewById(R.id.content_container)
        tasksContainer = findViewById(R.id.tasks_container)
        scrollView = findViewById(R.id.tasks_scroll_view)
        emptyStateContainer = findViewById(R.id.empty_state_container)
        ivTimyTasks = findViewById(R.id.ivTimyTasks)

        findViewById<ImageView>(R.id.ivBackArrow).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
        }

        loadCompletedTasks()
    }

    private fun loadCompletedTasks() {
        tasksContainer.removeAllViews()
        val completedTasks = TaskRepository.getCompletedTasks()

        if (completedTasks.isEmpty()) {
            scrollView.visibility = View.GONE
            ivTimyTasks.visibility = View.GONE
            emptyStateContainer.visibility = View.VISIBLE
        } else {
            scrollView.visibility = View.VISIBLE
            ivTimyTasks.visibility = View.VISIBLE
            emptyStateContainer.visibility = View.GONE

            val groupedTasks = completedTasks.groupBy {
                Calendar.getInstance().apply { timeInMillis = it.id }.get(Calendar.DAY_OF_YEAR)
            }
            val sortedGroups = groupedTasks.toSortedMap(compareByDescending { it })
            for ((_, tasks) in sortedGroups) {
                val dateLabel = Calendar.getInstance().apply { timeInMillis = tasks.first().id }
                addDateHeader(dateLabel)
                for (task in tasks) {
                    createCompletedTaskItem(task)
                }
            }

            // UBAH: Animasi diterapkan ke content_container
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
            setTextAppearance(this@CompletedTasksActivity, R.style.completedTasksLabel)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        tasksContainer.addView(dateText)
    }

    private fun createCompletedTaskItem(task: Task) {
        val context = this

        val taskItemContainer = ConstraintLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 48.dp
            ).apply {
                setMargins(12.dp, 8.dp, 12.dp, 16.dp)
            }
            setBackgroundResource(R.drawable.rectangle_settings)
        }

        val ivCheckmark = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                24.dp, 24.dp
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = 18.dp
            }
            setImageResource(R.drawable.ic_completed)
            setColorFilter(Color.parseColor("#283F6D"))
            contentDescription = "Completed"
        }
        taskItemContainer.addView(ivCheckmark)

        val tvTaskTitle = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToEnd = ivCheckmark.id
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = 8.dp
            }
            text = task.title
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            setTextAppearance(context, R.style.completedTasksContent)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setTextColor(Color.parseColor("#98A8C8"))
        }
        taskItemContainer.addView(tvTaskTitle)

        val tvTaskTime = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginEnd = 18.dp
            }
            text = task.time
            setTextAppearance(context, R.style.missedTasksContent)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setTextColor(Color.parseColor("#98A8C8"))
        }
        taskItemContainer.addView(tvTaskTime)

        tasksContainer.addView(taskItemContainer)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}