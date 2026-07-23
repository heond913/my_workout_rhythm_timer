package com.example.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.analytics.FirebaseAnalyticsRepository
import com.example.data.RetentionStateStore
import com.example.util.NotificationHelper
import com.google.firebase.analytics.FirebaseAnalytics

class RetentionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val retentionDayNumber = inputData.getInt(RetentionDay.KEY_RETENTION_DAY, 1)
        val retentionDay = RetentionDay.fromDayNumber(retentionDayNumber)
        Log.d("RetentionWorker", "Retention push check started for D$retentionDayNumber")

        val stateStore = RetentionStateStore(applicationContext)
        val analytics = FirebaseAnalyticsRepository(FirebaseAnalytics.getInstance(applicationContext))

        // 1. Check if already handled
        if (stateStore.isRetentionHandled(retentionDay)) {
            Log.d("RetentionWorker", "D$retentionDayNumber retention already handled. Skipping.")
            return Result.success()
        }

        // 2. Policy evaluation (D1 vs D3 separation)
        val currentTime = System.currentTimeMillis()
        when (retentionDay) {
            RetentionDay.D1 -> {
                // D1 Policy: First Workout Activation
                val firstWorkoutStartedAt = stateStore.getFirstWorkoutStartedAt()
                if (firstWorkoutStartedAt != 0L) {
                    Log.d("RetentionWorker", "User already completed first workout. Skipping D1 push.")
                    stateStore.setRetentionHandled(RetentionDay.D1, true)
                    analytics.logRetentionSkippedActive(RetentionDay.D1.dayNumber)
                    return Result.success()
                }
            }
            RetentionDay.D3 -> {
                // D3 Policy: Repeat Workout Activation (checked against 48h activity window)
                val lastWorkoutStartedAt = stateStore.getLastWorkoutStartedAt()
                if (lastWorkoutStartedAt > 0L && (currentTime - lastWorkoutStartedAt < RetentionConstants.D3_ACTIVITY_WINDOW_MS)) {
                    Log.d("RetentionWorker", "User worked out within the last 48 hours. Skipping D3 push.")
                    stateStore.setRetentionHandled(RetentionDay.D3, true)
                    analytics.logRetentionSkippedActive(RetentionDay.D3.dayNumber)
                    return Result.success()
                }
            }
        }

        // 3. Check Notification Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("RetentionWorker", "Notification permission denied for D$retentionDayNumber.")
                analytics.logRetentionPermissionDenied(retentionDay.dayNumber)
                // Do NOT set handled = true so permission grant later allows retry
                return Result.success()
            }
        }

        // 4. Send Notification
        val titleRes = when (retentionDay) {
            RetentionDay.D1 -> R.string.retention_push_d1_title
            RetentionDay.D3 -> R.string.retention_push_d3_title
        }
        val bodyRes = when (retentionDay) {
            RetentionDay.D1 -> R.string.retention_push_d1_body
            RetentionDay.D3 -> R.string.retention_push_d3_body
        }

        val title = applicationContext.getString(titleRes)
        val body = applicationContext.getString(bodyRes)

        NotificationHelper.showRetentionNotification(
            context = applicationContext,
            retentionDayNumber = retentionDay.dayNumber,
            title = title,
            body = body
        )

        stateStore.setRetentionHandled(retentionDay, true)
        analytics.logRetentionTriggered(retentionDay.dayNumber)
        Log.d("RetentionWorker", "Retention push notification sent successfully for D$retentionDayNumber")

        return Result.success()
    }
}
