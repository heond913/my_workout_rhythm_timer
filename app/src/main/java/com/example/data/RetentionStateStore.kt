package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.worker.RetentionConstants
import com.example.worker.RetentionDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RetentionStateStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("workout_rhythm_prefs", Context.MODE_PRIVATE)

    fun getFirstOpenAt(): Long {
        var firstOpen = prefs.getLong(KEY_FIRST_OPEN_AT, 0L)
        if (firstOpen == 0L) {
            // Fallback to legacy install_date if present
            firstOpen = prefs.getLong(KEY_LEGACY_INSTALL_DATE, 0L)
        }
        return firstOpen
    }

    fun recordFirstOpenAt(timestamp: Long = System.currentTimeMillis()): Long {
        val existing = getFirstOpenAt()
        if (existing != 0L) return existing

        prefs.edit {
            putLong(KEY_FIRST_OPEN_AT, timestamp)
            putLong(KEY_LEGACY_INSTALL_DATE, timestamp)
            putString(
                "first_open_formatted",
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
            )
        }
        return timestamp
    }

    fun recordAppForeground(timestamp: Long = System.currentTimeMillis()) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
        prefs.edit {
            putLong(KEY_LAST_APP_FOREGROUND_AT, timestamp)
            putString("last_launch_date", todayDate)
        }
    }

    fun getLastAppForegroundAt(): Long = prefs.getLong(KEY_LAST_APP_FOREGROUND_AT, 0L)

    fun recordWorkoutStarted(timestamp: Long = System.currentTimeMillis()) {
        val firstStarted = prefs.getLong(KEY_FIRST_WORKOUT_STARTED_AT, 0L)
        prefs.edit {
            if (firstStarted == 0L) {
                putLong(KEY_FIRST_WORKOUT_STARTED_AT, timestamp)
            }
            putLong(KEY_LAST_WORKOUT_STARTED_AT, timestamp)
            putBoolean(KEY_HAS_STARTED_WORKOUT, true)
        }
    }

    fun hasStartedWorkout(): Boolean = prefs.getBoolean(KEY_HAS_STARTED_WORKOUT, false)

    fun getFirstWorkoutStartedAt(): Long = prefs.getLong(KEY_FIRST_WORKOUT_STARTED_AT, 0L)

    fun getLastWorkoutStartedAt(): Long = prefs.getLong(KEY_LAST_WORKOUT_STARTED_AT, 0L)

    fun isRetentionHandled(retentionDay: RetentionDay): Boolean {
        return when (retentionDay) {
            RetentionDay.D1 -> prefs.getBoolean(KEY_D1_HANDLED, false)
            RetentionDay.D3 -> prefs.getBoolean(KEY_D3_HANDLED, false)
        }
    }

    fun setRetentionHandled(retentionDay: RetentionDay, handled: Boolean = true) {
        prefs.edit {
            when (retentionDay) {
                RetentionDay.D1 -> putBoolean(KEY_D1_HANDLED, handled)
                RetentionDay.D3 -> putBoolean(KEY_D3_HANDLED, handled)
            }
        }
    }

    fun isRetentionScheduled(retentionDay: RetentionDay): Boolean {
        return when (retentionDay) {
            RetentionDay.D1 -> prefs.getBoolean(KEY_D1_SCHEDULED, false)
            RetentionDay.D3 -> prefs.getBoolean(KEY_D3_SCHEDULED, false)
        }
    }

    fun setRetentionScheduled(retentionDay: RetentionDay, scheduled: Boolean = true) {
        prefs.edit {
            when (retentionDay) {
                RetentionDay.D1 -> putBoolean(KEY_D1_SCHEDULED, scheduled)
                RetentionDay.D3 -> putBoolean(KEY_D3_SCHEDULED, scheduled)
            }
        }
    }

    fun isRetentionPermissionPending(retentionDay: RetentionDay): Boolean {
        return when (retentionDay) {
            RetentionDay.D1 -> prefs.getBoolean(KEY_D1_PERMISSION_PENDING, false)
            RetentionDay.D3 -> prefs.getBoolean(KEY_D3_PERMISSION_PENDING, false)
        }
    }

    fun setRetentionPermissionPending(retentionDay: RetentionDay, pending: Boolean) {
        prefs.edit {
            when (retentionDay) {
                RetentionDay.D1 -> putBoolean(KEY_D1_PERMISSION_PENDING, pending)
                RetentionDay.D3 -> putBoolean(KEY_D3_PERMISSION_PENDING, pending)
            }
        }
    }

    fun setPendingRetentionClick(dayNumber: Int, timestamp: Long = System.currentTimeMillis()) {
        prefs.edit {
            putInt(KEY_PENDING_RETENTION_CLICK_DAY, dayNumber)
            putLong(KEY_PENDING_RETENTION_CLICK_AT, timestamp)
        }
    }

    fun getAndClearValidPendingRetentionClickDay(currentTime: Long = System.currentTimeMillis()): Int {
        val day = prefs.getInt(KEY_PENDING_RETENTION_CLICK_DAY, 0)
        val clickAt = prefs.getLong(KEY_PENDING_RETENTION_CLICK_AT, 0L)
        prefs.edit {
            remove(KEY_PENDING_RETENTION_CLICK_DAY)
            remove(KEY_PENDING_RETENTION_CLICK_AT)
        }
        return if (day != 0 && clickAt != 0L && (currentTime - clickAt <= RetentionConstants.RETENTION_ATTRIBUTION_WINDOW_MS)) {
            day
        } else {
            0
        }
    }

    companion object {
        const val KEY_FIRST_OPEN_AT = "first_open_at"
        const val KEY_LEGACY_INSTALL_DATE = "install_date"
        const val KEY_LAST_APP_FOREGROUND_AT = "last_app_foreground_at"
        const val KEY_FIRST_WORKOUT_STARTED_AT = "first_workout_started_at"
        const val KEY_LAST_WORKOUT_STARTED_AT = "last_workout_started_at"
        const val KEY_HAS_STARTED_WORKOUT = "has_started_workout"
        const val KEY_D1_HANDLED = "d1_retention_handled"
        const val KEY_D3_HANDLED = "d3_retention_handled"
        const val KEY_D1_SCHEDULED = "d1_retention_scheduled"
        const val KEY_D3_SCHEDULED = "d3_retention_scheduled"
        const val KEY_D1_PERMISSION_PENDING = "d1_retention_permission_pending"
        const val KEY_D3_PERMISSION_PENDING = "d3_retention_permission_pending"
        const val KEY_PENDING_RETENTION_CLICK_DAY = "pending_retention_click_day"
        const val KEY_PENDING_RETENTION_CLICK_AT = "pending_retention_click_at"
    }
}
