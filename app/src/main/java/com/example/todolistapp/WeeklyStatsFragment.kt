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
import androidx.core.content.ContextCompat
import kotlin.math.pow

class WeeklyStatsFragment : Fragment() {

    private lateinit var statsTitle: TextView
    private lateinit var streakValue: TextView
    private lateinit var barChartContainer: ConstraintLayout
    private lateinit var xAxisLabels: LinearLayout

    private val barViewsId = listOf(R.id.rectangle_1, R.id.rectangle_2, R.id.rectangle_3, R.id.rectangle_4)
    private val barViews = mutableListOf<View>()
    private val weekLabels = mutableListOf<TextView>()

    private var barData = listOf<Pair<View, Int>>()
    private var currentHighlightedIndex = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_weekly_stats, container, false)
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

        weekLabels.clear()
        val xAxisContainer = view.findViewById<LinearLayout>(R.id.x_axis_labels)
        for (i in 0 until xAxisContainer.childCount) {
            if (xAxisContainer.getChildAt(i) is TextView) {
                weekLabels.add(xAxisContainer.getChildAt(i) as TextView)
            }
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
                // 1. Define Period (Current Month)
                val today = Calendar.getInstance()
                val startOfMonth = today.clone() as Calendar
                startOfMonth.set(Calendar.DAY_OF_MONTH, 1)

                val endOfMonth = today.clone() as Calendar
                endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH))

                // 2. Fetch Data
                val completedTasksMap = withContext(Dispatchers.IO) {
                    TaskRepository.getWeeklyCompletedTasksCount(startOfMonth, endOfMonth)
                }

                // 3. Map Data to UI Order
                val weekToCount = mutableMapOf<Int, Int>()
                val maxWeek = 5

                for (i in 1..maxWeek) {
                    weekToCount[i] = completedTasksMap[i] ?: 0
                }

                if (barViews.size >= 4) {
                    // Konsolidasi Week 5 ke Week 4
                    weekToCount[4] = (weekToCount[4] ?: 0) + (weekToCount[5] ?: 0)
                }

                val maxTaskCount = weekToCount.filterKeys { it <= 4 }.values.maxOrNull()?.toFloat() ?: 1f

                var maxCount = -1
                var maxIndex = -1
                val tempBarData = mutableListOf<Pair<View, Int>>()

                // 4. Animation Logic
                for (i in barViews.indices) {
                    val weekNumber = i + 1
                    val taskCount = weekToCount[weekNumber] ?: 0
                    val bar = barViews[i]

                    tempBarData.add(Pair(bar, taskCount))

                    // LOGIKA BARU: >= untuk memilih yang terbaru jika nilainya sama
                    if (taskCount >= maxCount) {
                        maxCount = taskCount
                        maxIndex = i
                    }

                    val normalizedCount = (taskCount.toFloat() / maxTaskCount)
                    // MENGGUNAKAN PANGKAT 0.4 UNTUK SMOOTHING YANG LEBIH KUAT
                    val smoothedRatio = normalizedCount.toDouble().pow(0.4).toFloat()

                    // Min 0.05f untuk tugas > 0
                    val scaleRatio = smoothedRatio.coerceIn(if (taskCount > 0) 0.05f else 0.0f, 1f)

                    bar.setBackgroundResource(R.drawable.rectangle_2)

                    bar.pivotY = bar.height.toFloat()
                    bar.scaleY = 0.0f
                    bar.alpha = 1f

                    bar.animate()
                        .scaleY(scaleRatio)
                        .setDuration(400)
                        .start()

                    // Update label
                    if (i < weekLabels.size) {
                        weekLabels[i].text = "Wk ${weekNumber}"
                    }

                    bar.setOnClickListener {
                        setBarHighlight(i)
                    }
                }

                barData = tempBarData

                setBarHighlight(maxIndex)
            }
        }
    }
}