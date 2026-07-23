package com.example.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.R
import com.example.analytics.FirebaseAnalyticsRepository
import com.example.data.RetentionStateStore
import com.example.util.Clock
import com.example.util.NotificationHelper
import com.example.util.SystemClock
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.Date
import java.util.concurrent.TimeUnit

object PushScheduler {
    const val WORK_NAME_D1 = "retention_push_d1"
    const val WORK_NAME_D3 = "retention_push_d3"

    var clock: Clock = SystemClock

    fun scheduleRetentionWorkers(context: Context) {
        val stateStore = RetentionStateStore(context)
        val currentTime = clock.currentTimeMillis()

        // 1. Get or record first_open_at
        val firstOpenAt = stateStore.recordFirstOpenAt(currentTime)
        Log.d("PushScheduler", "First open timestamp: ${Date(firstOpenAt)}")

        val workManager = WorkManager.getInstance(context)
        val analytics = FirebaseAnalyticsRepository(FirebaseAnalytics.getInstance(context))

        // Check and process any pending retention permission campaigns if permission is now granted
        processPendingRetentionCampaigns(context)

        // 2. Schedule D1 Retention Worker (24 hours after first open)
        if (!stateStore.isRetentionHandled(RetentionDay.D1)) {
            val d1TargetTime = firstOpenAt + TimeUnit.DAYS.toMillis(1)
            val d1Delay = (d1TargetTime - currentTime).coerceAtLeast(0L)
            
            val d1Request = OneTimeWorkRequest.Builder(RetentionWorker::class.java)
                .setInitialDelay(d1Delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to RetentionDay.D1.dayNumber))
                .addTag("D1_RETENTION")
                .build()

            workManager.enqueueUniqueWork(
                WORK_NAME_D1,
                ExistingWorkPolicy.KEEP,
                d1Request
            )

            if (!stateStore.isRetentionScheduled(RetentionDay.D1)) {
                stateStore.setRetentionScheduled(RetentionDay.D1, true)
                analytics.logRetentionScheduled(RetentionDay.D1.dayNumber)
            }
            Log.d("PushScheduler", "Scheduled D1 Retention Worker with delay: ${d1Delay / 1000 / 3600} hours")
        } else {
            Log.d("PushScheduler", "D1 Retention already handled. Skipping schedule.")
        }

        // 3. Schedule D3 Retention Worker (72 hours after first open)
        if (!stateStore.isRetentionHandled(RetentionDay.D3)) {
            val d3TargetTime = firstOpenAt + TimeUnit.DAYS.toMillis(3)
            val d3Delay = (d3TargetTime - currentTime).coerceAtLeast(0L)

            val d3Request = OneTimeWorkRequest.Builder(RetentionWorker::class.java)
                .setInitialDelay(d3Delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to RetentionDay.D3.dayNumber))
                .addTag("D3_RETENTION")
                .build()

            workManager.enqueueUniqueWork(
                WORK_NAME_D3,
                ExistingWorkPolicy.KEEP,
                d3Request
            )

            if (!stateStore.isRetentionScheduled(RetentionDay.D3)) {
                stateStore.setRetentionScheduled(RetentionDay.D3, true)
                analytics.logRetentionScheduled(RetentionDay.D3.dayNumber)
            }
            Log.d("PushScheduler", "Scheduled D3 Retention Worker with delay: ${d3Delay / 1000 / 3600} hours")
        } else {
            Log.d("PushScheduler", "D3 Retention already handled. Skipping schedule.")
        }
    }

    fun processPendingRetentionCampaigns(context: Context) {
        val stateStore = RetentionStateStore(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val firstOpenAt = stateStore.getFirstOpenAt()
        if (firstOpenAt == 0L) return

        val currentTime = clock.currentTimeMillis()
        val analytics = FirebaseAnalyticsRepository(FirebaseAnalytics.getInstance(context))

        for (day in listOf(RetentionDay.D1, RetentionDay.D3)) {
            if (!stateStore.isRetentionHandled(day) && stateStore.isRetentionPermissionPending(day)) {
                when (day) {
                    RetentionDay.D1 -> {
                        val d1TargetTime = firstOpenAt + TimeUnit.DAYS.toMillis(1)
                        val d1GraceCutoff = d1TargetTime + RetentionConstants.D1_GRACE_PERIOD_MS
                        if (currentTime > d1GraceCutoff) {
                            stateStore.setRetentionHandled(RetentionDay.D1, true)
                            stateStore.setRetentionPermissionPending(RetentionDay.D1, false)
                            continue
                        }
                        val firstWorkoutStartedAt = stateStore.getFirstWorkoutStartedAt()
                        if (firstWorkoutStartedAt != 0L) {
                            stateStore.setRetentionHandled(RetentionDay.D1, true)
                            stateStore.setRetentionPermissionPending(RetentionDay.D1, false)
                            analytics.logRetentionSkippedActive(RetentionDay.D1.dayNumber)
                            continue
                        }

                        val title = context.getString(R.string.retention_push_d1_title)
                        val body = context.getString(R.string.retention_push_d1_body)

                        if (stateStore.tryClaimRetentionTrigger(RetentionDay.D1)) {
                            NotificationHelper.showRetentionNotification(context, RetentionDay.D1.dayNumber, title, body)
                            analytics.logRetentionTriggered(RetentionDay.D1.dayNumber)
                            Log.d("PushScheduler", "Pending retention push sent for D1")
                        }
                    }
                    RetentionDay.D3 -> {
                        val d3TargetTime = firstOpenAt + TimeUnit.DAYS.toMillis(3)
                        val d3GraceCutoff = d3TargetTime + RetentionConstants.D3_GRACE_PERIOD_MS
                        if (currentTime > d3GraceCutoff) {
                            stateStore.setRetentionHandled(RetentionDay.D3, true)
                            stateStore.setRetentionPermissionPending(RetentionDay.D3, false)
                            continue
                        }
                        val d3ActivityCutoff = d3TargetTime - RetentionConstants.D3_ACTIVITY_WINDOW_MS
                        val lastWorkoutStartedAt = stateStore.getLastWorkoutStartedAt()
                        if (lastWorkoutStartedAt >= d3ActivityCutoff) {
                            stateStore.setRetentionHandled(RetentionDay.D3, true)
                            stateStore.setRetentionPermissionPending(RetentionDay.D3, false)
                            analytics.logRetentionSkippedActive(RetentionDay.D3.dayNumber)
                            continue
                        }

                        val title = context.getString(R.string.retention_push_d3_title)
                        val body = context.getString(R.string.retention_push_d3_body)

                        if (stateStore.tryClaimRetentionTrigger(RetentionDay.D3)) {
                            NotificationHelper.showRetentionNotification(context, RetentionDay.D3.dayNumber, title, body)
                            analytics.logRetentionTriggered(RetentionDay.D3.dayNumber)
                            Log.d("PushScheduler", "Pending retention push sent for D3")
                        }
                    }
                }
            }
        }
    }
}
