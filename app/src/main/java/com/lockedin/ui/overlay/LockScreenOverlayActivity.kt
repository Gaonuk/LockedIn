package com.lockedin.ui.overlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lockedin.R
import com.lockedin.service.BlockingStateManager
import com.lockedin.ui.theme.LockedInTheme
import kotlinx.coroutines.delay

/**
 * Full-screen overlay activity that appears when a blocked app is detected.
 * This activity:
 * - Displays a blocking message
 * - Shows a countdown timer until the schedule ends
 * - Cannot be dismissed by the back button
 * - Covers the entire screen
 */
class LockScreenOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent back button from dismissing the overlay
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing - back button is blocked
            }
        })

        enableEdgeToEdge()
        setContent {
            LockedInTheme {
                LockScreenOverlayContent(
                    blockingStateManager = BlockingStateManager.getInstance(this),
                    onScheduleEnded = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // If user tries to switch away from overlay via recents, finish it
        // The accessibility service will re-launch it if needed
    }

    companion object {
        private const val TAG = "LockScreenOverlay"

        /**
         * Starts the lock screen overlay activity.
         * Uses FLAG_ACTIVITY_NEW_TASK to launch from a non-activity context (like AccessibilityService).
         * Uses FLAG_ACTIVITY_CLEAR_TOP to bring to front if already running.
         */
        fun start(context: Context) {
            val intent = Intent(context, LockScreenOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun LockScreenOverlayContent(
    blockingStateManager: BlockingStateManager,
    onScheduleEnded: () -> Unit
) {
    var remainingTimeMillis by remember { mutableLongStateOf(blockingStateManager.getRemainingTimeMillis() ?: 0L) }

    // Update the countdown timer every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            val remaining = blockingStateManager.getRemainingTimeMillis()
            if (remaining == null || remaining <= 0 || blockingStateManager.shouldEndBlocking()) {
                onScheduleEnded()
                break
            }
            remainingTimeMillis = remaining
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.overlay_app_blocked_message),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.overlay_time_remaining_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatRemainingTime(remainingTimeMillis),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = stringResource(R.string.overlay_stay_focused_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Formats milliseconds into a human-readable countdown format (HH:MM:SS or MM:SS)
 */
private fun formatRemainingTime(remainingMillis: Long): String {
    if (remainingMillis <= 0) return "0:00"

    val totalSeconds = remainingMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
