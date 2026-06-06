package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ExerciseType
import com.example.viewmodel.WorkoutViewModel
import com.example.ui.components.DrawExerciseIcon
import com.example.ui.components.RoutineEditDialog
import com.example.ui.components.TimerVisualizer
import com.example.ui.models.exercisePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(viewModel: WorkoutViewModel) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var systemVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    val uiState by viewModel.uiState.collectAsState()
    val totalSeconds = uiState.totalTargetSeconds
    val isRunning = uiState.timerRunning
    val mode = uiState.timerMode
    val interval = uiState.rhythmIntervalSeconds

    // Prevent screen from turning off while timer is running
    val view = LocalView.current
    DisposableEffect(isRunning) {
        if (isRunning) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Dynamically request notification permission on entry
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                (context as? androidx.activity.ComponentActivity)?.let { activity ->
                    androidx.core.app.ActivityCompat.requestPermissions(activity, arrayOf(permission), 101)
                }
            }
        }
    }

    // Isolate preset presentation colors & metadata using the UI presenter model
    val activePreset = uiState.timerPresetType.exercisePreset
    val activePresetColor = activePreset.themeColor
    val activePresetBgColor = activePreset.bgColor

    // Color Theme mappings - Vibrant Palette
    val tealActive = Color(0xFF006A60)
    val darkBg = Color(0xFFFBFDF9) // Light theme background
    val cardSurface = Color(0xFFF2F7F5) // Mint-grey card surfaces
    val secondaryGray = Color(0xFF3F4947) // Slate grey text / unselected labels
    val charcoalDark = Color(0xFF191C1B) // Deep dark text

    val scrollState = rememberScrollState()

    val density = androidx.compose.ui.platform.LocalDensity.current
    val scrollToOffsetPx = with(density) { 110.dp.toPx().toInt() }

    LaunchedEffect(uiState.isRoutineActive) {
        if (uiState.isRoutineActive) {
            scrollState.animateScrollTo(scrollToOffsetPx)
        }
    }

    val appLocalesForScreen = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
    val currentLocaleForScreen = if (!appLocalesForScreen.isEmpty) appLocalesForScreen.get(0)?.language else java.util.Locale.getDefault().language
    val isKo = currentLocaleForScreen == "ko"

    var showRoutineDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var editingRoutineId by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var routineNameInput by remember { androidx.compose.runtime.mutableStateOf("") }
    var routineStepsInput by remember { androidx.compose.runtime.mutableStateOf<List<com.example.data.RoutineStep>>(emptyList()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        if (!uiState.isRoutineActive) {
            // Language Selection Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        viewModel.updateShowLanguageSelection(true)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = tealActive
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "🌐 Language Selection",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            // Upper Title block
            Text(
                text = stringResource(id = R.string.title_timer),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = tealActive,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            Text(
                text = stringResource(id = R.string.subtitle_timer),
                fontSize = 12.sp,
                color = secondaryGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Custom Routine active playback visual track progress banner
        if (uiState.isRoutineActive) {
            val steps = com.example.data.CustomRoutine.deserializeSteps(uiState.routineStepsJson)
            val currentIdx = uiState.routineCurrentStepIndex
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECF5F3)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Color(0xFFD0E2DE), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF7A9390), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = if (isKo) "커스텀 루틴: ${uiState.routineName}" else "Custom Routine: ${uiState.routineName}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00564E),
                            fontSize = 14.sp
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = Color(0xFFD0E1DE)
                    )
                    
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Background connector line running through vertical center of circles (22.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp)
                                .padding(top = 22.dp)
                                .height(2.dp)
                                .background(Color(0xFFD2DBD9))
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Top
                        ) {
                            steps.forEachIndexed { index, step ->
                                val isCurrent = index == currentIdx
                                val isCompleted = index < currentIdx
                                
                                val stepType = ExerciseType.fromString(step.exerciseName)
                                val stepColor = when (stepType) {
                                    ExerciseType.SQUAT -> Color(0xFFE65100) // Orange
                                    ExerciseType.LUNGE -> Color(0xFF3F5F90)     // Blue
                                    ExerciseType.PLANK -> Color(0xFF93000A)   // Red
                                    ExerciseType.OTHER -> Color(0xFF006A60)                // Teal
                                }
                                
                                val circleBgColor = if (isCurrent) {
                                    when (stepType) {
                                        ExerciseType.SQUAT -> Color(0xFFFFECCC)
                                        ExerciseType.LUNGE -> Color(0xFFE8F0FE)
                                        ExerciseType.PLANK -> Color(0xFFFFDAD6)
                                        ExerciseType.OTHER -> Color(0xFFE6F3F1)
                                    }
                                } else {
                                    Color.White
                                }
                                
                                val circleBorderColor = if (isCurrent) {
                                    stepColor
                                } else {
                                    Color(0xFFD0D7D5)
                                }
                                
                                val iconColor = if (isCurrent) {
                                    stepColor
                                } else {
                                    if (isCompleted) Color(0xFFB5C0BE) else Color(0xFF859290)
                                }
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(circleBgColor, CircleShape)
                                            .border(
                                                width = if (isCurrent) 1.5.dp else 1.dp,
                                                color = circleBorderColor,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DrawExerciseIcon(
                                            exerciseName = step.exerciseName,
                                            iconColor = iconColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val exerciseLabel = if (stepType != ExerciseType.OTHER) {
                                        stringResource(id = stepType.displayNameResId)
                                    } else {
                                        step.exerciseName
                                    }
                                    
                                    val statusStr = if (isKo) "운동" else "work"
                                    val restStr = if (isKo) "휴식" else "rest"
                                    
                                    Text(
                                        text = "$exerciseLabel: ${step.durationSeconds}s ($statusStr)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) stepColor else Color(0xFF4A5553),
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    if (step.restSeconds > 0) {
                                        Text(
                                            text = "/ +${step.restSeconds}s ($restStr)",
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) Color(0xFF00796B) else Color(0xFF7A8682),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center circular timer visualizer (Optimized through Component Decomposition and State Slicing)
        TimerVisualizer(
            remainingSecondsProvider = { uiState.remainingSeconds },
            restRemainingSecondsProvider = { uiState.restRemainingSeconds },
            isRestingProvider = { uiState.isResting },
            restTotalSecondsProvider = { uiState.restTotalSeconds },
            totalTargetSecondsProvider = { uiState.totalTargetSeconds },
            timerRunningProvider = { uiState.timerRunning },
            timerPresetTypeProvider = { uiState.timerPresetType },
            workoutCountProvider = { uiState.workoutCount },
            rhythmIntervalSecondsProvider = { uiState.rhythmIntervalSeconds }
        )

        if (!uiState.isRoutineActive) {
            Spacer(modifier = Modifier.height(28.dp))

            // Preset Exercise quick interval speed suggestions (Vibrant Palette specific categorizations)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    Triple(ExerciseType.SQUAT, uiState.squatIntervalSeconds, stringResource(id = R.string.preset_interval_format, uiState.squatIntervalSeconds)),
                    Triple(ExerciseType.LUNGE, uiState.lungeIntervalSeconds, stringResource(id = R.string.preset_interval_format, uiState.lungeIntervalSeconds)),
                    Triple(ExerciseType.PLANK, totalSeconds, stringResource(id = R.string.preset_check_format, totalSeconds)),
                    Triple(ExerciseType.OTHER, uiState.otherIntervalSeconds, stringResource(id = R.string.preset_check_format, uiState.otherIntervalSeconds))
                )

                presets.forEach { (exeType, secs, desc) ->
                    val isSelected = ExerciseType.fromString(uiState.timerPresetType) == exeType
                    val (itemBgColor, itemBorderColor, itemTextColor) = when (exeType) {
                        ExerciseType.SQUAT -> Triple(Color(0xFFFFECCC), Color(0xFFE65100), Color(0xFFE65100))
                        ExerciseType.LUNGE -> Triple(Color(0xFFD7E3FF), Color(0xFF3F5F90), Color(0xFF3F5F90))
                        ExerciseType.PLANK -> Triple(Color(0xFFFFDAD6), Color(0xFF93000A), Color(0xFF93000A))
                        ExerciseType.OTHER -> Triple(Color(0xFFCCE8E3), Color(0xFF006A60), Color(0xFF006A60))
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) itemBgColor else cardSurface,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) itemBorderColor else Color(0xFFDCE5E2),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !isRunning) {
                                viewModel.selectPreset(exeType.name)
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val labelDisplay = stringResource(id = exeType.displayNameResId)
                        Text(
                            text = labelDisplay,
                            color = if (isSelected) itemTextColor else charcoalDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            color = if (isSelected) itemTextColor.copy(alpha = 0.8f) else secondaryGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Slider for Duration Target settings
        if (!isRunning && !uiState.isRoutineActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFDCE5E2), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(id = R.string.target_workout_time), color = charcoalDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        val targetTimeDisplay = if (totalSeconds >= 60) {
                            if (totalSeconds % 60 == 0) {
                                stringResource(id = R.string.target_time_format_minute_only, totalSeconds, totalSeconds / 60)
                            } else {
                                stringResource(id = R.string.target_time_format, totalSeconds, totalSeconds / 60, totalSeconds % 60)
                            }
                        } else {
                            stringResource(id = R.string.target_time_format_seconds_only, totalSeconds)
                        }
                        Text(targetTimeDisplay, color = tealActive, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Decrease by 5 seconds button (마이너스 버튼)
                        IconButton(
                            onClick = {
                                val current = viewModel.totalTargetSeconds
                                val next = (current - 5).coerceAtLeast(5)
                                viewModel.updateTargetSeconds(next)
                                viewModel.resetTimer()
                            },
                            modifier = Modifier
                                .size(48.dp) 
                                .padding(6.dp) 
                                .background(Color.White, CircleShape)
                                .border(1.dp, Color(0xFFCCE8E3), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = stringResource(id = R.string.desc_decrease_5_sec),
                                tint = tealActive,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // 2. Slider (슬라이더 영역)
                        Slider(
                            value = totalSeconds.toFloat(),
                            onValueChange = {
                                val rounded = (it.toInt() / 5) * 5 // Snap to 5-sec values
                                viewModel.updateTargetSeconds(rounded.coerceAtLeast(5))
                                viewModel.resetTimer()
                            },
                            valueRange = 5f..300f,
                            colors = SliderDefaults.colors(
                                thumbColor = tealActive,
                                activeTrackColor = tealActive,
                                inactiveTrackColor = Color(0xFFDAE5E1)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // 3. Increase by 5 seconds button (플러스 버튼)
                        IconButton(
                            onClick = {
                                val current = viewModel.totalTargetSeconds
                                val next = (current + 5).coerceAtMost(300)
                                viewModel.updateTargetSeconds(next)
                                viewModel.resetTimer()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(6.dp)
                                .background(tealActive, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.desc_increase_5_sec),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // System Volume setting Card representing stream music volume
            Card(
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFDCE5E2), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(id = R.string.desc_system_volume),
                                tint = tealActive,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(id = R.string.timer_sound_volume), color = charcoalDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${(systemVolume * 100 / maxVolume)}%",
                            color = tealActive,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Decrease Volume button
                        IconButton(
                            onClick = {
                                val next = (systemVolume - 1).coerceAtLeast(0)
                                systemVolume = next
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, AudioManager.FLAG_SHOW_UI)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(6.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, Color(0xFFCCE8E3), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = stringResource(id = R.string.desc_decrease_volume),
                                tint = tealActive,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Slider
                        Slider(
                            value = systemVolume.toFloat(),
                            onValueChange = {
                                val newVolume = it.toInt().coerceIn(0, maxVolume)
                                systemVolume = newVolume
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI)
                            },
                            valueRange = 0f..maxVolume.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = tealActive,
                                activeTrackColor = tealActive,
                                inactiveTrackColor = Color(0xFFDAE5E1)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Increase Volume button
                        IconButton(
                            onClick = {
                                val next = (systemVolume + 1).coerceAtMost(maxVolume)
                                systemVolume = next
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, AudioManager.FLAG_SHOW_UI)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(6.dp)
                                .background(tealActive, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.desc_increase_volume),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!isRunning && !uiState.isRoutineActive) {
            // RHYTHM TIMER EXERCISE CONFIGURATIONS
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.rhythm_individ_config_title),
                color = charcoalDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // 1. Squat Settings Card
            ExerciseSettingsCard(
                title = stringResource(id = R.string.preset_squat),
                themeColor = Color(0xFFE65100),
                bgColor = Color(0xFFFFECCC).copy(alpha = 0.3f),
                borderColor = Color(0xFFFFECCC),
                intervalLabel = stringResource(id = R.string.rhythm_pace_label),
                intervalValue = uiState.squatIntervalSeconds,
                onIntervalChange = { viewModel.updateSquatInterval(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Lunge Settings Card
            ExerciseSettingsCard(
                title = stringResource(id = R.string.preset_lunge),
                themeColor = Color(0xFF3F5F90),
                bgColor = Color(0xFFD7E3FF).copy(alpha = 0.3f),
                borderColor = Color(0xFFD7E3FF),
                intervalLabel = stringResource(id = R.string.rhythm_pace_label),
                intervalValue = uiState.lungeIntervalSeconds,
                onIntervalChange = { viewModel.updateLungeInterval(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Other Settings Card
            ExerciseSettingsCard(
                title = stringResource(id = R.string.preset_other),
                themeColor = Color(0xFF006A60),
                bgColor = Color(0xFFCCE8E3).copy(alpha = 0.3f),
                borderColor = Color(0xFFCCE8E3),
                intervalLabel = stringResource(id = R.string.rhythm_pace_label),
                intervalValue = uiState.otherIntervalSeconds,
                onIntervalChange = { viewModel.updateOtherInterval(it) }
            )

            // --- CUSTOM ROUTINES SECTION ---
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKo) "🎯 커스텀 루틴 목록" else "🎯 Custom Routines List",
                    color = charcoalDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                Box(
                    modifier = Modifier
                        .border(1.dp, tealActive, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            editingRoutineId = null
                            routineNameInput = ""
                            routineStepsInput = listOf(com.example.data.RoutineStep(ExerciseType.SQUAT.name, 60, 4, 15))
                            showRoutineDialog = true
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isKo) "+ 루틴 생성" else "+ Build Routine",
                        color = tealActive,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.customRoutines.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFDCE5E2), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                ) {
                    Text(
                        text = if (isKo) "없음" else "None",
                        color = secondaryGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                uiState.customRoutines.forEach { routine ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .border(1.dp, Color(0xFFDCE5E2), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = routine.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = charcoalDark
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable { viewModel.deleteRoutine(routine.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🗑️", fontSize = 12.sp)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                val totalSeconds = routine.steps.sumOf { it.durationSeconds + it.restSeconds }
                                val mins = totalSeconds / 60
                                val secs = totalSeconds % 60
                                val totalLabel = if (mins > 0) {
                                    if (secs > 0) "${mins} min ${secs} sec" else "${mins} min"
                                } else {
                                    "${secs} sec"
                                }
                                Text(
                                    text = "Total: $totalLabel",
                                    fontSize = 13.sp,
                                    color = charcoalDark.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    routine.steps.forEachIndexed { index, step ->
                                        val exType = ExerciseType.fromString(step.exerciseName)
                                        val displayExerciseName = if (isKo) {
                                            stringResource(id = exType.displayNameResId)
                                        } else {
                                            when (exType) {
                                                ExerciseType.SQUAT -> "Squat"
                                                ExerciseType.LUNGE -> "Lunge"
                                                ExerciseType.PLANK -> "Plank"
                                                ExerciseType.OTHER -> "Etc."
                                            }
                                        }
                                        
                                        val (chipBg, chipTx) = when (exType) {
                                            ExerciseType.SQUAT -> Pair(Color(0xFFD05404), Color.White)
                                            ExerciseType.LUNGE -> Pair(Color(0xFF2B4D7E), Color.White)
                                            ExerciseType.PLANK -> Pair(Color(0xFF93000A), Color.White)
                                            ExerciseType.OTHER -> Pair(Color(0xFF0C6052), Color.White)
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .background(chipBg, RoundedCornerShape(50.dp))
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "$displayExerciseName (${step.durationSeconds}s)",
                                                color = chipTx,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        if (step.restSeconds > 0) {
                                            Text(
                                                text = "→",
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFCFD1DC), RoundedCornerShape(50.dp))
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = if (isKo) "Rest (${step.restSeconds}s)" else "Rest (${step.restSeconds}s)",
                                                    color = Color(0xFF191C1B),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        if (index < routine.steps.size - 1) {
                                            Text(
                                                text = "→",
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Play Button
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.startRoutine(routine) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFF0C6052), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Start Routine",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .padding(start = 2.dp)
                                        )
                                    }
                                }
                                
                                // Edit Button
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            editingRoutineId = routine.id
                                            routineNameInput = routine.name
                                            routineStepsInput = routine.steps
                                            showRoutineDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color.White, CircleShape)
                                            .border(1.dp, Color(0xFFCCE8E3), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Routine",
                                            tint = Color(0xFF0C6052),
                                            modifier = Modifier.size(18.dp)
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

        // Floating Action Buttons (Start, Pause, Reset) at the bottom with a gentle background gradient overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            darkBg.copy(alpha = 0.9f),
                            darkBg
                        )
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop / Reset Button
                IconButton(
                    onClick = {
                        if (uiState.isRoutineActive) {
                            viewModel.stopRoutine()
                        } else {
                            viewModel.resetTimer()
                        }
                    },
                    enabled = !isRunning,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (isRunning) Color(0xFFEAEAEA)
                            else Color(0xFFE6F3F1),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (isRunning) Color(0xFFDDDDDD)
                            else Color(0xFFDCE5E2),
                            CircleShape
                        )
                        .testTag("reset_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (uiState.isRoutineActive) "Stop" else stringResource(id = R.string.desc_reset_timer),
                        tint = if (isRunning) Color.Gray.copy(alpha = 0.5f) else tealActive,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Main Play/Pause neon toggle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(tealActive, Color(0xFF004D46))
                            ),
                            CircleShape
                        )
                        .clickable {
                            if (isRunning) {
                                viewModel.pauseTimer()
                            } else {
                                viewModel.startTimer()
                            }
                        }
                        .testTag("start_pause_timer_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) stringResource(id = R.string.desc_pause) else stringResource(id = R.string.desc_start),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Navigate directly to manual logger with current timing prep
                val isManualInputEnabled = !isRunning && uiState.manualInputEnabled
                IconButton(
                    onClick = {
                        viewModel.updateInputDurationSeconds(totalSeconds.toString())
                        viewModel.setTab(com.example.viewmodel.AppTab.Log)
                    },
                    enabled = isManualInputEnabled,
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isManualInputEnabled) Color(0xFFE6F3F1) else Color(0xFFEAEAEA), CircleShape)
                        .border(1.dp, if (isManualInputEnabled) Color(0xFFDCE5E2) else Color(0xFFDDDDDD), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.desc_manual_log_write),
                        tint = if (isManualInputEnabled) tealActive else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (uiState.showCompletionDialog) {
        val completedSteps = remember(uiState.routineHistoryJson) {
            com.example.data.RoutineStepResult.deserializeList(uiState.routineHistoryJson)
        }

        val chosenPreset = remember(uiState.routineHistoryJson, uiState.timerPresetType) {
            if (completedSteps.isNotEmpty()) {
                val uniqueNames = completedSteps.map { it.exerciseName }.distinct()
                val randomName = uniqueNames.random()
                randomName.exercisePreset
            } else {
                uiState.timerPresetType.exercisePreset
            }
        }

        val encouragementRes = chosenPreset.encouragementResId
        val tip1Res = chosenPreset.tip1ResId
        val tip2Res = chosenPreset.tip2ResId

        AlertDialog(
            onDismissRequest = { viewModel.updateShowCompletionDialog(false) },
            title = {
                Text(
                    text = stringResource(id = R.string.goal_completed_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = tealActive
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.goal_completed_msg),
                        fontSize = 14.sp,
                        color = charcoalDark,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (completedSteps.isNotEmpty()) {
                        completedSteps.forEachIndexed { index, stepResult ->
                            val exePreset = stepResult.exerciseName.exercisePreset
                            val exerciseDisplay = stringResource(id = exePreset.displayNameResId)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F3F1)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Header with step number
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(tealActive, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${index + 1}단계",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(id = R.string.label_exercise_category), color = secondaryGray, fontSize = 13.sp)
                                        Text(text = exerciseDisplay, fontWeight = FontWeight.Bold, color = charcoalDark, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = stringResource(id = R.string.label_total_count), color = secondaryGray, fontSize = 13.sp)
                                        Text(text = stringResource(id = R.string.workout_count_format, stepResult.count), fontWeight = FontWeight.ExtraBold, color = tealActive, fontSize = 17.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(id = R.string.label_target_duration), color = secondaryGray, fontSize = 13.sp)
                                        Text(text = stringResource(id = R.string.seconds_format, stepResult.targetSeconds), fontWeight = FontWeight.Bold, color = charcoalDark, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        val exercise = stringResource(id = uiState.timerPresetType.exercisePreset.displayNameResId)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F3F1)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = stringResource(id = R.string.label_exercise_category), color = secondaryGray, fontSize = 13.sp)
                                    Text(text = exercise, fontWeight = FontWeight.Bold, color = charcoalDark, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = stringResource(id = R.string.label_total_count), color = secondaryGray, fontSize = 13.sp)
                                    Text(text = stringResource(id = R.string.workout_count_format, uiState.workoutCount), fontWeight = FontWeight.ExtraBold, color = tealActive, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = stringResource(id = R.string.label_target_duration), color = secondaryGray, fontSize = 13.sp)
                                    Text(text = stringResource(id = R.string.seconds_format, uiState.totalTargetSeconds), fontWeight = FontWeight.Bold, color = charcoalDark, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // Encouragement Message Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)), // Light gold tint
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✨", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = stringResource(id = encouragementRes),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF805B00),
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Useful Tips Title
                    Text(
                        text = stringResource(id = R.string.title_useful_tips),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = charcoalDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Tips Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F8)), // Very soft light gray
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "1️⃣",
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                                )
                                Text(
                                    text = stringResource(id = tip1Res),
                                    fontSize = 13.sp,
                                    color = charcoalDark,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .height(1.dp)
                                    .background(Color.LightGray.copy(alpha = 0.4f))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "2️⃣",
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                                )
                                Text(
                                    text = stringResource(id = tip2Res),
                                    fontSize = 13.sp,
                                    color = charcoalDark,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.updateShowCompletionDialog(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = tealActive),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(id = R.string.confirm), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    if (showRoutineDialog) {
        RoutineEditDialog(
            routineId = editingRoutineId,
            initialName = routineNameInput,
            initialSteps = routineStepsInput,
            isKo = isKo,
            onDismiss = { showRoutineDialog = false },
            onSave = { name, steps ->
                viewModel.createOrUpdateRoutine(
                    name = name,
                    steps = steps,
                    id = editingRoutineId
                )
                showRoutineDialog = false
            }
        )
    }
}



@Composable
fun ExerciseSettingsCard(
    title: String,
    themeColor: Color,
    bgColor: Color,
    borderColor: Color,
    intervalLabel: String,
    intervalValue: Int,
    onIntervalChange: (Int) -> Unit
) {
    val charcoalDark = Color(0xFF191C1B)

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Title
            Text(
                text = title,
                color = themeColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // Right: Interval/Speed control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = intervalLabel,
                    color = charcoalDark.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, borderColor, CircleShape)
                            .clickable { onIntervalChange((intervalValue - 1).coerceAtLeast(1)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", color = themeColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = stringResource(id = R.string.seconds_format, intervalValue),
                        color = charcoalDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 36.dp),
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(themeColor, CircleShape)
                            .clickable { onIntervalChange((intervalValue + 1).coerceAtMost(60)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


