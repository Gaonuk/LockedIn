package com.lockedin.data.repository

import android.content.Context
import com.lockedin.data.AppDatabase
import com.lockedin.data.dao.SessionStatisticDao
import com.lockedin.data.entity.SessionStatistic
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing session statistics data.
 * Provides a clean API for tracking and retrieving focus session metrics.
 */
class SessionStatisticsRepository(context: Context) {

    private val sessionStatisticDao: SessionStatisticDao =
        AppDatabase.getInstance(context).sessionStatisticDao()

    /**
     * Starts a new session and returns its ID.
     */
    suspend fun startSession(): Long {
        val session = SessionStatistic(
            startTime = System.currentTimeMillis()
        )
        return sessionStatisticDao.insert(session)
    }

    /**
     * Ends the session with the given ID.
     * @param sessionId The ID of the session to end
     * @param wasCompleted Whether the session completed successfully (reached scheduled end time)
     */
    suspend fun endSession(sessionId: Long, wasCompleted: Boolean) {
        sessionStatisticDao.endSession(
            id = sessionId,
            endTime = System.currentTimeMillis(),
            wasCompleted = wasCompleted
        )
    }

    /**
     * Records a blocked app attempt for the session.
     * Increments the blocked attempts counter and adds estimated time saved.
     * @param sessionId The ID of the active session
     * @param timeSavedSeconds Additional time saved in seconds (default: 5 minutes)
     */
    suspend fun recordBlockedAttempt(
        sessionId: Long,
        timeSavedSeconds: Long = SessionStatisticDao.DEFAULT_TIME_SAVED_PER_ATTEMPT
    ) {
        sessionStatisticDao.incrementBlockedAttempts(sessionId, timeSavedSeconds)
    }

    /**
     * Gets the currently active session (one with no end time).
     */
    suspend fun getActiveSession(): SessionStatistic? {
        return sessionStatisticDao.getActiveSession()
    }

    /**
     * Gets a specific session by ID.
     */
    suspend fun getSession(sessionId: Long): SessionStatistic? {
        return sessionStatisticDao.getSession(sessionId)
    }

    /**
     * Gets all sessions as a Flow.
     */
    fun getAllSessions(): Flow<List<SessionStatistic>> {
        return sessionStatisticDao.getAllSessions()
    }

    /**
     * Gets the total number of blocked attempts across all sessions.
     */
    suspend fun getTotalBlockedAttempts(): Int {
        return sessionStatisticDao.getTotalBlockedAttempts() ?: 0
    }

    /**
     * Gets the total time saved in seconds across all sessions.
     */
    suspend fun getTotalTimeSavedSeconds(): Long {
        return sessionStatisticDao.getTotalTimeSavedSeconds() ?: 0L
    }

    /**
     * Gets the count of successfully completed sessions.
     */
    suspend fun getCompletedSessionsCount(): Int {
        return sessionStatisticDao.getCompletedSessionsCount()
    }

    /**
     * Gets sessions within a date range.
     */
    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<SessionStatistic>> {
        return sessionStatisticDao.getSessionsInRange(startTime, endTime)
    }

    /**
     * Closes any active (interrupted) sessions.
     * This is called on app startup or boot to handle sessions that were
     * interrupted due to phone restart, crash, or other interruption.
     * The session is marked as not completed successfully since it was interrupted.
     */
    suspend fun closeInterruptedSessions() {
        val activeSession = sessionStatisticDao.getActiveSession()
        if (activeSession != null) {
            sessionStatisticDao.endSession(
                id = activeSession.id,
                endTime = System.currentTimeMillis(),
                wasCompleted = false
            )
        }
    }
}
