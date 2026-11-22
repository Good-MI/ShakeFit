package com.example.shakefit.data.repository

import android.content.ContentValues
import com.example.shakefit.data.DbHelper
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.SettingsUiState
import com.example.shakefit.util.Tuning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SettingsRepository mengelola penyimpanan pengaturan aplikasi di tabel settings (SQLite).
 * Berjalan di Dispatchers.IO.
 */
class SettingsRepository(private val dbHelper: DbHelper) {

    companion object {
        const val KEY_SQUAT_SENSITIVITY = "squat_sensitivity"
        const val KEY_PUSHUP_SENSITIVITY = "pushup_sensitivity"
        const val KEY_VIBRATION_ON = "vibration_on"
        const val KEY_SOUND_ON = "sound_on"
        const val KEY_COUNTDOWN_SEC = "countdown_sec"
        const val KEY_TREADMILL_STEP_FACTOR = "treadmill_step_length_factor"
    }

    suspend fun getSettings(): SettingsUiState = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val map = mutableMapOf<String, String>()

        val cursor = db.rawQuery("SELECT key, value FROM settings", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                map[c.getString(0)] = c.getString(1)
            }
        }

        val squatSens = map[KEY_SQUAT_SENSITIVITY]?.toFloatOrNull() ?: Tuning.DEFAULT_SENSITIVITY
        val pushupSens = map[KEY_PUSHUP_SENSITIVITY]?.toFloatOrNull() ?: Tuning.DEFAULT_SENSITIVITY
        val vibration = map[KEY_VIBRATION_ON]?.toBooleanStrictOrNull() ?: true
        val sound = map[KEY_SOUND_ON]?.toBooleanStrictOrNull() ?: true
        val countdown = map[KEY_COUNTDOWN_SEC]?.toIntOrNull() ?: Tuning.DEFAULT_COUNTDOWN_SEC
        val stepFactor = map[KEY_TREADMILL_STEP_FACTOR]?.toFloatOrNull() ?: Tuning.DEFAULT_STEP_LENGTH_FACTOR

        SettingsUiState(
            squatSensitivity = squatSens,
            pushupSensitivity = pushupSens,
            vibrationOn = vibration,
            soundOn = sound,
            countdownSec = countdown,
            treadmillStepLengthFactor = stepFactor,
            isLoading = false,
            error = null
        )
    }

    suspend fun setSensitivity(type: ExerciseType, value: Float) = withContext(Dispatchers.IO) {
        val key = if (type == ExerciseType.SQUAT) KEY_SQUAT_SENSITIVITY else KEY_PUSHUP_SENSITIVITY
        saveSetting(key, value.coerceIn(0f, 1f).toString())
    }

    suspend fun setVibration(on: Boolean) = withContext(Dispatchers.IO) {
        saveSetting(KEY_VIBRATION_ON, on.toString())
    }

    suspend fun setSound(on: Boolean) = withContext(Dispatchers.IO) {
        saveSetting(KEY_SOUND_ON, on.toString())
    }

    suspend fun setCountdown(sec: Int) = withContext(Dispatchers.IO) {
        saveSetting(KEY_COUNTDOWN_SEC, sec.coerceIn(3, 10).toString())
    }

    suspend fun setTreadmillStepLengthFactor(factor: Float) = withContext(Dispatchers.IO) {
        saveSetting(KEY_TREADMILL_STEP_FACTOR, factor.toString())
    }

    private fun saveSetting(key: String, value: String) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.insertWithOnConflict(
            "settings",
            null,
            cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}
