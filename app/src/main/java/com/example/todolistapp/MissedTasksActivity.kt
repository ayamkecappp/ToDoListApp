// main/java/com/example/todolistapp/MissedTasksActivity.kt
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
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.withContext
import java.util.Date

class MissedTasksActivity : AppCompatActivity() {

    private lateinit var contentContainer: ConstraintLayout
    private lateinit var tasksContainer: LinearLayout
    private lateinit var scrollView: androidx.core.widget.NestedScrollView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var ivTimyTasks: ImageView
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))
    private val groupingDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Launcher untuk Edit Task Activity (Reschedule)
    private val rescheduleTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Reload missed tasks setelah reschedule
            loadMissedTasks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.missed_tasks)

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


    }

    override fun onResume() {
        super.onResume()
        loadMissedTasks()
    }

    private fun loadMissedTasks() {
        tasksContainer.removeAllViews()

        lifecycleScope.launch(Dispatchers.IO) {
            val missedTasks = TaskRepository.getMissedTasks()

            // ✅ DEBUG: Log semua tasks
            Log.d("MissedTasks", "Total tasks: ${missedTasks.size}")
            missedTasks.forEachIndexed { index, task ->
                Log.d("MissedTasks", "Task $index: id=${task.id}, title=${task.title}, missedAt=${task.missedAt?.toDate()}")
            }

            withContext(Dispatchers.Main) {
                if (missedTasks.isEmpty()) {
                    scrollView.visibility = View.GONE
                    ivTimyTasks.visibility = View.GONE
                    emptyStateContainer.visibility = View.VISIBLE
                } else {
                    scrollView.visibility = View.VISIBLE
                    ivTimyTasks.visibility = View.VISIBLE
                    emptyStateContainer.visibility = View.GONE

                    val groupedTasks = missedTasks.groupBy {
                        val timeToUse = it.missedAt?.toDate()?.time ?: it.dueDate.toDate().time
                        groupingDateFormat.format(Date(timeToUse))
                    }

                    // ✅ DEBUG: Log grouped data
                    groupedTasks.forEach { (dateKey, tasks) ->
                        Log.d("MissedTasks", "Date: $dateKey, Count: ${tasks.size}")
                        tasks.forEach { task ->
                            Log.d("MissedTasks", "  - ${task.title} (id: ${task.id})")
                        }
                    }

                    val sortedGroups = groupedTasks.toSortedMap(compareByDescending { it })

                    sortedGroups.forEach { (dateString, tasksForDate) ->
                        val firstTask = tasksForDate.first()
                        val timeToUse = firstTask.missedAt?.toDate()?.time
                            ?: firstTask.dueDate.toDate().time
                        val dateLabel = Calendar.getInstance().apply {
                            timeInMillis = timeToUse
                        }
                        addDateHeader(dateLabel)

                        tasksForDate.forEach { task ->
                            createMissedTaskItem(task)
                        }
                    }

                    val slideDown = AnimationUtils.loadAnimation(
                        this@MissedTasksActivity,
                        R.anim.slide_down_bounce
                    )
                    contentContainer.startAnimation(slideDown)
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

        // TextView for Task Title (Dibutuhkan untuk ConstraintLayout)
        val tvTaskTitle = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                // FIX: Set width menjadi 0 (MATCH_CONSTRAINT)
                0, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = 18.dp
                // Constraint akhir akan diatur setelah ivRescheduleBg dibuat
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

            // FITUR RESCHEDULE
            setOnClickListener {
                val intent = Intent(context, EditTaskActivity::class.java).apply {
                    putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id) // ID sekarang String
                    putExtra(EditTaskActivity.EXTRA_RESCHEDULE_MODE, true)
                }
                rescheduleTaskLauncher.launch(intent)
                (context as AppCompatActivity).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
        taskItemContainer.addView(ivRescheduleBg)

        // FIX: Hubungkan tvTaskTitle ke ivRescheduleBg
        (tvTaskTitle.layoutParams as ConstraintLayout.LayoutParams).apply {
            endToStart = ivRescheduleBg.id
            marginEnd = 8.dp
        }
        tvTaskTitle.requestLayout()

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

            // FITUR RESCHEDULE (juga pada text)
            setOnClickListener {
                ivRescheduleBg.performClick()
            }
        }
        taskItemContainer.addView(tvRescheduleText)

        tasksContainer.addView(taskItemContainer)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}