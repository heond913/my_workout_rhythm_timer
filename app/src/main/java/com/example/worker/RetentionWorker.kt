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
import com.example.util.Clock
import com.example.util.NotificationHelper
import com.example.util.SystemClock
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.concurrent.TimeUnit

class RetentionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        var clock: Clock = SystemClock
    }

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

        val currentTime = clock.currentTimeMillis()
        val firstOpenAt = stateStore.getFirstOpenAt()

        // 2. Policy evaluation (D1 vs D3 separation)
        when (retentionDay) {
            RetentionDay.D1 -> {
                val d1TargetTime = if (firstOpenAt != 0L) firstOpenAt + TimeUnit.DAYS.toMillis(1) else currentTime
                val d1GraceCutoff = d1TargetTime + RetentionConstants.D1_GRACE_PERIOD_MS
                if (firstOpenAt != 0L && currentTime > d1GraceCutoff) {
                    Log.d("RetentionWorker", "D1 campaign expired (> grace period). Skipping.")
                    stateStore.setRetentionHandled(RetentionDay.D1, true)
                    stateStore.setRetentionPermissionPending(RetentionDay.D1, false)
                    return Result.success()
                }

                val firstWorkoutStartedAt = stateStore.getFirstWorkoutStartedAt()
                if (firstWorkoutStartedAt != 0L) {
                    Log.d("RetentionWorker", "User already completed first workout. Skipping D1 push.")
                    stateStore.setRetentionHandled(RetentionDay.D1, true)
                    stateStore.setRetentionPermissionPending(RetentionDay.D1, false)
                    analytics.logRetentionSkippedActive(RetentionDay.D1.dayNumber)
                    return Result.success()
                }
            }
            RetentionDay.D3 -> {
                val d3TargetTime = if (firstOpenAt != 0L) firstOpenAt + TimeUnit.DAYS.toMillis(3) else currentTime
                val d3GraceCutoff = d3TargetTime + RetentionConstants.D3_GRACE_PERIOD_MS
                if (firstOpenAt != 0L && currentTime > d3GraceCutoff) {
                    Log.d("RetentionWorker", "D3 campaign expired (> grace period). Skipping.")
                    stateStore.setRetentionHandled(RetentionDay.D3, true)
                    stateStore.setRetentionPermissionPending(RetentionDay.D3, false)
                    return Result.success()
                }

                // D3 Activity Window is relative to D3 Target Time (d3TargetTime - 48h)
                val d3ActivityCutoff = d3TargetTime - RetentionConstants.D3_ACTIVITY_WINDOW_MS
                val lastWorkoutStartedAt = stateStore.getLastWorkoutStartedAt()
                if (lastWorkoutStartedAt >= d3ActivityCutoff) {
                    Log.d("RetentionWorker", "User worked out within the D3 activity window. Skipping D3 push.")
                    stateStore.setRetentionHandled(RetentionDay.D3, true)
                    stateStore.setRetentionPermissionPending(RetentionDay.D3, false)
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
                Log.d("RetentionWorker", "Notification permission denied for D$retentionDayNumber. Saving pending state.")
                stateStore.setRetentionPermissionPending(retentionDay, true)
                analytics.logRetentionPermissionDenied(retentionDay.dayNumber)
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
        stateStore.setRetentionPermissionPending(retentionDay, false)
        analytics.logRetentionTriggered(retentionDay.dayNumber)
        Log.d("RetentionWorker", "Retention push notification sent successfully for D$retentionDayNumber")

        return Result.success()
    }
}
