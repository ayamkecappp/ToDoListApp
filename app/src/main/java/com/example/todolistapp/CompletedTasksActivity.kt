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

class CompletedTasksActivity : AppCompatActivity() {

    private lateinit var tasksContainer: LinearLayout
    private val uiDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.completed_tasks)

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

        // Hapus semua tampilan hardcoded di dalam ConstraintLayout (kecuali jika ada yang perlu dipertahankan)
        innerConstraintLayout.removeAllViews()

        // Buat LinearLayout baru untuk menampung daftar tugas yang telah selesai
        tasksContainer = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        innerConstraintLayout.addView(tasksContainer)

        loadCompletedTasks()
    }

    private fun loadCompletedTasks() {
        tasksContainer.removeAllViews()
        // Mengambil daftar tugas yang selesai
        val completedTasks = TaskRepository.getCompletedTasks()

        if (completedTasks.isEmpty()) {
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
                setImageResource(R.drawable.timy_complete_task) // Menggunakan gambar Timy
                contentDescription = "Timy Happy"
                setPadding(0, 0, 0, 16.dp)
            }
            emptyStateContainer.addView(ivTimyHappy)

            // 2. Teks "Yay, all tasks completed!"
            val tvMessage = TextView(this).apply {
                text = "Yay, all tasks completed!"
                textSize = 16f
                setTextColor(resources.getColor(R.color.very_dark_blue, theme))
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            emptyStateContainer.addView(tvMessage)

            tasksContainer.addView(emptyStateContainer)

            // =======================================================
        } else {
            // Kelompokkan tugas berdasarkan tanggal penyelesaian (tanggal ID tugas)
            val groupedTasks = completedTasks.groupBy {
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
                    createCompletedTaskItem(task)
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

        // Checklist/Centang (Tambahkan ikon centang)
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
            // Menggunakan ic_completed (bulat dengan centang) sebagai indikator selesai
            setImageResource(R.drawable.ic_completed)
            setColorFilter(Color.parseColor("#283F6D")) // Warna navy
            contentDescription = "Completed"
        }
        taskItemContainer.addView(ivCheckmark)

        // TextView for Task Title (geser ke kanan karena ada ikon centang)
        val tvTaskTitle = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToEnd = ivCheckmark.id // Mulai setelah ikon centang
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = 8.dp
            }
            // Tambahkan dekorasi garis tengah untuk menandakan selesai
            text = task.title
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG // Menambahkan garis coret
            setTextAppearance(context, R.style.completedTasksContent)
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setTextColor(Color.parseColor("#98A8C8")) // Warna lebih pudar
        }
        taskItemContainer.addView(tvTaskTitle)

        // TextView for Task Time (di kanan)
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
            setTextColor(Color.parseColor("#98A8C8")) // Warna lebih pudar
        }
        taskItemContainer.addView(tvTaskTime)

        tasksContainer.addView(taskItemContainer)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}