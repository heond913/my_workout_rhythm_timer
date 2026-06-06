package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<WorkoutRecord>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: WorkoutRecord)

    @Delete
    suspend fun deleteRecord(record: WorkoutRecord)

    @Delete
    suspend fun deleteRecords(records: List<WorkoutRecord>)

    @Query("DELETE FROM workout_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("SELECT * FROM workout_records WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getRecordsSince(sinceTimestamp: Long): Flow<List<WorkoutRecord>>
}
