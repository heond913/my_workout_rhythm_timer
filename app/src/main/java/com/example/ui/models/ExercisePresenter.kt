package com.example.ui.models

import androidx.compose.ui.graphics.Color
import com.example.R

/**
 * Presenter model that isolates exercise metadata, themes, colors, and string resources.
 * This decouples business and presentation decisions from the @Composable view components.
 */
data class ExercisePresentation(
    val themeColor: Color,
    val bgColor: Color,
    val displayNameResId: Int,
    val encouragementResId: Int,
    val tip1ResId: Int,
    val tip2ResId: Int
) {
    companion object {
        val SQUAT = ExercisePresentation(
            themeColor = Color(0xFFE65100), // Orange
            bgColor = Color(0xFFFFECCC), // Orange background
            displayNameResId = R.string.preset_squat,
            encouragementResId = R.string.squat_encouragement,
            tip1ResId = R.string.squat_tip1,
            tip2ResId = R.string.squat_tip2
        )

        val LUNGE = ExercisePresentation(
            themeColor = Color(0xFF3F5F90),
            bgColor = Color(0xFFD7E3FF),
            displayNameResId = R.string.preset_lunge,
            encouragementResId = R.string.lunge_encouragement,
            tip1ResId = R.string.lunge_tip1,
            tip2ResId = R.string.lunge_tip2
        )

        val PLANK = ExercisePresentation(
            themeColor = Color(0xFF93000A),
            bgColor = Color(0xFFFFDAD6),
            displayNameResId = R.string.preset_plank,
            encouragementResId = R.string.plank_encouragement,
            tip1ResId = R.string.plank_tip1,
            tip2ResId = R.string.plank_tip2
        )

        val OTHER = ExercisePresentation(
            themeColor = Color(0xFF006A60), // Teal
            bgColor = Color(0xFFCCE8E3), // Teal background
            displayNameResId = R.string.preset_other,
            encouragementResId = R.string.other_encouragement,
            tip1ResId = R.string.other_tip1,
            tip2ResId = R.string.other_tip2
        )

        /**
         * Returns corresponding [ExercisePresentation] based on exercise type name.
         */
        fun from(name: String): ExercisePresentation {
            return when (name) {
                "스쿼트", "Squat" -> SQUAT
                "런지", "Lunge" -> LUNGE
                "플랭크", "Plank" -> PLANK
                else -> OTHER
            }
        }
    }
}

/**
 * Extension property to easily retrieve UI Presenter metadata from a String representation.
 */
val String.exercisePreset: ExercisePresentation
    get() = ExercisePresentation.from(this)
