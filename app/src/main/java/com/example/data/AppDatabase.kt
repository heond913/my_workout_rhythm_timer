package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WorkoutRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safely migrate from version 1 to 2 by adding non-null columns rating and note.
                // We use try-catch block to ignore errors if the table/columns already exist,
                // ensuring a seamless migration sequence.
                try {
                    db.execSQL("ALTER TABLE workout_records ADD COLUMN rating INTEGER NOT NULL DEFAULT 3")
                } catch (e: Exception) {
                    // Column already exists or table doesn't match
                }
                try {
                    db.execSQL("ALTER TABLE workout_records ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Column already exists or table doesn't match
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_rhythm_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
