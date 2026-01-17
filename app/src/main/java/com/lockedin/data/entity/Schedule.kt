package com.lockedin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startTimeMinutes: Int,
    val endTimeMinutes: Int,
    val daysOfWeek: Int,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
