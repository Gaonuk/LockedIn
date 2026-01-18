package com.lockedin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lockedin.data.AppDatabase
import com.lockedin.ui.navigation.AppNavigation
import com.lockedin.ui.navigation.Routes
import com.lockedin.ui.theme.LockedInTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LockedInTheme {
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val isSetupComplete = withContext(Dispatchers.IO) {
                        val db = AppDatabase.getInstance(this@MainActivity)
                        db.setupStateDao().isSetupCompleted() ?: false
                    }
                    startDestination = if (isSetupComplete) {
                        Routes.HOME
                    } else {
                        Routes.SETUP_WIZARD
                    }
                }

                if (startDestination != null) {
                    AppNavigation(
                        modifier = Modifier.fillMaxSize(),
                        startDestination = startDestination!!
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
