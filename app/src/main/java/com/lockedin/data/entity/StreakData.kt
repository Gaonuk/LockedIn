package com.lockedin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak_data")
data class StreakData(
    @PrimaryKey
    val id: Int = 1, // Single row table
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedSessionDate: Long? = null, // Midnight timestamp of last completed session day
    val lastMilestoneShown: Int = 0 // Track which milestone was last shown (0, 7, 30, 100)
)
