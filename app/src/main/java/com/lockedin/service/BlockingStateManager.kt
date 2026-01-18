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

    private val _awaitingEndConfirmation = MutableStateFlow(false)
    val awaitingEndConfirmation: StateFlow<Boolean> = _awaitingEndConfirmation.asStateFlow()

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    /**
     * Activates blocking for a given schedule.
     * Calculates the end time based on the schedule's duration (end - start time).
     * If activated outside schedule hours, uses the schedule's duration from now.
     *
     * @param schedule The schedule to activate
     * @param blockedAppsCount The number of currently enabled blocked apps
     * @param sessionId The ID of the session statistics record for this session
     */
    fun activateBlocking(schedule: Schedule, blockedAppsCount: Int = 0, sessionId: Long? = null) {
        val now = System.currentTimeMillis()
        val endTimeMillis = calculateEndTimeMillis(schedule.startTimeMinutes, schedule.endTimeMinutes)

        _isBlocking.value = true
        _scheduleEndTimeMillis.value = endTimeMillis
        _activeScheduleName.value = schedule.name
        _sessionStartTimeMillis.value = now
        _blockedAppsCount.value = blockedAppsCount
        _currentSessionId.value = sessionId

        Log.d(TAG, "Blocking activated for schedule: ${schedule.name}, ends at: $endTimeMillis, blocked apps: $blockedAppsCount, sessionId: $sessionId")
    }

    /**
     * Deactivates blocking and clears all state.
     * Returns the session ID that was active (if any) for recording statistics.
     */
    fun deactivateBlocking(): Long? {
        val sessionId = _currentSessionId.value
        _isBlocking.value = false
        _scheduleEndTimeMillis.value = null
        _activeScheduleName.value = null
        _sessionStartTimeMillis.value = null
        _blockedAppsCount.value = 0
        _awaitingEndConfirmation.value = false
        _currentSessionId.value = null

        Log.d(TAG, "Blocking deactivated, sessionId was: $sessionId")
        return sessionId
    }

    /**
     * Sets the state to await NFC confirmation to end the session.
     * The user must tap NFC again to confirm ending the blocking session.
     */
    fun requestEndConfirmation() {
        _awaitingEndConfirmation.value = true
        Log.d(TAG, "Awaiting NFC confirmation to end session")
    }

    /**
     * Cancels the pending end confirmation request.
     */
    fun cancelEndConfirmation() {
        _awaitingEndConfirmation.value = false
        Log.d(TAG, "End confirmation cancelled")
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
     * Extends the current session by the specified number of minutes.
     * Only works if a blocking session is currently active.
     *
     * @param minutes The number of minutes to add to the current session
     */
    fun extendSession(minutes: Int) {
        val currentEndTime = _scheduleEndTimeMillis.value ?: return
        val extensionMillis = minutes * 60 * 1000L
        val newEndTime = currentEndTime + extensionMillis

        _scheduleEndTimeMillis.value = newEndTime

        Log.d(TAG, "Session extended by $minutes minutes, new end time: $newEndTime")
    }

    /**
     * Calculates the end time in milliseconds based on the schedule's duration.
     * Uses the schedule's duration (endTimeMinutes - startTimeMinutes) from now.
     * This ensures sessions work regardless of when the NFC tag is tapped.
     *
     * @param startTimeMinutes The schedule start time in minutes from midnight
     * @param endTimeMinutes The schedule end time in minutes from midnight
     * @return The session end time in milliseconds
     */
    private fun calculateEndTimeMillis(startTimeMinutes: Int, endTimeMinutes: Int): Long {
        // Calculate the schedule duration in minutes
        val durationMinutes = if (endTimeMinutes > startTimeMinutes) {
            endTimeMinutes - startTimeMinutes
        } else {
            // Handle overnight schedules (e.g., 22:00 to 06:00)
            (24 * 60 - startTimeMinutes) + endTimeMinutes
        }

        // Convert to milliseconds and add to current time
        val durationMillis = durationMinutes * 60 * 1000L
        val endTime = System.currentTimeMillis() + durationMillis

        Log.d(TAG, "Session duration: $durationMinutes minutes, ends at: $endTime")
        return endTime
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
