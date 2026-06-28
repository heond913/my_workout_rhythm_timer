package com.example.data

import com.example.viewmodel.TimerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * TimerConfig: Clean, read-only immutable configuration container.
 * Controlled strictly by UI/User configurations.
 */
data class TimerConfig(
    val timerMode: TimerMode = TimerMode.Countdown,
    val timerPresetType: String = "스쿼트",
    val totalTargetSeconds: Int = 60,
    val rhythmIntervalSeconds: Int = 3,
    val autoRestEnabled: Boolean = false,
    val squatAutoRestEnabled: Boolean = false,
    val lungeAutoRestEnabled: Boolean = false,
    val plankAutoRestEnabled: Boolean = false,
    val otherAutoRestEnabled: Boolean = false,
    val restTotalSeconds: Int = 30,
    val squatRestSeconds: Int = 30,
    val lungeRestSeconds: Int = 30,
    val plankRestSeconds: Int = 30,
    val otherRestSeconds: Int = 30,
    val isRoutineActive: Boolean = false,
    val routineName: String = "",
    val routineStepsJson: String = "",
    val routineCurrentStepIndex: Int = 0,
    val routineHistoryJson: String = "",
    val manualInputEnabled: Boolean = true
)

/**
 * TimerRuntimeState: Service engine dedicated indicators container.
 * Directly controlled by WorkoutTimerService.
 */
data class TimerRuntimeState(
    val isRunning: Boolean = false,
    val elapsedSeconds: Int = 0,
    val remainingSeconds: Int = 60,
    val rhythmTickCount: Int = 0,
    val workoutCount: Int = 0,
    val showCompletionDialog: Boolean = false,
    val isResting: Boolean = false,
    val restRemainingSeconds: Int = 0,
    val restTotalSeconds: Int = 30,
    val routineCurrentStepIndex: Int = 0,
    val isRoutineActive: Boolean = false,
    val timerPresetType: String = "스쿼트",
    val rhythmIntervalSeconds: Int = 3,
    val totalTargetSeconds: Int = 60,
    val routineHistoryJson: String = "",
    val manualInputEnabled: Boolean = true,
    val timerMode: TimerMode = TimerMode.Countdown
)

/**
 * TimerSnapshot: Immutable consolidated snapshot of the timer state.
 * Unified read-only projection consumed cleanly by UI, Widget, and Notifications.
 */
data class TimerSnapshot(
    val isRunning: Boolean = false,
    val timerMode: TimerMode = TimerMode.Countdown,
    val timerPresetType: String = "스쿼트",
    val totalTargetSeconds: Int = 60,
    val rhythmIntervalSeconds: Int = 3,
    val elapsedSeconds: Int = 0,
    val remainingSeconds: Int = 60,
    val rhythmTickCount: Int = 0,
    val workoutCount: Int = 0,
    val showCompletionDialog: Boolean = false,
    val autoRestEnabled: Boolean = false,
    val squatAutoRestEnabled: Boolean = false,
    val lungeAutoRestEnabled: Boolean = false,
    val plankAutoRestEnabled: Boolean = false,
    val otherAutoRestEnabled: Boolean = false,
    val isResting: Boolean = false,
    val restRemainingSeconds: Int = 0,
    val restTotalSeconds: Int = 30,
    val squatRestSeconds: Int = 30,
    val lungeRestSeconds: Int = 30,
    val plankRestSeconds: Int = 30,
    val otherRestSeconds: Int = 30,
    val isRoutineActive: Boolean = false,
    val routineName: String = "",
    val routineStepsJson: String = "",
    val routineCurrentStepIndex: Int = 0,
    val routineHistoryJson: String = "",
    val manualInputEnabled: Boolean = true
)

/**
 * Access-control interface to encapsulate Service-exclusive writes.
 */
interface ServiceTimerControl {
    fun updateRuntimeState(transform: (TimerRuntimeState) -> TimerRuntimeState)
    fun setRuntimeState(state: TimerRuntimeState)
}

/**
 * CQRS Timer Repository
 */
