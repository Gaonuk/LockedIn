package com.lockedin.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lockedin.data.entity.StreakData
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak_data WHERE id = 1")
    fun getStreakData(): Flow<StreakData?>

    @Query("SELECT * FROM streak_data WHERE id = 1")
    suspend fun getStreakDataOnce(): StreakData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(streakData: StreakData)

    @Query("UPDATE streak_data SET currentStreak = :currentStreak, longestStreak = :longestStreak, lastCompletedSessionDate = :lastCompletedDate WHERE id = 1")
    suspend fun updateStreak(currentStreak: Int, longestStreak: Int, lastCompletedDate: Long)

    @Query("UPDATE streak_data SET lastMilestoneShown = :milestone WHERE id = 1")
    suspend fun updateLastMilestoneShown(milestone: Int)

    @Query("UPDATE streak_data SET currentStreak = 0 WHERE id = 1")
    suspend fun resetCurrentStreak()
}
