package com.example.data

import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val workoutDao: WorkoutDao) {
    val allRecords: Flow<List<WorkoutRecord>> = workoutDao.getAllRecords()

    fun getRecordsSince(since: Long): Flow<List<WorkoutRecord>> = workoutDao.getRecordsSince(since)

    suspend fun insert(record: WorkoutRecord) {
        workoutDao.insertRecord(record)
    }

    suspend fun delete(record: WorkoutRecord) {
        workoutDao.deleteRecord(record)
    }

    suspend fun deleteById(id: Int) {
        workoutDao.deleteRecordById(id)
    }
}
