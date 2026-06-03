package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.ui.screens.ShareCardStyle
import com.example.ui.screens.ShareData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SocialShareHelper {

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareToPlatform(
        context: Context,
        appName: String,
        packageName: String,
        text: String,
        shareData: ShareData? = null,
        activeStyle: ShareCardStyle? = null
    ) {
        // First copy the formatted text & hashtags to the clipboard so the user can paste easily
        copyToClipboard(context, "Workout Stat", text)

        val resolvedPackageName = if (packageName == "com.zhiliaoapp.musically") {
            if (isSinglePackageInstalled(context, "com.zhiliaoapp.musically")) {
                "com.zhiliaoapp.musically"
            } else if (isSinglePackageInstalled(context, "com.ss.android.ugc.trill")) {
                "com.ss.android.ugc.trill"
            } else if (isSinglePackageInstalled(context, "com.ss.android.ugc.aweme")) {
                "com.ss.android.ugc.aweme"
            } else if (isSinglePackageInstalled(context, "com.zhiliaoapp.musically.go")) {
                "com.zhiliaoapp.musically.go"
            } else {
                "com.zhiliaoapp.musically"
            }
        } else {
            packageName
        }

        val isInstalled = isAppInstalled(context, packageName)
        if (isInstalled) {
            val toastMsg = context.getString(R.string.toast_copied_and_opening_app, appName)
            Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()

            // Generate image card to share if shareData and style are provided!
            var imageUri: Uri? = null
            if (shareData != null && activeStyle != null) {
                val bitmap = generateWorkoutCardBitmap(context, shareData, activeStyle)
                val file = saveBitmapToCache(context, bitmap)
                if (file != null) {
                    try {
                        imageUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                if (imageUri != null) {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    // Instagram ignores EXTRA_TEXT for image sharing to Feed/Stories.
                    // If EXTRA_TEXT is present, Instagram forces image share into DM/Direct Messages instead of showing the Feed/Stories chooser.
                    // Omit EXTRA_TEXT for Instagram when image is shared. The text is already copied to clipboard so they can paste it manually.
                    if (resolvedPackageName != "com.instagram.android") {
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                } else {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }

                setPackage(resolvedPackageName)
            }

            try {
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                // If direct packaged share fails, try packaged share with fallback
                try {
                    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                        if (imageUri != null) {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                        }
                        if (resolvedPackageName != "com.instagram.android" || imageUri == null) {
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        setPackage(resolvedPackageName)
                    }
                    context.startActivity(fallbackIntent)
                } catch (ex: Exception) {
                    fallbackSystemShare(context, text, imageUri)
                }
            }
        } else {
            val toastMsg = context.getString(R.string.toast_app_not_installed, appName)
            Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
            
            // Try fallback sharing even if target app is missing, user can choose matching target in system default
            var imageUri: Uri? = null
            if (shareData != null && activeStyle != null) {
                val bitmap = generateWorkoutCardBitmap(context, shareData, activeStyle)
                val file = saveBitmapToCache(context, bitmap)
                if (file != null) {
                    try {
                        imageUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            fallbackSystemShare(context, text, imageUri)
        }
    }

    fun shareToSystemDefault(
        context: Context,
        text: String,
        shareData: ShareData? = null,
        activeStyle: ShareCardStyle? = null
    ) {
        copyToClipboard(context, "Workout Stat", text)
        var imageUri: Uri? = null
        if (shareData != null && activeStyle != null) {
            val bitmap = generateWorkoutCardBitmap(context, shareData, activeStyle)
            val file = saveBitmapToCache(context, bitmap)
            if (file != null) {
                try {
                    imageUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        fallbackSystemShare(context, text, imageUri)
    }

    private fun fallbackSystemShare(context: Context, text: String, imageUri: Uri? = null) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            if (imageUri != null) {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.social_share_title))
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        if (packageName == "com.zhiliaoapp.musically") {
            return isSinglePackageInstalled(context, "com.zhiliaoapp.musically") ||
                    isSinglePackageInstalled(context, "com.ss.android.ugc.trill") ||
                    isSinglePackageInstalled(context, "com.ss.android.ugc.aweme") ||
                    isSinglePackageInstalled(context, "com.zhiliaoapp.musically.go")
        }
        return isSinglePackageInstalled(context, packageName)
    }

    private fun isSinglePackageInstalled(context: Context, packageName: String): Boolean {
        // Method 1: standard getPackageInfo
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            return true
        } catch (e: Exception) {
            // ignore and try next
        }

        // Method 2: launch intent check (bypasses queries list on various Android OS structures)
        try {
            if (context.packageManager.getLaunchIntentForPackage(packageName) != null) {
                return true
            }
        } catch (e: Exception) {
            // ignore and try next
        }

        // Method 3: query intent activity mapping
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                setPackage(packageName)
            }
            val resolved = context.packageManager.queryIntentActivities(intent, 0)
            if (resolved.isNotEmpty()) {
                return true
            }
        } catch (e: Exception) {
            // ignore
        }

        return false
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): File? {
        return try {
            // Remove previous share files to avoid disk pollution
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("workout_share_") && file.name.endsWith(".jpg")) {
                    file.delete()
                }
            }
            val file = File(cacheDir, "workout_share_${System.currentTimeMillis()}.jpg")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            out.flush()
            out.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateWorkoutCardBitmap(
        context: Context,
        shareData: ShareData,
        style: ShareCardStyle
    ): Bitmap {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw modern gradient background
        val colors = when (style) {
            ShareCardStyle.NEON -> intArrayOf(0xFF833AB4.toInt(), 0xFFFD1D1D.toInt(), 0xFFF56040.toInt())
            ShareCardStyle.COSMIC -> intArrayOf(0xFF0F2027.toInt(), 0xFF203A43.toInt(), 0xFF2C5364.toInt())
            ShareCardStyle.FOREST -> intArrayOf(0xFF11998E.toInt(), 0xFF38EF7D.toInt())
            ShareCardStyle.CORAL -> intArrayOf(0xFFFF512F.toInt(), 0xFFDD2476.toInt())
        }
        val gradient = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors, null, Shader.TileMode.CLAMP)
        val bgPaint = Paint().apply {
            shader = gradient
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Draw glassy translucent inner container card
        val innerCardPaint = Paint().apply {
            color = 0x22FFFFFF // 13% opacity white
            this.style = Paint.Style.FILL
            isAntiAlias = true
        }
        val innerBorderPaint = Paint().apply {
            color = 0x55FFFFFF // 33% opacity white border
            this.style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        val cardLeft = 80f
        val cardTop = 80f
        val cardRight = width - 80f
        val cardBottom = height - 80f
        val radius = 50f
        canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, radius, radius, innerCardPaint)
        canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, radius, radius, innerBorderPaint)

        // 3. Draw Header App Label & Side Tag
        val headerPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 34f
        }
        canvas.drawText(context.getString(R.string.app_name), 140f, 175f, headerPaint)

        val tagPaint = Paint().apply {
            color = 0xAFFFFFFF.toInt() // 68% opacity white text
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 30f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(context.getString(R.string.social_share_card_tag), 940f, 175f, tagPaint)

        // 4. Render Main statistics based on ShareData
        when (shareData) {
            is ShareData.SingleWorkout -> {
                val record = shareData.record
                val exerciseTitle = when (record.exerciseName) {
                    "스쿼트" -> context.getString(R.string.preset_squat)
                    "런지" -> context.getString(R.string.preset_lunge)
                    "플랭크" -> context.getString(R.string.preset_plank)
                    else -> record.exerciseName
                }

                // Exercise Name (Centered, large)
                val titlePaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textSize = 86f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(exerciseTitle, 540f, 410f, titlePaint)

                // Stats details row (Reps, Sets, Weight)
                val detailBuilder = StringBuilder()
                if (record.reps != null && record.reps > 0) {
                    detailBuilder.append(context.getString(R.string.workout_count_format, record.reps))
                    detailBuilder.append("    ")
                }
                if (record.sets != null && record.sets > 0) {
                    detailBuilder.append("${record.sets}${context.getString(R.string.unit_sets)}")
                    detailBuilder.append("    ")
                }
                if (record.weightKg != null && record.weightKg > 0.0) {
                    val wStr = if (record.weightKg % 1.0 == 0.0) record.weightKg.toInt().toString() else record.weightKg.toString()
                    detailBuilder.append("$wStr${context.getString(R.string.unit_weight)}")
                }
                val specText = detailBuilder.toString().trim()

                val specPaint = Paint().apply {
                    color = 0xEEFFFFFF.toInt() // 93% opacity white
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textSize = 48f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(specText, 540f, 540f, specPaint)

                // Duration section
                val minutes = (record.durationSeconds ?: 0) / 60
                val seconds = (record.durationSeconds ?: 0) % 60
                val durationStr = if (minutes > 0) {
                    context.getString(R.string.minutes_seconds_format, minutes, seconds)
                } else {
                    context.getString(R.string.seconds_format, seconds)
                }
                val durText = "⏱️  $durationStr"

                val durPaint = Paint().apply {
                    color = 0xCCFFFFFF.toInt() // 80% opacity white
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    textSize = 42f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(durText, 540f, 650f, durPaint)

                // Rating Stars rendering
                val starBuilder = StringBuilder()
                repeat(record.rating) {
                    starBuilder.append("★")
                }
                repeat(5 - record.rating) {
                    starBuilder.append("☆")
                }
                val starsText = starBuilder.toString()

                val starPaint = Paint().apply {
                    color = 0xFFFFD700.toInt() // Gold color
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    textSize = 58f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(starsText, 540f, 760f, starPaint)
            }
            is ShareData.GeneralStats -> {
                // Streak header
                val streakText = "🔥  ${context.getString(R.string.streak_reached_format, shareData.streak)}"
                val streakPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textSize = 72f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(streakText, 540f, 380f, streakPaint)

                // Draw vertical separators for metrics grid
                val sepPaint = Paint().apply {
                    color = 0x44FFFFFF // Soft transparent divider lines
                    strokeWidth = 3f
                    this.style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawLine(410f, 530f, 410f, 710f, sepPaint)
                canvas.drawLine(670f, 530f, 670f, 710f, sepPaint)

                val mLabelPaint = Paint().apply {
                    color = 0xB2FFFFFF.toInt() // 70% opacity white
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    textSize = 32f
                    textAlign = Paint.Align.CENTER
                }
                val mValuePaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textSize = 48f
                    textAlign = Paint.Align.CENTER
                }

                // Column 1 - Total Workouts Count
                canvas.drawText("🏋️‍♂️", 285f, 520f, mValuePaint)
                canvas.drawText(context.getString(R.string.tab_daily), 285f, 585f, mLabelPaint)
                canvas.drawText("${shareData.totalSessions}${context.getString(R.string.unit_times)}", 285f, 665f, mValuePaint)

                // Column 2 - Total Sets
                canvas.drawText("🔄", 540f, 520f, mValuePaint)
                canvas.drawText(context.getString(R.string.label_sets), 540f, 585f, mLabelPaint)
                canvas.drawText("${shareData.totalSets}${context.getString(R.string.unit_sets)}", 540f, 665f, mValuePaint)

                // Column 3 - Total Minutes duration
                canvas.drawText("⏱️", 795f, 520f, mValuePaint)
                canvas.drawText(context.getString(R.string.stat_metric_duration), 795f, 585f, mLabelPaint)
                canvas.drawText("${shareData.totalMinutes}${context.getString(R.string.unit_minutes)}", 795f, 665f, mValuePaint)
            }
        }

        // 5. Draw Date Watermark at bottom
        val todayStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        val datePaint = Paint().apply {
            color = 0x80FFFFFF.toInt() // 50% opacity white
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(todayStr, 540f, 915f, datePaint)

        return bitmap
    }
}
