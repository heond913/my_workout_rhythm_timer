package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkoutRecord
import com.example.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import androidx.compose.ui.res.stringResource
import com.example.R
import java.util.*

@Composable
fun StatsScreen(viewModel: WorkoutViewModel, workoutRecords: List<WorkoutRecord>) {
    // Color Theme - Vibrant Palette
    val tealActive = Color(0xFF006A60)
    val darkBg = Color(0xFFFBFDF9) // Light theme background
    val cardSurface = Color(0xFFF2F7F5) // Mint-grey card surfaces
    val secondaryGray = Color(0xFF3F4947) // Slate grey text
    val charcoalDark = Color(0xFF191C1B) // Deep dark text
    val borderColor = Color(0xFFDCE5E2) // Soft borders
    val fireOrange = Color(0xFFE05220) // Vibrant orange with solid white contrast

    val streak = viewModel.getWorkoutStreak(workoutRecords)
    val scrollState = rememberScrollState()

    // Aggregate statistics
    val totalSessions = workoutRecords.size
    val squatCount = workoutRecords.count { it.exerciseName.contains("스쿼트") }
    val lungeCount = workoutRecords.count { it.exerciseName.contains("런지") }
    val plankCount = workoutRecords.count { it.exerciseName.contains("플랭크") }
    val otherCount = workoutRecords.count {
        val name = it.exerciseName
        !name.contains("스쿼트") && !name.contains("런지") && !name.contains("플랭크")
    }

    val totalReps = workoutRecords.sumOf { it.reps ?: 0 }
    val totalSets = workoutRecords.sumOf { it.sets ?: 0 }
    val maxWeight = workoutRecords.mapNotNull { it.weightKg }.maxOrNull() ?: 0.0
    val totalSeconds = workoutRecords.sumOf { it.durationSeconds ?: 0 }
    val totalMins = totalSeconds / 60
    val totalSecsRemainder = totalSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stats Header Frame
        Text(
            text = stringResource(id = R.string.title_stats),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = tealActive,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Text(
            text = stringResource(id = R.string.subtitle_stats),
            fontSize = 12.sp,
            color = secondaryGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. Streak burning badge
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (streak > 0) Color(0xFFE6F3F1) else Color(0xFFFCE8E6)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = if (streak > 0) fireOrange else Color(0xFFF1B0A9),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            color = if (streak > 0) Color(0xFFFFDAD6) else Color(0xFFF5D6D6),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak Fire Map",
                        tint = if (streak > 0) fireOrange else secondaryGray,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (streak > 0) {
                            stringResource(id = R.string.streak_reached_format, streak)
                        } else {
                            stringResource(id = R.string.streak_broken)
                        },
                        color = if (streak > 0) fireOrange else charcoalDark,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (streak > 0) {
                            stringResource(id = R.string.streak_reached_sub)
                        } else {
                            stringResource(id = R.string.streak_broken_sub)
                        },
                        color = secondaryGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Summary telemetry metric columns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card A: Sessions
            Card(
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Total sessions",
                        tint = tealActive,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.label_total_sessions),
                        fontSize = 11.sp,
                        color = secondaryGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(id = R.string.sessions_count_format, totalSessions),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = tealActive,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Card B: Reps
            Card(
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = "Total repetitions",
                        tint = tealActive,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.label_accumulated_reps),
                        fontSize = 11.sp,
                        color = secondaryGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(id = R.string.workout_count_format, totalReps),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = tealActive,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Card C: Duration
            Card(
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Total duration",
                        tint = tealActive,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.label_accumulated_duration),
                        fontSize = 11.sp,
                        color = secondaryGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    val textStr = if (totalMins > 0) {
                        stringResource(id = R.string.minutes_format, totalMins)
                    } else {
                        stringResource(id = R.string.seconds_format, totalSecsRemainder)
                    }
                    Text(
                        text = textStr,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = tealActive,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card D: Sets
            Card(
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Total sets",
                        tint = tealActive,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.label_sets),
                        fontSize = 11.sp,
                        color = secondaryGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$totalSets ${stringResource(id = R.string.unit_sets)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = tealActive,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Card E: Max Weight
            Card(
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = "Max Weight",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.label_weight),
                        fontSize = 11.sp,
                        color = secondaryGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${if (maxWeight % 1.0 == 0.0) maxWeight.toInt().toString() else maxWeight} ${stringResource(id = R.string.unit_weight)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE65100),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Custom Canvas rendering Interactive Bar Graph Dashboard (Daily/Weekly/Monthly)
        var selectedPeriod by remember { mutableStateOf(0) } // 0: Daily, 1: Weekly, 2: Monthly
        var selectedMetric by remember { mutableStateOf(0) } // 0: Count, 1: Duration

        Text(
            text = stringResource(id = R.string.label_overall_habit),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = charcoalDark,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Period tab selection: [ Daily ] [ Weekly ] [ Monthly ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val periods = listOf(
                        Pair(0, R.string.tab_daily),
                        Pair(1, R.string.tab_weekly),
                        Pair(2, R.string.tab_monthly)
                    )
                    periods.forEach { (index, stringRes) ->
                        val isSelected = selectedPeriod == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) tealActive else Color(0xFFE6F3F1))
                                .clickable { selectedPeriod = index }
                                .padding(vertical = 10.dp)
                                .testTag("period_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = stringRes),
                                color = if (isSelected) Color.White else tealActive,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Metric selection: [ Workout Count ] [ Total Duration ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val metrics = listOf(
                        Pair(0, R.string.stat_metric_count),
                        Pair(1, R.string.stat_metric_duration)
                    )
                    metrics.forEach { (index, stringRes) ->
                        val isSelected = selectedMetric == index
                        val highlightColor = if (index == 0) tealActive else Color(0xFFE65100)
                        val lightHighlight = if (index == 0) Color(0xFFE6F3F1) else Color(0xFFFFECCC)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) highlightColor else Color.Transparent, RoundedCornerShape(10.dp))
                                .background(if (isSelected) lightHighlight else Color(0xFFF2F7F5))
                                .clickable { selectedMetric = index }
                                .padding(vertical = 10.dp)
                                .testTag("metric_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = stringRes),
                                color = if (isSelected) highlightColor else secondaryGray,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Retrieve data according to selected Period & Metric
                val chartData: List<Triple<String, Int, Int>> = when (selectedPeriod) {
                    0 -> {
                        // Daily: last 7 days back from today
                        val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                        val displayFormat = SimpleDateFormat("M/d", Locale.getDefault())
                        (0..6).map { offset ->
                            val dayCal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, -offset)
                            }
                            val dateStr = dayKeyFormat.format(dayCal.time)
                            val dayWorkouts = workoutRecords.filter { record ->
                                val recCal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                                dayKeyFormat.format(recCal.time) == dateStr
                            }
                            val count = dayWorkouts.size
                            val seconds = dayWorkouts.sumOf { it.durationSeconds ?: 0 }
                            val minutes = seconds / 60
                            Triple(displayFormat.format(dayCal.time), count, minutes)
                        }.reversed()
                    }
                    1 -> {
                        // Weekly Data is rolling 5 weeks back
                        (0..4).map { wOffset ->
                            val startCal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, -(wOffset * 7 + 6))
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val endCal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, -(wOffset * 7))
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }
                            
                            val label = if (wOffset == 0) {
                                stringResource(id = R.string.this_week)
                            } else {
                                stringResource(id = R.string.weeks_ago_format, wOffset)
                            }
                            
                            val weekWorkouts = workoutRecords.filter { record ->
                                record.timestamp in startCal.timeInMillis..endCal.timeInMillis
                            }
                            val count = weekWorkouts.size
                            val seconds = weekWorkouts.sumOf { it.durationSeconds ?: 0 }
                            val minutes = seconds / 60
                            
                            Triple(label, count, minutes)
                        }.reversed()
                    }
                    else -> {
                        // Monthly Data is last 6 months back
                        (0..5).map { mOffset ->
                            val startCal = Calendar.getInstance().apply {
                                add(Calendar.MONTH, -mOffset)
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val endCal = (startCal.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }
                            
                            val label = if (mOffset == 0) {
                                stringResource(id = R.string.current_month_short)
                            } else {
                                val monthFormat = SimpleDateFormat("M월", Locale.getDefault())
                                val monthFormatEn = SimpleDateFormat("MMM", Locale.getDefault())
                                val locale = Locale.getDefault()
                                if (locale.language == "ko") {
                                    monthFormat.format(startCal.time)
                                } else {
                                    monthFormatEn.format(startCal.time)
                                }
                            }
                            
                            val monthWorkouts = workoutRecords.filter { record ->
                                record.timestamp in startCal.timeInMillis..endCal.timeInMillis
                            }
                            val count = monthWorkouts.size
                            val seconds = monthWorkouts.sumOf { it.durationSeconds ?: 0 }
                            val minutes = seconds / 60
                            
                            Triple(label, count, minutes)
                        }.reversed()
                    }
                }

                val hasAnyData = chartData.any { if (selectedMetric == 0) it.second > 0 else it.third > 0 }
                val maxVal = chartData.maxOfOrNull { if (selectedMetric == 0) it.second else it.third }?.coerceAtLeast(1) ?: 1
                val valueUnit = if (selectedMetric == 0) stringResource(id = R.string.unit_times) else stringResource(id = R.string.unit_minutes)

                if (!hasAnyData) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = secondaryGray.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.no_data_for_period),
                                fontSize = 12.sp,
                                color = secondaryGray.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chartData.forEach { (label, count, minutes) ->
                            val value = if (selectedMetric == 0) count else minutes
                            val isValueActive = value > 0
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth(0.4f)
                                        .height(95.dp)
                                ) {
                                    val barHeight = (value.toFloat() / maxVal.toFloat()) * size.height
                                    
                                    // Draw background column guide with soft green-grey
                                    drawRoundRect(
                                        color = Color(0xFFDAE5E1),
                                        topLeft = Offset(0f, 0f),
                                        size = Size(size.width, size.height),
                                        cornerRadius = CornerRadius(12f, 12f)
                                    )

                                    if (isValueActive) {
                                        val brush = if (selectedMetric == 0) {
                                            Brush.verticalGradient(
                                                colors = listOf(tealActive, Color(0xFFCCE8E3))
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                colors = listOf(Color(0xFFE65100), Color(0xFFFFECCC))
                                            )
                                        }
                                        drawRoundRect(
                                            brush = brush,
                                            topLeft = Offset(0f, size.height - barHeight),
                                            size = Size(size.width, barHeight),
                                            cornerRadius = CornerRadius(12f, 12f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = if (isValueActive) (if (selectedMetric == 0) tealActive else Color(0xFFE65100)) else secondaryGray,
                                    fontWeight = if (isValueActive) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$value$valueUnit",
                                    fontSize = 9.sp,
                                    color = if (isValueActive) charcoalDark else secondaryGray.copy(alpha = 0.6f),
                                    fontWeight = if (isValueActive) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Exercise type allocation percentages
        Text(
            text = stringResource(id = R.string.title_exercise_proportion),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = charcoalDark,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(bottom = 30.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val totalComp = squatCount + lungeCount + plankCount + otherCount
                val sum = if (totalComp > 0) totalComp.toFloat() else 1f

                val squatPercent = (squatCount.toFloat() / sum * 100).toInt()
                val lungePercent = (lungeCount.toFloat() / sum * 100).toInt()
                val plankPercent = (plankCount.toFloat() / sum * 100).toInt()
                val otherPercent = (otherCount.toFloat() / sum * 100).toInt()

                // Row representing percentage distribution bar using exercise category themed colors!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(Color(0xFFDAE5E1), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (squatPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(squatPercent.toFloat())
                                    .background(Color(0xFFE65100)) // Squat Orange
                            )
                        }
                        if (lungePercent > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(lungePercent.toFloat())
                                    .background(Color(0xFF3F5F90)) // Lunge Indigo
                            )
                        }
                        if (plankPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(plankPercent.toFloat())
                                    .background(Color(0xFF93000A)) // Plank Crimson
                            )
                        }
                        if (otherPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(otherPercent.toFloat())
                                    .background(Color(0xFF006A60)) // Other Teal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown list of Exercises with unified category tones including 기타
                val squatLabel = stringResource(id = R.string.preset_squat)
                val lungeLabel = stringResource(id = R.string.preset_lunge)
                val plankLabel = stringResource(id = R.string.preset_plank)
                val otherLabel = stringResource(id = R.string.preset_other)

                val breakdown = listOf(
                    Triple("$squatLabel (Squats)", squatCount, Color(0xFFE65100)),
                    Triple("$lungeLabel (Lunges)", lungeCount, Color(0xFF3F5F90)),
                    Triple("$plankLabel (Planks)", plankCount, Color(0xFF93000A)),
                    Triple("$otherLabel (Others)", otherCount, Color(0xFF006A60))
                )

                breakdown.forEach { (label, count, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = label, color = charcoalDark, fontSize = 12.sp)
                        }
                        Text(
                            text = stringResource(id = R.string.records_count_format, count),
                            color = tealActive,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Premium Social Share card for stats
        var showShareDialog by remember { mutableStateOf(false) }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F3F1)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, tealActive, RoundedCornerShape(16.dp))
                .clickable { showShareDialog = true }
                .padding(4.dp)
                .testTag("stats_social_share_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(tealActive, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share overall stats",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.social_share_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = charcoalDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(id = R.string.social_share_subtitle),
                        fontSize = 11.sp,
                        color = secondaryGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        if (showShareDialog) {
            SocialShareDialog(
                shareData = ShareData.GeneralStats(
                    streak = streak,
                    totalSessions = totalSessions,
                    totalSets = totalSets,
                    maxWeight = maxWeight,
                    totalMinutes = totalMins,
                    totalSeconds = totalSecsRemainder
                ),
                onDismiss = { showShareDialog = false }
            )
        }
    }
}
