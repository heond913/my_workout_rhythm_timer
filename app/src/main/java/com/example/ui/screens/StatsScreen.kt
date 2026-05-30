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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            text = "전체 통계 대시보드",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = tealActive,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Text(
            text = "지속적으로 이어지고 있는 건강 기록을 통계로 확인하세요",
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
                        text = if (streak > 0) "${streak}일 연속 목표 도달!" else "연속 운동 흐름이 끊겼어요",
                        color = if (streak > 0) fireOrange else charcoalDark,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (streak > 0) "매일 꾸준한 노력의 결실입니다. 계속 박자를 이어가세요!" else "오늘 운동을 완료해서 1일차 흐름을 시작해보세요!",
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
                        text = "총 완료 세션",
                        fontSize = 11.sp,
                        color = secondaryGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${totalSessions}회",
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
                        text = "누적 횟수",
                        fontSize = 11.sp,
                        color = secondaryGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${totalReps}회",
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
                        text = "누적 수행 시간",
                        fontSize = 11.sp,
                        color = secondaryGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    val textStr = if (totalMins > 0) "${totalMins}분" else "${totalSecsRemainder}초"
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

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Custom Canvas rendering 7-Day Consistency Bar Graph
        Text(
            text = "최근 7일간의 운동 일관성",
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
            Column(modifier = Modifier.padding(18.dp)) {
                val sdf = SimpleDateFormat("M/d", Locale.getDefault())
                val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                
                val last7Days = (0..6).map { offset ->
                    Calendar.getInstance().apply {
                        set(2026, Calendar.MAY, 29)
                        add(Calendar.DAY_OF_YEAR, -offset)
                    }
                }.reversed()

                val workoutCounts7Days = last7Days.map { day ->
                    val dayStr = dayKeyFormat.format(day.time)
                    val count = workoutRecords.count { record ->
                        val recCal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                        dayKeyFormat.format(recCal.time) == dayStr
                    }
                    Pair(sdf.format(day.time), count)
                }

                val maxCountInLast7Days = workoutCounts7Days.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    workoutCounts7Days.forEach { (label, count) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Bar column canvas with clean pastel background column guidelines
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth(0.4f)
                                    .height(100.dp)
                            ) {
                                val barHeight = (count.toFloat() / maxCountInLast7Days.toFloat()) * size.height
                                
                                // Draw background column guide with soft green-grey
                                drawRoundRect(
                                    color = Color(0xFFDAE5E1),
                                    topLeft = Offset(0f, 0f),
                                    size = Size(size.width, size.height),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )

                                // Draw filled gradient column in Vibrant Teal
                                if (count > 0) {
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(tealActive, Color(0xFFCCE8E3))
                                        ),
                                        topLeft = Offset(0f, size.height - barHeight),
                                        size = Size(size.width, barHeight),
                                        cornerRadius = CornerRadius(12f, 12f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = if (count > 0) tealActive else secondaryGray,
                                fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "${count}회",
                                fontSize = 9.sp,
                                color = if (count > 0) charcoalDark else secondaryGray.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Exercise type allocation percentages
        Text(
            text = "피트니스 종목별 비중 분석",
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
                                    .background(Color(0xFF006A60)) // Squat Teal
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
                                    .background(Color(0xFF3F4947)) // Other Charcoal Grey
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown list of Exercises with unified category tones including 기타
                val breakdown = listOf(
                    Triple("스쿼트 (Squats)", squatCount, Color(0xFF006A60)),
                    Triple("런지 (Lunges)", lungeCount, Color(0xFF3F5F90)),
                    Triple("플랭크 (Planks)", plankCount, Color(0xFF93000A)),
                    Triple("기타 (Others)", otherCount, Color(0xFF3F4947))
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
                            text = "${count}회 기록",
                            color = tealActive,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
