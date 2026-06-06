package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun DrawExerciseIcon(exerciseName: String, iconColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 3.dp.toPx()
        
        when (exerciseName) {
            "스쿼트", "Squat" -> {
                // Head
                drawCircle(
                    color = iconColor,
                    radius = w * 0.10f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.18f)
                )
                // Torso
                val torsoStart = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.32f)
                val hip = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.48f)
                drawLine(
                    color = iconColor,
                    start = torsoStart,
                    end = hip,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Thigh
                val knee = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.48f)
                drawLine(
                    color = iconColor,
                    start = hip,
                    end = knee,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Calf/Shin
                val ankle = androidx.compose.ui.geometry.Offset(w * 0.48f, h * 0.76f)
                drawLine(
                    color = iconColor,
                    start = knee,
                    end = ankle,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Foot
                drawLine(
                    color = iconColor,
                    start = ankle,
                    end = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.76f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Arms reaching forward
                drawLine(
                    color = iconColor,
                    start = torsoStart,
                    end = androidx.compose.ui.geometry.Offset(w * 0.80f, h * 0.32f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            "런지", "Lunge" -> {
                // Head
                drawCircle(
                    color = iconColor,
                    radius = w * 0.10f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.18f)
                )
                // Torso
                val torsoStart = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.32f)
                val hip = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.52f)
                drawLine(
                    color = iconColor,
                    start = torsoStart,
                    end = hip,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Back Arm (resting on lower-back/hip)
                val elbow = androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.44f)
                drawLine(
                    color = iconColor,
                    start = torsoStart,
                    end = elbow,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = elbow,
                    end = androidx.compose.ui.geometry.Offset(w * 0.48f, h * 0.50f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Front Leg
                val frontKnee = androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.56f)
                val frontAnkle = androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.78f)
                drawLine(
                    color = iconColor,
                    start = hip,
                    end = frontKnee,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = frontKnee,
                    end = frontAnkle,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = frontAnkle,
                    end = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.78f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Back Leg
                val backKnee = androidx.compose.ui.geometry.Offset(w * 0.34f, h * 0.68f)
                val backAnkle = androidx.compose.ui.geometry.Offset(w * 0.24f, h * 0.78f)
                drawLine(
                    color = iconColor,
                    start = hip,
                    end = backKnee,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = backKnee,
                    end = backAnkle,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = backAnkle,
                    end = androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.78f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            "플랭크", "Plank" -> {
                // Head
                drawCircle(
                    color = iconColor,
                    radius = w * 0.10f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.32f)
                )
                // Straight plank alignment torso to ankle
                val shoulder = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.45f)
                val ankle = androidx.compose.ui.geometry.Offset(w * 0.20f, h * 0.55f)
                drawLine(
                    color = iconColor,
                    start = shoulder,
                    end = ankle,
                    strokeWidth = strokeWidth + 0.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Vertical forearm support arm
                val elbow = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.75f)
                val hand = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.75f)
                drawLine(
                    color = iconColor,
                    start = shoulder,
                    end = elbow,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = elbow,
                    end = hand,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Toes supporting the feet on floor
                drawLine(
                    color = iconColor,
                    start = ankle,
                    end = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.75f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            else -> {
                // Standard Bar Handle
                drawLine(
                    color = iconColor,
                    start = androidx.compose.ui.geometry.Offset(w * 0.32f, h * 0.50f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.50f),
                    strokeWidth = strokeWidth * 1.5f,
                    cap = StrokeCap.Round
                )
                // Left Dumbbell Weight
                drawRoundRect(
                    color = iconColor,
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.28f),
                    size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.44f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f, h * 0.04f)
                )
                // Right Dumbbell Weight
                drawRoundRect(
                    color = iconColor,
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.28f),
                    size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.44f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f, h * 0.04f)
                )
            }
        }
    }
}
