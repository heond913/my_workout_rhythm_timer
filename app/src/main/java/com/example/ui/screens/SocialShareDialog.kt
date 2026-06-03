package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.WorkoutRecord
import com.example.util.SocialShareHelper
import java.text.SimpleDateFormat
import java.util.*

enum class ShareCardStyle {
    NEON,
    COSMIC,
    FOREST,
    CORAL
}

sealed class ShareData {
    data class SingleWorkout(val record: WorkoutRecord) : ShareData()
    data class GeneralStats(
        val streak: Int,
        val totalSessions: Int,
        val totalSets: Int,
        val maxWeight: Double,
        val totalMinutes: Int,
        val totalSeconds: Int
    ) : ShareData()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialShareDialog(
    shareData: ShareData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeStyle by remember { mutableStateOf(ShareCardStyle.NEON) }

    // Color definitions
    val modalBg = Color(0xFFFBFDF9)
    val txtPrimary = Color(0xFF191C1B)
    val txtSecondary = Color(0xFF3F4947)

    // Gradients for the card depending on active style
    val cardBrush = when (activeStyle) {
        ShareCardStyle.NEON -> Brush.linearGradient(
            colors = listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFF56040))
        )
        ShareCardStyle.COSMIC -> Brush.linearGradient(
            colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        )
        ShareCardStyle.FOREST -> Brush.linearGradient(
            colors = listOf(Color(0xFF11998E), Color(0xFF38EF7D))
        )
        ShareCardStyle.CORAL -> Brush.linearGradient(
            colors = listOf(Color(0xFFFF512F), Color(0xFFDD2476))
        )
    }

    val cardTextColor = Color.White
    val secondaryCardTextColor = Color.White.copy(alpha = 0.85f)

    // Formatted Text generators for social copying
    val generatedShareText = remember(shareData, activeStyle, context) {
        when (shareData) {
            is ShareData.SingleWorkout -> {
                val record = shareData.record
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateStr = sdf.format(Date(record.timestamp))

                val exerciseTitle = when (record.exerciseName) {
                    "스쿼트" -> context.getString(R.string.preset_squat)
                    "런지" -> context.getString(R.string.preset_lunge)
                    "플랭크" -> context.getString(R.string.preset_plank)
                    else -> record.exerciseName
                }

                val detailsBuilder = StringBuilder()
                if (record.reps != null && record.reps > 0) {
                    detailsBuilder.append("💪 ")
                    detailsBuilder.append(context.getString(R.string.workout_count_format, record.reps))
                    detailsBuilder.append("\n")
                }
                if (record.sets != null && record.sets > 0) {
                    detailsBuilder.append("🔄 ")
                    detailsBuilder.append(record.sets)
                    detailsBuilder.append(context.getString(R.string.unit_sets))
                    detailsBuilder.append("\n")
                }
                if (record.weightKg != null && record.weightKg > 0.0) {
                    detailsBuilder.append("🏋️‍♂️ ")
                    detailsBuilder.append(if (record.weightKg % 1.0 == 0.0) record.weightKg.toInt().toString() else record.weightKg)
                    detailsBuilder.append(context.getString(R.string.unit_weight))
                    detailsBuilder.append("\n")
                }

                val minutes = (record.durationSeconds ?: 0) / 60
                val seconds = (record.durationSeconds ?: 0) % 60
                val durStr = if (minutes > 0) {
                    context.getString(R.string.minutes_seconds_format, minutes, seconds)
                } else {
                    context.getString(R.string.seconds_format, seconds)
                }

                val ratingStars = "⭐".repeat(record.rating)

                context.getString(
                    R.string.share_text_success_format,
                    exerciseTitle,
                    dateStr,
                    detailsBuilder.toString(),
                    durStr,
                    ratingStars
                )
            }
            is ShareData.GeneralStats -> {
                context.getString(
                    R.string.share_text_stats_format,
                    shareData.streak,
                    shareData.totalSessions,
                    shareData.totalSets,
                    if (shareData.maxWeight % 1.0 == 0.0) shareData.maxWeight.toInt().toString() else shareData.maxWeight.toString(),
                    shareData.totalMinutes,
                    shareData.totalSeconds
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = modalBg),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .border(1.dp, Color(0xFFDCE5E2), RoundedCornerShape(24.dp))
                .testTag("social_share_dialog_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header of Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.social_share_title),
                            color = txtPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = stringResource(id = R.string.social_share_subtitle),
                            color = txtSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color(0xFFE6F3F1), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close share",
                            tint = Color(0xFF006A60),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preview Card Card Frame (Aesthetic UI)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(cardBrush)
                            .padding(20.dp)
                    ) {
                        // Title header inside card
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = cardTextColor.copy(alpha = 0.9f),
                            modifier = Modifier.align(Alignment.TopStart)
                        )

                        Text(
                            text = stringResource(id = R.string.social_share_card_tag),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = cardTextColor.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.TopEnd)
                        )

                        // Main Content layout dependent on ShareData
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (shareData) {
                                is ShareData.SingleWorkout -> {
                                    val record = shareData.record
                                    val exerciseTitle = when (record.exerciseName) {
                                        "스쿼트" -> stringResource(id = R.string.preset_squat)
                                        "런지" -> stringResource(id = R.string.preset_lunge)
                                        "플랭크" -> stringResource(id = R.string.preset_plank)
                                        else -> record.exerciseName
                                    }

                                    Text(
                                        text = exerciseTitle,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = cardTextColor,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (record.reps != null && record.reps > 0) {
                                            Text(
                                                text = stringResource(id = R.string.workout_count_format, record.reps),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = secondaryCardTextColor
                                            )
                                        }
                                        if (record.sets != null && record.sets > 0) {
                                            Text(
                                                text = "${record.sets}${stringResource(id = R.string.unit_sets)}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = secondaryCardTextColor
                                            )
                                        }
                                        if (record.weightKg != null && record.weightKg > 0.0) {
                                            Text(
                                                text = "${if (record.weightKg % 1.0 == 0.0) record.weightKg.toInt().toString() else record.weightKg}${stringResource(id = R.string.unit_weight)}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = secondaryCardTextColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Duration
                                    val minutes = (record.durationSeconds ?: 0) / 60
                                    val seconds = (record.durationSeconds ?: 0) % 60
                                    val durationStr = if (minutes > 0) {
                                        stringResource(id = R.string.minutes_seconds_format, minutes, seconds)
                                    } else {
                                        stringResource(id = R.string.seconds_format, seconds)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = secondaryCardTextColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = durationStr,
                                            fontSize = 12.sp,
                                            color = secondaryCardTextColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Intensity Star ratings
                                    Row {
                                        repeat(record.rating) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFBC02D),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                is ShareData.GeneralStats -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Color(0xFFFFCC00),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(id = R.string.streak_reached_format, shareData.streak),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = cardTextColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        // Total workouts
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.FitnessCenter,
                                                contentDescription = null,
                                                tint = secondaryCardTextColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${shareData.totalSessions}${stringResource(id = R.string.unit_times)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = cardTextColor
                                            )
                                            Text(
                                                text = stringResource(id = R.string.tab_daily),
                                                fontSize = 9.sp,
                                                color = secondaryCardTextColor
                                            )
                                        }

                                        // Total sets
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Restore,
                                                contentDescription = null,
                                                tint = secondaryCardTextColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${shareData.totalSets}${stringResource(id = R.string.unit_sets)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = cardTextColor
                                            )
                                            Text(
                                                text = stringResource(id = R.string.label_sets),
                                                fontSize = 9.sp,
                                                color = secondaryCardTextColor
                                            )
                                        }

                                        // Total Minutes
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = secondaryCardTextColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${shareData.totalMinutes}${stringResource(id = R.string.unit_minutes)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = cardTextColor
                                            )
                                            Text(
                                                text = stringResource(id = R.string.stat_metric_duration),
                                                fontSize = 9.sp,
                                                color = secondaryCardTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom date watermark
                        val todayStr = remember {
                            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                            sdf.format(Date())
                        }
                        Text(
                            text = todayStr,
                            fontSize = 10.sp,
                            color = cardTextColor.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Style selection controls (Theme customization)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(id = R.string.share_card_style),
                        color = txtSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShareCardStyle.values().forEach { style ->
                            val isSelected = activeStyle == style
                            val borderCol = if (isSelected) Color(0xFF006A60) else Color.Transparent
                            val backCol = when (style) {
                                ShareCardStyle.NEON -> Color(0xFFE52D27)
                                ShareCardStyle.COSMIC -> Color(0xFF1F2B36)
                                ShareCardStyle.FOREST -> Color(0xFF009688)
                                ShareCardStyle.CORAL -> Color(0xFFFF5722)
                            }
                            val styleLabel = when (style) {
                                ShareCardStyle.NEON -> stringResource(id = R.string.share_card_style_neon)
                                ShareCardStyle.COSMIC -> stringResource(id = R.string.share_card_style_cosmic)
                                ShareCardStyle.FOREST -> stringResource(id = R.string.share_card_style_forest)
                                ShareCardStyle.CORAL -> stringResource(id = R.string.share_card_style_coral)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(backCol)
                                    .border(2.dp, borderCol, RoundedCornerShape(10.dp))
                                    .clickable { activeStyle = style }
                                    .padding(vertical = 10.dp)
                                    .testTag("style_tab_${style.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = styleLabel,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Platform share action row (Instagram, TikTok, and General standard Intent)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Instagram Button
                    Button(
                        onClick = {
                            SocialShareHelper.shareToPlatform(
                                context = context,
                                appName = "Instagram",
                                packageName = "com.instagram.android",
                                text = generatedShareText,
                                shareData = shareData,
                                activeStyle = activeStyle
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC13584) // Instagram pinkish gradient base
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("instagram_share_btn"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📷  " + stringResource(id = R.string.btn_share_instagram),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // System Custom Share Button
                    OutlinedButton(
                        onClick = {
                            SocialShareHelper.shareToSystemDefault(
                                context = context,
                                text = generatedShareText,
                                shareData = shareData,
                                activeStyle = activeStyle
                            )
                        },
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(Color(0xFF006A60), Color(0xFF006A60)))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("system_share_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF006A60),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.btn_share_system),
                                color = Color(0xFF006A60),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
