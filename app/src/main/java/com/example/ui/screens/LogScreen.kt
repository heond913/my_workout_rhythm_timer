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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.viewmodel.WorkoutViewModel
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.components.DrawExerciseIcon
import com.example.ui.models.exercisePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(viewModel: WorkoutViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedExercise = uiState.inputExerciseName
    val repsVal = uiState.inputReps
    val setsVal = uiState.inputSets
    val weightVal = uiState.inputWeightKg
    val secsVal = uiState.inputDurationSeconds
    val rating = uiState.inputRating
    val note = uiState.inputNote

    val context = LocalContext.current
    var logCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val daySelectedFormat = stringResource(id = R.string.day_selected_format)
    val dateFormatter = remember(daySelectedFormat) { SimpleDateFormat(daySelectedFormat, Locale.getDefault()) }

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
            text = stringResource(id = R.string.title_log),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = tealActive,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Text(
            text = stringResource(id = R.string.subtitle_log),
            fontSize = 12.sp,
            color = secondaryGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Exercise selector cards (Squat, Lunge, Plank, Custom)
        Text(
            text = stringResource(id = R.string.select_exercise),
            fontSize = 14.sp,
            color = charcoalDark,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            val exercises = listOf("스쿼트", "런지", "플랭크", "기타")
            exercises.forEach { exe ->
                val isSelected = selectedExercise == exe
                
                // Isolate preset presentation colors & metadata using the UI presenter model
                val preset = exe.exercisePreset
                val itemBg = preset.bgColor
                val itemBorder = preset.themeColor
                val itemAccent = preset.themeColor

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
                            viewModel.updateInputExerciseName(exe)
                            // Set intelligent defaults for easy flow
                            if (exe == "스쿼트" || exe == "런지") {
                                if (uiState.inputReps.isEmpty()) viewModel.updateInputReps("15")
                            } else if (exe == "플랭크") {
                                if (uiState.inputDurationSeconds.isEmpty()) viewModel.updateInputDurationSeconds("60")
                            }
                        },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DrawExerciseIcon(
                            exerciseName = exe,
                            iconColor = if (isSelected) itemAccent else secondaryGray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val exeDisplay = stringResource(id = preset.displayNameResId)
                        Text(
                            text = exeDisplay,
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
                    viewModel.updateInputExerciseName(it)
                },
                label = { Text(stringResource(id = R.string.lbl_custom_exercise), color = secondaryGray) },
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

        // Recording Date selection option
        Text(
            text = stringResource(id = R.string.select_date),
            fontSize = 14.sp,
            color = charcoalDark,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            textAlign = TextAlign.Start
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .clickable {
                    val year = logCalendar.get(Calendar.YEAR)
                    val month = logCalendar.get(Calendar.MONTH)
                    val day = logCalendar.get(Calendar.DAY_OF_MONTH)

                    DatePickerDialog(
                        context,
                        { _, selectedYear, selectedMonth, selectedDay ->
                            val newCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, selectedYear)
                                set(Calendar.MONTH, selectedMonth)
                                set(Calendar.DAY_OF_MONTH, selectedDay)
                            }
                            logCalendar = newCal
                        },
                        year,
                        month,
                        day
                    ).show()
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(id = R.string.desc_selected_date),
                        tint = tealActive,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.lbl_record_date),
                            color = charcoalDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateFormatter.format(logCalendar.time),
                            color = secondaryGray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = stringResource(id = R.string.change_date),
                    color = tealActive,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Repetitions input (Squats or Lunges highlight)
        Card(
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.label_reps),
                    color = if (selectedExercise == "스쿼트" || selectedExercise == "런지") tealActive else charcoalDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.sub_reps),
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
                            viewModel.updateInputReps(newVal.toString())
                        },
                        modifier = Modifier.background(Color(0xFFE6F3F1), RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = stringResource(id = R.string.desc_decrease_reps), tint = tealActive)
                    }

                    OutlinedTextField(
                        value = repsVal,
                        onValueChange = { viewModel.updateInputReps(it) },
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
                            viewModel.updateInputReps(newVal.toString())
                        },
                        modifier = Modifier.background(tealActive, RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(id = R.string.desc_increase_reps), tint = Color.White)
                    }
                }
            }
        }

        // Set Count input
        Card(
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.label_sets),
                    color = if (selectedExercise == "스쿼트" || selectedExercise == "런지" || selectedExercise == "플랭크") tealActive else charcoalDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.sub_sets),
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
                            val currentIdx = setsVal.toIntOrNull() ?: 1
                            val newVal = (currentIdx - 1).coerceAtLeast(1)
                            viewModel.updateInputSets(newVal.toString())
                        },
                        modifier = Modifier.background(Color(0xFFE6F3F1), RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = stringResource(id = R.string.desc_decrease_sets), tint = tealActive)
                    }

                    OutlinedTextField(
                        value = setsVal,
                        onValueChange = { viewModel.updateInputSets(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp).testTag("sets_input_field"),
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
                            val currentIdx = setsVal.toIntOrNull() ?: 1
                            val newVal = currentIdx + 1
                            viewModel.updateInputSets(newVal.toString())
                        },
                        modifier = Modifier.background(tealActive, RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(id = R.string.desc_increase_sets), tint = Color.White)
                    }
                }
            }
        }

        // Weight input (Squats or Lunges highlight)
        Card(
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.label_weight),
                    color = if (selectedExercise == "스쿼트" || selectedExercise == "런지") tealActive else charcoalDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.sub_weight),
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
                            val currentVal = weightVal.toDoubleOrNull() ?: 0.0
                            val newVal = (currentVal - 2.5).coerceAtLeast(0.0)
                            viewModel.updateInputWeightKg(if (newVal % 1.0 == 0.0) newVal.toInt().toString() else newVal.toString())
                        },
                        modifier = Modifier.background(Color(0xFFE6F3F1), RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = stringResource(id = R.string.desc_decrease_weight), tint = tealActive)
                    }

                    OutlinedTextField(
                        value = weightVal,
                        onValueChange = { viewModel.updateInputWeightKg(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp).testTag("weight_input_field"),
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
                            val currentVal = weightVal.toDoubleOrNull() ?: 0.0
                            val newVal = currentVal + 2.5
                            viewModel.updateInputWeightKg(if (newVal % 1.0 == 0.0) newVal.toInt().toString() else newVal.toString())
                        },
                        modifier = Modifier.background(tealActive, RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(id = R.string.desc_increase_weight), tint = Color.White)
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
                .padding(vertical = 8.dp)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.label_duration_secs),
                    color = if (selectedExercise == "플랭크" || selectedExercise == "기타") tealActive else charcoalDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.sub_duration_secs),
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
                            viewModel.updateInputDurationSeconds(newVal.toString())
                        },
                        modifier = Modifier.background(Color(0xFFE6F3F1), RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = stringResource(id = R.string.desc_decrease_duration), tint = tealActive)
                    }

                    OutlinedTextField(
                        value = secsVal,
                        onValueChange = { viewModel.updateInputDurationSeconds(it) },
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
                            viewModel.updateInputDurationSeconds(newVal.toString())
                        },
                        modifier = Modifier.background(tealActive, RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(id = R.string.desc_increase_duration), tint = Color.White)
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
                .padding(vertical = 8.dp)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.label_fatigue_satisfaction),
                    color = charcoalDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val intensityLabel = when (rating) {
                    1 -> stringResource(id = R.string.intensity_very_light)
                    2 -> stringResource(id = R.string.intensity_moderate)
                    3 -> stringResource(id = R.string.intensity_perfect)
                    4 -> stringResource(id = R.string.intensity_hard)
                    5 -> stringResource(id = R.string.intensity_extreme)
                    else -> stringResource(id = R.string.intensity_general)
                }
                Text(
                    text = stringResource(id = R.string.intensity_score_format, intensityLabel),
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
                                .clickable { viewModel.updateInputRating(star) }
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
                .padding(vertical = 8.dp)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.label_memo),
                    color = charcoalDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { viewModel.updateInputNote(it) },
                    placeholder = { Text(stringResource(id = R.string.placeholder_memo), color = secondaryGray, fontSize = 12.sp) },
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
                viewModel.saveWorkoutRecord(timestamp = logCalendar.timeInMillis)
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
                text = stringResource(id = R.string.save_workout_log),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
