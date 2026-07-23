package com.example.worker

object RetentionConstants {
    const val D1_DELAY_HOURS = 24L
    const val D1_GRACE_PERIOD_HOURS = 24L
    const val D1_GRACE_PERIOD_MS = D1_GRACE_PERIOD_HOURS * 60 * 60 * 1000L // 24 hours

    const val D3_DELAY_HOURS = 72L
    const val D3_ACTIVITY_WINDOW_HOURS = 48L
    const val D3_ACTIVITY_WINDOW_MS = D3_ACTIVITY_WINDOW_HOURS * 60 * 60 * 1000L // 48 hours
    const val D3_GRACE_PERIOD_HOURS = 24L
    const val D3_GRACE_PERIOD_MS = D3_GRACE_PERIOD_HOURS * 60 * 60 * 1000L // 24 hours

    const val RETENTION_ATTRIBUTION_WINDOW_MINUTES = 30L
    const val RETENTION_ATTRIBUTION_WINDOW_MS = RETENTION_ATTRIBUTION_WINDOW_MINUTES * 60 * 1000L // 30 minutes
}
