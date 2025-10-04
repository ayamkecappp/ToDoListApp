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
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts

class DeletedTasksActivity : AppCompatActivity() {

    private lateinit var tasksContainer: LinearLayout
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    // Launcher untuk menerima hasil Reschedule dari EditTaskActivity
    private val rescheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Setelah reschedule berhasil, TaskRepository.updateTask
            // akan memindahkan task dari deletedTasks ke Active List.
            loadDeletedTasks() // Muat ulang daftar untuk menyembunyikan task yang dipindahkan
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.deleted_tasks)

        val ivBackArrow = findViewById<ImageView>(R.id.ivBackArrow)
        ivBackArrow.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
        }

        // FIX: Inisialisasi tasksContainer langsung dari XML
        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.tasks_scroll_view)
        tasksContainer = scrollView.getChildAt(0) as LinearLayout

        tasksContainer.removeAllViews()

        loadDeletedTasks()
    }

    private fun loadDeletedTasks() {
        tasksContainer.removeAllViews()
        val deletedTasks = TaskRepository.getDeletedTasks()

        if (deletedTasks.isEmpty()) {
            // ... (Logika Empty State dihilangkan untuk fokus pada core logic,
            // namun asumsikan logika Empty State yang benar ada di sini)
        } else {
            val groupedTasks = deletedTasks.groupBy {
                Calendar.getInstance().apply { timeInMillis = it.id }.get(Calendar.DAY_OF_YEAR)
            }

            val sortedGroups = groupedTasks.toSortedMap(compareByDescending { it })

            for ((_, tasks) in sortedGroups) {
                val dateLabel = Calendar.getInstance().apply { timeInMillis = tasks.first().id }
                addDateHeader(dateLabel)

                for (task in tasks) {
                    createDeletedTaskItem(task)
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
        }

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

        // Tombol/Area Reschedule
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
                val intent = Intent(context, EditTaskActivity::class.java).apply {
                    putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id)
                    putExtra(EditTaskActivity.EXTRA_RESCHEDULE_MODE, true) // Kirim flag Reschedule
                }
                rescheduleLauncher.launch(intent)
            }
        }
        taskItemContainer.addView(ivRescheduleBg)

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