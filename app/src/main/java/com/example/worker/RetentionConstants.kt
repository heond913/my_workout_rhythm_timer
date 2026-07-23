package com.example.worker

object RetentionConstants {
    const val D3_ACTIVITY_WINDOW_HOURS = 48L
    const val D3_ACTIVITY_WINDOW_MS = D3_ACTIVITY_WINDOW_HOURS * 60 * 60 * 1000L // 48 hours

    const val RETENTION_ATTRIBUTION_WINDOW_MINUTES = 30L
    const val RETENTION_ATTRIBUTION_WINDOW_MS = RETENTION_ATTRIBUTION_WINDOW_MINUTES * 60 * 1000L // 30 minutes
}
