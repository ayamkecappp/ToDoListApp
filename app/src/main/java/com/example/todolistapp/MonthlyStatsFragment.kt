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

class MonthlyStatsFragment : Fragment() {

    private lateinit var statsTitle: TextView
    private lateinit var streakValue: TextView
    private lateinit var barChartContainer: ConstraintLayout
    private lateinit var xAxisLabels: LinearLayout

    private val barViewsId = listOf(R.id.rectangle_1, R.id.rectangle_2, R.id.rectangle_3, R.id.rectangle_4, R.id.rectangle_5, R.id.rectangle_6)
    private val barViews = mutableListOf<View>()
    // [DIHAPUS] monthNamesUs hardcoded list dihapus
    private val monthLabels = mutableListOf<TextView>()

    private var barData = listOf<Pair<View, Int>>()
    private var currentHighlightedIndex = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_monthly_stats, container, false)
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

        monthLabels.clear()
        val xAxisContainer = view.findViewById<LinearLayout>(R.id.x_axis_labels)
        for (i in 0 until xAxisContainer.childCount) {
            if (xAxisContainer.getChildAt(i) is TextView) {
                monthLabels.add(xAxisContainer.getChildAt(i) as TextView)
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

    /**
     * [BARU]: Fungsi helper untuk mendapatkan singkatan bulan (MMM)
     * Menggunakan Locale.US untuk memastikan kunci data cocok dengan TaskRepository.
     */
    private fun getMonthAbbreviation(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("MMM", Locale.US)
        return dateFormat.format(calendar.time)
    }


    private fun animateContent(rootView: View) {
        rootView.post {
            val headerViews = listOf<View>(statsTitle, streakValue, xAxisLabels)
            headerViews.forEach { view ->
                view.alpha = 0f
                view.animate().alpha(1f).setDuration(400).start()
            }

            lifecycleScope.launch(Dispatchers.Main) {
                // 1. Define Semi-annual Period (Jan-Jun atau Jul-Dec)
                val today = Calendar.getInstance()
                val currentMonth = today.get(Calendar.MONTH)

                val startMonthCalendarConst = if (currentMonth < Calendar.JULY) Calendar.JANUARY else Calendar.JULY

                val startCal = Calendar.getInstance().apply {
                    set(Calendar.MONTH, startMonthCalendarConst)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.YEAR, today.get(Calendar.YEAR))
                }

                val endCal = startCal.clone() as Calendar
                endCal.add(Calendar.MONTH, 5)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))

                // 2. Fetch Data
                val completedTasksMap = withContext(Dispatchers.IO) {
                    TaskRepository.getMonthlyCompletedTasksCount(startCal, endCal)
                }

                // 3. Map Data to UI Order
                val monthToCount = mutableMapOf<String, Int>()
                val uiMonths = mutableListOf<String>()

                for (i in 0 until 6) {
                    val cal = startCal.clone() as Calendar
                    cal.add(Calendar.MONTH, i)
                    // [UBAH]: Gunakan fungsi helper untuk mendapatkan singkatan bulan
                    val monthAbbreviation = getMonthAbbreviation(cal)

                    // Kunci data di TaskRepository adalah singkatan bulan (MMM)
                    monthToCount[monthAbbreviation] = completedTasksMap[monthAbbreviation] ?: 0
                    uiMonths.add(monthAbbreviation)
                }

                val maxTaskCount = monthToCount.values.maxOrNull()?.toFloat() ?: 1f

                var maxCount = -1
                var maxIndex = -1
                val tempBarData = mutableListOf<Pair<View, Int>>()

                // 4. Update UI
                val MIN_SCALE = 0.05f // Skala minimum untuk batang yang memiliki nilai > 0

                for (i in monthLabels.indices) {
                    if (i < uiMonths.size) {
                        // [PERBAIKAN 1]: Menampilkan nama bulan dalam singkatan
                        monthLabels[i].text = uiMonths[i]
                        monthLabels[i].visibility = View.VISIBLE
                    } else {
                        monthLabels[i].visibility = View.GONE
                    }
                }

                // Animate Bars
                for (i in barViews.indices) {
                    val monthName = uiMonths[i]
                    val taskCount = monthToCount[monthName] ?: 0
                    val bar = barViews[i]

                    tempBarData.add(Pair(bar, taskCount))

                    // LOGIKA BARU: >= untuk memilih yang terbaru jika nilainya sama
                    if (taskCount >= maxCount) {
                        maxCount = taskCount
                        maxIndex = i
                    }

                    // [PERBAIKAN 2]: Skala batang yang seimbang dan proporsional (Linear Scaling)
                    val scaleRatio = if (taskCount == 0) {
                        0.0f
                    } else {
                        // Skala Linear murni: nilai / max.
                        val normalizedCount = (taskCount.toFloat() / maxTaskCount).coerceIn(0.0f, 1f)

                        // Terapkan skala minimum untuk visibilitas
                        normalizedCount.coerceAtLeast(MIN_SCALE)
                    }

                    bar.setBackgroundResource(R.drawable.rectangle_2)

                    // Pastikan pivotY diatur ke dasar batang untuk animasi dari bawah
                    bar.pivotY = bar.height.toFloat()
                    bar.scaleY = 0.0f // Mulai dari 0
                    bar.alpha = 1f

                    bar.animate()
                        .scaleY(scaleRatio)
                        .setDuration(400)
                        .start()

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