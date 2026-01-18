package com.lockedin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.lockedin.data.AppDatabase
import com.lockedin.service.NfcHandler
import com.lockedin.ui.navigation.AppNavigation
import com.lockedin.ui.navigation.Routes
import com.lockedin.ui.theme.LockedInTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var nfcHandler: NfcHandler

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled - notifications will work if granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcHandler = NfcHandler.getInstance(this)

        // Request notification permission on Android 13+
        requestNotificationPermission()

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

        // Handle NFC intent if launched from NFC tag tap
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Enable foreground dispatch for NFC when activity is in foreground
        nfcHandler.enableForegroundDispatch(this)
    }

    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch when activity loses focus
        nfcHandler.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        intent?.let {
            nfcHandler.handleIntent(it, lifecycleScope)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
