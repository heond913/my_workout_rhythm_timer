package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.worker.PushScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("WorkoutApplication", "Application onCreate started")

        // 1. Update the last launch date/access time for retention push suppression (중복 방지 정책)
        val prefs = getSharedPreferences("workout_rhythm_prefs", Context.MODE_PRIVATE)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit().putString("last_launch_date", todayDate).apply()
        Log.d("WorkoutApplication", "Updated last_launch_date to: $todayDate")

        // 2. Initialize and schedule retention push workers (D1 and D3)
        try {
            PushScheduler.scheduleRetentionWorkers(this)
        } catch (e: Exception) {
            Log.e("WorkoutApplication", "Failed to schedule retention workers", e)
        }
    }
}
