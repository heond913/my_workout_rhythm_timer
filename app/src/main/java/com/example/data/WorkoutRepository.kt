package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.R
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val sharedPreferences: SharedPreferences,
    private val context: Context
) {
    val allRecords: Flow<List<WorkoutRecord>> = workoutDao.getAllRecords()

    fun getRecordsSince(since: Long): Flow<List<WorkoutRecord>> = workoutDao.getRecordsSince(since)

    suspend fun insert(record: WorkoutRecord) {
        workoutDao.insertRecord(record)
    }

    suspend fun delete(record: WorkoutRecord) {
        workoutDao.deleteRecord(record)
    }

    suspend fun deleteRecords(records: List<WorkoutRecord>) {
        workoutDao.deleteRecords(records)
    }

    suspend fun deleteById(id: Int) {
        workoutDao.deleteRecordById(id)
    }

    // --- Preferences Operations ---
    fun getTimerPresetType(): String {
        val type = sharedPreferences.getString("timer_preset_type", "스쿼트") ?: "스쿼트"
        return if (type == "없음") "기타" else type
    }

    fun saveTimerPresetType(preset: String) {
        sharedPreferences.edit { putString("timer_preset_type", preset) }
    }

    fun getSquatInterval(): Int = sharedPreferences.getInt("squat_interval", 4)
    fun saveSquatInterval(value: Int) {
        sharedPreferences.edit { putInt("squat_interval", value) }
    }

    fun getLungeInterval(): Int = sharedPreferences.getInt("lunge_interval", 5)
    fun saveLungeInterval(value: Int) {
        sharedPreferences.edit { putInt("lunge_interval", value) }
    }

    fun getPlankInterval(): Int = sharedPreferences.getInt("plank_interval", 10)
    fun savePlankInterval(value: Int) {
        sharedPreferences.edit { putInt("plank_interval", value) }
    }

    fun getOtherInterval(): Int = sharedPreferences.getInt("other_interval", 10)
    fun saveOtherInterval(value: Int) {
        sharedPreferences.edit { putInt("other_interval", value) }
    }

    fun getSquatTargetSeconds(): Int = sharedPreferences.getInt("squat_target_seconds", 60)
    fun saveSquatTargetSeconds(value: Int) {
        sharedPreferences.edit { putInt("squat_target_seconds", value) }
    }

    fun getLungeTargetSeconds(): Int = sharedPreferences.getInt("lunge_target_seconds", 60)
    fun saveLungeTargetSeconds(value: Int) {
        sharedPreferences.edit { putInt("lunge_target_seconds", value) }
    }

    fun getPlankTargetSeconds(): Int = sharedPreferences.getInt("plank_target_seconds", 60)
    fun savePlankTargetSeconds(value: Int) {
        sharedPreferences.edit { putInt("plank_target_seconds", value) }
    }

    fun getOtherTargetSeconds(): Int = sharedPreferences.getInt("other_target_seconds", 60)
    fun saveOtherTargetSeconds(value: Int) {
        sharedPreferences.edit { putInt("other_target_seconds", value) }
    }

    fun getRhythmInterval(): Int = sharedPreferences.getInt("rhythm_interval_seconds", 4)
    fun saveRhythmInterval(value: Int) {
        sharedPreferences.edit { putInt("rhythm_interval_seconds", value) }
    }

    // --- Auto Rest Preferences ---
    fun getAutoRestEnabled(): Boolean = sharedPreferences.getBoolean("auto_rest_enabled", false)
    fun saveAutoRestEnabled(value: Boolean) {
        sharedPreferences.edit { putBoolean("auto_rest_enabled", value) }
    }

    fun getSquatAutoRestEnabled(): Boolean = sharedPreferences.getBoolean("squat_auto_rest_enabled", false)
    fun saveSquatAutoRestEnabled(value: Boolean) {
        sharedPreferences.edit { putBoolean("squat_auto_rest_enabled", value) }
    }

    fun getLungeAutoRestEnabled(): Boolean = sharedPreferences.getBoolean("lunge_auto_rest_enabled", false)
    fun saveLungeAutoRestEnabled(value: Boolean) {
        sharedPreferences.edit { putBoolean("lunge_auto_rest_enabled", value) }
    }

    fun getPlankAutoRestEnabled(): Boolean = sharedPreferences.getBoolean("plank_auto_rest_enabled", false)
    fun savePlankAutoRestEnabled(value: Boolean) {
        sharedPreferences.edit { putBoolean("plank_auto_rest_enabled", value) }
    }

    fun getOtherAutoRestEnabled(): Boolean = sharedPreferences.getBoolean("other_auto_rest_enabled", false)
    fun saveOtherAutoRestEnabled(value: Boolean) {
        sharedPreferences.edit { putBoolean("other_auto_rest_enabled", value) }
    }

    fun getSquatRestSeconds(): Int = sharedPreferences.getInt("squat_rest_seconds", 30)
    fun saveSquatRestSeconds(value: Int) {
        sharedPreferences.edit { putInt("squat_rest_seconds", value) }
    }

    fun getLungeRestSeconds(): Int = sharedPreferences.getInt("lunge_rest_seconds", 30)
    fun saveLungeRestSeconds(value: Int) {
        sharedPreferences.edit { putInt("lunge_rest_seconds", value) }
    }

    fun getPlankRestSeconds(): Int = sharedPreferences.getInt("plank_rest_seconds", 30)
    fun savePlankRestSeconds(value: Int) {
        sharedPreferences.edit { putInt("plank_rest_seconds", value) }
    }

    fun getOtherRestSeconds(): Int = sharedPreferences.getInt("other_rest_seconds", 30)
    fun saveOtherRestSeconds(value: Int) {
        sharedPreferences.edit { putInt("other_rest_seconds", value) }
    }

    // --- Language Selection ---
    fun isLanguageSelected(): Boolean = sharedPreferences.getBoolean("is_language_selected", false)

    fun saveLanguageSelected(selected: Boolean) {
        sharedPreferences.edit { putBoolean("is_language_selected", selected) }
    }

    // --- Custom Routines Persistence ---
    private fun getLocalizedString(resId: Int, langCode: String): String {
        val locale = when (langCode) {
            "ko" -> java.util.Locale.KOREAN
            "ja" -> java.util.Locale.JAPANESE
            "es" -> java.util.Locale("es")
            "de" -> java.util.Locale.GERMAN
            "fr" -> java.util.Locale.FRENCH
            else -> java.util.Locale.ENGLISH
        }
        val configuration = android.content.res.Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        val localizedContext = context.createConfigurationContext(configuration)
        return localizedContext.getString(resId)
    }

    private fun localizeDefaultRoutine(routine: CustomRoutine, langCode: String): CustomRoutine {
        return when (routine.id) {
            "default_1" -> {
                CustomRoutine(
                    id = "default_1",
                    name = getLocalizedString(R.string.default_routine_1_name, langCode),
                    steps = listOf(
                        RoutineStep(getLocalizedString(R.string.preset_squat, langCode), 60, 4, 15),
                        RoutineStep(getLocalizedString(R.string.preset_lunge, langCode), 60, 5, 0)
                    ),
                    timestamp = routine.timestamp
                )
            }
            "default_2" -> {
                CustomRoutine(
                    id = "default_2",
                    name = getLocalizedString(R.string.default_routine_2_name, langCode),
                    steps = listOf(
                        RoutineStep(getLocalizedString(R.string.preset_squat, langCode), 45, 3, 20),
                        RoutineStep(getLocalizedString(R.string.preset_lunge, langCode), 45, 4, 20),
                        RoutineStep(getLocalizedString(R.string.preset_other, langCode), 60, 6, 0)
                    ),
                    timestamp = routine.timestamp
                )
            }
            else -> routine
        }
    }

    fun getCustomRoutines(): List<CustomRoutine> {
        val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        val currentLocale = if (!appLocales.isEmpty) appLocales.get(0)?.language ?: "en" else java.util.Locale.getDefault().language
        val langCode = currentLocale

        val ids = sharedPreferences.getStringSet("custom_routines_ids", null)
        if (ids == null) {
            // Provide default presets if none saved yet
            val defaultRoutines = listOf(
                CustomRoutine(
                    id = "default_1",
                    name = "전신 리듬 세트",
                    steps = listOf(
                        RoutineStep("스쿼트", 60, 4, 15),
                        RoutineStep("런지", 60, 5, 0)
                    )
                ),
                CustomRoutine(
                    id = "default_2",
                    name = "하체 단련 세트",
                    steps = listOf(
                        RoutineStep("스쿼트", 45, 3, 20),
                        RoutineStep("런지", 45, 4, 20),
                        RoutineStep("기타", 60, 6, 0)
                    )
                )
            )
            saveCustomRoutines(defaultRoutines)
            return defaultRoutines.map { localizeDefaultRoutine(it, langCode) }
        }
        
        val rawRoutines = ids.map { id ->
            val name = sharedPreferences.getString("custom_routine_name_$id", "") ?: ""
            val stepsStr = sharedPreferences.getString("custom_routine_steps_$id", "") ?: ""
            val timestamp = sharedPreferences.getLong("custom_routine_time_$id", System.currentTimeMillis())
            CustomRoutine(
                id = id,
                name = name,
                steps = CustomRoutine.deserializeSteps(stepsStr),
                timestamp = timestamp
            )
        }

        return rawRoutines.map { localizeDefaultRoutine(it, langCode) }.sortedBy { it.timestamp }
    }

    fun saveCustomRoutines(routines: List<CustomRoutine>) {
        val ids = routines.map { it.id }.toSet()
        sharedPreferences.edit {
            putStringSet("custom_routines_ids", ids)
            routines.forEach { routine ->
                putString("custom_routine_name_${routine.id}", routine.name)
                putString("custom_routine_steps_${routine.id}", routine.serializeSteps())
                putLong("custom_routine_time_${routine.id}", routine.timestamp)
            }
        }
    }
}
