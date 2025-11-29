package com.example.todolistapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View
import android.widget.GridLayout
import androidx.core.content.ContextCompat

class StreakBadgeActivity : AppCompatActivity() {

    // Main Badge tiers - DIMULAI DARI 7 HARI
    private val mainBadgeTiers = listOf(
        MainBadge(7, R.drawable.star_7, "7-Day Warrior", "One week strong!"),
        MainBadge(15, R.drawable.star_15, "15-Day Champion", "Two weeks of dedication!"),
        MainBadge(30, R.drawable.star_30, "Monthly Master", "A month of consistency!"),
        MainBadge(50, R.drawable.star_50, "50-Day Legend", "Unstoppable force!"),
        MainBadge(100, R.drawable.star_100, "Century Icon", "The ultimate achiever!")
    )

    // Grid badge thresholds (untuk tampilan di bawah)
    private val gridBadgeTiers = listOf(
        GridBadge(7, R.drawable.streak_badge_2),
        GridBadge(15, R.drawable.streak_badge_3),
        GridBadge(30, R.drawable.streak_badge_4),
        GridBadge(50, R.drawable.streak_badge_5),
        GridBadge(100, R.drawable.streak_badge_6)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streak_badge)

        val ivBackArrow = findViewById<ImageView>(R.id.ivBackArrow)

