package com.example.data

import com.example.R

/**
 * Type-safe enumeration for exercise preset categories.
 * Eliminates primitive obsession and magic hardcoded strings in DB conditions,
 * ViewModel state machines, and view layers.
 */
enum class ExerciseType(
    val key: String,
    val displayNameResId: Int
) {
    SQUAT("SQUAT", R.string.preset_squat),
    LUNGE("LUNGE", R.string.preset_lunge),
    PLANK("PLANK", R.string.preset_plank),
    OTHER("OTHER", R.string.preset_other);

    companion object {
        /**
         * Safely parses any database, state, or user input string (English/Korean/Key)
         * into a strongly-typed [ExerciseType]. Maintains complete backward compatibility.
         */
        fun fromString(value: String?): ExerciseType {
            if (value == null) return OTHER
            val trimmed = value.trim()
            return when {
                trimmed.equals("SQUAT", ignoreCase = true) ||
                        trimmed == "스쿼트" ||
                        trimmed.equals("Squat", ignoreCase = true) -> SQUAT

                trimmed.equals("LUNGE", ignoreCase = true) ||
                        trimmed == "런지" ||
                        trimmed.equals("Lunge", ignoreCase = true) -> LUNGE

                trimmed.equals("PLANK", ignoreCase = true) ||
                        trimmed == "플랭크" ||
                        trimmed.equals("Plank", ignoreCase = true) -> PLANK

                else -> OTHER
            }
        }
    }
}
