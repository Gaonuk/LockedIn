package com.lockedin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_statistics")
data class SessionStatistic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val blockedAttempts: Int = 0,
    val timeSavedSeconds: Long = 0,
    val wasCompletedSuccessfully: Boolean = false
)
