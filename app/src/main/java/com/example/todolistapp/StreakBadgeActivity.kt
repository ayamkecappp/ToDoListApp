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

    // Badge tier thresholds - DIMULAI DARI 1, BUKAN 0
    private val badgeTiers = listOf(
        BadgeTier(1, R.drawable.streak_badge_1, "First Step", "Your journey begins!"),
        BadgeTier(7, R.drawable.streak_badge_2, "7-Day Warrior", "One week strong!"),
        BadgeTier(15, R.drawable.streak_badge_3, "15-Day Champion", "Two weeks of dedication!"),
        BadgeTier(30, R.drawable.streak_badge_4, "Monthly Master", "A month of consistency!"),
        BadgeTier(50, R.drawable.streak_badge_5, "50-Day Legend", "Unstoppable force!"),
        BadgeTier(100, R.drawable.streak_badge_6, "Century Icon", "The ultimate achiever!")
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
            val unlockedBadges = getUnlockedBadges(currentStreak)

            // Update UI
            updateBadgeDisplay(currentStreak, unlockedBadges)
        }
    }

    /**
     * Mendapatkan daftar badge yang sudah unlocked berdasarkan streak
     */
    private fun getUnlockedBadges(streak: Int): List<Int> {
        val unlocked = mutableListOf<Int>()
        for (i in badgeTiers.indices) {
            if (streak >= badgeTiers[i].threshold) {
                unlocked.add(i)
            }
        }
        return unlocked
    }

    /**
     * Mendapatkan badge tier yang sedang aktif (tertinggi yang telah di-unlock)
     */
    private fun getCurrentBadgeTier(unlockedBadges: List<Int>): BadgeTier? {
        return if (unlockedBadges.isNotEmpty()) {
            badgeTiers[unlockedBadges.last()]
        } else {
            null // Belum ada badge yang unlocked
        }
    }

    /**
     * Mendapatkan badge tier berikutnya yang akan di-unlock
     */
    private fun getNextBadgeTier(unlockedBadges: List<Int>): BadgeTier? {
        val nextIndex = if (unlockedBadges.isEmpty()) 0 else unlockedBadges.last() + 1
        return if (nextIndex < badgeTiers.size) {
            badgeTiers[nextIndex]
        } else {
            null // Sudah unlock semua
        }
    }

    private fun updateBadgeDisplay(currentStreak: Int, unlockedBadges: List<Int>) {
        // Find views
        val ivMainBadge = findViewById<ImageView>(R.id.ivMainBadge)
        val tvOnFire = findViewById<TextView>(R.id.tvOnFire)
        val tvStreakInfo = findViewById<TextView>(R.id.tvStreakInfo)

        val currentBadge = getCurrentBadgeTier(unlockedBadges)
        val nextBadge = getNextBadgeTier(unlockedBadges)

        if (currentBadge != null) {
            // User sudah punya minimal 1 badge
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
            // User belum punya badge sama sekali (streak 0)
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
        updateGridBadges(unlockedBadges)
    }

    private fun updateGridBadges(unlockedBadges: List<Int>) {
        val scrollView = findViewById<android.widget.ScrollView>(R.id.scrollViewBadges)
        val linearLayout = scrollView?.getChildAt(0) as? android.widget.LinearLayout

        if (linearLayout != null) {
            // Find the first GridLayout (contains badges 2, 3, 4)
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

            // Update first grid (badges 2, 3, 4 - index 1, 2, 3)
            firstGridLayout?.let { grid ->
                for (i in 0 until grid.childCount) {
                    val imageView = grid.getChildAt(i) as? ImageView
                    imageView?.let {
                        val badgeIndex = i + 1 // Badge 2, 3, 4 (index 1, 2, 3)
                        updateBadgeAppearance(it, badgeIndex, unlockedBadges)
                    }
                }
            }

            // Update second grid (badges 5, 6 - index 4, 5)
            secondGridLayout?.let { grid ->
                for (i in 0 until grid.childCount) {
                    val imageView = grid.getChildAt(i) as? ImageView
                    imageView?.let {
                        val badgeIndex = i + 4 // Badge 5, 6 (index 4, 5)
                        updateBadgeAppearance(it, badgeIndex, unlockedBadges)
                    }
                }
            }
        }
    }

    private fun updateBadgeAppearance(imageView: ImageView, badgeIndex: Int, unlockedBadges: List<Int>) {
        // Check if badge is unlocked
        val isUnlocked = badgeIndex in unlockedBadges

        if (isUnlocked) {
            // Badge unlocked - show in full color
            imageView.alpha = 1.0f
            imageView.clearColorFilter()
            imageView.setImageResource(badgeTiers[badgeIndex].imageRes)
        } else {
            // Badge locked - show empty/locked state
            // Opsi 1: Tetap tampilkan gambar tapi grayscale
            imageView.setImageResource(badgeTiers[badgeIndex].imageRes)
            imageView.alpha = 0.2f
            imageView.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))

            // Opsi 2: Tampilkan placeholder empty badge (uncomment jika ingin gunakan)
            // imageView.setImageResource(R.drawable.ic_badge_locked)
            // imageView.alpha = 0.5f
            // imageView.clearColorFilter()
        }

        // Add content description for accessibility
        val badge = badgeTiers.getOrNull(badgeIndex)
        badge?.let {
            val description = if (isUnlocked) {
                "Unlocked: ${it.name} - ${it.threshold}+ days"
            } else {
                "Locked: ${it.name} - Requires ${it.threshold}+ days"
            }
            imageView.contentDescription = description
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    data class BadgeTier(
        val threshold: Int,
        val imageRes: Int,
        val name: String,
        val description: String
    )
}