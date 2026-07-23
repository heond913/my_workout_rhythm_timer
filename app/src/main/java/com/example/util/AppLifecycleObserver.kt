package com.example.util

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.data.RetentionStateStore
import com.example.worker.PushScheduler

class AppLifecycleObserver(private val context: Context) : DefaultLifecycleObserver {
    private val stateStore = RetentionStateStore(context)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d("AppLifecycleObserver", "App entered foreground")
        stateStore.recordAppForeground()
        try {
            PushScheduler.processPendingRetentionCampaigns(context)
        } catch (e: Exception) {
            Log.e("AppLifecycleObserver", "Failed to process pending retention campaigns", e)
        }
    }
}
