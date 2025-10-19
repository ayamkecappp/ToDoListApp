// MonthlyStatsFragment.kt
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

class MonthlyStatsFragment : Fragment() {

    // Deklarasi View Fragment
    private lateinit var statsTitle: TextView
    private lateinit var streakValue: TextView
    private lateinit var fireIcon: ImageView
    private lateinit var barChartContainer: ConstraintLayout
    private lateinit var xAxisLabels: LinearLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_monthly_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hubungkan semua Views
        statsTitle = view.findViewById(R.id.stats_header_title)
        streakValue = view.findViewById(R.id.streak_value)
        fireIcon = view.findViewById(R.id.fire_icon2)
        barChartContainer = view.findViewById(R.id.bar_chart_container)
        // Hubungkan LinearLayout Sumbu X
        xAxisLabels = view.findViewById(R.id.x_axis_labels)
    }

    override fun onResume() {
        super.onResume()
        // Panggil animasi setiap kali Fragment terlihat (aktif)
        view?.let { animateContent(it) }
    }

    override fun onPause() {
        super.onPause()
        // Reset View agar animasi siap diputar lagi saat onResume dipanggil
        view?.let { resetContent(it) }
    }

    private fun resetContent(rootView: View) {
        val viewsToReset = mutableListOf<View>(statsTitle, streakValue, fireIcon, xAxisLabels)

        // Reset Header & Label Views
        viewsToReset.forEach { view ->
            view.alpha = 0f
            view.clearAnimation()
        }

        // Reset Bar Views (Alpha 0f dan scaleY 0.0f)
        val barContainer = rootView.findViewById<ConstraintLayout>(R.id.bar_chart_container)
        for (i in 0 until barContainer.childCount) {
            val bar = barContainer.getChildAt(i) as View
            if (bar.id != View.NO_ID) {
                bar.alpha = 0f
                bar.scaleY = 0.0f // Set tinggi awal batang ke nol
                bar.clearAnimation()
            }
        }
    }


    private fun animateContent(rootView: View) {
        // Meminta penundaan eksekusi hingga View selesai diukur (di-layout)
        rootView.post {
            val headerViews = listOf<View>(statsTitle, streakValue, fireIcon, xAxisLabels)
            val barContainer = rootView.findViewById<ConstraintLayout>(R.id.bar_chart_container)

            // 1. ANMASI HEADER & LABEL (Fade-in Serentak)
            headerViews.forEach { view ->
                view.alpha = 0f
                view.animate().alpha(1f).setDuration(400).start()
            }

            // 2. ANIMASI BAR CHART (Grow Vertikal Serentak)
            for (i in 0 until barContainer.childCount) {
                val bar = barContainer.getChildAt(i) as View

                if (bar.id != View.NO_ID) {
                    // Konfigurasi awal
                    bar.alpha = 1f

                    // KUNCI: Set titik pivot ke DASAR batang (tinggi penuh View)
                    bar.pivotY = bar.height.toFloat()
                    bar.scaleY = 0.0f

                    // Animasikan pertumbuhan
                    bar.animate()
                        .scaleY(1.0f) // Tumbuh ke tinggi penuh
                        .setDuration(400)
                        .start()
                }
            }
        }
    }
}