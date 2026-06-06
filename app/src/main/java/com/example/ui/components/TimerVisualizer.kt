package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.models.exercisePreset

/**
 * Highly optimized, self-contained circular timer visualizer that decouples high-frequency ticking states
 * (like remainingSeconds) from the parent composable using deferred lambda providers (`() -> T`).
 *
 * This prevents the entire screen (with custom lists, sliders, volume controllers) from recomposing every second.
 */
@Composable
fun TimerVisualizer(
    remainingSecondsProvider: () -> Int,
    restRemainingSecondsProvider: () -> Int,
    isRestingProvider: () -> Boolean,
    restTotalSecondsProvider: () -> Int,
    totalTargetSecondsProvider: () -> Int,
    timerRunningProvider: () -> Boolean,
    timerPresetTypeProvider: () -> String,
    workoutCountProvider: () -> Int,
    rhythmIntervalSecondsProvider: () -> Int,
    modifier: Modifier = Modifier
) {
    // Read states inside the Composable body of TimerVisualizer, confining recomposition to this scope
    val remaining = remainingSecondsProvider()
    val restRemaining = restRemainingSecondsProvider()
    val isResting = isRestingProvider()
    val restTotal = restTotalSecondsProvider()
    val totalSeconds = totalTargetSecondsProvider()
    val isRunning = timerRunningProvider()
    val presetType = timerPresetTypeProvider()
    val workoutCount = workoutCountProvider()
    val interval = rhythmIntervalSecondsProvider()

    // Resolve color theme based on active preset
    val activePreset = presetType.exercisePreset
    val activePresetColor = activePreset.themeColor
    val activePresetBgColor = activePreset.bgColor

    val charcoalDark = Color(0xFF191C1B)
    val secondaryGray = Color(0xFF3F4947)

    // Calculate progress fraction defensively
    val progressFraction = if (isResting) {
        if (restTotal > 0) restRemaining.toFloat() / restTotal.toFloat() else 0f
    } else {
        if (totalSeconds > 0) remaining.toFloat() / totalSeconds.toFloat() else 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        label = "TimerProgress"
    )

    Box(
        modifier = modifier
            .size(240.dp)
            .drawBehind {
                // Draw clean background ring matching active preset color theme
                drawArc(
                    color = if (isResting) Color(0xFFE0E0E0) else activePresetBgColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
                // Draw progress arc in active preset custom color
                drawArc(
                    color = if (isResting) Color(0xFF00796B) else activePresetColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = stringResource(id = R.string.desc_timer_clock),
                tint = if (isResting) Color(0xFF00796B) else activePresetColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Giant time numbers in high-contrast charcoal
            val displayRemaining = if (isResting) restRemaining else remaining
            val minutesString = String.format(java.util.Locale.getDefault(), "%02d", displayRemaining / 60)
            val secondsString = String.format(java.util.Locale.getDefault(), "%02d", displayRemaining % 60)

            Text(
                text = "$minutesString:$secondsString",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = charcoalDark
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(id = R.string.workout_count_format, workoutCount),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isResting) Color(0xFF00796B) else activePresetColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            if (isRunning) {
                val isPreparing = remaining > totalSeconds
                Text(
                    text = if (isResting) {
                        stringResource(id = R.string.rest_timer_status)
                    } else if (isPreparing) {
                        stringResource(id = R.string.timer_preparing)
                    } else if (interval > 0) {
                        stringResource(id = R.string.rhythm_interval_notifying, interval)
                    } else {
                        stringResource(id = R.string.timer_in_progress)
                    },
                    color = if (isResting) Color(0xFF00796B) else activePresetColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = stringResource(id = R.string.ready),
                    color = secondaryGray,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
