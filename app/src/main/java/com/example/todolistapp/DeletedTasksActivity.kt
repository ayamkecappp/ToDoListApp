package com.example.todolistapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color
import android.view.animation.AnimationUtils

class DeletedTasksActivity : AppCompatActivity() {

    private lateinit var contentContainer: ConstraintLayout
    private lateinit var tasksContainer: LinearLayout
    private lateinit var scrollView: androidx.core.widget.NestedScrollView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var ivTimyTasks: ImageView
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.deleted_tasks)

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

        loadDeletedTasks()
    }

    override fun onResume() {
        super.onResume()
        // Refresh list ketika kembali dari EditTaskActivity
        loadDeletedTasks()
    }

    private fun loadDeletedTasks() {
        tasksContainer.removeAllViews()
        val deletedTasks = TaskRepository.getDeletedTasks()

        if (deletedTasks.isEmpty()) {
            scrollView.visibility = View.GONE
            ivTimyTasks.visibility = View.GONE
            emptyStateContainer.visibility = View.VISIBLE
        } else {
            scrollView.visibility = View.VISIBLE
            ivTimyTasks.visibility = View.VISIBLE
            emptyStateContainer.visibility = View.GONE

            val groupedTasks = deletedTasks.groupBy {
                // MODIFIED: Group by actionDateMillis (deletion date)
                val timeToUse = it.actionDateMillis ?: it.id // Fallback to original ID if somehow null
                Calendar.getInstance().apply { timeInMillis = timeToUse }.get(Calendar.DAY_OF_YEAR)
            }
            // Sort groups by date descending (latest deletion date first)
            val sortedGroups = groupedTasks.toSortedMap(compareByDescending { it })

            for ((_, tasks) in sortedGroups) {
                // MODIFIED: Display actionDateMillis
                val timeToUse = tasks.first().actionDateMillis ?: tasks.first().id
                val dateLabel = Calendar.getInstance().apply { timeInMillis = timeToUse }
                addDateHeader(dateLabel)
                for (task in tasks) {
                    createDeletedTaskItem(task)
                }
            }

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
            setTextAppearance(this@DeletedTasksActivity, R.style.completedTasksLabel)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
        }
        tasksContainer.addView(dateText)
    }

    private fun createDeletedTaskItem(task: Task) {
        val context = this

        val taskItemContainer = ConstraintLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 48.dp
            ).apply {
                setMargins(12.dp, 8.dp, 12.dp, 16.dp)
            }
            setBackgroundResource(R.drawable.rectangle_settings)

            // Tambahkan click listener untuk whole item - arahkan ke reschedule/restore
            setOnClickListener {
                val intent = Intent(context, EditTaskActivity::class.java).apply {
                    putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id) // Menggunakan konstanta
                    putExtra(EditTaskActivity.EXTRA_RESCHEDULE_MODE, true) // Menggunakan mode reschedule untuk restore/pindah ke active
                }
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        // ImageView for Reschedule/Restore Button Background (rectangle_5) - BUAT DULU
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
            contentDescription = "Restore Button"

            // Click listener untuk button restore (sama dengan parent)
            setOnClickListener {
                val intent = Intent(context, EditTaskActivity::class.java).apply {
                    putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id) // Menggunakan konstanta
                    putExtra(EditTaskActivity.EXTRA_RESCHEDULE_MODE, true)
                }
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
        taskItemContainer.addView(ivRescheduleBg)

        // TextView for Task Title - BUAT SETELAH ivRescheduleBg
        val tvTaskTitle = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                0, // width 0 dengan constraint = match_constraint
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                endToStart = ivRescheduleBg.id
                marginStart = 18.dp
                marginEnd = 8.dp
            }
            text = task.title
            setTextAppearance(context, R.style.completedTasksContent)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        taskItemContainer.addView(tvTaskTitle)

        // TextView for Restore Text
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
            // PERBAIKAN: Mengganti teks menjadi "Restore"
            text = "Restore"
            setTextAppearance(context, R.style.deletedTasksLabel)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)

            // Nonaktifkan click untuk text agar click diteruskan ke parent
            isClickable = false
        }
        taskItemContainer.addView(tvRescheduleText)

        tasksContainer.addView(taskItemContainer)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}