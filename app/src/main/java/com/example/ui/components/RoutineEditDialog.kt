package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExerciseType
import com.example.data.RoutineStep
import com.example.ui.models.exercisePreset

/**
 * A horizontal row of selectable chips to choose the type of exercise step.
 */
@Composable
fun ExerciseChipGroup(
    selectedExercise: String,
    onExerciseSelected: (ExerciseType) -> Unit,
    modifier: Modifier = Modifier
) {
    val exerciseTypes = ExerciseType.values()
    val charcoalDark = Color(0xFF191C1B)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        exerciseTypes.forEach { exeType ->
            val isSelected = ExerciseType.fromString(selectedExercise) == exeType
            val preset = exeType.exercisePreset
            val selectedBg = preset.bgColor
            val selectedBorder = preset.themeColor
            val txtColor = preset.themeColor

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) selectedBg else Color.White,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (isSelected) selectedBorder else Color(0xFFDCE5E2),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onExerciseSelected(exeType) }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(id = exeType.displayNameResId),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) txtColor else charcoalDark
                )
            }
        }
    }
}

/**
 * Display card for a single step inside the custom routine editor.
 */
@Composable
fun RoutineStepRowItem(
    index: Int,
    step: RoutineStep,
    isMoveUpEnabled: Boolean,
    isMoveDownEnabled: Boolean,
    isLastStep: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onStepChange: (RoutineStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val tealActive = Color(0xFF006A60)
    val charcoalDark = Color(0xFF191C1B)
    val secondaryGray = Color(0xFF3F4947)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F7F5)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFDCE5E2), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row with step index, order shift indicators, and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.routine_step_format, index + 1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tealActive
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = isMoveUpEnabled,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("🔼", fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = isMoveDownEnabled,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("🔽", fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("❌", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Exercise Preset Chip Choices
            ExerciseChipGroup(
                selectedExercise = step.exerciseName,
                onExerciseSelected = { selected ->
                    onStepChange(step.copy(exerciseName = selected.name))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Exercise duration controller
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.label_exercise_duration),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryGray
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFCCE8E3), CircleShape)
                            .clickable {
                                onStepChange(step.copy(durationSeconds = (step.durationSeconds - 5).coerceAtLeast(5)))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", color = tealActive, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "${step.durationSeconds}초",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = charcoalDark,
                        modifier = Modifier.widthIn(min = 36.dp),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(tealActive, CircleShape)
                            .clickable {
                                onStepChange(step.copy(durationSeconds = (step.durationSeconds + 5).coerceAtMost(300)))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pulse/Beat pacing controls (Excluding plank)
            if (ExerciseType.fromString(step.exerciseName) != ExerciseType.PLANK) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.label_rhythm_setting),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryGray
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, Color(0xFFCCE8E3), CircleShape)
                                .clickable {
                                    onStepChange(step.copy(rhythmIntervalSeconds = (step.rhythmIntervalSeconds - 1).coerceAtLeast(1)))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", color = tealActive, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${step.rhythmIntervalSeconds}초",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = charcoalDark,
                            modifier = Modifier.widthIn(min = 36.dp),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(tealActive, CircleShape)
                                .clickable {
                                    onStepChange(step.copy(rhythmIntervalSeconds = (step.rhythmIntervalSeconds + 1).coerceAtMost(15)))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Recovery/Rest restSeconds controller (Hidden for the last step of the routine)
            if (!isLastStep) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.label_rest_duration_after),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryGray
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, Color(0xFFCCE8E3), CircleShape)
                                .clickable {
                                    onStepChange(step.copy(restSeconds = (step.restSeconds - 5).coerceAtLeast(0)))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", color = tealActive, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${step.restSeconds}초",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = charcoalDark,
                            modifier = Modifier.widthIn(min = 36.dp),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(tealActive, CircleShape)
                                .clickable {
                                    onStepChange(step.copy(restSeconds = (step.restSeconds + 5).coerceAtMost(120)))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extracted & Decomposed custom routine build configuration details dialog.
 */
@Composable
fun RoutineEditDialog(
    routineId: String?,
    initialName: String,
    initialSteps: List<RoutineStep>,
    onDismiss: () -> Unit,
    onSave: (String, List<RoutineStep>) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var steps by remember { mutableStateOf(initialSteps) }

    val tealActive = Color(0xFF006A60)
    val secondaryGray = Color(0xFF3F4947)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (routineId == null) {
                    stringResource(id = R.string.title_create_custom_routine)
                } else {
                    stringResource(id = R.string.title_edit_custom_routine)
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = tealActive
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.label_routine_name),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = secondaryGray
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(id = R.string.placeholder_routine_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.label_exercise_steps_format, steps.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryGray
                    )

                    TextButton(
                        onClick = {
                            steps = steps + RoutineStep(ExerciseType.SQUAT.name, 60, 4, 15)
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_add_step),
                            color = tealActive,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (steps.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.msg_no_steps_added),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }

                steps.forEachIndexed { index, step ->
                    RoutineStepRowItem(
                        index = index,
                        step = step,
                        isMoveUpEnabled = index > 0,
                        isMoveDownEnabled = index < steps.size - 1,
                        isLastStep = index == steps.size - 1,
                        onMoveUp = {
                            if (index > 0) {
                                val mutable = steps.toMutableList()
                                val temp = mutable[index]
                                mutable[index] = mutable[index - 1]
                                mutable[index - 1] = temp
                                steps = mutable
                            }
                        },
                        onMoveDown = {
                            if (index < steps.size - 1) {
                                val mutable = steps.toMutableList()
                                val temp = mutable[index]
                                mutable[index] = mutable[index + 1]
                                mutable[index + 1] = temp
                                steps = mutable
                            }
                        },
                        onDelete = {
                            steps = steps.filterIndexed { idx, _ -> idx != index }
                        },
                        onStepChange = { updatedStep ->
                            steps = steps.mapIndexed { idx, s ->
                                if (idx == index) updatedStep else s
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        // Dynamically force the very last step's rest duration to 0 as it terminates the workout
                        val sanitizedSteps = steps.mapIndexed { index, step ->
                            if (index == steps.size - 1) {
                                step.copy(restSeconds = 0)
                            } else {
                                step
                            }
                        }
                        onSave(name, sanitizedSteps)
                    }
                },
                enabled = name.isNotBlank() && steps.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = tealActive),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(id = R.string.btn_save), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = secondaryGray, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}
