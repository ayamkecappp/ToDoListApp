// main/java/com/example/todolistapp/DeletedTasksActivity.kt (Modified)
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
import android.graphics.Color // Import Color

class DeletedTasksActivity : AppCompatActivity() {

    private lateinit var tasksContainer: LinearLayout // Container untuk list tugas
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.deleted_tasks)

        // Ambil ImageView panah kiri (back arrow)
        val ivBackArrow = findViewById<ImageView>(R.id.ivBackArrow)
        ivBackArrow.setOnClickListener {
            // Kembali ke ProfileActivity
            val intent = Intent(this, ProfileActivity::class.java)
            // Optional: mencegah multiple instance
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish() // optional, menutup DeletedTasksActivity
        }

        // --- Inisialisasi dan Muat Data ---
        // Dapatkan ConstraintLayout di dalam NestedScrollView (yang menampung item hardcoded)
        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.bgCompletedTasks)
        val innerConstraintLayout = scrollView.getChildAt(0) as ConstraintLayout

        // Hapus semua tampilan hardcoded di dalam ConstraintLayout
        innerConstraintLayout.removeAllViews()

        // Buat LinearLayout baru untuk menampung daftar tugas yang dihapus
        tasksContainer = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        // Tambahkan LinearLayout baru ke ConstraintLayout
        innerConstraintLayout.addView(tasksContainer)

        loadDeletedTasks()
    }

    private fun loadDeletedTasks() {
        tasksContainer.removeAllViews()
        val deletedTasks = TaskRepository.getDeletedTasks()

        if (deletedTasks.isEmpty()) {
            // =======================================================
            // BARU: Tampilan Kosong (Empty State) dengan Gambar Timy
            // =======================================================

            val emptyStateContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Berikan padding agar berada di tengah layar scrollable
                    setMargins(0, 100.dp, 0, 100.dp)
                }
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
            }

            // 1. Gambar Timy
            val ivTimyHappy = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    220.dp, 220.dp
                )
                // Menggunakan drawable yang serupa
                setImageResource(R.drawable.timy_complete_task)
                contentDescription = "Timy Happy"
                setPadding(0, 0, 0, 16.dp)
            }
            emptyStateContainer.addView(ivTimyHappy)

            // 2. Teks "Yay, trash is empty!"
            val tvMessage = TextView(this).apply {
                text = "Yay, trash is empty!"
                textSize = 16f
                setTextColor(resources.getColor(R.color.very_dark_blue, theme))
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            emptyStateContainer.addView(tvMessage)

            tasksContainer.addView(emptyStateContainer)

            // =======================================================
        } else {
            // Kelompokkan tugas berdasarkan tanggal pembuatan (Day of Year)
            val groupedTasks = deletedTasks.groupBy {
                Calendar.getInstance().apply { timeInMillis = it.id }.get(Calendar.DAY_OF_YEAR)
            }

            // Urutkan grup berdasarkan tanggal (terbaru di atas)
            val sortedGroups = groupedTasks.toSortedMap(compareByDescending { it })

            for ((_, tasks) in sortedGroups) {
                // Tambahkan Label Tanggal
                val dateLabel = Calendar.getInstance().apply { timeInMillis = tasks.first().id }
                addDateHeader(dateLabel)

                // Tambahkan tugas
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
                // Margin sesuai tvWednesday di deleted_tasks.xml
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
                // Margin sesuai rectangleSettings1 di deleted_tasks.xml
                setMargins(12.dp, 8.dp, 12.dp, 16.dp)
            }
            setBackgroundResource(R.drawable.rectangle_settings) // white background
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

        // ImageView for Reschedule Button Background (rectangle_5)
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
            // Tambahkan OnClickListener untuk Reschedule (opsional)
            setOnClickListener {
                Toast.makeText(context, "Reschedule ${task.title} clicked", Toast.LENGTH_SHORT).show()
                // Jika ingin mengimplementasikan reschedule, Anda perlu memindahkan kembali tugas dari deletedTasks ke tasks.
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