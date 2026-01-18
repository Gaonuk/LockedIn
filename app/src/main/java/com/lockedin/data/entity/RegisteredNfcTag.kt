package com.lockedin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registered_nfc_tag")
data class RegisteredNfcTag(
    @PrimaryKey val id: Int = 1, // Only one tag allowed
    val tagId: String,
    val registeredAt: Long,
    val nickname: String? = null
)
