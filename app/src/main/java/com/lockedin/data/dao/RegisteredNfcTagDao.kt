package com.lockedin.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lockedin.data.entity.RegisteredNfcTag
import kotlinx.coroutines.flow.Flow

@Dao
interface RegisteredNfcTagDao {
    @Query("SELECT * FROM registered_nfc_tag WHERE id = 1")
    fun getRegisteredTag(): Flow<RegisteredNfcTag?>

    @Query("SELECT * FROM registered_nfc_tag WHERE id = 1")
    suspend fun getRegisteredTagOnce(): RegisteredNfcTag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: RegisteredNfcTag)

    @Query("DELETE FROM registered_nfc_tag WHERE id = 1")
    suspend fun delete()

    @Query("SELECT tagId FROM registered_nfc_tag WHERE id = 1")
    suspend fun getRegisteredTagId(): String?
}
