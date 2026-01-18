package com.lockedin.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lockedin.data.entity.SetupState
import kotlinx.coroutines.flow.Flow

@Dao
interface SetupStateDao {
    @Query("SELECT * FROM setup_state WHERE id = 1")
    suspend fun getSetupState(): SetupState?

    @Query("SELECT * FROM setup_state WHERE id = 1")
    fun getSetupStateFlow(): Flow<SetupState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setupState: SetupState)

    @Query("SELECT isSetupCompleted FROM setup_state WHERE id = 1")
    suspend fun isSetupCompleted(): Boolean?
}
