package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RetentionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RetentionWorker", "Retention push check started")
        val prefs = applicationContext.getSharedPreferences("workout_rhythm_prefs", Context.MODE_PRIVATE)
        
        // Check if the user is already active today (마지막 접속일 체크)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastLaunchDate = prefs.getString("last_launch_date", "")
        
        if (lastLaunchDate == todayDate) {
            Log.d("RetentionWorker", "User is already active today ($todayDate). Skipping notification.")
            return Result.success()
        }

        val title = applicationContext.getString(R.string.retention_push_title)
        val body = applicationContext.getString(R.string.retention_push_body)

        // Show notification
        NotificationHelper.showRetentionNotification(applicationContext, title, body)
        Log.d("RetentionWorker", "Retention push notification sent successfully")

        return Result.success()
    }
}
