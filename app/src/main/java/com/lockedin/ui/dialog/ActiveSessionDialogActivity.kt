package com.lockedin.ui.dialog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lockedin.R
import com.lockedin.service.BlockingForegroundService
import com.lockedin.service.BlockingStateManager
import com.lockedin.ui.theme.LockedInTheme
import kotlinx.coroutines.delay

/**
 * Activity that shows a dialog when NFC is tapped while a blocking session is active.
 * Displays remaining time and offers options to extend or end the session.
 */
class ActiveSessionDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockingStateManager = BlockingStateManager.getInstance(this)

        enableEdgeToEdge()
        setContent {
            LockedInTheme {
                Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)) {
                    ActiveSessionDialog(
                        blockingStateManager = blockingStateManager,
                        onExtend = { minutes ->
                            blockingStateManager.extendSession(minutes)
                            finish()
                        },
                        onEndNow = {
                            blockingStateManager.requestEndConfirmation()
                            NfcConfirmationDialogActivity.start(this@ActiveSessionDialogActivity)
                            finish()
                        },
                        onDismiss = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        /**
         * Starts the active session dialog activity.
         */
        fun start(context: Context) {
            val intent = Intent(context, ActiveSessionDialogActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun ActiveSessionDialog(
    blockingStateManager: BlockingStateManager,
    onExtend: (minutes: Int) -> Unit,
    onEndNow: () -> Unit,
    onDismiss: () -> Unit
) {
    var remainingTimeMillis by remember {
        mutableLongStateOf(blockingStateManager.getRemainingTimeMillis() ?: 0L)
    }
    var showExtendOptions by remember { mutableStateOf(false) }

    // Update the countdown timer every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            val remaining = blockingStateManager.getRemainingTimeMillis()
            if (remaining == null || remaining <= 0) {
                onDismiss()
                break
            }
            remainingTimeMillis = remaining
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.active_session_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.active_session_time_remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatRemainingTime(remainingTimeMillis),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (showExtendOptions) {
                    ExtendOptionsSection(
                        onExtend = onExtend,
                        onBack = { showExtendOptions = false }
                    )
                } else {
                    MainOptionsSection(
                        onExtendClick = { showExtendOptions = true },
                        onEndNow = onEndNow
                    )
                }
            }
        }
    }
}

@Composable
private fun MainOptionsSection(
    onExtendClick: () -> Unit,
    onEndNow: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onExtendClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.active_session_extend),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onEndNow,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = stringResource(R.string.active_session_end_now),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ExtendOptionsSection(
    onExtend: (minutes: Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.active_session_extend_by),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ExtendButton(
                label = stringResource(R.string.active_session_extend_30m),
                onClick = { onExtend(30) }
            )
            ExtendButton(
                label = stringResource(R.string.active_session_extend_1h),
                onClick = { onExtend(60) }
            )
            ExtendButton(
                label = stringResource(R.string.active_session_extend_2h),
                onClick = { onExtend(120) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.active_session_back),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ExtendButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
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