object TimerRepository {
    private val _timerConfig = MutableStateFlow(TimerConfig())
    val timerConfig: StateFlow<TimerConfig> = _timerConfig.asStateFlow()

    private val _runtimeState = MutableStateFlow(TimerRuntimeState())

    val timerSnapshot: StateFlow<TimerSnapshot> = combine(_timerConfig, _runtimeState) { config, runtime ->
        TimerSnapshot(
            // Settings from Config
            autoRestEnabled = config.autoRestEnabled,
            squatAutoRestEnabled = config.squatAutoRestEnabled,
            lungeAutoRestEnabled = config.lungeAutoRestEnabled,
            plankAutoRestEnabled = config.plankAutoRestEnabled,
            otherAutoRestEnabled = config.otherAutoRestEnabled,
            squatRestSeconds = config.squatRestSeconds,
            lungeRestSeconds = config.lungeRestSeconds,
            plankRestSeconds = config.plankRestSeconds,
            otherRestSeconds = config.otherRestSeconds,
            routineName = config.routineName,
            routineStepsJson = config.routineStepsJson,

            // Dynamic tracking states from Runtime
            isRunning = runtime.isRunning,
            elapsedSeconds = if (runtime.isRunning) runtime.elapsedSeconds else 0,
            remainingSeconds = if (runtime.isRunning) runtime.remainingSeconds else config.totalTargetSeconds,
            rhythmTickCount = if (runtime.isRunning) runtime.rhythmTickCount else 0,
            workoutCount = if (runtime.isRunning) runtime.workoutCount else 0,
            showCompletionDialog = runtime.showCompletionDialog,
            isResting = if (runtime.isRunning) runtime.isResting else false,
            restRemainingSeconds = if (runtime.isRunning) runtime.restRemainingSeconds else 0,

            // Config settings overridden by active execution values during a running workout
            timerMode = if (runtime.isRunning) runtime.timerMode else config.timerMode,
            timerPresetType = if (runtime.isRunning) runtime.timerPresetType else config.timerPresetType,
            totalTargetSeconds = if (runtime.isRunning) runtime.totalTargetSeconds else config.totalTargetSeconds,
            rhythmIntervalSeconds = if (runtime.isRunning) runtime.rhythmIntervalSeconds else config.rhythmIntervalSeconds,
            restTotalSeconds = if (runtime.isRunning) runtime.restTotalSeconds else config.restTotalSeconds,
            routineCurrentStepIndex = if (runtime.isRunning) runtime.routineCurrentStepIndex else config.routineCurrentStepIndex,
            isRoutineActive = if (runtime.isRunning) runtime.isRoutineActive else config.isRoutineActive,
            routineHistoryJson = if (runtime.isRunning) runtime.routineHistoryJson else config.routineHistoryJson,
            manualInputEnabled = if (runtime.isRunning) runtime.manualInputEnabled else config.manualInputEnabled
        )
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.Eagerly,
        initialValue = TimerSnapshot()
    )

    fun updateConfig(transform: (TimerConfig) -> TimerConfig) {
        _timerConfig.value = transform(_timerConfig.value)
    }

    /**
     * Resets runtime state back to default initial values.
     */
    fun resetRuntimeState() {
        _runtimeState.value = TimerRuntimeState()
    }

    /**
     * User/UI-triggered command to clear transient completion dialog state.
     */
    fun dismissCompletionDialog() {
        _runtimeState.value = _runtimeState.value.copy(
            showCompletionDialog = false,
            routineHistoryJson = ""
        )
    }

    /**
     * Secure Access Control: Returns ServiceTimerControl only to callers inside the Service layer.
     */
    fun getServiceControl(caller: Any): ServiceTimerControl {
        require(caller.javaClass.simpleName.contains("WorkoutTimerService")) {
            "Access Denied: Only WorkoutTimerService is permitted to update RuntimeState!"
        }
        return object : ServiceTimerControl {
            override fun updateRuntimeState(transform: (TimerRuntimeState) -> TimerRuntimeState) {
                _runtimeState.value = transform(_runtimeState.value)
            }

            override fun setRuntimeState(state: TimerRuntimeState) {
                _runtimeState.value = state
            }
        }
    }
}
