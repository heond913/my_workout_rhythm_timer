package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkoutRecord
import com.example.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(viewModel: WorkoutViewModel, workoutRecords: List<WorkoutRecord>) {
    val uiState by viewModel.uiState.collectAsState()
    val currentMonthCal = uiState.calendarYearMonth
    var selectedDayCal by remember { mutableStateOf(Calendar.getInstance()) }
    var recordToDelete by remember { mutableStateOf<WorkoutRecord?>(null) }

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
    val daySelectedFormatter = SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault())
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
            text = "운동 완주 달력",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = tealActive,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Text(
            text = "초록색으로 표시된 날짜는 타이머 또는 수동 기록이 저장된 날입니다",
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
                    contentDescription = "이전달",
                    tint = tealActive
                )
            }

            val monthSdf = SimpleDateFormat("yyyy년 M월", Locale.getDefault())
            Text(
                text = monthSdf.format(currentMonthCal.time),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = charcoalDark
            )

            IconButton(onClick = { viewModel.changeMonth(1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음달",
                    tint = tealActive
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Weekday Label Grid Row (Sun - Sat)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val weekdays = listOf("일", "월", "화", "수", "목", "금", "토")
            weekdays.forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (dayLabel == "일") Color(0xFF93000A) else if (dayLabel == "토") Color(0xFF3F5F90) else secondaryGray
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
                            val isSelected = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(dayCal.time) ==
                                    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(selectedDayCal.time)
                            
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
                text = "${daySelectedFormatter.format(selectedDayCal.time)} 운동 기록",
                color = charcoalDark,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            val countStr = if (selectedDayWorkouts.isNotEmpty()) "${selectedDayWorkouts.size}개 완료" else "기록 없음"
            Text(
                text = countStr,
                color = tealActive,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
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
                        text = "이날 기록된 운동이 없습니다",
                        color = charcoalDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "타이머를 완주하거나 수동으로 로그를 기록하여 달력을 채워보세요!",
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
                                Text(
                                    text = workout.exerciseName,
                                    fontWeight = FontWeight.Bold,
                                    color = charcoalDark,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row {
                                    if (workout.reps != null && workout.reps > 0) {
                                        Text(
                                            text = "${workout.reps}회",
                                            fontSize = 12.sp,
                                            color = charcoalDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (workout.durationSeconds != null && workout.durationSeconds > 0) {
                                        val m = workout.durationSeconds / 60
                                        val s = workout.durationSeconds % 60
                                        val displayStr = if (m > 0) "${m}분 ${s}초" else "${s}초"
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
                                        contentDescription = "기록 삭제",
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
                    Text("삭제", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("취소", color = secondaryGray)
                }
            },
            title = {
                Text("기록 삭제", fontWeight = FontWeight.Bold, color = charcoalDark)
            },
            text = {
                Text("정말 이 운동 기록을 삭제하시겠습니까?", color = charcoalDark)
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
