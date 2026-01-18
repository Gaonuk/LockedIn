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

    private fun activateBlockingSession(schedule: Schedule) {
        blockingStateManager.activateBlocking(schedule)
        startForeground(NOTIFICATION_ID, createNotification())
        startCountdownUpdates()
        Log.d(TAG, "Blocking session started for schedule: ${schedule.name}")
    }

    private fun stopBlocking() {
        Log.d(TAG, "Stopping blocking session")
        countdownJob?.cancel()
        blockingStateManager.deactivateBlocking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startCountdownUpdates() {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            while (true) {
                if (blockingStateManager.shouldEndBlocking()) {
                    Log.d(TAG, "Schedule end time reached, stopping blocking")
                    stopBlocking()
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(scheduleName)
            .setContentText("Time remaining: $timeText")
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

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "BlockingForegroundSvc"
        private const val CHANNEL_ID = "blocking_service_channel"
        private const val NOTIFICATION_ID = 1002
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
