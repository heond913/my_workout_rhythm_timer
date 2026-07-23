package com.example.util

interface Clock {
    fun currentTimeMillis(): Long
}

object SystemClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

class FakeClock(private var timeMs: Long) : Clock {
    override fun currentTimeMillis(): Long = timeMs

    fun setTime(newTimeMs: Long) {
        timeMs = newTimeMs
    }

    fun advanceBy(millis: Long) {
        timeMs += millis
    }
}
