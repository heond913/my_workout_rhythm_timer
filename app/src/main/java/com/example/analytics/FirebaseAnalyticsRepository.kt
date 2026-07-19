package com.example.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

class FirebaseAnalyticsRepository @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsRepository {

    override fun logWorkoutStarted(workoutType: String, intervalCount: Int, workSeconds: Int, restSeconds: Int) {
        val bundle = Bundle().apply {
            putString(AnalyticsParam.WORKOUT_TYPE, workoutType)
            putInt(AnalyticsParam.INTERVAL_COUNT, intervalCount)
            putInt(AnalyticsParam.WORK_SECONDS, workSeconds)
            putInt(AnalyticsParam.REST_SECONDS, restSeconds)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.WORKOUT_START, bundle)
    }

    override fun logWorkoutFinished(durationSec: Int, completed: Boolean, workoutType: String) {
        val bundle = Bundle().apply {
            putInt(AnalyticsParam.DURATION_SEC, durationSec)
            putBoolean(AnalyticsParam.COMPLETED, completed)
            putString(AnalyticsParam.WORKOUT_TYPE, workoutType)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.WORKOUT_FINISH, bundle)
    }

    override fun logWorkoutAbandoned(elapsedSec: Int, completedRound: Int, reason: String) {
        val bundle = Bundle().apply {
            putInt(AnalyticsParam.ELAPSED_SEC, elapsedSec)
            putInt(AnalyticsParam.COMPLETED_ROUND, completedRound)
            putString(AnalyticsParam.REASON, reason)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.WORKOUT_ABANDON, bundle)
    }

    override fun logAdLoaded(adType: String, success: Boolean) {
        val bundle = Bundle().apply {
            putString(AnalyticsParam.AD_TYPE, adType)
            putBoolean(AnalyticsParam.SUCCESS, success)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.AD_LOAD, bundle)
    }

    override fun logAdShow(adType: String, from: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsParam.AD_TYPE, adType)
            putString(AnalyticsParam.FROM, from)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.AD_SHOW, bundle)
    }

    override fun logAdClicked(adType: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsParam.AD_TYPE, adType)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.AD_CLICK, bundle)
    }

    override fun logAdDismiss(adType: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsParam.AD_TYPE, adType)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.AD_DISMISS, bundle)
    }

    override fun logAdShowFailed(adType: String, errorCode: Int, errorDomain: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsParam.AD_TYPE, adType)
            putInt(AnalyticsParam.ERROR_CODE, errorCode)
            putString(AnalyticsParam.ERROR_DOMAIN, errorDomain)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.AD_SHOW_FAILED, bundle)
    }

    override fun logAppOpen() {
        firebaseAnalytics.logEvent(AnalyticsEvent.APP_OPEN, null)
    }

    override fun logAppClose() {
        firebaseAnalytics.logEvent(AnalyticsEvent.APP_CLOSE, null)
    }

    override fun logThemeChanged(theme: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsParam.THEME, theme)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.THEME_CHANGE, bundle)
    }

    override fun logLanguageChanged(language: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsParam.LANGUAGE, language)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.LANGUAGE_CHANGE, bundle)
    }

    override fun logSoundChanged(enabled: Boolean) {
        val bundle = Bundle().apply {
            putBoolean(AnalyticsParam.ENABLED, enabled)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.SOUND_CHANGE, bundle)
    }

    override fun logVibrationChanged(enabled: Boolean) {
        val bundle = Bundle().apply {
            putBoolean(AnalyticsParam.ENABLED, enabled)
        }
        firebaseAnalytics.logEvent(AnalyticsEvent.VIBRATION_CHANGE, bundle)
    }

    override fun logScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
}
