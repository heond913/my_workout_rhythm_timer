package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(viewModel: WorkoutViewModel) {
    val selectedExercise = viewModel.inputExerciseName
    val repsVal = viewModel.inputReps
    val secsVal = viewModel.inputDurationSeconds
    val rating = viewModel.inputRating
    val note = viewModel.inputNote

    // Color Theme - Vibrant Palette
    val tealActive = Color(0xFF006A60)
    val darkBg = Color(0xFFFBFDF9) // Light theme background
    val cardSurface = Color(0xFFF2F7F5) // Mint-grey card surfaces
    val secondaryGray = Color(0xFF3F4947) // Slate grey text
    val charcoalDark = Color(0xFF191C1B) // Deep dark text
    val borderColor = Color(0xFFDCE5E2) // Soft borders

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App title header
        Text(
            text = "운동 완료 기록",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = tealActive,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Text(
            text = "완료한 세션의 세부 수치와 컨디션을 간편하게 제출하세요",
            fontSize = 12.sp,
            color = secondaryGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Exercise selector cards (Squat, Lunge, Plank, Custom)
        Text(
            text = "운동 종목 선택",
            fontSize = 14.sp,
            color = charcoalDark,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val exercises = listOf("스쿼트", "런지", "플랭크", "기타")
            exercises.forEach { exe ->
                val isSelected = selectedExercise == exe
                
                // Exercise-specific colorful styling matching Vibrant Palette HTML
                val (itemBg, itemBorder, itemAccent) = when (exe) {
                    "스쿼트" -> Triple(Color(0xFFCCE8E3), Color(0xFF006A60), Color(0xFF006A60))
                    "런지" -> Triple(Color(0xFFD7E3FF), Color(0xFF3F5F90), Color(0xFF3F5F90))
                    "플랭크" -> Triple(Color(0xFFFFDAD6), Color(0xFF93000A), Color(0xFF93000A))
                    else -> Triple(Color(0xFFE6F3F1), Color(0xFF3F4947), Color(0xFF3F4947))
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) itemBg else cardSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (isSelected) itemBorder else borderColor,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            viewModel.inputExerciseName = exe
                            // Set intelligent defaults for easy flow
                            if (exe == "스쿼트" || exe == "런지") {
                                if (viewModel.inputReps.isEmpty()) viewModel.inputReps = "15"
                            } else if (exe == "플랭크") {
                                if (viewModel.inputDurationSeconds.isEmpty()) viewModel.inputDurationSeconds = "60"
                            }
                        },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = exe,
                            tint = if (isSelected) itemAccent else secondaryGray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exe,
                            color = if (isSelected) charcoalDark else secondaryGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Custom exercise name field (only shown if "기타" is selected)
        AnimatedVisibility(visible = selectedExercise == "기타") {
            var customName by remember { mutableStateOf("") }
            OutlinedTextField(
                value = customName,
                onValueChange = {
                    customName = it
                    viewModel.inputExerciseName = it
                },
                label = { Text("운동 이름 직접 입력", color = secondaryGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tealActive,
                    unfocusedBorderColor = borderColor,
                    focusedLabelColor = tealActive,
                    unfocusedLabelColor = secondaryGray,
                    focusedTextColor = charcoalDark,
                    unfocusedTextColor = charcoalDark,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        // Repetitions input (Squats or Lunges highlight)
        Card(
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "수행 횟수 (Reps)",
                    color = if (selectedExercise == "스쿼트" || selectedExercise == "런지") tealActive else charcoalDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "회 단위로 수행한 횟수를 입력하세요",
                    color = secondaryGray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            val currentIdx = repsVal.toIntOrNull() ?: 0
                            val newVal = (currentIdx - 5).coerceAtLeast(0)
                            viewModel.inputReps = newVal.toString()
                        },
                        modifier = Modifier.background(Color(0xFFE6F3F1), RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease reps", tint = tealActive)
                    }

                    OutlinedTextField(
                        value = repsVal,
                        onValueChange = { viewModel.inputReps = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp).testTag("reps_input_field"),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            color = charcoalDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tealActive,
                            unfocusedBorderColor = borderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val currentIdx = repsVal.toIntOrNull() ?: 0
                            val newVal = currentIdx + 5
                            viewModel.inputReps = newVal.toString()
                        },
                        modifier = Modifier.background(tealActive, RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase reps", tint = Color.White)
                    }
                }
            }
        }

        // Duration Seconds input (Planks highlight)
        Card(
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "수행 시간 (Seconds)",
                    color = if (selectedExercise == "플랭크" || selectedExercise == "기타") tealActive else charcoalDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "초 단위로 수행한 시간을 입력하세요 (예: 60초 = 1분)",
                    color = secondaryGray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            val currentIdx = secsVal.toIntOrNull() ?: 0
                            val newVal = (currentIdx - 10).coerceAtLeast(0)
                            viewModel.inputDurationSeconds = newVal.toString()
                        },
                        modifier = Modifier.background(Color(0xFFE6F3F1), RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease duration", tint = tealActive)
                    }

                    OutlinedTextField(
                        value = secsVal,
                        onValueChange = { viewModel.inputDurationSeconds = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp).testTag("duration_input_field"),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            color = charcoalDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tealActive,
                            unfocusedBorderColor = borderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val currentIdx = secsVal.toIntOrNull() ?: 0
                            val newVal = currentIdx + 10
                            viewModel.inputDurationSeconds = newVal.toString()
                        },
                        modifier = Modifier.background(tealActive, RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase duration", tint = Color.White)
                    }
                }
            }
        }

        // Workout intensity tracker rating (1-5 stars)
        Card(
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "운동 만족도 및 운동 피로도",
                    color = charcoalDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val intensityLabel = when (rating) {
                    1 -> "아주 가벼운 운동 (가볍게 몸풀기)"
                    2 -> "적당한 운동 (수월했음)"
                    3 -> "안성맞춤 리듬 (자극이 충분했음)"
                    4 -> "매우 무거운 하드 트레이닝 (땀 한바가지)"
                    5 -> "한계돌파 극한 트레이닝 (탈진 수준)"
                    else -> "일반"
                }
                Text(
                    text = "강도 점수: $intensityLabel",
                    color = tealActive,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        val isFilled = star <= rating
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "$star Stars",
                            tint = if (isFilled) tealActive else Color(0xFFDAE5E1),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { viewModel.inputRating = star }
                        )
                    }
                }
            }
        }

        // Custom text field input note for any specific remarks
        Card(
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "운동 피드백 메모 (선택)",
                    color = charcoalDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { viewModel.inputNote = it },
                    placeholder = { Text("자세, 컨디션, 다음 목표 등의 일지를 남기세요", color = secondaryGray, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = charcoalDark,
                        unfocusedTextColor = charcoalDark,
                        focusedBorderColor = tealActive,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(80.dp).testTag("note_input_field")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Save Button
        Button(
            onClick = {
                viewModel.saveWorkoutRecord()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("save_workout_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = tealActive,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "운동 기록 저장하기 (완성)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
