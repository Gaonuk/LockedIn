package com.lockedin.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lockedin.MainActivity
import com.lockedin.R
import com.lockedin.data.AppDatabase
import com.lockedin.data.entity.Schedule
import com.lockedin.data.repository.SessionStatisticsRepository
import com.lockedin.data.repository.StreakRepository
import com.lockedin.ui.dialog.StreakMilestoneDialogActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that maintains blocking state and shows a persistent notification
 * with countdown timer during an active focus session.
 */
class BlockingForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var countdownJob: Job? = null

    private val blockingStateManager: BlockingStateManager by lazy {
        BlockingStateManager.getInstance(applicationContext)
    }

    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(applicationContext)
    }

    private val sessionStatisticsRepository: SessionStatisticsRepository by lazy {
        SessionStatisticsRepository(applicationContext)
    }

    private val streakRepository: StreakRepository by lazy {
        StreakRepository(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_START_BLOCKING -> {
                val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                if (scheduleId != -1L) {
                    serviceScope.launch {
                        startBlockingWithScheduleId(scheduleId)
                    }
                } else {
                    serviceScope.launch {
                        startBlockingWithFirstSchedule()
                    }
                }
            }
            ACTION_STOP_BLOCKING -> {
                stopBlocking()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        countdownJob?.cancel()
        serviceScope.cancel()
    }

    private suspend fun startBlockingWithScheduleId(scheduleId: Long) {
        val schedule = database.scheduleDao().getSchedule(scheduleId)
        if (schedule != null) {
            activateBlockingSession(schedule)
        } else {
            Log.e(TAG, "Schedule not found with id: $scheduleId")
            stopSelf()
        }
    }

    private suspend fun startBlockingWithFirstSchedule() {
        val enabledSchedules = database.scheduleDao().getEnabledSchedules().first()
        if (enabledSchedules.isNotEmpty()) {
            activateBlockingSession(enabledSchedules.first())
        } else {
            Log.e(TAG, "No enabled schedules found")
            stopSelf()
        }
    }

    private suspend fun activateBlockingSession(schedule: Schedule) {
        val blockedAppsCount = database.blockedAppDao().getEnabledBlockedAppsCount()

        // Refresh blocked apps in the accessibility service to ensure the list is current
        AppBlockerAccessibilityService.refreshBlockedAppsIfNeeded()

        // Create a new session statistics record
        val sessionId = sessionStatisticsRepository.startSession()
        Log.d(TAG, "Created session statistics record with id: $sessionId")

        blockingStateManager.activateBlocking(schedule, blockedAppsCount, sessionId)

        // Show heads-up notification alert before starting foreground
        showSessionStartedAlert(schedule.name, blockedAppsCount)

        startForeground(NOTIFICATION_ID, createNotification())
        startCountdownUpdates()
        Log.d(TAG, "Blocking session started for schedule: ${schedule.name}, blocking $blockedAppsCount apps, sessionId: $sessionId")
    }

    private fun stopBlocking(wasCompletedSuccessfully: Boolean = false) {
        Log.d(TAG, "Stopping blocking session, wasCompleted: $wasCompletedSuccessfully")
        countdownJob?.cancel()

        // End the session statistics record
        val sessionId = blockingStateManager.deactivateBlocking()
        if (sessionId != null) {
            serviceScope.launch {
                sessionStatisticsRepository.endSession(sessionId, wasCompletedSuccessfully)
                Log.d(TAG, "Ended session statistics record with id: $sessionId")

                // Update streak if session completed successfully
                if (wasCompletedSuccessfully) {
                    val newMilestone = streakRepository.onSessionCompleted()
                    if (newMilestone != null) {
                        Log.d(TAG, "Streak milestone reached: $newMilestone")
                        showMilestoneCelebration(newMilestone)
                    }
                }
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun showMilestoneCelebration(milestone: Int) {
        val currentData = streakRepository.getStreakDataOnce()
        val intent = Intent(applicationContext, StreakMilestoneDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(StreakMilestoneDialogActivity.EXTRA_MILESTONE, milestone)
            putExtra(StreakMilestoneDialogActivity.EXTRA_CURRENT_STREAK, currentData.currentStreak)
        }
        applicationContext.startActivity(intent)
    }

    private fun startCountdownUpdates() {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            while (true) {
                if (blockingStateManager.shouldEndBlocking()) {
                    Log.d(TAG, "Schedule end time reached, stopping blocking")
                    stopBlocking(wasCompletedSuccessfully = true)
                    break
                }

                updateNotification()
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BlockingForegroundService::class.java).apply {
            action = ACTION_STOP_BLOCKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val scheduleName = blockingStateManager.activeScheduleName.value ?: "Focus Session"
        val remainingTime = blockingStateManager.getRemainingTimeMillis()
        val timeText = formatRemainingTime(remainingTime)
        val blockedAppsCount = blockingStateManager.blockedAppsCount.value
        val appsText = if (blockedAppsCount == 1) "1 app blocked" else "$blockedAppsCount apps blocked"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(scheduleName)
            .setContentText("$timeText remaining · $appsText")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_notification,
                "End Session",
                stopPendingIntent
            )
            .build()
    }

    private fun formatRemainingTime(remainingMillis: Long?): String {
        if (remainingMillis == null || remainingMillis <= 0) {
            return "0m"
        }

        val totalMinutes = remainingMillis / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    private fun showSessionStartedAlert(scheduleName: String, blockedAppsCount: Int) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val appsText = if (blockedAppsCount == 1) "1 app" else "$blockedAppsCount apps"

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("Focus Session Started")
            .setContentText("$scheduleName - Blocking $appsText")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setTimeoutAfter(5000) // Auto-dismiss after 5 seconds
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when a focus session is active"
                setShowBadge(false)
            }

            // High-priority alert channel for session start/end notifications
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Focus Session Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a focus session starts or ends"
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        private const val TAG = "BlockingForegroundSvc"
        private const val CHANNEL_ID = "blocking_service_channel"
        private const val NOTIFICATION_ID = 1002
        private const val ALERT_CHANNEL_ID = "focus_session_alerts"
        private const val ALERT_NOTIFICATION_ID = 1003
        private const val UPDATE_INTERVAL_MS = 60_000L // Update every minute

        const val ACTION_START_BLOCKING = "com.lockedin.action.START_BLOCKING"
        const val ACTION_STOP_BLOCKING = "com.lockedin.action.STOP_BLOCKING"
        const val EXTRA_SCHEDULE_ID = "schedule_id"

        /**
         * Start the blocking foreground service with the specified schedule.
         */
        fun start(context: Context, scheduleId: Long? = null) {
            val intent = Intent(context, BlockingForegroundService::class.java).apply {
                action = ACTION_START_BLOCKING
                scheduleId?.let { putExtra(EXTRA_SCHEDULE_ID, it) }
            }
            context.startForegroundService(intent)
        }

        /**
         * Stop the blocking foreground service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, BlockingForegroundService::class.java).apply {
                action = ACTION_STOP_BLOCKING
            }
            context.startService(intent)
        }
    }
}
