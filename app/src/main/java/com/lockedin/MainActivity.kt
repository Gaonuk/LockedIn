package com.lockedin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.lockedin.service.AppBlockerAccessibilityService
import com.lockedin.ui.navigation.AppNavigation
import com.lockedin.ui.navigation.Routes
import com.lockedin.ui.theme.LockedInTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LockedInTheme {
                val startDestination = if (AppBlockerAccessibilityService.isAccessibilityServiceEnabled(this)) {
                    Routes.APP_SELECTION
                } else {
                    Routes.ACCESSIBILITY_PERMISSION
                }

                AppNavigation(
                    modifier = Modifier.fillMaxSize(),
                    startDestination = startDestination
                )
            }
        }
    }
}
