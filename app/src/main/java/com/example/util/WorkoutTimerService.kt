package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.TimerRepository
import com.example.data.WorkoutRecord
import com.example.data.WorkoutRepository
import com.example.viewmodel.TimerMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WorkoutTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var shutdownJob: Job? = null
    private var sessionStartTime: Long = 0L

    private lateinit var soundHelper: SoundHelper
    private lateinit var ttsHelper: TtsHelper
    private lateinit var repository: WorkoutRepository
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "workout_timer_channel_v2"
        const val NOTIFICATION_ID = 2026 // Custom unique id for the foreground notification

        const val ACTION_START = "com.example.ACTION_START"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_RESET = "com.example.ACTION_RESET"
    }

    override fun attachBaseContext(newBase: Context) {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            val locale = appLocales.get(0)
            if (locale != null) {
                val config = newBase.resources.configuration
                config.setLocale(locale)
                val context = newBase.createConfigurationContext(config)
                super.attachBaseContext(context)
                return
            }
        }
        super.attachBaseContext(newBase)
    }

    override fun onCreate() {
        super.onCreate()
        soundHelper = SoundHelper(this)
        ttsHelper = TtsHelper(this)
        repository = WorkoutRepository(
            AppDatabase.getDatabase(this).workoutDao(),
            getSharedPreferences("workout_rhythm_prefs", MODE_PRIVATE)
        )
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d("WorkoutTimerService", "onStartCommand action: $action")

        when (action) {
            ACTION_START -> {
                startTimerLoop()
            }
            ACTION_PAUSE -> {
                pauseTimerLoop()
            }
            ACTION_RESET -> {
                resetTimerLoop()
            }
        }

        return START_NOT_STICKY
    }

    private fun startTimerLoop() {
        shutdownJob?.cancel()
        shutdownJob = null

        val currentState = TimerRepository.timerState.value
        if (currentState.isRunning && timerJob?.isActive == true) return

        var elapsed = currentState.elapsedSeconds
        var remaining = currentState.remainingSeconds
        var workoutCount = currentState.workoutCount
        var rhythmTickCount = currentState.rhythmTickCount
        var showCompletionDialog = currentState.showCompletionDialog

        val isAutoResetNeeded = (currentState.timerMode == TimerMode.Countdown && currentState.remainingSeconds <= 0)

        if (isAutoResetNeeded) {
            elapsed = 0
            remaining = currentState.totalTargetSeconds
            rhythmTickCount = 0
            workoutCount = 0
            showCompletionDialog = false
        }

        val isFreshStart = (elapsed == 0)

        if (sessionStartTime == 0L) {
            sessionStartTime = System.currentTimeMillis()
        }

        val freshRemaining = if (isFreshStart) {
            remaining + 3
        } else {
            remaining
        }

        // Set state running with correct remainingSeconds accounting for preparation phase
        TimerRepository.updateState {
            it.copy(
                isRunning = true,
                elapsedSeconds = elapsed,
                remainingSeconds = freshRemaining,
                rhythmTickCount = rhythmTickCount,
                workoutCount = workoutCount,
                showCompletionDialog = showCompletionDialog
            )
        }

        // Start Foreground Service immediately to satisfy system mandates
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (isFreshStart) {
            ttsHelper.speak(getString(R.string.tts_prep_3))
        } else {
            soundHelper.playStrongBeep()
            ttsHelper.speak(getString(R.string.tts_workout_start))
        }

        val finalState = TimerRepository.timerState.value
        var startTime = System.currentTimeMillis() - finalState.elapsedSeconds * 1000L

        timerJob = serviceScope.launch {
            while (TimerRepository.timerState.value.isRunning) {
                delay(100L) // Poll atomic timestamp for high precision
                val state = TimerRepository.timerState.value
                val targetTotalElapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                val diff = targetTotalElapsed - state.elapsedSeconds
                if (diff >= 5) {
                    // Robust bulk time drift detection (Time Skipping Prevention)
                    // If a drastic jump of >= 5 seconds occurs (e.g. system slowdown or recovery from CPU Doze mode),
                    // we immediately pause the timer to protect user data coherence, prevent rapid coroutine triggers,
                    // and gracefully inform the user to resume manually.
                    TimerRepository.updateState { it.copy(isRunning = false, manualInputEnabled = true) }
                    soundHelper.playDoubleBeep()
                    ttsHelper.speak("시간 편차가 감지되어 타이머가 일시정지되었습니다. 확인 후 재개해 주세요.")
                    updateNotification()
                    break
                }
                if (diff > 0) {
                    for (i in 1..diff) {
                        val currentLoopState = TimerRepository.timerState.value
                        if (!currentLoopState.isRunning) break

                        var newElapsed = currentLoopState.elapsedSeconds
                        var newRemaining = currentLoopState.remainingSeconds
                        var newRhythmTick = currentLoopState.rhythmTickCount
                        var newWorkoutCount = currentLoopState.workoutCount
                        var newRunning = currentLoopState.isRunning
                        var newShowDialog = currentLoopState.showCompletionDialog
                        var newIsResting = currentLoopState.isResting
                        var newRestRemaining = currentLoopState.restRemainingSeconds
                        var newRestTotal = currentLoopState.restTotalSeconds
                        var newCurrentStepIndex = currentLoopState.routineCurrentStepIndex
                        var newIsRoutineActive = currentLoopState.isRoutineActive
                        var newPresetType = currentLoopState.timerPresetType
                        var newInterval = currentLoopState.rhythmIntervalSeconds
                        var newTotalTarget = currentLoopState.totalTargetSeconds
                        var newRoutineHistoryJson = currentLoopState.routineHistoryJson

                        var speakText: String? = null
                        var coachingText: String? = null

                        val targetTotal = newTotalTarget

                        if (currentLoopState.isResting) {
                            newElapsed++
                            if (newRestRemaining > 0) {
                                newRestRemaining--
                            }
                            if (newRestRemaining == 10 && newRestTotal > 15) {
                                speakText = getString(R.string.rest_10_seconds_left)
                            } else if (newRestRemaining in 1..3) {
                                speakText = ttsHelper.getCountdownWord(newRestRemaining)
                            }

                            if (newRestRemaining <= 0) {
                                if (currentLoopState.isRoutineActive) {
                                    val steps = com.example.data.CustomRoutine.deserializeSteps(currentLoopState.routineStepsJson)
                                    val nextStepIdx = currentLoopState.routineCurrentStepIndex + 1
                                    if (nextStepIdx < steps.size) {
                                        val nextStep = steps[nextStepIdx]
                                        newCurrentStepIndex = nextStepIdx
                                        newPresetType = nextStep.exerciseName
                                        newInterval = nextStep.rhythmIntervalSeconds
                                        newTotalTarget = nextStep.durationSeconds
                                        
                                        newIsResting = false
                                        newElapsed = 0
                                        startTime = System.currentTimeMillis()
                                        newRhythmTick = 0
                                        newWorkoutCount = 0
                                        newRemaining = nextStep.durationSeconds + 3
                                        
                                        speakText = "쉬는 시간 끝! 다음 운동은 " + nextStep.exerciseName + "입니다. 준비하세요!"
                                        soundHelper.playStrongBeep()
                                    } else {
                                        newIsResting = false
                                        newRunning = false
                                        newIsRoutineActive = false
                                        newShowDialog = true
                                        speakText = "축하합니다! 모든 루틴을 완료하였습니다!"
                                        val originalPreset = repository.getTimerPresetType()
                                        newPresetType = originalPreset
                                        newTotalTarget = when (originalPreset) {
                                            "스쿼트" -> repository.getSquatTargetSeconds()
                                            "런지" -> repository.getLungeTargetSeconds()
                                            "플랭크" -> repository.getPlankTargetSeconds()
                                            "기타" -> repository.getOtherTargetSeconds()
                                            else -> 60
                                        }
                                        newInterval = when (originalPreset) {
                                            "스쿼트" -> repository.getSquatInterval()
                                            "런지" -> repository.getLungeInterval()
                                            "플랭크" -> newTotalTarget
                                            "기타" -> repository.getOtherInterval()
                                            else -> 4
                                        }
                                        newRemaining = newTotalTarget
                                        soundHelper.playSetFinished()
                                    }
                                } else {
                                    newIsResting = false
                                    newElapsed = 0
                                    startTime = System.currentTimeMillis()
                                    newRhythmTick = 0
                                    newWorkoutCount = 0
                                    newRemaining = targetTotal + 3
                                    speakText = getString(R.string.rest_finished_prepare)
                                    soundHelper.playStrongBeep()
                                    ttsHelper.speak(getString(R.string.tts_prep_3))
                                }
                            }
                        } else {
                            if (currentLoopState.timerMode == TimerMode.Countdown) {
                            newElapsed++
                            if (newRemaining > 0) {
                                newRemaining--
                            }

                            if (newRemaining > targetTotal) {
                                // 3-second preparation phase
                                if (newRemaining == targetTotal + 2) {
                                    speakText = getString(R.string.tts_prep_2)
                                } else if (newRemaining == targetTotal + 1) {
                                    speakText = getString(R.string.tts_prep_1)
                                }
                            } else if (newRemaining == targetTotal) {
                                // Transitioning to actual start
                                speakText = getString(R.string.tts_prep_start)
                                soundHelper.playStrongBeep()
                                newRhythmTick = 0
                            } else {
                                // --- NORMAL WORKOUT COUNTDOWN ---
                                val currentExType = com.example.data.ExerciseType.fromString(currentLoopState.timerPresetType)
                                if (currentExType == com.example.data.ExerciseType.PLANK && newRemaining in 1..10) {
                                    speakText = ttsHelper.getCountdownWord(newRemaining)
                                } else {
                                    // Check coaching alerts
                                    val targetHalf = targetTotal / 2
                                    if (newRemaining == targetHalf && targetHalf >= 10) {
                                        coachingText = getString(R.string.tts_workout_half)
                                    } else if (newRemaining == 10 && targetTotal > 15) {
                                        coachingText = getString(R.string.tts_workout_last10)
                                    }

                                    // Rhythm counts
                                    val interval = currentLoopState.rhythmIntervalSeconds
                                    var repTriggered = false
                                    if (interval > 0 && currentExType != com.example.data.ExerciseType.PLANK) {
                                        newRhythmTick++
                                        if (newRhythmTick >= interval) {
                                            if (newRemaining > 0) {
                                                soundHelper.playTick()
                                            }
                                            repTriggered = true
                                            newWorkoutCount++
                                            newRhythmTick = 0
                                        }
                                    }

                                    if (repTriggered) {
                                        speakText = ttsHelper.getNumberWord(newWorkoutCount)
                                    }

                                    // Combine if both exist: prioritize number then coaching message
                                    if (speakText != null && coachingText != null) {
                                        speakText = "$speakText, $coachingText"
                                    } else if (speakText == null && coachingText != null) {
                                        speakText = coachingText
                                    }
                                }

                                // Completed Countdown Condition
                                if (newRemaining <= 0) {
                                    if (currentLoopState.isRoutineActive) {
                                         val steps = com.example.data.CustomRoutine.deserializeSteps(currentLoopState.routineStepsJson)
                                         val currentStepIdx = currentLoopState.routineCurrentStepIndex
                                         val currentStep = steps[currentStepIdx]
                                         
                                         logCurrentTimerWorkout()

                                         val oldHistory = com.example.data.RoutineStepResult.deserializeList(newRoutineHistoryJson)
                                         val newHistory = oldHistory + com.example.data.RoutineStepResult(
                                             exerciseName = currentStep.exerciseName,
                                             count = newWorkoutCount,
                                             targetSeconds = currentStep.durationSeconds
                                         )
                                         newRoutineHistoryJson = com.example.data.RoutineStepResult.serializeList(newHistory)
                                         
                                         if (currentStep.restSeconds > 0) {
                                             newIsResting = true
                                             newRestTotal = currentStep.restSeconds
                                             newRestRemaining = currentStep.restSeconds
                                             newElapsed = 0
                                             startTime = System.currentTimeMillis()
                                             soundHelper.playSetFinished()
                                             speakText = currentStep.exerciseName + " 완료! " + currentStep.restSeconds + "초간 휴식하세요."
                                         } else {
                                             val nextStepIdx = currentStepIdx + 1
                                             if (nextStepIdx < steps.size) {
                                                 val nextStep = steps[nextStepIdx]
                                                 newCurrentStepIndex = nextStepIdx
                                                 newPresetType = nextStep.exerciseName
                                                 newInterval = nextStep.rhythmIntervalSeconds
                                                 newTotalTarget = nextStep.durationSeconds
                                                 newRemaining = nextStep.durationSeconds + 3
                                                 newElapsed = 0
                                                 startTime = System.currentTimeMillis()
                                                 newRhythmTick = 0
                                                 newWorkoutCount = 0
                                                 
                                                 speakText = currentStep.exerciseName + " 완료! 다음은 " + nextStep.exerciseName + "입니다. 준비하세요!"
                                                 soundHelper.playStrongBeep()
                                             } else {
                                                 newRunning = false
                                                 newIsRoutineActive = false
                                                 newShowDialog = true
                                                 speakText = "축하합니다! 모든 루틴을 완주하셨습니다!"
                                                 val originalPreset = repository.getTimerPresetType()
                                                 newPresetType = originalPreset
                                                 newTotalTarget = when (originalPreset) {
                                                     "스쿼트" -> repository.getSquatTargetSeconds()
                                                     "런지" -> repository.getLungeTargetSeconds()
                                                     "플랭크" -> repository.getPlankTargetSeconds()
                                                     "기타" -> repository.getOtherTargetSeconds()
                                                     else -> 60
                                                 }
                                                 newInterval = when (originalPreset) {
                                                     "스쿼트" -> repository.getSquatInterval()
                                                     "런지" -> repository.getLungeInterval()
                                                     "플랭크" -> newTotalTarget
                                                     "기타" -> repository.getOtherInterval()
                                                     else -> 4
                                                 }
                                                 newRemaining = newTotalTarget
                                                 soundHelper.playSetFinished()
                                             }
                                         }
                                    } else {
                                        newRunning = false
                                        soundHelper.playSetFinished()

                                        val completionMsg = getString(R.string.tts_workout_completed)
                                        if (speakText != null) {
                                            speakText = "$speakText, $completionMsg"
                                        } else {
                                            speakText = completionMsg
                                        }

                                        logCurrentTimerWorkout()
                                        newShowDialog = true
                                    }
                                }
                            }
                        } else {
                            // Count-up Mode
                            val isPreparing = newRemaining > targetTotal
                            if (isPreparing) {
                                if (newRemaining > 0) {
                                    newRemaining--
                                }
                                if (newRemaining == targetTotal + 2) {
                                    speakText = getString(R.string.tts_prep_2)
                                } else if (newRemaining == targetTotal + 1) {
                                    speakText = getString(R.string.tts_prep_1)
                                }
                            } else if (newRemaining == targetTotal && currentLoopState.elapsedSeconds == 0) {
                                // Note: transition to start
                                speakText = getString(R.string.tts_prep_start)
                                soundHelper.playStrongBeep()
                                newRhythmTick = 0
                            } else {
                                // --- NORMAL WORKOUT COUNT-UP ---
                                newElapsed++
                                val currentExType = com.example.data.ExerciseType.fromString(currentLoopState.timerPresetType)
                                val interval = currentLoopState.rhythmIntervalSeconds
                                var repTriggered = false
                                if (interval > 0 && currentExType != com.example.data.ExerciseType.PLANK) {
                                    newRhythmTick++
                                    if (newRhythmTick >= interval) {
                                        soundHelper.playTick()
                                        repTriggered = true
                                        newWorkoutCount++
                                        newRhythmTick = 0
                                    }
                                }

                                if (repTriggered) {
                                    speakText = ttsHelper.getNumberWord(newWorkoutCount)
                                }
                            }
                        }
                        }

                        // Update State atomically
                        TimerRepository.updateState {
                            it.copy(
                                elapsedSeconds = newElapsed,
                                remainingSeconds = newRemaining,
                                rhythmTickCount = newRhythmTick,
                                workoutCount = newWorkoutCount,
                                isRunning = newRunning,
                                showCompletionDialog = newShowDialog,
                                isResting = newIsResting,
                                restRemainingSeconds = newRestRemaining,
                                restTotalSeconds = newRestTotal,
                                routineCurrentStepIndex = newCurrentStepIndex,
                                isRoutineActive = newIsRoutineActive,
                                timerPresetType = newPresetType,
                                rhythmIntervalSeconds = newInterval,
                                totalTargetSeconds = newTotalTarget,
                                routineHistoryJson = newRoutineHistoryJson,
                                manualInputEnabled = if (!newRunning) true else it.manualInputEnabled
                            )
                        }

                        // Speech Audio trigger
                        if (speakText != null) {
                            ttsHelper.speak(speakText)
                        }

                        // Dynamically update notification text every second
                        updateNotification()

                        if (!newRunning) {
                            // If countdown completed, delay stopping service to allow TTS congratulations & applause to play fully
                            shutdownJob = serviceScope.launch {
                                delay(6000L) // 6 seconds is perfect to play both TTS and the SoundPool cheer
                                ServiceCompat.stopForeground(this@WorkoutTimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                                stopSelf()
                            }
                            break
                        }
                    }
                }
            }
        }
    }

    private fun pauseTimerLoop() {
        TimerRepository.updateState { it.copy(isRunning = false, manualInputEnabled = true) }
        timerJob?.cancel()
        soundHelper.playDoubleBeep()
        ttsHelper.speak(getString(R.string.tts_workout_paused))
        updateNotification()
    }

    private fun resetTimerLoop() {
        shutdownJob?.cancel()
        shutdownJob = null
        timerJob?.cancel()
        sessionStartTime = 0L

        val isRoutineActiveBeforeReset = TimerRepository.timerState.value.isRoutineActive
        val originalPreset = repository.getTimerPresetType()
        val originalTotalTarget = when (originalPreset) {
            "스쿼트" -> repository.getSquatTargetSeconds()
            "런지" -> repository.getLungeTargetSeconds()
            "플랭크" -> repository.getPlankTargetSeconds()
            "기타" -> repository.getOtherTargetSeconds()
            else -> 60
        }
        val originalInterval = when (originalPreset) {
            "스쿼트" -> repository.getSquatInterval()
            "런지" -> repository.getLungeInterval()
            "플랭크" -> originalTotalTarget
            "기타" -> repository.getOtherInterval()
            else -> 4
        }

        TimerRepository.updateState {
            it.copy(
                isRunning = false,
                elapsedSeconds = 0,
                remainingSeconds = if (isRoutineActiveBeforeReset || it.isRoutineActive) originalTotalTarget else it.totalTargetSeconds,
                rhythmTickCount = 0,
                workoutCount = 0,
                manualInputEnabled = true,
                isRoutineActive = false,
                routineName = "",
                routineStepsJson = "",
                routineCurrentStepIndex = 0,
                routineHistoryJson = "",
                timerPresetType = if (isRoutineActiveBeforeReset || it.isRoutineActive) originalPreset else it.timerPresetType,
                totalTargetSeconds = if (isRoutineActiveBeforeReset || it.isRoutineActive) originalTotalTarget else it.totalTargetSeconds,
                rhythmIntervalSeconds = if (isRoutineActiveBeforeReset || it.isRoutineActive) originalInterval else it.rhythmIntervalSeconds
            )
        }
        ttsHelper.stop()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun logCurrentTimerWorkout() {
        val state = TimerRepository.timerState.value
        val type = com.example.data.ExerciseType.fromString(state.timerPresetType)
        val exercise = when (type) {
            com.example.data.ExerciseType.SQUAT -> "스쿼트"
            com.example.data.ExerciseType.LUNGE -> "런지"
            com.example.data.ExerciseType.PLANK -> "플랭크"
            com.example.data.ExerciseType.OTHER -> {
                if (state.timerPresetType == "기타" || state.timerPresetType == "OTHER" || state.timerPresetType.isBlank()) {
                    "기타"
                } else {
                    state.timerPresetType
                }
            }
        }
        val duration = if (state.timerMode == TimerMode.Countdown) state.totalTargetSeconds else state.elapsedSeconds
        serviceScope.launch {
            try {
                repository.insert(
                    WorkoutRecord(
                        exerciseName = exercise,
                        reps = if (state.workoutCount > 0) state.workoutCount else null,
                        sets = 1,
                        durationSeconds = duration,
                        note = if (state.isRoutineActive) {
                            "[${state.routineName}] " + getString(R.string.log_timer_auto_completed)
                        } else {
                            getString(R.string.log_timer_auto_completed)
                        },
                        rating = 4
                    )
                )
            } catch (e: Exception) {
                Log.e("WorkoutTimerService", "Error logging workout", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableVibration(false)
                vibrationPattern = null
                setSound(null, null)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        try {
            val notification = buildNotification()
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("WorkoutTimerService", "Failed to update notification", e)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val state = TimerRepository.timerState.value
        val type = com.example.data.ExerciseType.fromString(state.timerPresetType)
        val exerciseDisplay = when (type) {
            com.example.data.ExerciseType.SQUAT -> getString(R.string.preset_squat)
            com.example.data.ExerciseType.LUNGE -> getString(R.string.preset_lunge)
            com.example.data.ExerciseType.PLANK -> getString(R.string.preset_plank)
            com.example.data.ExerciseType.OTHER -> {
                if (state.timerPresetType == "기타" || state.timerPresetType == "OTHER" || state.timerPresetType.isBlank()) {
                    getString(R.string.preset_other)
                } else {
                    state.timerPresetType
                }
            }
        }
        val titleText = if (state.isRoutineActive) {
            "[${state.routineName}] ${state.routineCurrentStepIndex + 1}단계 - $exerciseDisplay"
        } else {
            "${getString(R.string.notification_training_prefix)} - $exerciseDisplay"
        }
        
        val contentText = if (state.isResting) {
            getString(R.string.notification_content_resting, state.restRemainingSeconds)
        } else if (state.remainingSeconds > state.totalTargetSeconds) {
            getString(R.string.timer_preparing)
        } else if (state.timerMode == TimerMode.Countdown) {
            getString(R.string.notification_content_countdown, state.remainingSeconds, state.workoutCount)
        } else {
            getString(R.string.notification_content_countup, state.elapsedSeconds, state.workoutCount)
        }

        // Open MainActivity when user clicks the notification card
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pending Intents for Action buttons
        val pauseIntent = Intent(this, WorkoutTimerService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startIntent = Intent(this, WorkoutTimerService::class.java).apply { action = ACTION_START }
        val startPendingIntent = PendingIntent.getService(
            this, 2, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = Intent(this, WorkoutTimerService::class.java).apply { action = ACTION_RESET }
        val resetPendingIntent = PendingIntent.getService(
            this, 3, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setSubText(getString(R.string.app_name))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppPendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)

        if (sessionStartTime != 0L) {
            builder.setWhen(sessionStartTime)
            builder.setShowWhen(false)
        }

        if (state.isRunning) {
            builder.addAction(android.R.drawable.ic_media_pause, getString(R.string.notification_action_pause), pausePendingIntent)
        } else {
            builder.addAction(android.R.drawable.ic_media_play, getString(R.string.notification_action_resume), startPendingIntent)
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_action_reset), resetPendingIntent)

        return builder.build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("WorkoutTimerService", "onTaskRemoved called - task cleared from recent apps")
        resetTimerLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        soundHelper.release()
        ttsHelper.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
