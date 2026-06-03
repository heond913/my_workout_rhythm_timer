package com.example.data

import java.io.Serializable

data class RoutineStep(
    val exerciseName: String,         // "스쿼트", "런지", "플랭크", "기타"
    val durationSeconds: Int,         // Target duration for the exercise (e.g. 60 seconds)
    val rhythmIntervalSeconds: Int,   // The rhythm pace in seconds (e.g. 4 seconds)
    val restSeconds: Int              // Rest duration in seconds after this exercise (e.g. 15 seconds)
) : Serializable {
    fun serialize(): String {
        return "$exerciseName,$durationSeconds,$rhythmIntervalSeconds,$restSeconds"
    }

    companion object {
        fun deserialize(str: String): RoutineStep? {
            val parts = str.split(",")
            if (parts.size < 4) return null
            return RoutineStep(
                exerciseName = parts[0],
                durationSeconds = parts[1].toIntOrNull() ?: 60,
                rhythmIntervalSeconds = parts[2].toIntOrNull() ?: 4,
                restSeconds = parts[3].toIntOrNull() ?: 15
            )
        }
    }
}

data class CustomRoutine(
    val id: String,                  // Unique identifier or name
    val name: String,                // Name of the custom routine
    val steps: List<RoutineStep>,    // List of steps
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {
    fun serializeSteps(): String {
        return steps.joinToString(";") { it.serialize() }
    }

    companion object {
        fun deserializeSteps(str: String): List<RoutineStep> {
            if (str.isBlank()) return emptyList()
            return str.split(";").mapNotNull { RoutineStep.deserialize(it) }
        }
    }
}
