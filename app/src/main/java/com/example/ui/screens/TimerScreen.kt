package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import android.media.AudioManager
import java.util.Locale
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.TimerMode
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

        Spacer(modifier = Modifier.height(16.dp))

        // Center circular timer visualizer
        val progressFraction = if (totalSeconds > 0) remaining.toFloat() / totalSeconds.toFloat() else 0f

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
                        color = activePresetBgColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Draw progress arc in active preset custom color
                    drawArc(
                        color = activePresetColor,
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
                    tint = activePresetColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Giant time numbers in high-contrast charcoal
                val minutesString = String.format(Locale.getDefault(), "%02d", remaining / 60)
                val secondsString = String.format(Locale.getDefault(), "%02d", remaining % 60)

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
                    color = activePresetColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                if (isRunning) {
                    Text(
                        text = if (interval > 0) stringResource(id = R.string.rhythm_interval_notifying, interval) else stringResource(id = R.string.timer_in_progress),
                        color = activePresetColor,
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
        if (!isRunning) {
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
                            // [방법 3 적용] 터치 영역은 48.dp, 눈에 보이는 하얀 원은 36.dp 크기로 축소
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
                                modifier = Modifier.size(14.dp) // 버튼 크기에 맞게 내부 대시 기호 크기 조정
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
                            // [방법 3 적용] 마이너스 버튼과 완벽한 대칭을 위해 동일한 터치 영역 및 패딩 지정
                            modifier = Modifier
                                .size(48.dp)
                                .padding(6.dp) // 안쪽으로 6.dp 밀려 들어가 실제 보이는 녹색 원은 36.dp 크기가 됨
                                .background(tealActive, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.desc_increase_5_sec),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp) // 마이너스 아이콘과 동일하게 14.dp로 조절하여 밸런스 매칭
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
                            // [방법 3 적용] 터치 영역은 48.dp, 눈에 보이는 하얀 원은 36.dp 크기로 축소
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
                                modifier = Modifier.size(14.dp) // 버튼 크기에 맞게 내부 대시 기호 크기 조정
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
                            // [방법 3 적용] 마이너스 버튼과 완벽한 대칭을 위해 동일한 터치 영역 및 패딩 지정
                            modifier = Modifier
                                .size(48.dp)
                                .padding(6.dp) // 안쪽으로 6.dp 밀려 들어가 실제 보이는 녹색 원은 36.dp 크기가 됨
                                .background(tealActive, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.desc_increase_volume),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp) // 마이너스 아이콘과 동일하게 14.dp로 조절하여 밸런스 매칭
                            )
                        }
                    }
                }
            }
        }

        if (!isRunning) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFDCE5E2), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(id = R.string.rhythm_individ_config_title),
                        color = charcoalDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Squat Config Column Block - Styled in Orange Theme (Swapped)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFFFECCC).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFFFECCC), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(stringResource(id = R.string.preset_squat), color = Color(0xFFE65100), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .background(Color.White, CircleShape)
                                            .border(1.dp, Color(0xFFFFECCC), CircleShape)
                                            .clickable { viewModel.updateSquatInterval(uiState.squatIntervalSeconds - 1) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", color = Color(0xFFE65100), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = stringResource(id = R.string.seconds_format, uiState.squatIntervalSeconds),
                                        color = charcoalDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1.3f),
                                        textAlign = TextAlign.Center
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .background(Color(0xFFE65100), CircleShape)
                                            .clickable { viewModel.updateSquatInterval(uiState.squatIntervalSeconds + 1) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
 
                            // Lunge Config Column Block
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFD7E3FF).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFD7E3FF), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(stringResource(id = R.string.preset_lunge), color = Color(0xFF3F5F90), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .background(Color.White, CircleShape)
                                            .border(1.dp, Color(0xFFD7E3FF), CircleShape)
                                            .clickable { viewModel.updateLungeInterval(uiState.lungeIntervalSeconds - 1) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", color = Color(0xFF3F5F90), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = stringResource(id = R.string.seconds_format, uiState.lungeIntervalSeconds),
                                        color = charcoalDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1.3f),
                                        textAlign = TextAlign.Center
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .background(Color(0xFF3F5F90), CircleShape)
                                            .clickable { viewModel.updateLungeInterval(uiState.lungeIntervalSeconds + 1) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Other/기타 Config Column Block - Styled in Teal Theme (Swapped)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFE6F3F1), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFCCE8E3), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(stringResource(id = R.string.preset_other), color = Color(0xFF006A60), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .background(Color.White, CircleShape)
                                            .border(1.dp, Color(0xFFCCE8E3), CircleShape)
                                            .clickable { viewModel.updateOtherInterval(uiState.otherIntervalSeconds - 1) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", color = Color(0xFF006A60), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = stringResource(id = R.string.seconds_format, uiState.otherIntervalSeconds),
                                        color = charcoalDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1.3f),
                                        textAlign = TextAlign.Center
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .background(Color(0xFF006A60), CircleShape)
                                            .clickable { viewModel.updateOtherInterval(uiState.otherIntervalSeconds + 1) },
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
                // Reset Button
                IconButton(
                    onClick = { viewModel.resetTimer() },
                    enabled = !isRunning,
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isRunning) Color(0xFFEAEAEA) else Color(0xFFE6F3F1), CircleShape)
                        .border(1.dp, if (isRunning) Color(0xFFDDDDDD) else Color(0xFFDCE5E2), CircleShape)
                        .testTag("reset_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.desc_reset_timer),
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
                Column {
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
                        modifier = Modifier.fillMaxWidth(),
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
}
