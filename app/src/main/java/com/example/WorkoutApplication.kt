package com.example

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.data.RetentionStateStore
import com.example.util.AppLifecycleObserver
import com.example.worker.PushScheduler

class WorkoutApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("WorkoutApplication", "Application onCreate started")

        val stateStore = RetentionStateStore(this)
        stateStore.recordFirstOpenAt()
        stateStore.recordAppForeground()

        // Register process lifecycle observer for accurate foreground tracking across process lifetime
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver(this))

        // Initialize and schedule retention push workers (D1 and D3)
        try {
            PushScheduler.scheduleRetentionWorkers(this)
        } catch (e: Exception) {
            Log.e("WorkoutApplication", "Failed to schedule retention workers", e)
        }
    }
}
