package com.lockedin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "setup_state")
data class SetupState(
    @PrimaryKey val id: Int = 1,
    val isSetupCompleted: Boolean = false,
    val completedAt: Long? = null
)
