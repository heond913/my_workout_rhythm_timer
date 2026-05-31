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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.WorkoutRecord
import com.example.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: WorkoutViewModel, workoutRecords: List<WorkoutRecord>) {
    val uiState by viewModel.uiState.collectAsState()
    val currentMonthCal = uiState.calendarYearMonth
    var selectedDayCal by remember { mutableStateOf(Calendar.getInstance()) }
    var recordToDelete by remember { mutableStateOf<WorkoutRecord?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Keep selectedDayCal in sync with currentMonthCal's month and year when month changes
    LaunchedEffect(currentMonthCal) {
        val selMonth = selectedDayCal.get(Calendar.MONTH)
        val selYear = selectedDayCal.get(Calendar.YEAR)
        val curMonth = currentMonthCal.get(Calendar.MONTH)
        val curYear = currentMonthCal.get(Calendar.YEAR)

        if (selMonth != curMonth || selYear != curYear) {
            val today = Calendar.getInstance()
            if (curMonth == today.get(Calendar.MONTH) && curYear == today.get(Calendar.YEAR)) {
                selectedDayCal = today
            } else {
                selectedDayCal = (currentMonthCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }
    }

    // Color Theme - Vibrant Palette
    val tealActive = Color(0xFF006A60)
    val darkBg = Color(0xFFFBFDF9) // Light theme background
    val cardSurface = Color(0xFFF2F7F5) // Mint-grey card surfaces
    val secondaryGray = Color(0xFF3F4947) // Slate grey text
    val charcoalDark = Color(0xFF191C1B) // Deep dark text
    val borderColor = Color(0xFFDCE5E2) // Soft borders

    // Calculate grid for the current month representation
    val tempCal = currentMonthCal.clone() as Calendar
    tempCal.set(Calendar.DAY_OF_MONTH, 1)
    val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayOfWeekOffset = tempCal.get(Calendar.DAY_OF_WEEK) - 1 // 0-based index for Sun-Sat

    // Days Grid Array construction
    val daysList = mutableListOf<Calendar?>()
    for (i in 0 until dayOfWeekOffset) {
        daysList.add(null) // Empty days at beginning of month grid
    }
    for (day in 1..maxDays) {
        val entry = currentMonthCal.clone() as Calendar
        entry.set(Calendar.DAY_OF_MONTH, day)
        daysList.add(entry)
    }

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
            val chunks = daysList.chunked(7)
            chunks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    week.forEach { dayCal ->
                        if (dayCal == null) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val isSelected = SimpleDateFormat("yyyyMMdd", LocalLocale.current.platformLocale).format(dayCal.time) ==
                                    SimpleDateFormat("yyyyMMdd", LocalLocale.current.platformLocale).format(selectedDayCal.time)
                            
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
                                    .clickable { selectedDayCal = dayCal }
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
                            .clickable { showDeleteAllDialog = true }
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
                            // Circular icon wrapper - categorized to exercises!
                            val (taskIconBg, taskIconAccent) = when (workout.exerciseName) {
                                "스쿼트" -> Pair(Color(0xFFFFECCC), Color(0xFFE65100))
                                "런지" -> Pair(Color(0xFFD7E3FF), Color(0xFF3F5F90))
                                "플랭크" -> Pair(Color(0xFFFFDAD6), Color(0xFF93000A))
                                else -> Pair(Color(0xFFCCE8E3), Color(0xFF006A60))
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(taskIconBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = workout.exerciseName,
                                    tint = taskIconAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details text
                            Column(modifier = Modifier.weight(1f)) {
                                val workoutExerciseDisplay = when (workout.exerciseName) {
                                    "스쿼트" -> stringResource(id = R.string.preset_squat)
                                    "런지" -> stringResource(id = R.string.preset_lunge)
                                    "플랭크" -> stringResource(id = R.string.preset_plank)
                                    else -> workout.exerciseName
                                }
                                Text(
                                    text = workoutExerciseDisplay,
                                    fontWeight = FontWeight.Bold,
                                    color = charcoalDark,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row {
                                    if (workout.reps != null && workout.reps > 0) {
                                        Text(
                                            text = stringResource(id = R.string.workout_count_format, workout.reps),
                                            fontSize = 12.sp,
                                            color = charcoalDark,
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
                                IconButton(
                                    onClick = { recordToDelete = workout },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(id = R.string.desc_delete_record),
                                        tint = Color(0xFF93000A), // Red Plank outline crimson
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

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordToDelete?.let { viewModel.deleteWorkoutRecord(it) }
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF93000A))
                ) {
                    Text(stringResource(id = R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
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
            onDismissRequest = { showDeleteAllDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWorkoutRecords(selectedDayWorkouts)
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF93000A))
                ) {
                    Text(stringResource(id = R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
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
}
