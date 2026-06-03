package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseName: String,     // e.g., "스쿼트", "런지", "플랭크"
    val reps: Int? = null,        // reps (e.g., 15)
    val sets: Int? = null,        // set count (e.g., 3)
    val weightKg: Double? = null,  // weight in kg (e.g., 60.5)
    val durationSeconds: Int? = null, // duration (e.g., 60 seconds)
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Int = 3,          // workout intensity/difficulty (1-5)
    val note: String = ""         // optional user note
) : Serializable
