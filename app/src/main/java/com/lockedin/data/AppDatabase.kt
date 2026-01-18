package com.lockedin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lockedin.data.dao.BlockedAppDao
import com.lockedin.data.dao.ScheduleDao
import com.lockedin.data.dao.SessionStatisticDao
import com.lockedin.data.dao.SetupStateDao
import com.lockedin.data.entity.BlockedApp
import com.lockedin.data.entity.Schedule
import com.lockedin.data.entity.SessionStatistic
import com.lockedin.data.entity.SetupState

@Database(
    entities = [
        BlockedApp::class,
        Schedule::class,
        SessionStatistic::class,
        SetupState::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun sessionStatisticDao(): SessionStatisticDao
    abstract fun setupStateDao(): SetupStateDao

    companion object {
        private const val DATABASE_NAME = "lockedin_database"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS setup_state (
                        id INTEGER PRIMARY KEY NOT NULL,
                        isSetupCompleted INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

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
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
