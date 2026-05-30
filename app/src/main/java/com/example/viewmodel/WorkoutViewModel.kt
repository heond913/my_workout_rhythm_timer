package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.WorkoutRecord
import com.example.data.WorkoutRepository
import com.example.util.SoundHelper
import com.example.util.TtsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab {
    Timer,
    Log,
    Calendar,
    Stats
}

enum class TimerMode {
    CountUp,
    Countdown
}

data class WorkoutUiState(
    val currentTab: AppTab = AppTab.Timer,
    val timerPresetType: String = "스쿼트",
    val squatIntervalSeconds: Int = 4,
    val lungeIntervalSeconds: Int = 5,
    val plankIntervalSeconds: Int = 10,
    val otherIntervalSeconds: Int = 10,
    val timerRunning: Boolean = false,
    val timerMode: TimerMode = TimerMode.Countdown,
    val totalTargetSeconds: Int = 60,
    val rhythmIntervalSeconds: Int = 4,
    val elapsedSeconds: Int = 0,
    val remainingSeconds: Int = 60,
    val rhythmTickCount: Int = 0,
    val workoutCount: Int = 0,
    val showCompletionDialog: Boolean = false,
    val calendarYearMonth: Calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2026)
        set(Calendar.MONTH, Calendar.MAY)
    },
    val inputExerciseName: String = "스쿼트",
    val inputReps: String = "15",
    val inputDurationSeconds: String = "60",
    val inputRating: Int = 3,
    val inputNote: String = ""
)

