package com.example.analytics

interface AnalyticsRepository {
    fun logWorkoutStarted(workoutType: String, intervalCount: Int, workSeconds: Int, restSeconds: Int)
    fun logWorkoutFinished(durationSec: Int, completed: Boolean, workoutType: String)
    fun logWorkoutAbandoned(elapsedSec: Int, completedRound: Int, reason: String)
    
    fun logAdLoaded(adType: String, success: Boolean)
    fun logAdShow(adType: String, from: String)
    fun logAdClicked(adType: String)
    fun logAdDismiss(adType: String)
    fun logAdShowFailed(adType: String, errorCode: Int, errorDomain: String)
    
    fun logAppOpen()
    fun logAppClose()
    
    fun logThemeChanged(theme: String)
    fun logLanguageChanged(language: String)
    fun logSoundChanged(enabled: Boolean)
    fun logVibrationChanged(enabled: Boolean)
    
    fun logScreenView(screenName: String, screenClass: String = "MainActivity")
}
