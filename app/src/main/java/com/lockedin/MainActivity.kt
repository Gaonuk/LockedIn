package com.lockedin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.lockedin.ui.schedule.ScheduleConfigScreen
import com.lockedin.ui.theme.LockedInTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LockedInTheme {
                ScheduleConfigScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
