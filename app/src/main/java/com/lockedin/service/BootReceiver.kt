package com.lockedin.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lockedin.data.repository.SessionStatisticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Broadcast receiver that handles device boot completion.
 *
 * On device restart:
 * - Clears any active blocking state (user must tap NFC to re-enable)
 * - Stops the blocking foreground service (clears notification)
 * - Cancels any active schedules
 *
 * Note: Accessibility services are typically restarted automatically by the system
 * if they were enabled before reboot.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d(TAG, "Boot completed, resetting blocking state")

            // Close any interrupted session statistics before clearing blocking state
            val sessionStatisticsRepository = SessionStatisticsRepository(context)
            scope.launch {
                sessionStatisticsRepository.closeInterruptedSessions()
                Log.d(TAG, "Closed any interrupted session statistics")
            }

            // Clear blocking state - user must tap NFC to start a new session
            val blockingStateManager = BlockingStateManager.getInstance(context)
            blockingStateManager.deactivateBlocking()
            Log.d(TAG, "Blocking state cleared on boot")

            // Stop the blocking foreground service (clears notification)
            BlockingForegroundService.stop(context)
            Log.d(TAG, "Blocking foreground service stopped")

            // Check if accessibility service is enabled
            // The system will automatically restart enabled accessibility services
            val isEnabled = AppBlockerAccessibilityService.isAccessibilityServiceEnabled(context)
            Log.d(TAG, "Accessibility service enabled: $isEnabled")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
