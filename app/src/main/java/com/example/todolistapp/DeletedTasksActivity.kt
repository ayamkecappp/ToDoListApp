// main/java/com/example/todolistapp/DeletedTasksActivity.kt
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.Date

class DeletedTasksActivity : AppCompatActivity() {

    private lateinit var contentContainer: ConstraintLayout
    private lateinit var tasksContainer: LinearLayout
    private lateinit var scrollView: androidx.core.widget.NestedScrollView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var ivTimyTasks: ImageView
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))
    private val groupingDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
        loadDeletedTasks()
    }

    private fun loadDeletedTasks() {
        tasksContainer.removeAllViews()

        lifecycleScope.launch(Dispatchers.IO) { // Menggunakan lifecycleScope
            val deletedTasks = TaskRepository.getDeletedTasks()

            withContext(Dispatchers.Main) {
                if (deletedTasks.isEmpty()) {
                    scrollView.visibility = View.GONE
                    ivTimyTasks.visibility = View.GONE
                    emptyStateContainer.visibility = View.VISIBLE
                } else {
                    scrollView.visibility = View.VISIBLE
                    ivTimyTasks.visibility = View.VISIBLE
                    emptyStateContainer.visibility = View.GONE

                    val groupedTasks = deletedTasks.groupBy {
                        // Logika pengelompokan yang memastikan header tanggal hanya muncul sekali per hari
                        val timeToUse = it.deletedAt?.toDate()?.time ?: it.dueDate.toDate().time
                        groupingDateFormat.format(Date(timeToUse))
                    }

                    // Sort berdasarkan tanggal string (descending)
                    val sortedGroups = groupedTasks.toSortedMap(compareByDescending { it })

                    for ((_, tasks) in sortedGroups) {
                        // FIX: addDateHeader dipanggil SEKALI per grup tanggal
                        val timeToUse = tasks.first().deletedAt?.toDate()?.time ?: tasks.first().dueDate.toDate().time
                        val dateLabel = Calendar.getInstance().apply { timeInMillis = timeToUse }
                        addDateHeader(dateLabel)

                        for (task in tasks) {
                            createDeletedTaskItem(task)
                        }
                    }

                    val slideDown = AnimationUtils.loadAnimation(this@DeletedTasksActivity, R.anim.slide_down_bounce)
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

            // Menggunakan Task.id yang sekarang String
            setOnClickListener {
                val intent = Intent(context, EditTaskActivity::class.java).apply {
                    putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id)
                    putExtra(EditTaskActivity.EXTRA_RESCHEDULE_MODE, true)
                }
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

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
                    putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id)
                    putExtra(EditTaskActivity.EXTRA_RESCHEDULE_MODE, true)
                }
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
        taskItemContainer.addView(ivRescheduleBg)

        // TextView for Task Title
        val tvTaskTitle = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                // FIX: Set width menjadi 0 (MATCH_CONSTRAINT)
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                // FIX: Arahkan ke ivRescheduleBg
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
            text = "Restore"
            setTextAppearance(context, R.style.deletedTasksLabel)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)

            isClickable = false
        }
        taskItemContainer.addView(tvRescheduleText)

        tasksContainer.addView(taskItemContainer)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}