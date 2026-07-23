package com.example.worker

enum class RetentionDay(val dayNumber: Int) {
    D1(1),
    D3(3);

    companion object {
        const val KEY_RETENTION_DAY = "retention_day"

        fun fromDayNumber(day: Int): RetentionDay {
            return when (day) {
                3 -> D3
                else -> D1
            }
        }
    }
}
