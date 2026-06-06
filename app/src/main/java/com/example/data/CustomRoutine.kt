package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class RoutineStep(
    val exerciseName: String,         // "스쿼트", "런지", "플랭크", "기타"
    val durationSeconds: Int,         // Target duration for the exercise (e.g. 60 seconds)
    val rhythmIntervalSeconds: Int,   // The rhythm pace in seconds (e.g. 4 seconds)
    val restSeconds: Int              // Rest duration in seconds after this exercise (e.g. 15 seconds)
) : Serializable {
    fun serialize(): String {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(RoutineStep::class.java)
            adapter.toJson(this)
        } catch (e: Exception) {
            "$exerciseName,$durationSeconds,$rhythmIntervalSeconds,$restSeconds"
        }
    }

    companion object {
        fun deserialize(str: String): RoutineStep? {
            if (str.isBlank()) return null
            if (str.trim().startsWith("{")) {
                return try {
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val adapter = moshi.adapter(RoutineStep::class.java)
                    adapter.fromJson(str)
                } catch (e: Exception) {
                    null
                }
            }
            // Fallback for old delimiter format
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

@JsonClass(generateAdapter = true)
data class CustomRoutine(
    val id: String,                  // Unique identifier or name
    val name: String,                // Name of the custom routine
    val steps: List<RoutineStep>,    // List of steps
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {
    fun serializeSteps(): String {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, RoutineStep::class.java)
            val adapter = moshi.adapter<List<RoutineStep>>(type)
            adapter.toJson(steps)
        } catch (e: Exception) {
            steps.joinToString(";") { it.serialize() }
        }
    }

    companion object {
        fun deserializeSteps(str: String): List<RoutineStep> {
            if (str.isBlank()) return emptyList()
            if (str.trim().startsWith("[")) {
                return try {
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val type = Types.newParameterizedType(List::class.java, RoutineStep::class.java)
                    val adapter = moshi.adapter<List<RoutineStep>>(type)
                    adapter.fromJson(str) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            // Fallback for old semicolon format
            return str.split(";").mapNotNull { RoutineStep.deserialize(it) }
        }
    }
}
