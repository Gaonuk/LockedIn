package com.lockedin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lockedin.data.dao.BlockedAppDao
import com.lockedin.data.dao.ScheduleDao
import com.lockedin.data.dao.SessionStatisticDao
import com.lockedin.data.entity.BlockedApp
import com.lockedin.data.entity.Schedule
import com.lockedin.data.entity.SessionStatistic

@Database(
    entities = [
        BlockedApp::class,
        Schedule::class,
        SessionStatistic::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun sessionStatisticDao(): SessionStatisticDao

    companion object {
        private const val DATABASE_NAME = "lockedin_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}
