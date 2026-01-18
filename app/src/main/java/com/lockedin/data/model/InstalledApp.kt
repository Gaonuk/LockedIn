package com.lockedin.data.model

import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val isSelected: Boolean = false
)
