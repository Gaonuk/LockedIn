package com.lockedin.service

import android.content.Context
import android.util.Log
import com.lockedin.data.entity.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Manages the blocking state for the focus session.
 * This singleton provides centralized state management that can be accessed
 * by any component in the application.
 */
class BlockingStateManager private constructor(private val context: Context) {

    private val _isBlocking = MutableStateFlow(false)
    val isBlocking: StateFlow<Boolean> = _isBlocking.asStateFlow()

    private val _scheduleEndTimeMillis = MutableStateFlow<Long?>(null)
    val scheduleEndTimeMillis: StateFlow<Long?> = _scheduleEndTimeMillis.asStateFlow()

    private val _activeScheduleName = MutableStateFlow<String?>(null)
    val activeScheduleName: StateFlow<String?> = _activeScheduleName.asStateFlow()

    private val _sessionStartTimeMillis = MutableStateFlow<Long?>(null)
    val sessionStartTimeMillis: StateFlow<Long?> = _sessionStartTimeMillis.asStateFlow()

    private val _blockedAppsCount = MutableStateFlow(0)
    val blockedAppsCount: StateFlow<Int> = _blockedAppsCount.asStateFlow()

    /**
     * Activates blocking for a given schedule.
     * Calculates the end time based on the schedule's endTimeMinutes and today's date.
     *
     * @param schedule The schedule to activate
     * @param blockedAppsCount The number of currently enabled blocked apps
     */
    fun activateBlocking(schedule: Schedule, blockedAppsCount: Int = 0) {
        val now = System.currentTimeMillis()
        val endTimeMillis = calculateEndTimeMillis(schedule.endTimeMinutes)

        _isBlocking.value = true
        _scheduleEndTimeMillis.value = endTimeMillis
        _activeScheduleName.value = schedule.name
        _sessionStartTimeMillis.value = now
        _blockedAppsCount.value = blockedAppsCount

        Log.d(TAG, "Blocking activated for schedule: ${schedule.name}, ends at: $endTimeMillis, blocked apps: $blockedAppsCount")
    }

    /**
     * Deactivates blocking and clears all state.
     */
    fun deactivateBlocking() {
        _isBlocking.value = false
        _scheduleEndTimeMillis.value = null
        _activeScheduleName.value = null
        _sessionStartTimeMillis.value = null
        _blockedAppsCount.value = 0

        Log.d(TAG, "Blocking deactivated")
    }

    /**
     * Returns the remaining time in milliseconds, or null if not blocking.
     */
    fun getRemainingTimeMillis(): Long? {
        val endTime = _scheduleEndTimeMillis.value ?: return null
        val remaining = endTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }

    /**
     * Checks if blocking should end based on the schedule end time.
     * Returns true if the current time has passed the schedule end time.
     */
    fun shouldEndBlocking(): Boolean {
        val endTime = _scheduleEndTimeMillis.value ?: return false
        return System.currentTimeMillis() >= endTime
    }

    /**
     * Calculates the end time in milliseconds based on the schedule's end time in minutes.
     * The end time is set for today. If the end time has already passed today,
     * it will still return today's end time (blocking will end immediately).
     */
    private fun calculateEndTimeMillis(endTimeMinutes: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endTimeMinutes / 60)
            set(Calendar.MINUTE, endTimeMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    companion object {
        private const val TAG = "BlockingStateManager"

        @Volatile
        private var instance: BlockingStateManager? = null

        fun getInstance(context: Context): BlockingStateManager {
            return instance ?: synchronized(this) {
                instance ?: BlockingStateManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
