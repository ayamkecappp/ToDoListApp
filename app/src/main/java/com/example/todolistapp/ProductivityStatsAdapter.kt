// ProductivityStatsAdapter.kt
package com.example.todolistapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ProductivityStatsAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DailyStatsFragment()
            1 -> WeeklyStatsFragment()
            2 -> MonthlyStatsFragment()
            else -> DailyStatsFragment()
        }
    }
}