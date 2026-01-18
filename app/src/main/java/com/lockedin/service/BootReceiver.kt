package com.lockedin.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver that handles device boot completion.
 * This ensures the accessibility service is prompted to restart if it was enabled before reboot.
 *
 * Note: Accessibility services are typically restarted automatically by the system
 * if they were enabled before reboot. This receiver serves as a fallback and
 * can be used for additional initialization tasks.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d(TAG, "Boot completed, checking accessibility service status")

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
