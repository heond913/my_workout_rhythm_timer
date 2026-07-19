package com.example.worker

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object PushScheduler {
    private const val WORK_NAME_D1 = "retention_push_d1"
    private const val WORK_NAME_D3 = "retention_push_d3"

    fun scheduleRetentionWorkers(context: Context) {
        val prefs = context.getSharedPreferences("workout_rhythm_prefs", Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()

        // 1. Check and save install_date
        var installTime = prefs.getLong("install_date", 0L)
        if (installTime == 0L) {
            installTime = currentTime
            prefs.edit {
                putLong("install_date", installTime)
                putString(
                    "install_date_formatted",
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(installTime))
                )
            }
            Log.d("PushScheduler", "First launch: Saved install_date as ${Date(installTime)}")
        } else {
            Log.d("PushScheduler", "Existing install_date: ${Date(installTime)}")
        }

        val workManager = WorkManager.getInstance(context)

        // 2. Schedule D1 Retention Worker (24 hours after install)
        val d1TargetTime = installTime + TimeUnit.DAYS.toMillis(1)
        val d1Delay = d1TargetTime - currentTime
        if (d1Delay > 0) {
            val d1Request = OneTimeWorkRequest.Builder(RetentionWorker::class.java)
                .setInitialDelay(d1Delay, TimeUnit.MILLISECONDS)
                .addTag("D1_RETENTION")
                .build()

            workManager.enqueueUniqueWork(
                WORK_NAME_D1,
                ExistingWorkPolicy.KEEP,
                d1Request
            )
            Log.d("PushScheduler", "Scheduled D1 Retention Worker with delay: ${d1Delay / 1000 / 3600} hours")
        } else {
            Log.d("PushScheduler", "D1 Retention time has already passed. Skipping schedule.")
        }

        // 3. Schedule D3 Retention Worker (72 hours after install)
        val d3TargetTime = installTime + TimeUnit.DAYS.toMillis(3)
        val d3Delay = d3TargetTime - currentTime
        if (d3Delay > 0) {
            val d3Request = OneTimeWorkRequest.Builder(RetentionWorker::class.java)
                .setInitialDelay(d3Delay, TimeUnit.MILLISECONDS)
                .addTag("D3_RETENTION")
                .build()

            workManager.enqueueUniqueWork(
                WORK_NAME_D3,
                ExistingWorkPolicy.KEEP,
                d3Request
            )
            Log.d("PushScheduler", "Scheduled D3 Retention Worker with delay: ${d3Delay / 1000 / 3600} hours")
        } else {
            Log.d("PushScheduler", "D3 Retention time has already passed. Skipping schedule.")
        }
    }
}
