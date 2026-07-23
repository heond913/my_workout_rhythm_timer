package com.example.analytics

object AnalyticsEvent {
    const val WORKOUT_START = "workout_start"
    const val WORKOUT_FINISH = "workout_finish"
    const val WORKOUT_ABANDON = "workout_abandon"
    
    const val AD_LOAD = "ad_load"
    const val AD_SHOW = "ad_show"
    const val AD_CLICK = "ad_click"
    const val AD_DISMISS = "ad_dismiss"
    const val AD_SHOW_FAILED = "ad_show_failed"
    
    const val APP_OPEN = "app_open"
    const val APP_CLOSE = "app_close"
    
    const val THEME_CHANGE = "theme_change"
    const val LANGUAGE_CHANGE = "language_change"
    const val SOUND_CHANGE = "sound_change"
    const val VIBRATION_CHANGE = "vibration_change"
    
    const val SCREEN_VIEW = "screen_view"

    const val RETENTION_PUSH_SCHEDULED = "retention_push_scheduled"
    const val RETENTION_PUSH_TRIGGERED = "retention_push_triggered"
    const val RETENTION_PUSH_SKIPPED_ACTIVE = "retention_push_skipped_active"
    const val RETENTION_PUSH_PERMISSION_DENIED = "retention_push_permission_denied"
    const val RETENTION_PUSH_CLICKED = "retention_push_clicked"
    const val WORKOUT_STARTED_FROM_RETENTION_PUSH = "workout_started_from_retention_push"
}

object AnalyticsParam {
    const val WORKOUT_TYPE = "workout_type"
    const val INTERVAL_COUNT = "interval_count"
    const val WORK_SECONDS = "work_seconds"
    const val REST_SECONDS = "rest_seconds"
    
    const val DURATION_SEC = "duration_sec"
    const val COMPLETED = "completed"
    
    const val ELAPSED_SEC = "elapsed_sec"
    const val COMPLETED_ROUND = "completed_round"
    const val REASON = "reason"
    
    const val AD_TYPE = "ad_type"
    const val SUCCESS = "success"
    const val FROM = "from"
    const val ERROR_CODE = "error_code"
    const val ERROR_DOMAIN = "error_domain"
    
    const val THEME = "theme"
    const val LANGUAGE = "language"
    const val ENABLED = "enabled"
    
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
    
    const val RETENTION_DAY = "retention_day"
}
