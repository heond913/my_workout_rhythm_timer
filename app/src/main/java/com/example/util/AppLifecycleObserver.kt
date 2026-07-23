package com.example.util

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.data.RetentionStateStore

class AppLifecycleObserver(private val context: Context) : DefaultLifecycleObserver {
    private val stateStore = RetentionStateStore(context)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d("AppLifecycleObserver", "App entered foreground")
        stateStore.recordAppForeground()
    }
}