        ivBackArrow.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        loadStreakBadges()
    }

    private fun loadStreakBadges() {
        lifecycleScope.launch(Dispatchers.Main) {
            // Get current streak from TaskRepository
            val currentStreak = withContext(Dispatchers.IO) {
                TaskRepository.getCurrentUserStreakState().currentStreak
            }

            // Determine unlocked badges
            val unlockedMainBadges = getUnlockedMainBadges(currentStreak)
            val unlockedGridBadges = getUnlockedGridBadges(currentStreak)

            // Update UI
            updateBadgeDisplay(currentStreak, unlockedMainBadges, unlockedGridBadges)
        }
    }

    /**
     * Mendapatkan daftar main badge yang sudah unlocked
     */
    private fun getUnlockedMainBadges(streak: Int): List<Int> {
        val unlocked = mutableListOf<Int>()
        for (i in mainBadgeTiers.indices) {
            if (streak >= mainBadgeTiers[i].threshold) {
                unlocked.add(i)
            }
        }
        return unlocked
    }

    /**
     * Mendapatkan daftar grid badge yang sudah unlocked
     */
    private fun getUnlockedGridBadges(streak: Int): List<Int> {
        val unlocked = mutableListOf<Int>()
        for (i in gridBadgeTiers.indices) {
            if (streak >= gridBadgeTiers[i].threshold) {
                unlocked.add(i)
            }
        }
        return unlocked
    }

    /**
     * Mendapatkan main badge yang sedang aktif (tertinggi yang telah di-unlock)
     */
    private fun getCurrentMainBadge(unlockedBadges: List<Int>): MainBadge? {
        return if (unlockedBadges.isNotEmpty()) {
            mainBadgeTiers[unlockedBadges.last()]
        } else {
            null // Belum mencapai 7 hari
        }
    }

    /**
     * Mendapatkan main badge berikutnya yang akan di-unlock
     */
    private fun getNextMainBadge(unlockedBadges: List<Int>): MainBadge? {
        val nextIndex = if (unlockedBadges.isEmpty()) 0 else unlockedBadges.last() + 1
        return if (nextIndex < mainBadgeTiers.size) {
            mainBadgeTiers[nextIndex]
        } else {
            null // Sudah unlock semua
        }
    }

    private fun updateBadgeDisplay(currentStreak: Int, unlockedMainBadges: List<Int>, unlockedGridBadges: List<Int>) {
        // Find views
        val ivMainBadge = findViewById<ImageView>(R.id.ivMainBadge)
        val tvOnFire = findViewById<TextView>(R.id.tvOnFire)
        val tvStreakInfo = findViewById<TextView>(R.id.tvStreakInfo)

        val currentBadge = getCurrentMainBadge(unlockedMainBadges)
        val nextBadge = getNextMainBadge(unlockedMainBadges)

        if (currentBadge != null) {
            // User sudah punya minimal 1 main badge (streak >= 7)
            ivMainBadge.setImageResource(currentBadge.imageRes)
            ivMainBadge.alpha = 1.0f
            ivMainBadge.clearColorFilter()

            // Update title text based on current badge
            val titleText = when {
                currentStreak >= 100 -> "You're a Century Icon! 🏆"
                currentStreak >= 50 -> "You're a Legend! 🔥"
                currentStreak >= 30 -> "You're on Fire! 🔥"
                currentStreak >= 15 -> "Keep Going Strong! 💪"
                currentStreak >= 7 -> "You're Doing Great! ⭐"
                else -> "Great Start! 🚀"
            }
            tvOnFire.text = titleText

            // Show info about next badge
            if (nextBadge != null) {
                val daysToNext = nextBadge.threshold - currentStreak
                tvStreakInfo.text = "Next badge in $daysToNext days: ${nextBadge.name}"
                tvStreakInfo.visibility = View.VISIBLE
            } else {
                tvStreakInfo.text = "You've unlocked all badges! 🎉"
                tvStreakInfo.visibility = View.VISIBLE
            }
        } else {
            // User belum mencapai 7 hari
            ivMainBadge.setImageResource(R.drawable.ic_badge_empty) // Placeholder kosong
            ivMainBadge.alpha = 0.3f
            ivMainBadge.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))

            tvOnFire.text = "Start Your Journey! 🚀"

            if (nextBadge != null) {
                val daysToFirst = nextBadge.threshold - currentStreak
                tvStreakInfo.text = "Complete $daysToFirst day${if(daysToFirst > 1) "s" else ""} to unlock your first badge!"
                tvStreakInfo.visibility = View.VISIBLE
            }
        }

        // Display badges in grid
        updateGridBadges(unlockedGridBadges)
    }

    private fun updateGridBadges(unlockedBadges: List<Int>) {
        val scrollView = findViewById<android.widget.ScrollView>(R.id.scrollViewBadges)
        val linearLayout = scrollView?.getChildAt(0) as? android.widget.LinearLayout

        if (linearLayout != null) {
            // Find the GridLayouts
            var firstGridLayout: GridLayout? = null
            var secondGridLayout: GridLayout? = null

            for (i in 0 until linearLayout.childCount) {
                val child = linearLayout.getChildAt(i)
                if (child is GridLayout) {
                    if (firstGridLayout == null) {
                        firstGridLayout = child
                    } else if (secondGridLayout == null) {
                        secondGridLayout = child
                        break
                    }
                }
            }

            // Update first grid (badges index 0, 1, 2 = 7, 15, 30 days)
            firstGridLayout?.let { grid ->
                for (i in 0 until grid.childCount) {
                    val imageView = grid.getChildAt(i) as? ImageView
                    imageView?.let {
                        val badgeIndex = i // Index 0, 1, 2
                        updateBadgeAppearance(it, badgeIndex, unlockedBadges)
                    }
                }
            }

            // Update second grid (badges index 3, 4 = 50, 100 days)
            secondGridLayout?.let { grid ->
                for (i in 0 until grid.childCount) {
                    val imageView = grid.getChildAt(i) as? ImageView
                    imageView?.let {
                        val badgeIndex = i + 3 // Index 3, 4
                        updateBadgeAppearance(it, badgeIndex, unlockedBadges)
                    }
                }
            }
        }
    }

    private fun updateBadgeAppearance(imageView: ImageView, badgeIndex: Int, unlockedBadges: List<Int>) {
        // Check if badge is unlocked
        val isUnlocked = badgeIndex in unlockedBadges

        if (badgeIndex < gridBadgeTiers.size) {
            val badge = gridBadgeTiers[badgeIndex]

            if (isUnlocked) {
                // Badge unlocked - show in full color
                imageView.alpha = 1.0f
                imageView.clearColorFilter()
                imageView.setImageResource(badge.imageRes)
            } else {
                // Badge locked - show grayed out
                imageView.setImageResource(badge.imageRes)
                imageView.alpha = 0.2f
                imageView.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
            }

            // Add content description for accessibility
            val description = if (isUnlocked) {
                "Unlocked: ${badge.threshold} days badge"
            } else {
                "Locked: Requires ${badge.threshold}+ days"
            }
            imageView.contentDescription = description
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    data class MainBadge(
        val threshold: Int,
        val imageRes: Int,
        val name: String,
        val description: String
    )

    data class GridBadge(
        val threshold: Int,
        val imageRes: Int
    )
}