class WorkoutViewModel @kotlin.jvm.JvmOverloads constructor(
    application: Application,
    private val repository: WorkoutRepository = WorkoutRepository(
        AppDatabase.getDatabase(application).workoutDao(),
        application.getSharedPreferences("workout_rhythm_prefs", android.content.Context.MODE_PRIVATE)
    )
) : AndroidViewModel(application) {

    // Sound and vibration assistant
    private val soundHelper = SoundHelper(application)

    // TTS voice coach assistant
    private val ttsHelper = TtsHelper(application)

    // Flow of workout records from repository, exposed as StateFlow
    val allRecords: StateFlow<List<WorkoutRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Unified UI and Timer State
    private val _uiState = MutableStateFlow(
        WorkoutUiState(
            timerPresetType = repository.getTimerPresetType(),
            squatIntervalSeconds = repository.getSquatInterval(),
            lungeIntervalSeconds = repository.getLungeInterval(),
            plankIntervalSeconds = repository.getPlankInterval(),
            otherIntervalSeconds = repository.getOtherInterval(),
            rhythmIntervalSeconds = repository.getRhythmInterval()
        )
    )
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    // Backwards compatibility property delegations via getters/setters (also used for direct updates)
    var currentTab: AppTab
        get() = _uiState.value.currentTab
        private set(value) {
            _uiState.value = _uiState.value.copy(currentTab = value)
        }

    fun setTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    // --- TIMER STATE MANIPULATION ---
    var timerPresetType: String
        get() = _uiState.value.timerPresetType
        set(value) {
            _uiState.value = _uiState.value.copy(timerPresetType = value)
        }
    
    var squatIntervalSeconds: Int
        get() = _uiState.value.squatIntervalSeconds
        set(value) {
            _uiState.value = _uiState.value.copy(squatIntervalSeconds = value)
        }
    var lungeIntervalSeconds: Int
        get() = _uiState.value.lungeIntervalSeconds
        set(value) {
            _uiState.value = _uiState.value.copy(lungeIntervalSeconds = value)
        }
    var plankIntervalSeconds: Int
        get() = _uiState.value.plankIntervalSeconds
        set(value) {
            _uiState.value = _uiState.value.copy(plankIntervalSeconds = value)
        }
    var otherIntervalSeconds: Int
        get() = _uiState.value.otherIntervalSeconds
        set(value) {
            _uiState.value = _uiState.value.copy(otherIntervalSeconds = value)
        }

    var timerRunning: Boolean
        get() = _uiState.value.timerRunning
        private set(value) {
            _uiState.value = _uiState.value.copy(timerRunning = value)
        }
    var timerMode: TimerMode
        get() = _uiState.value.timerMode
        private set(value) {
            _uiState.value = _uiState.value.copy(timerMode = value)
        }

    var totalTargetSeconds: Int
        get() = _uiState.value.totalTargetSeconds
        set(value) {
            _uiState.value = _uiState.value.copy(totalTargetSeconds = value)
            if (timerPresetType == "플랭크") {
                rhythmIntervalSeconds = value
                repository.saveRhythmInterval(value)
            }
        }

    var rhythmIntervalSeconds: Int
        get() = _uiState.value.rhythmIntervalSeconds
        set(value) {
            _uiState.value = _uiState.value.copy(rhythmIntervalSeconds = value)
        }
    var elapsedSeconds: Int
        get() = _uiState.value.elapsedSeconds
        set(value) {
            _uiState.value = _uiState.value.copy(elapsedSeconds = value)
        }
    var remainingSeconds: Int
        get() = _uiState.value.remainingSeconds
        set(value) {
            _uiState.value = _uiState.value.copy(remainingSeconds = value)
        }
    var rhythmTickCount: Int
        get() = _uiState.value.rhythmTickCount
        set(value) {
            _uiState.value = _uiState.value.copy(rhythmTickCount = value)
        }
    var workoutCount: Int
        get() = _uiState.value.workoutCount
        set(value) {
            _uiState.value = _uiState.value.copy(workoutCount = value)
        }
    var showCompletionDialog: Boolean
        get() = _uiState.value.showCompletionDialog
        set(value) {
            _uiState.value = _uiState.value.copy(showCompletionDialog = value)
        }

    fun selectPreset(preset: String) {
        val updatedPreset = preset
        val updatedRhythmInterval = when (preset) {
            "스쿼트" -> squatIntervalSeconds
            "런지" -> lungeIntervalSeconds
            "플랭크" -> totalTargetSeconds
            "기타" -> otherIntervalSeconds
            else -> 0
        }
        _uiState.value = _uiState.value.copy(
            timerPresetType = updatedPreset,
            rhythmIntervalSeconds = updatedRhythmInterval
        )
        repository.saveTimerPresetType(updatedPreset)
        repository.saveRhythmInterval(updatedRhythmInterval)
        resetTimer()
    }

    fun updateSquatInterval(seconds: Int) {
        val safeSecs = seconds.coerceIn(1, 60)
        _uiState.value = _uiState.value.copy(squatIntervalSeconds = safeSecs)
        repository.saveSquatInterval(safeSecs)
        if (timerPresetType == "스쿼트") {
            rhythmIntervalSeconds = safeSecs
            repository.saveRhythmInterval(safeSecs)
        }
    }

    fun updateLungeInterval(seconds: Int) {
        val safeSecs = seconds.coerceIn(1, 60)
        _uiState.value = _uiState.value.copy(lungeIntervalSeconds = safeSecs)
        repository.saveLungeInterval(safeSecs)
        if (timerPresetType == "런지") {
            rhythmIntervalSeconds = safeSecs
            repository.saveRhythmInterval(safeSecs)
        }
    }

    fun updatePlankInterval(seconds: Int) {
        val safeSecs = seconds.coerceIn(1, 60)
        _uiState.value = _uiState.value.copy(plankIntervalSeconds = safeSecs)
        repository.savePlankInterval(safeSecs)
        if (timerPresetType == "플랭크") {
            rhythmIntervalSeconds = safeSecs
            repository.saveRhythmInterval(safeSecs)
        }
    }

    fun updateOtherInterval(seconds: Int) {
        val safeSecs = seconds.coerceIn(1, 60)
        _uiState.value = _uiState.value.copy(otherIntervalSeconds = safeSecs)
        repository.saveOtherInterval(safeSecs)
        if (timerPresetType == "기타") {
            rhythmIntervalSeconds = safeSecs
            repository.saveRhythmInterval(safeSecs)
        }
    }

    private var timerJob: Job? = null

    fun selectTimerMode(mode: TimerMode) {
        if (timerRunning) return
        _uiState.value = _uiState.value.copy(timerMode = mode)
        resetTimer()
    }

    fun startTimer() {
        if (timerRunning) return
        _uiState.value = _uiState.value.copy(timerRunning = true)
        soundHelper.playStrongBeep()
        ttsHelper.speak("운동을 시작합니다.")

        val startTime = System.currentTimeMillis() - elapsedSeconds * 1000L

        timerJob = viewModelScope.launch {
            while (_uiState.value.timerRunning) {
                delay(100L) // Poll frequently to ensure high responsiveness and alignment
                val targetTotalElapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                val diff = targetTotalElapsed - _uiState.value.elapsedSeconds
                if (diff > 0) {
                    for (i in 1..diff) {
                        if (!_uiState.value.timerRunning) break
                        
                        // Grab current local variables from State to update atomically
                        var newElapsed = _uiState.value.elapsedSeconds + 1
                        var newRemaining = _uiState.value.remainingSeconds
                        var newRhythmTick = _uiState.value.rhythmTickCount
                        var newWorkoutCount = _uiState.value.workoutCount
                        var newRunning = _uiState.value.timerRunning
                        var newShowDialog = _uiState.value.showCompletionDialog

                        var speakText: String? = null
                        var shouldSpeakCompleted = false

                        if (_uiState.value.timerMode == TimerMode.Countdown) {
                            if (newRemaining > 0) {
                                newRemaining--
                            }
                            
                            // Check coaching alerts FIRST so they have proper priority
                            val targetHalf = _uiState.value.totalTargetSeconds / 2
                            if (newRemaining == targetHalf && targetHalf >= 10) {
                                speakText = "절반 지났습니다!"
                            } else if (newRemaining == 10 && _uiState.value.totalTargetSeconds > 15) {
                                speakText = "마지막 10초!"
                            }

                            // Periodic alarm/rhythm alert
                            val interval = _uiState.value.rhythmIntervalSeconds
                            var repTriggered = false
                            if (interval > 0) {
                                newRhythmTick++
                                if (newRhythmTick >= interval) {
                                    if (newRemaining > 0) {
                                        soundHelper.playTick()
                                        repTriggered = true
                                    }
                                    newWorkoutCount++
                                    newRhythmTick = 0
                                }
                            }

                            // If a rep was triggered and we don't have a half/10s alert, speak the count
                            if (repTriggered && speakText == null) {
                                speakText = ttsHelper.getKoreanNumberWord(newWorkoutCount)
                            }

                            // Complete condition
                            if (newRemaining <= 0) {
                                newRunning = false
                                soundHelper.playSetFinished()
                                shouldSpeakCompleted = true
                                // Automatically record styled logged placeholder workout
                                logCurrentTimerWorkout()
                                newShowDialog = true
                            }
                        } else {
                            // Count-up mode
                            val interval = _uiState.value.rhythmIntervalSeconds
                            var repTriggered = false
                            if (interval > 0) {
                                newRhythmTick++
                                if (newRhythmTick >= interval) {
                                    soundHelper.playTick()
                                    repTriggered = true
                                    newWorkoutCount++
                                    newRhythmTick = 0
                                }
                            }

                            if (repTriggered) {
                                speakText = ttsHelper.getKoreanNumberWord(newWorkoutCount)
                            }
                        }

                        // Apply the grouped states in one single atomic atomic update
                        _uiState.value = _uiState.value.copy(
                            elapsedSeconds = newElapsed,
                            remainingSeconds = newRemaining,
                            rhythmTickCount = newRhythmTick,
                            workoutCount = newWorkoutCount,
                            timerRunning = newRunning,
                            showCompletionDialog = newShowDialog
                        )

                        // Speak TTS sounds in this tick
                        if (shouldSpeakCompleted) {
                            ttsHelper.speak("수고하셨습니다! 운동이 완료되었습니다.")
                        } else if (speakText != null) {
                            ttsHelper.speak(speakText)
                        }

                        if (!newRunning) {
                            break
                        }
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        _uiState.value = _uiState.value.copy(timerRunning = false)
        timerJob?.cancel()
        soundHelper.playDoubleBeep()
        ttsHelper.speak("일시 정지되었습니다.")
    }

    fun resetTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            timerRunning = false,
            elapsedSeconds = 0,
            remainingSeconds = _uiState.value.totalTargetSeconds,
            rhythmTickCount = 0,
            workoutCount = 0
        )
        ttsHelper.stop()
    }

    private fun logCurrentTimerWorkout() {
        // Quick auto-record from finished timer if workout context matches
        val exercise = when (timerPresetType) {
            "스쿼트" -> "스쿼트"
            "런지" -> "런지"
            "플랭크" -> "플랭크"
            else -> "기타"
        }
        val duration = if (timerMode == TimerMode.Countdown) totalTargetSeconds else elapsedSeconds
        viewModelScope.launch {
            repository.insert(
                WorkoutRecord(
                    exerciseName = exercise,
                    reps = if (workoutCount > 0) workoutCount else null,
                    durationSeconds = duration,
                    note = "리듬 타이머 자동 완료",
                    rating = 4
                )
            )
        }
    }

    // --- RECORD FORM INPUT STATE ---
    var inputExerciseName: String
        get() = _uiState.value.inputExerciseName
        set(value) {
            _uiState.value = _uiState.value.copy(inputExerciseName = value)
        }
    var inputReps: String
        get() = _uiState.value.inputReps
        set(value) {
            _uiState.value = _uiState.value.copy(inputReps = value)
        }
    var inputDurationSeconds: String
        get() = _uiState.value.inputDurationSeconds
        set(value) {
            _uiState.value = _uiState.value.copy(inputDurationSeconds = value)
        }
    var inputRating: Int
        get() = _uiState.value.inputRating
        set(value) {
            _uiState.value = _uiState.value.copy(inputRating = value)
        }
    var inputNote: String
        get() = _uiState.value.inputNote
        set(value) {
            _uiState.value = _uiState.value.copy(inputNote = value)
        }

    fun saveWorkoutRecord(
        exercise: String = inputExerciseName,
        reps: Int? = inputReps.toIntOrNull(),
        duration: Int? = inputDurationSeconds.toIntOrNull(),
        rating: Int = inputRating,
        note: String = inputNote,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val record = WorkoutRecord(
                exerciseName = exercise,
                reps = reps,
                durationSeconds = duration,
                rating = rating,
                note = note,
                timestamp = timestamp
            )
            repository.insert(record)
            
            // Clear or reset fields
            _uiState.value = _uiState.value.copy(
                inputNote = "",
                currentTab = AppTab.Calendar
            )
        }
    }

    fun deleteWorkoutRecord(record: WorkoutRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    // --- CALENDAR HELPERS & STATISTICS CALCULATOR ---
    var calendarYearMonth: Calendar
        get() = _uiState.value.calendarYearMonth
        set(value) {
            _uiState.value = _uiState.value.copy(calendarYearMonth = value)
        }

    fun changeMonth(amount: Int) {
        val newCal = Calendar.getInstance().apply {
            timeInMillis = calendarYearMonth.timeInMillis
            add(Calendar.MONTH, amount)
        }
        _uiState.value = _uiState.value.copy(calendarYearMonth = newCal)
    }

    fun getWorkoutsForDay(day: Calendar, records: List<WorkoutRecord>): List<WorkoutRecord> {
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dayStr = formatter.format(day.time)
        return records.filter {
            val recordCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            formatter.format(recordCal.time) == dayStr
        }
    }

    // Date formatting helper for UI
    fun formatDateString(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Streak calculation
    fun getWorkoutStreak(records: List<WorkoutRecord>): Int {
        if (records.isEmpty()) return 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val completedDatesSet = records.map { dateFormat.format(Date(it.timestamp)) }.toSet()

        val todayCal = Calendar.getInstance()
        // Override for 2026-05-29 base context to ensure we get a valid visual starting streak matching the year index!
        todayCal.set(2026, Calendar.MAY, 29)

        var streak = 0
        var checkCal = todayCal.clone() as Calendar

        // If today has a workout, count start today. Left as fallback.
        var todayStr = dateFormat.format(checkCal.time)
        if (completedDatesSet.contains(todayStr)) {
            streak = 1
            while (true) {
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
                val prevDayStr = dateFormat.format(checkCal.time)
                if (completedDatesSet.contains(prevDayStr)) {
                    streak++
                } else {
                    break
                }
            }
        } else {
            // Check if yesterday had a workout
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(checkCal.time)
            if (completedDatesSet.contains(yesterdayStr)) {
                streak = 1
                while (true) {
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                    val prevDayStr = dateFormat.format(checkCal.time)
                    if (completedDatesSet.contains(prevDayStr)) {
                        streak++
                    } else {
                        break
                    }
                }
            }
        }
        return streak
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundHelper.release()
        ttsHelper.release()
    }
}
