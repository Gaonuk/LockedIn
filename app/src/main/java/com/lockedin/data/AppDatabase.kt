package com.lockedin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lockedin.data.dao.BlockedAppDao
import com.lockedin.data.dao.RegisteredNfcTagDao
import com.lockedin.data.dao.ScheduleDao
import com.lockedin.data.dao.SessionStatisticDao
import com.lockedin.data.dao.SetupStateDao
import com.lockedin.data.dao.StreakDao
import com.lockedin.data.entity.BlockedApp
import com.lockedin.data.entity.RegisteredNfcTag
import com.lockedin.data.entity.Schedule
import com.lockedin.data.entity.SessionStatistic
import com.lockedin.data.entity.SetupState
import com.lockedin.data.entity.StreakData

@Database(
    entities = [
        BlockedApp::class,
        Schedule::class,
        SessionStatistic::class,
        SetupState::class,
        StreakData::class,
        RegisteredNfcTag::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun sessionStatisticDao(): SessionStatisticDao
    abstract fun setupStateDao(): SetupStateDao
    abstract fun streakDao(): StreakDao
    abstract fun registeredNfcTagDao(): RegisteredNfcTagDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE session_statistics ADD COLUMN timeSavedSeconds INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS streak_data (
                        id INTEGER PRIMARY KEY NOT NULL,
                        currentStreak INTEGER NOT NULL DEFAULT 0,
                        longestStreak INTEGER NOT NULL DEFAULT 0,
                        lastCompletedSessionDate INTEGER,
                        lastMilestoneShown INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS registered_nfc_tag (
                        id INTEGER PRIMARY KEY NOT NULL,
                        tagId TEXT NOT NULL,
                        registeredAt INTEGER NOT NULL,
                        nickname TEXT
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
        }
    }
}
