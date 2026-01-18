package com.lockedin.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lockedin.data.entity.SessionStatistic
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionStatisticDao {
    companion object {
        const val DEFAULT_TIME_SAVED_PER_ATTEMPT = 300L // 5 minutes in seconds
    }
    @Query("SELECT * FROM session_statistics ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionStatistic>>

    @Query("SELECT * FROM session_statistics WHERE id = :id")
    suspend fun getSession(id: Long): SessionStatistic?

    @Query("SELECT * FROM session_statistics WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): SessionStatistic?

    @Query("SELECT * FROM session_statistics WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<SessionStatistic>>

    @Query("SELECT SUM(blockedAttempts) FROM session_statistics")
    suspend fun getTotalBlockedAttempts(): Int?

    @Query("SELECT COUNT(*) FROM session_statistics WHERE wasCompletedSuccessfully = 1")
    suspend fun getCompletedSessionsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionStatistic): Long

    @Update
    suspend fun update(session: SessionStatistic)

    @Query("UPDATE session_statistics SET blockedAttempts = blockedAttempts + 1, timeSavedSeconds = timeSavedSeconds + :additionalSeconds WHERE id = :id")
    suspend fun incrementBlockedAttempts(id: Long, additionalSeconds: Long = DEFAULT_TIME_SAVED_PER_ATTEMPT)

    @Query("UPDATE session_statistics SET endTime = :endTime, wasCompletedSuccessfully = :wasCompleted WHERE id = :id")
    suspend fun endSession(id: Long, endTime: Long, wasCompleted: Boolean)

    @Query("SELECT SUM(timeSavedSeconds) FROM session_statistics")
    suspend fun getTotalTimeSavedSeconds(): Long?

    @Query("DELETE FROM session_statistics")
    suspend fun deleteAll()
}
