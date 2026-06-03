package com.example.data

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val sharedPreferences: SharedPreferences
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

    // --- Language Selection ---
    fun isLanguageSelected(): Boolean = sharedPreferences.getBoolean("is_language_selected", false)

    fun saveLanguageSelected(selected: Boolean) {
        sharedPreferences.edit { putBoolean("is_language_selected", selected) }
    }
}
