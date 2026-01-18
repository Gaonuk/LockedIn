package com.lockedin.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.lockedin.MainActivity
import com.lockedin.R
import com.lockedin.data.AppDatabase
import com.lockedin.data.repository.SessionStatisticsRepository
import com.lockedin.ui.overlay.LockScreenOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Accessibility Service that monitors app launches and blocks configured apps
 * when a blocking session is active.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var blockedPackages: Set<String> = emptySet()
    private var lastDetectedPackage: String? = null

    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(applicationContext)
    }

    private val blockingStateManager: BlockingStateManager by lazy {
        BlockingStateManager.getInstance(applicationContext)
    }

    private val sessionStatisticsRepository: SessionStatisticsRepository by lazy {
        SessionStatisticsRepository(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }

        instance = this
        _isServiceRunning.value = true

        // Perform immediate synchronous load of blocked apps to avoid race condition
        serviceScope.launch {
            val initialBlockedApps = database.blockedAppDao().getEnabledBlockedApps().first()
            blockedPackages = initialBlockedApps.map { it.packageName }.toSet()
            Log.d(TAG, "Initial load: ${blockedPackages.size} blocked apps")
        }

        // Also set up continuous Flow collection for updates
        loadBlockedApps()
        startForegroundServiceNotification()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Ignore system UI and our own app
            if (packageName == "com.android.systemui" ||
                packageName == applicationContext.packageName) {
                return
            }

            // Avoid processing the same package repeatedly
            if (packageName == lastDetectedPackage) {
                return
            }
            lastDetectedPackage = packageName

            Log.d(TAG, "Detected foreground app: $packageName")

            // Notify listeners about the detected app
            _detectedPackage.value = packageName

            // Check if the app is blocked and blocking is active
            if (isAppBlocked(packageName) && blockingStateManager.isBlocking.value) {
                Log.d(TAG, "Blocked app detected while blocking active: $packageName")
                _blockedAppDetected.value = packageName
                handleBlockedApp(packageName)
            }
        }
    }

    /**
     * Handles a blocked app detection by:
     * 1. Recording the blocked attempt in session statistics
     * 2. Sending the blocked app to background (using HOME action)
     * 3. Launching the lock screen overlay
     */
    private fun handleBlockedApp(packageName: String) {
        Log.d(TAG, "Handling blocked app: $packageName")

        // Record the blocked attempt in session statistics
        val sessionId = blockingStateManager.currentSessionId.value
        if (sessionId != null) {
            serviceScope.launch {
                sessionStatisticsRepository.recordBlockedAttempt(sessionId)
                Log.d(TAG, "Recorded blocked attempt for session: $sessionId")
            }
        }

        // Send the blocked app to background by going to home screen
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Launch the lock screen overlay activity
        LockScreenOverlayActivity.start(applicationContext)

        // Reset lastDetectedPackage so we can re-detect if user tries again
        lastDetectedPackage = null

        Log.d(TAG, "Lock screen overlay launched for blocked app: $packageName")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        instance = null
        _isServiceRunning.value = false
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        // Return START_STICKY to ensure service restarts if killed
        return START_STICKY
    }

    private fun loadBlockedApps() {
        serviceScope.launch {
            database.blockedAppDao().getEnabledBlockedApps().collect { blockedApps ->
                blockedPackages = blockedApps.map { it.packageName }.toSet()
                Log.d(TAG, "Loaded ${blockedPackages.size} blocked apps")
            }
        }
    }

    private fun isAppBlocked(packageName: String): Boolean {
        return blockedPackages.contains(packageName)
    }

    private fun startForegroundServiceNotification() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun refreshBlockedApps() {
        loadBlockedApps()
    }

    companion object {
        private const val TAG = "AppBlockerService"
        private const val CHANNEL_ID = "app_blocker_service_channel"
        private const val NOTIFICATION_ID = 1001

        private var instance: AppBlockerAccessibilityService? = null

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _detectedPackage = MutableStateFlow<String?>(null)
        val detectedPackage: StateFlow<String?> = _detectedPackage.asStateFlow()

        private val _blockedAppDetected = MutableStateFlow<String?>(null)
        val blockedAppDetected: StateFlow<String?> = _blockedAppDetected.asStateFlow()

        fun getInstance(): AppBlockerAccessibilityService? = instance

        fun clearBlockedAppDetection() {
            _blockedAppDetected.value = null
        }

        /**
         * Refresh blocked apps in the running service instance.
         * Call this before activating a blocking session to ensure the list is current.
         */
        fun refreshBlockedAppsIfNeeded() {
            instance?.refreshBlockedApps()
        }

        /**
         * Check if the accessibility service is enabled for this app
         */
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val serviceName = "${context.packageName}/${AppBlockerAccessibilityService::class.java.canonicalName}"

            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(serviceName, ignoreCase = true)) {
                    return true
                }
            }

            return false
        }
    }
}
