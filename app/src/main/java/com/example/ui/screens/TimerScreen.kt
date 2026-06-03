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
import com.example.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(viewModel: WorkoutViewModel) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        val resolvedContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            context.createAttributionContext("timer")
        } else {
            context
        }
        resolvedContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var systemVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    val uiState by viewModel.uiState.collectAsState()
    val totalSeconds = uiState.totalTargetSeconds
    val remaining = uiState.remainingSeconds
    val elapsed = uiState.elapsedSeconds
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

    // Dynamic color matching based on currently active preset (with swapped Squat <-> Other colors)
    val activePresetColor = when (uiState.timerPresetType) {
        "스쿼트" -> Color(0xFFE65100) // Swapped to Orange
        "런지" -> Color(0xFF3F5F90)
        "플랭크" -> Color(0xFF93000A)
        "기타" -> Color(0xFF006A60) // Swapped to Teal
        else -> Color(0xFF006A60)
    }

    val activePresetBgColor = when (uiState.timerPresetType) {
        "스쿼트" -> Color(0xFFFFECCC) // Swapped to Orange background
        "런지" -> Color(0xFFD7E3FF)
        "플랭크" -> Color(0xFFFFDAD6)
        "기타" -> Color(0xFFCCE8E3) // Swapped to Teal background
        else -> Color(0xFFCCE8E3)
    }

    // Color Theme mappings - Vibrant Palette
    val tealActive = Color(0xFF006A60)
    val darkBg = Color(0xFFFBFDF9) // Light theme background
    val cardSurface = Color(0xFFF2F7F5) // Mint-grey card surfaces
    val secondaryGray = Color(0xFF3F4947) // Slate grey text / unselected labels
    val charcoalDark = Color(0xFF191C1B) // Deep dark text

    val scrollState = rememberScrollState()

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
        // Language Toggle Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
            val currentLocale = if (!appLocales.isEmpty) appLocales.get(0)?.language else java.util.Locale.getDefault().language
            val isKo = currentLocale == "ko"
            
            TextButton(
                onClick = {
                    val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags("ko")
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isKo) tealActive else secondaryGray
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "한국어",
                    fontWeight = if (isKo) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            }
            Text(text = "|", color = secondaryGray.copy(alpha = 0.5f), fontSize = 11.sp)
            TextButton(
                onClick = {
                    val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags("en")
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (!isKo) tealActive else secondaryGray
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "English",
                    fontWeight = if (!isKo) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
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

        // Custom Routine active playback visual track progress banner
        if (uiState.isRoutineActive) {
            val steps = com.example.data.CustomRoutine.deserializeSteps(uiState.routineStepsJson)
            val currentIdx = uiState.routineCurrentStepIndex
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F3F1)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Color(0xFFCCE8E3), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isKo) "🔄 커스텀 루틴: ${uiState.routineName}" else "🔄 Routine: ${uiState.routineName}",
                        fontWeight = FontWeight.ExtraBold,
                        color = tealActive,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.forEachIndexed { index, step ->
                            val isCurrent = index == currentIdx
                            val isCompleted = index < currentIdx
                            
                            val stepColor = when (step.exerciseName) {
                                "스쿼트" -> Color(0xFFE65100)
                                "런지" -> Color(0xFF3F5F90)
                                "플랭크" -> Color(0xFF93000A)
                                else -> Color(0xFF006A60)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isCurrent) stepColor.copy(alpha = 0.2f) else if (isCompleted) Color.LightGray.copy(alpha = 0.3f) else Color.White,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        if (isCurrent) 2.dp else 1.dp,
                                        if (isCurrent) stepColor else if (isCompleted) Color.Gray.copy(alpha = 0.5f) else Color.LightGray,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = step.exerciseName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                        color = if (isCurrent) stepColor else if (isCompleted) Color.Gray else charcoalDark
                                    )
                                    Text(
                                        text = "${step.durationSeconds}s",
                                        fontSize = 9.sp,
                                        color = if (isCurrent) stepColor.copy(alpha = 0.8f) else Color.Gray
                                    )
                                    if (step.restSeconds > 0) {
                                        Text(
                                            text = "+${step.restSeconds}s rest",
                                            fontSize = 8.sp,
                                            color = Color(0xFF00796B),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            if (index < steps.size - 1) {
                                Text("➡️", fontSize = 10.sp, color = secondaryGray)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center circular timer visualizer
        val progressFraction = if (uiState.isResting) {
            if (uiState.restTotalSeconds > 0) uiState.restRemainingSeconds.toFloat() / uiState.restTotalSeconds.toFloat() else 0f
        } else {
            if (totalSeconds > 0) remaining.toFloat() / totalSeconds.toFloat() else 0f
        }

        val animatedProgress by animateFloatAsState(
            targetValue = progressFraction,
            label = "TimerProgress"
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .drawBehind {
                    // Draw clean background ring matching active preset color theme
                    drawArc(
                        color = if (uiState.isResting) Color(0xFFE0E0E0) else activePresetBgColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Draw progress arc in active preset custom color
                    drawArc(
                        color = if (uiState.isResting) Color(0xFF00796B) else activePresetColor,
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
                    tint = if (uiState.isResting) Color(0xFF00796B) else activePresetColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Giant time numbers in high-contrast charcoal
                val displayRemaining = if (uiState.isResting) uiState.restRemainingSeconds else remaining
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
                    text = stringResource(id = R.string.workout_count_format, uiState.workoutCount),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (uiState.isResting) Color(0xFF00796B) else activePresetColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                if (isRunning) {
                    val isPreparing = remaining > totalSeconds
                    Text(
                        text = if (uiState.isResting) {
                            stringResource(id = R.string.rest_timer_status)
                        } else if (isPreparing) {
                            stringResource(id = R.string.timer_preparing)
                        } else if (interval > 0) {
                            stringResource(id = R.string.rhythm_interval_notifying, interval)
                        } else {
                            stringResource(id = R.string.timer_in_progress)
                        },
                        color = if (uiState.isResting) Color(0xFF00796B) else activePresetColor,
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

        Spacer(modifier = Modifier.height(28.dp))

        // Preset Exercise quick interval speed suggestions (Vibrant Palette specific categorizations)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                Triple("스쿼트", uiState.squatIntervalSeconds, stringResource(id = R.string.preset_interval_format, uiState.squatIntervalSeconds)),
                Triple("런지", uiState.lungeIntervalSeconds, stringResource(id = R.string.preset_interval_format, uiState.lungeIntervalSeconds)),
                Triple("플랭크", totalSeconds, stringResource(id = R.string.preset_check_format, totalSeconds)),
                Triple("기타", uiState.otherIntervalSeconds, stringResource(id = R.string.preset_check_format, uiState.otherIntervalSeconds))
            )

            presets.forEach { (label, secs, desc) ->
                val isSelected = uiState.timerPresetType == label
                val (itemBgColor, itemBorderColor, itemTextColor) = when (label) {
                    "스쿼트" -> Triple(Color(0xFFFFECCC), Color(0xFFE65100), Color(0xFFE65100)) // Swapped to Orange
                    "런지" -> Triple(Color(0xFFD7E3FF), Color(0xFF3F5F90), Color(0xFF3F5F90))
                    "플랭크" -> Triple(Color(0xFFFFDAD6), Color(0xFF93000A), Color(0xFF93000A))
                    "기타" -> Triple(Color(0xFFCCE8E3), Color(0xFF006A60), Color(0xFF006A60)) // Swapped to Teal
                    else -> Triple(Color(0xFFE6F3F1), Color(0xFF3F4947), Color(0xFF3F4947))
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
                            viewModel.selectPreset(label)
                        }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val labelDisplay = when (label) {
                        "스쿼트" -> stringResource(id = R.string.preset_squat)
                        "런지" -> stringResource(id = R.string.preset_lunge)
                        "플랭크" -> stringResource(id = R.string.preset_plank)
                        else -> stringResource(id = R.string.preset_other)
                    }
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
                                viewModel.totalTargetSeconds = next
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
                                viewModel.totalTargetSeconds = rounded.coerceAtLeast(5)
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
                                viewModel.totalTargetSeconds = next
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
                            routineStepsInput = listOf(com.example.data.RoutineStep("스쿼트", 60, 4, 15))
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
                        text = if (isKo) "등록된 커스텀 루틴이 없습니다." else "No custom routines saved.",
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
                                    if (!routine.id.startsWith("default_")) {
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
                                        val displayExerciseName = when (step.exerciseName) {
                                            "스쿼트", "Squat" -> "Squat"
                                            "런지", "Lunge" -> "Lunge"
                                            "플랭크", "Plank" -> "Plank"
                                            else -> "Etc."
                                        }
                                        
                                        val (chipBg, chipTx) = when (step.exerciseName) {
                                            "스쿼트", "Squat" -> Pair(Color(0xFFD05404), Color.White)
                                            "런지", "Lunge" -> Pair(Color(0xFF2B4D7E), Color.White)
                                            "플랭크", "Plank" -> Pair(Color(0xFF93000A), Color.White)
                                            else -> Pair(Color(0xFF0C6052), Color.White)
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
                    enabled = uiState.isRoutineActive || !isRunning,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (uiState.isRoutineActive) Color(0xFFFFDAD6)
                            else if (isRunning) Color(0xFFEAEAEA)
                            else Color(0xFFE6F3F1),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (uiState.isRoutineActive) Color(0xFFFFDAD6)
                            else if (isRunning) Color(0xFFDDDDDD)
                            else Color(0xFFDCE5E2),
                            CircleShape
                        )
                        .testTag("reset_timer_button")
                ) {
                    if (uiState.isRoutineActive) {
                        Icon(
                            imageVector = Icons.Default.Refresh, // base target, drawn box over it
                            contentDescription = "Stop",
                            tint = Color(0xFFBA1A1A),
                            modifier = Modifier.size(24.dp).drawBehind {
                                drawRect(
                                    color = Color(0xFFBA1A1A),
                                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.25f),
                                    size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height * 0.5f)
                                )
                            }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.desc_reset_timer),
                            tint = if (isRunning) Color.Gray.copy(alpha = 0.5f) else tealActive,
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
                IconButton(
                    onClick = {
                        viewModel.inputDurationSeconds = totalSeconds.toString()
                        viewModel.setTab(com.example.viewmodel.AppTab.Log)
                    },
                    enabled = !isRunning,
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isRunning) Color(0xFFEAEAEA) else Color(0xFFE6F3F1), CircleShape)
                        .border(1.dp, if (isRunning) Color(0xFFDDDDDD) else Color(0xFFDCE5E2), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.desc_manual_log_write),
                        tint = if (isRunning) Color.Gray.copy(alpha = 0.5f) else tealActive,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (uiState.showCompletionDialog) {
        val encouragementRes = when (uiState.timerPresetType) {
            "스쿼트" -> R.string.squat_encouragement
            "런지" -> R.string.lunge_encouragement
            "플랭크" -> R.string.plank_encouragement
            else -> R.string.other_encouragement
        }
        val tip1Res = when (uiState.timerPresetType) {
            "스쿼트" -> R.string.squat_tip1
            "런지" -> R.string.lunge_tip1
            "플랭크" -> R.string.plank_tip1
            else -> R.string.other_tip1
        }
        val tip2Res = when (uiState.timerPresetType) {
            "스쿼트" -> R.string.squat_tip2
            "런지" -> R.string.lunge_tip2
            "플랭크" -> R.string.plank_tip2
            else -> R.string.other_tip2
        }

        AlertDialog(
            onDismissRequest = { viewModel.showCompletionDialog = false },
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

                    val exercise = when (uiState.timerPresetType) {
                        "스쿼트" -> stringResource(id = R.string.preset_squat)
                        "런지" -> stringResource(id = R.string.preset_lunge)
                        "플랭크" -> stringResource(id = R.string.preset_plank)
                        else -> stringResource(id = R.string.preset_other)
                    }

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
                    onClick = { viewModel.showCompletionDialog = false },
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
fun RoutineEditDialog(
    routineId: String?,
    initialName: String,
    initialSteps: List<com.example.data.RoutineStep>,
    isKo: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, List<com.example.data.RoutineStep>) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var steps by remember { mutableStateOf(initialSteps) }
    
    val tealActive = Color(0xFF006A60)
    val charcoalDark = Color(0xFF191C1B)
    val secondaryGray = Color(0xFF3F4947)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (routineId == null) {
                    if (isKo) "새 커스텀 루틴 생성" else "Create Custom Routine"
                } else {
                    if (isKo) "커스텀 루틴 편집" else "Edit Custom Routine"
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
                    text = if (isKo) "루틴명" else "Routine Name",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = secondaryGray
                )
                
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(if (isKo) "예: 하체 버닝 세트" else "e.g. Legs Burn") },
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
                        text = if (isKo) "운동 단계 (${steps.size})" else "Exercise Steps (${steps.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryGray
                    )
                    
                    TextButton(
                        onClick = {
                            steps = steps + com.example.data.RoutineStep("스쿼트", 60, 4, 15)
                        }
                    ) {
                        Text(
                            text = if (isKo) "➕ 단계 추가" else "➕ Add Step",
                            color = tealActive,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (steps.isEmpty()) {
                    Text(
                        text = if (isKo) "등록된 운동이 없습니다. 단계를 추가해주세요." else "No steps added. Please add some exercises.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }

                steps.forEachIndexed { index, step ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F7F5)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFDCE5E2), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isKo) "${index + 1}단계" else "Step ${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = tealActive
                                )
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = steps.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index - 1]
                                                mutable[index - 1] = temp
                                                steps = mutable
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("🔼", fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < steps.size - 1) {
                                                val mutable = steps.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index + 1]
                                                mutable[index + 1] = temp
                                                steps = mutable
                                            }
                                        },
                                        enabled = index < steps.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("🔽", fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            steps = steps.filterIndexed { idx, _ -> idx != index }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("❌", fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val exerciseTypes = listOf("스쿼트", "런지", "플랭크", "기타")
                                exerciseTypes.forEach { exe ->
                                    val isSelected = step.exerciseName == exe
                                    val (selectedBg, selectedBorder, txtColor) = when (exe) {
                                        "스쿼트" -> Triple(Color(0xFFFFECCC), Color(0xFFE65100), Color(0xFFE65100))
                                        "런지" -> Triple(Color(0xFFD7E3FF), Color(0xFF3F5F90), Color(0xFF3F5F90))
                                        "플랭크" -> Triple(Color(0xFFFFDAD6), Color(0xFF93000A), Color(0xFF93000A))
                                        else -> Triple(Color(0xFFCCE8E3), Color(0xFF006A60), Color(0xFF006A60))
                                    }
                                    
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
                                            .clickable {
                                                steps = steps.mapIndexed { idx, s ->
                                                    if (idx == index) s.copy(exerciseName = exe) else s
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = exe,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) txtColor else charcoalDark
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isKo) "운동 시간" else "Duration",
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
                                                steps = steps.mapIndexed { idx, s ->
                                                    if (idx == index) s.copy(durationSeconds = (s.durationSeconds - 5).coerceAtLeast(5)) else s
                                                }
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
                                                steps = steps.mapIndexed { idx, s ->
                                                    if (idx == index) s.copy(durationSeconds = (s.durationSeconds + 5).coerceAtMost(300)) else s
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (step.exerciseName != "플랭크") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isKo) "박자 세팅" else "Rhythm Interval",
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
                                                    steps = steps.mapIndexed { idx, s ->
                                                        if (idx == index) s.copy(rhythmIntervalSeconds = (s.rhythmIntervalSeconds - 1).coerceAtLeast(1)) else s
                                                    }
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
                                                    steps = steps.mapIndexed { idx, s ->
                                                        if (idx == index) s.copy(rhythmIntervalSeconds = (s.rhythmIntervalSeconds + 1).coerceAtMost(15)) else s
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isKo) "운동 끝난 후 휴식시간" else "Rest Duration After",
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
                                                steps = steps.mapIndexed { idx, s ->
                                                    if (idx == index) s.copy(restSeconds = (s.restSeconds - 5).coerceAtLeast(0)) else s
                                                }
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
                                                steps = steps.mapIndexed { idx, s ->
                                                    if (idx == index) s.copy(restSeconds = (s.restSeconds + 5).coerceAtMost(120)) else s
                                                }
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, steps)
                    }
                },
                enabled = name.isNotBlank() && steps.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = tealActive),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isKo) "저장" else "Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isKo) "취소" else "Cancel", color = secondaryGray, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
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
