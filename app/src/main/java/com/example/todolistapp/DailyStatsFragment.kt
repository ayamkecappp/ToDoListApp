package com.example.todolistapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.constraintlayout.widget.ConstraintLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.content.ContextCompat
import kotlin.math.pow

class DailyStatsFragment : Fragment() {

    private lateinit var statsTitle: TextView
    private lateinit var streakValue: TextView
    private lateinit var barChartContainer: ConstraintLayout
    private lateinit var xAxisLabels: LinearLayout

    private val barViewsId = listOf(R.id.rectangle_1, R.id.rectangle_2, R.id.rectangle_3, R.id.rectangle_4, R.id.rectangle_5, R.id.rectangle_6, R.id.rectangle_7)
    private val barViews = mutableListOf<View>()
    private val dayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    private var barData = listOf<Pair<View, Int>>()
    private var currentHighlightedIndex = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_daily_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statsTitle = view.findViewById(R.id.stats_header_title)
        streakValue = view.findViewById(R.id.streak_value)
        barChartContainer = view.findViewById(R.id.bar_chart_container)
        xAxisLabels = view.findViewById(R.id.x_axis_labels)

        barViews.clear()
        for (id in barViewsId) {
            barChartContainer.findViewById<View>(id)?.let { barViews.add(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { animateContent(it) }
    }

    override fun onPause() {
        super.onPause()
        view?.let { resetContent(it) }
    }

    private fun resetContent(rootView: View) {
        val viewsToReset = mutableListOf<View>(statsTitle, streakValue, xAxisLabels)

        viewsToReset.forEach { view ->
            view.alpha = 0f
            view.clearAnimation()
        }

        for (bar in barViews) {
            bar.alpha = 0f
            bar.scaleY = 0.0f
            bar.setBackgroundResource(R.drawable.rectangle_2)
            bar.clearAnimation()
        }
    }

    private fun getStartOfWeek(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return calendar
    }

    private fun setBarHighlight(index: Int) {
        if (index < 0 || index >= barData.size) return

        if (currentHighlightedIndex != -1 && currentHighlightedIndex < barViews.size) {
            barViews[currentHighlightedIndex].setBackgroundResource(R.drawable.rectangle_2)
        }

        val bar = barData[index].first
        val count = barData[index].second

        bar.setBackgroundResource(R.drawable.rectangle_2_gold)
        currentHighlightedIndex = index

        streakValue.text = count.toString()
    }


    private fun animateContent(rootView: View) {
        rootView.post {
            val headerViews = listOf<View>(statsTitle, streakValue, xAxisLabels)
            headerViews.forEach { view ->
                view.alpha = 0f
                view.animate().alpha(1f).setDuration(400).start()
            }

            lifecycleScope.launch(Dispatchers.Main) {
                val startOfWeek = getStartOfWeek()
                val endOfWeek = startOfWeek.clone() as Calendar
                endOfWeek.add(Calendar.DAY_OF_YEAR, 6)

                val completedTasksMap = withContext(Dispatchers.IO) {
                    TaskRepository.getDailyCompletedTasksCount(startOfWeek, endOfWeek)
                }

                val dayToCount = dayOrder.associateWith { completedTasksMap[it] ?: 0 }
                val maxTaskCount = dayToCount.values.maxOrNull()?.toFloat() ?: 1f
                var maxCount = -1
                var maxIndex = -1

                val tempBarData = mutableListOf<Pair<View, Int>>()

                val MIN_SCALE = 0.05f // Skala minimum untuk batang yang memiliki nilai > 0

                for (i in barViews.indices) {
                    if (i >= dayOrder.size) break

                    val dayName = dayOrder[i]
                    val taskCount = dayToCount[dayName] ?: 0
                    val bar = barViews[i]

                    tempBarData.add(Pair(bar, taskCount))

                    // LOGIKA BARU: >= untuk memilih yang terbaru jika nilainya sama
                    if (taskCount >= maxCount) {
                        maxCount = taskCount
                        maxIndex = i
                    }

                    // [PERBAIKAN]: Skala linear proporsional
                    val scaleRatio = if (taskCount == 0) {
                        0.0f
                    } else {
                        // Skala Linear murni: nilai / max.
                        val normalizedCount = (taskCount.toFloat() / maxTaskCount).coerceIn(0.0f, 1f)

                        // Terapkan skala minimum untuk visibilitas
                        normalizedCount.coerceAtLeast(MIN_SCALE)
                    }

                    bar.setBackgroundResource(R.drawable.rectangle_2)

                    bar.pivotY = bar.height.toFloat()
                    bar.scaleY = 0.0f
                    bar.alpha = 1f

                    bar.animate()
                        .scaleY(scaleRatio)
                        .setDuration(400)
                        .start()

                    // --- PERBAIKAN: Hanya aktifkan klik jika ada task (taskCount > 0) ---
                    if (taskCount > 0) {
                        bar.setOnClickListener {
                            setBarHighlight(i)
                        }
                    } else {
                        // Penting: Hapus listener lama jika bar tidak memiliki data
                        bar.setOnClickListener(null)
                    }

                }

                barData = tempBarData

                // Set highlight awal, pastikan bar yang dipilih memiliki count > 0 jika maxCount > 0
                if (maxIndex != -1 && maxCount > 0) {
                    setBarHighlight(maxIndex)
                } else {
                    // Jika tidak ada data sama sekali, set streak value ke 0 dan reset highlight
                    barViews.forEach { it.setBackgroundResource(R.drawable.rectangle_2) }
                    streakValue.text = "0"
                    currentHighlightedIndex = -1
                }
            }
        }
    }
}