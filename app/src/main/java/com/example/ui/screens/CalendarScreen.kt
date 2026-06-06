package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DrawExerciseIcon
import com.example.ui.models.exercisePreset
import com.example.data.WorkoutRecord
import com.example.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: WorkoutViewModel, workoutRecords: List<WorkoutRecord>) {
    val uiState by viewModel.uiState.collectAsState()
    val currentMonthCal = uiState.calendarYearMonth
    val selectedDayCal = uiState.selectedCalendarDay

    val recordToDelete = uiState.recordToDelete
    val workoutToShare = uiState.workoutToShare
    val showDeleteAllDialog = uiState.showDeleteAllDialog

    // Color Theme - Vibrant Palette
    val tealActive = Color(0xFF006A60)
    val darkBg = Color(0xFFFBFDF9) // Light theme background
    val cardSurface = Color(0xFFF2F7F5) // Mint-grey card surfaces
    val secondaryGray = Color(0xFF3F4947) // Slate grey text
    val charcoalDark = Color(0xFF191C1B) // Deep dark text
    val borderColor = Color(0xFFDCE5E2) // Soft borders

    // Selected day formatted string helper
    val daySelectedFormat = stringResource(id = R.string.day_selected_format)
    val daySelectedFormatter = remember(daySelectedFormat) { SimpleDateFormat(daySelectedFormat, Locale.getDefault()) }
    val selectedDayWorkouts = viewModel.getWorkoutsForDay(selectedDayCal, workoutRecords)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title Screen block
        Text(
            text = stringResource(id = R.string.title_calendar),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = tealActive,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Text(
            text = stringResource(id = R.string.subtitle_calendar),
            fontSize = 11.sp,
            color = secondaryGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Month Selection Panel Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE6F3F1), RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.changeMonth(-1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(id = R.string.desc_prev_month),
                    tint = tealActive
                )
            }

            val monthFormatString = stringResource(id = R.string.month_format)
            val monthSdf = remember(monthFormatString) { SimpleDateFormat(monthFormatString, Locale.getDefault()) }
            Text(
                text = monthSdf.format(currentMonthCal.time),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = charcoalDark
            )

            IconButton(onClick = { viewModel.changeMonth(1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(id = R.string.desc_next_month),
                    tint = tealActive
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Weekday Label Grid Row (Sun - Sat)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val weekdays = listOf(
                stringResource(id = R.string.sun),
                stringResource(id = R.string.mon),
                stringResource(id = R.string.tue),
                stringResource(id = R.string.wed),
                stringResource(id = R.string.thu),
                stringResource(id = R.string.fri),
                stringResource(id = R.string.sat)
            )
            weekdays.forEachIndexed { index, dayLabel ->
                Text(
                    text = dayLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (index == 0) Color(0xFF93000A) else if (index == 6) Color(0xFF3F5F90) else secondaryGray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid cells
        Column(modifier = Modifier.fillMaxWidth()) {
            val chunks = uiState.calendarGrid
            chunks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    week.forEach { dayCal ->
                        if (dayCal == null) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val isSelected = SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(dayCal.time) ==
                                    SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(selectedDayCal.time)
                            
                            val hasWorkout = viewModel.getWorkoutsForDay(dayCal, workoutRecords).isNotEmpty()

                            val calDayText = dayCal.get(Calendar.DAY_OF_MONTH).toString()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(3.dp)
                                    .background(
                                        color = when {
                                            isSelected -> tealActive
                                            hasWorkout -> Color(0xFFCCE8E3) // VibrantTealSoftBg
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = when {
                                            isSelected -> tealActive
                                            hasWorkout -> Color(0xFFCCE8E3)
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.selectCalendarDay(dayCal) }
                                    .testTag("calendar_day_${calDayText}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = calDayText,
                                        color = when {
                                            isSelected -> Color.White
                                            hasWorkout -> tealActive
                                            else -> charcoalDark
                                        },
                                        fontWeight = if (isSelected || hasWorkout) FontWeight.Black else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                    if (hasWorkout) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(5.dp)
                                                .background(if (isSelected) Color.White else tealActive, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Handle incomplete final week chunk
                    if (week.size < 7) {
                        for (i in 0 until (7 - week.size)) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Date label
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.selected_day_record_title, daySelectedFormatter.format(selectedDayCal.time)),
                color = charcoalDark,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val countStr = if (selectedDayWorkouts.isNotEmpty()) {
                    stringResource(id = R.string.completed_count_format, selectedDayWorkouts.size)
                } else {
                    stringResource(id = R.string.no_records)
                }
                Text(
                    text = countStr,
                    color = tealActive,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (selectedDayWorkouts.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.delete_all),
                        color = Color(0xFF93000A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.setShowDeleteAllDialog(true) }
                            .padding(4.dp)
                    )
                }
            }
        }

        // List display below calendar
        if (selectedDayWorkouts.isEmpty()) {
            // Friendly Empty State layout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = "No workouts recorded",
                        tint = secondaryGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.empty_calendar_msg),
                        color = charcoalDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.empty_calendar_sub),
                        color = secondaryGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                selectedDayWorkouts.forEach { workout ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Isolate preset presentation elements using the UI presenter model
                            val preset = workout.exerciseName.exercisePreset
                            val taskIconBg = preset.bgColor
                            val taskIconAccent = preset.themeColor

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(taskIconBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                DrawExerciseIcon(
                                    exerciseName = workout.exerciseName,
                                    iconColor = taskIconAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details text
                            Column(modifier = Modifier.weight(1f)) {
                                val workoutExerciseDisplay = if (workout.exerciseName == "스쿼트" || workout.exerciseName == "런지" || workout.exerciseName == "플랭크") {
                                    stringResource(id = preset.displayNameResId)
                                } else {
                                    workout.exerciseName
                                }
                                Text(
                                    text = workoutExerciseDisplay,
                                    fontWeight = FontWeight.Bold,
                                    color = charcoalDark,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (workout.reps != null && workout.reps > 0) {
                                        Text(
                                            text = stringResource(id = R.string.workout_count_format, workout.reps),
                                            fontSize = 12.sp,
                                            color = charcoalDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (workout.sets != null && workout.sets > 0) {
                                        Text(
                                            text = "${workout.sets}${stringResource(id = R.string.unit_sets)}",
                                            fontSize = 12.sp,
                                            color = charcoalDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (workout.weightKg != null && workout.weightKg > 0.0) {
                                        Text(
                                            text = "${if (workout.weightKg % 1.0 == 0.0) workout.weightKg.toInt().toString() else workout.weightKg}${stringResource(id = R.string.unit_weight)}",
                                            fontSize = 12.sp,
                                            color = Color(0xFFE65100),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (workout.durationSeconds != null && workout.durationSeconds > 0) {
                                        val m = workout.durationSeconds / 60
                                        val s = workout.durationSeconds % 60
                                        val displayStr = if (m > 0) {
                                            stringResource(id = R.string.minutes_seconds_format, m, s)
                                        } else {
                                            stringResource(id = R.string.seconds_format, s)
                                        }
                                        Text(
                                            text = displayStr,
                                            fontSize = 12.sp,
                                            color = tealActive,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (workout.note.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = workout.note,
                                        fontSize = 11.sp,
                                        color = secondaryGray,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            // Star indicator + deletion helper
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row {
                                    repeat(workout.rating) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = tealActive,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.setWorkoutToShare(workout) },
                                        modifier = Modifier.size(24.dp).testTag("share_record_btn_${workout.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share on social media",
                                            tint = tealActive,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.setRecordToDelete(workout) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(id = R.string.desc_delete_record),
                                            tint = Color(0xFF93000A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.setRecordToDelete(null) },
            confirmButton = {
                TextButton(
                     onClick = {
                         recordToDelete.let { viewModel.deleteWorkoutRecord(it) }
                         viewModel.setRecordToDelete(null)
                     },
                     colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF93000A))
                ) {
                    Text(stringResource(id = R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setRecordToDelete(null) }) {
                    Text(stringResource(id = R.string.cancel), color = secondaryGray)
                }
            },
            title = {
                Text(stringResource(id = R.string.desc_delete_record), fontWeight = FontWeight.Bold, color = charcoalDark)
            },
            text = {
                Text(stringResource(id = R.string.delete_confirm_msg), color = charcoalDark)
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowDeleteAllDialog(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWorkoutRecords(selectedDayWorkouts)
                        viewModel.setShowDeleteAllDialog(false)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF93000A))
                ) {
                    Text(stringResource(id = R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowDeleteAllDialog(false) }) {
                    Text(stringResource(id = R.string.cancel), color = secondaryGray)
                }
            },
            title = {
                Text(stringResource(id = R.string.delete_all), fontWeight = FontWeight.Bold, color = charcoalDark)
            },
            text = {
                Text(stringResource(id = R.string.delete_all_confirm_msg), color = charcoalDark)
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (workoutToShare != null) {
        SocialShareDialog(
            shareData = ShareData.SingleWorkout(workoutToShare),
            onDismiss = { viewModel.setWorkoutToShare(null) }
        )
    }
}
