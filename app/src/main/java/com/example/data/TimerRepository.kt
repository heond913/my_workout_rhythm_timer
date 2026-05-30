package com.example.data

import com.example.viewmodel.TimerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TimerState(
    val isRunning: Boolean = false,
    val timerMode: TimerMode = TimerMode.Countdown,
    val timerPresetType: String = "스쿼트",
    val totalTargetSeconds: Int = 60,
    val rhythmIntervalSeconds: Int = 4,
    val elapsedSeconds: Int = 0,
    val remainingSeconds: Int = 60,
    val rhythmTickCount: Int = 0,
    val workoutCount: Int = 0,
    val showCompletionDialog: Boolean = false
)

object TimerRepository {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    fun updateState(transform: (TimerState) -> TimerState) {
        _timerState.value = transform(_timerState.value)
    }

    fun setState(state: TimerState) {
        _timerState.value = state
    }
}
