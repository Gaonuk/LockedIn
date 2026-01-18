package com.lockedin.data.repository

import android.content.Context
import com.lockedin.data.AppDatabase
import com.lockedin.data.dao.StreakDao
import com.lockedin.data.entity.StreakData
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repository for managing streak data.
 * Handles streak tracking, updates, and milestone detection.
 */
class StreakRepository(context: Context) {

    private val streakDao: StreakDao = AppDatabase.getInstance(context).streakDao()

    companion object {
        val MILESTONES = listOf(7, 30, 100)
    }

    /**
     * Gets the current streak data as a Flow.
     */
    fun getStreakData(): Flow<StreakData?> = streakDao.getStreakData()

    /**
     * Gets the current streak data once (not as a Flow).
     */
    suspend fun getStreakDataOnce(): StreakData {
        return streakDao.getStreakDataOnce() ?: StreakData().also {
            streakDao.insertOrUpdate(it)
        }
    }

    /**
     * Called when a session is successfully completed.
     * Updates the streak based on whether this is a consecutive day or not.
     * @return The new milestone reached, or null if no new milestone
     */
    suspend fun onSessionCompleted(): Int? {
        val currentData = getStreakDataOnce()
        val todayMidnight = getTodayMidnight()
        val yesterdayMidnight = getYesterdayMidnight()

        val lastCompletedDate = currentData.lastCompletedSessionDate

        // Already completed a session today
        if (lastCompletedDate == todayMidnight) {
            return null
        }

        val newStreak: Int
        val isNewDay: Boolean

        when (lastCompletedDate) {
            // First session ever or continuing after reset
            null -> {
                newStreak = 1
                isNewDay = true
            }
            // Completed yesterday - streak continues
            yesterdayMidnight -> {
                newStreak = currentData.currentStreak + 1
                isNewDay = true
            }
            // Completed today already (handled above, but for safety)
            todayMidnight -> {
                return null
            }
            // Missed a day - streak resets
            else -> {
                newStreak = 1
                isNewDay = true
            }
        }

        val newLongestStreak = maxOf(currentData.longestStreak, newStreak)

        streakDao.updateStreak(
            currentStreak = newStreak,
            longestStreak = newLongestStreak,
            lastCompletedDate = todayMidnight
        )

        // Check if we hit a new milestone
        val newMilestone = MILESTONES.find { milestone ->
            newStreak >= milestone && currentData.lastMilestoneShown < milestone
        }

        if (newMilestone != null) {
            streakDao.updateLastMilestoneShown(newMilestone)
        }

        return newMilestone
    }

    /**
     * Checks if the streak should be reset due to a missed day.
     * Should be called on app startup.
     */
    suspend fun checkAndResetStreakIfNeeded(): Boolean {
        val currentData = getStreakDataOnce()
        val lastCompletedDate = currentData.lastCompletedSessionDate ?: return false

        val todayMidnight = getTodayMidnight()
        val yesterdayMidnight = getYesterdayMidnight()

        // If last completed session was before yesterday, reset the streak
        if (lastCompletedDate < yesterdayMidnight) {
            streakDao.resetCurrentStreak()
            return true
        }

        return false
    }

    /**
     * Gets the milestone to show for the current streak if not already shown.
     * @return The milestone to show, or null if no milestone or already shown
     */
    suspend fun getUnshownMilestone(): Int? {
        val currentData = getStreakDataOnce()
        return MILESTONES.find { milestone ->
            currentData.currentStreak >= milestone && currentData.lastMilestoneShown < milestone
        }
    }

    /**
     * Marks a milestone as shown.
     */
    suspend fun markMilestoneShown(milestone: Int) {
        streakDao.updateLastMilestoneShown(milestone)
    }

    private fun getTodayMidnight(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getYesterdayMidnight(): Long {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
