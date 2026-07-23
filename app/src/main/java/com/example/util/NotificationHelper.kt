package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.worker.RetentionDay

data class NotificationRecord(
    val retentionDayNumber: Int,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

object NotificationHelper {
    private const val CHANNEL_ID = "retention_channel"
    private const val NOTIFICATION_ID_D1 = 20261
    private const val NOTIFICATION_ID_D3 = 20263

    @VisibleForTesting
    val postedNotifications = mutableListOf<NotificationRecord>()

    @VisibleForTesting
    fun clearPostedNotificationsForTest() {
        postedNotifications.clear()
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.retention_channel_name)
            val descriptionText = context.getString(R.string.retention_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showRetentionNotification(
        context: Context,
        retentionDayNumber: Int,
        title: String,
        body: String
    ) {
        // Record notification for unit testing and assertion
        postedNotifications.add(NotificationRecord(retentionDayNumber, title, body))

        // Android 13+ Notification Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_retention_push", true)
            putExtra(RetentionDay.KEY_RETENTION_DAY, retentionDayNumber)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            retentionDayNumber,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = if (retentionDayNumber == 3) NOTIFICATION_ID_D3 else NOTIFICATION_ID_D1

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
