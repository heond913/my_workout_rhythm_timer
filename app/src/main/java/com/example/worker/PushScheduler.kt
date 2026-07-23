package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.analytics.FirebaseAnalyticsRepository
import com.example.data.RetentionStateStore
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.Date
import java.util.concurrent.TimeUnit

object PushScheduler {
    const val WORK_NAME_D1 = "retention_push_d1"
    const val WORK_NAME_D3 = "retention_push_d3"

    fun scheduleRetentionWorkers(context: Context) {
        val stateStore = RetentionStateStore(context)
        val currentTime = System.currentTimeMillis()

        // 1. Get or record first_open_at
        val firstOpenAt = stateStore.recordFirstOpenAt(currentTime)
        Log.d("PushScheduler", "First open timestamp: ${Date(firstOpenAt)}")

        val workManager = WorkManager.getInstance(context)
        val analytics = FirebaseAnalyticsRepository(FirebaseAnalytics.getInstance(context))

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
}
