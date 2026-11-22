package com.example.shakefit.data.repository

import android.content.ContentValues
import com.example.shakefit.data.DbHelper
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.GoalItem
import com.example.shakefit.model.GoalProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * GoalRepository mengelola target harian (daily_goals) dan perhitungan progres hari ini.
 * Berjalan di Dispatchers.IO.
 */
class GoalRepository(private val dbHelper: DbHelper) {

    /**
     * Mengambil daftar target harian (GoalItem) beserta progres hari ini.
     */
    suspend fun getGoals(): List<GoalItem> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<GoalItem>()

        val todayProgressMap = getTodayExerciseTotals()

        val cursor = db.rawQuery(
            """
            SELECT g.id, g.exercise_id, e.code AS exercise_code, g.target_value, g.unit, g.active
            FROM daily_goals g
            JOIN exercises e ON g.exercise_id = e.id
            ORDER BY g.id ASC
            """.trimIndent(),
            null
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow("id"))
                val codeStr = c.getString(c.getColumnIndexOrThrow("exercise_code"))
                val exerciseType = ExerciseType.valueOf(codeStr)
                val target = c.getFloat(c.getColumnIndexOrThrow("target_value"))
                val unit = c.getString(c.getColumnIndexOrThrow("unit"))
                val active = c.getInt(c.getColumnIndexOrThrow("active")) == 1

                val current = todayProgressMap[exerciseType] ?: 0f
                val progress = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f

                list.add(
                    GoalItem(
                        id = id,
                        type = exerciseType,
                        target = target,
                        unit = unit,
                        active = active,
                        progress = progress
                    )
                )
            }
        }

        list
    }

    /**
     * Menambah target baru.
     */
    suspend fun addGoal(type: ExerciseType, target: Float): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val exerciseId = when (type) {
            ExerciseType.SQUAT -> 1
            ExerciseType.PUSHUP -> 2
            ExerciseType.RUNNING -> 3
        }
        val unit = if (type == ExerciseType.RUNNING) "KM" else "REPS"

        val cv = ContentValues().apply {
            put("exercise_id", exerciseId)
            put("target_value", target)
            put("unit", unit)
            put("active", 1)
        }

        db.insert("daily_goals", null, cv)
    }

    /**
     * Memperbarui angka target.
     */
    suspend fun updateGoal(id: Long, target: Float) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("target_value", target)
        }
        db.update("daily_goals", cv, "id = ?", arrayOf(id.toString()))
    }

    /**
     * Mengaktifkan/menonaktifkan target.
     */
    suspend fun setActive(id: Long, active: Boolean) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("active", if (active) 1 else 0)
        }
        db.update("daily_goals", cv, "id = ?", arrayOf(id.toString()))
    }

    /**
     * Menghapus target.
     */
    suspend fun deleteGoal(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("daily_goals", "id = ?", arrayOf(id.toString()))
    }

    /**
     * Mengambil progres target harian aktif untuk ditampilkan di HomeUiState (todayGoals).
     */
    suspend fun getTodayGoalProgress(): List<GoalProgress> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<GoalProgress>()
        val todayTotals = getTodayExerciseTotals()

        val cursor = db.rawQuery(
            """
            SELECT g.id, e.code AS exercise_code, g.target_value, g.unit
            FROM daily_goals g
            JOIN exercises e ON g.exercise_id = e.id
            WHERE g.active = 1
            ORDER BY g.id ASC
            """.trimIndent(),
            null
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                val goalId = c.getLong(0)
                val exerciseType = ExerciseType.valueOf(c.getString(1))
                val target = c.getFloat(2)
                val unit = c.getString(3)

                val current = todayTotals[exerciseType] ?: 0f
                val fraction = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f

                list.add(
                    GoalProgress(
                        goalId = goalId,
                        type = exerciseType,
                        current = current,
                        target = target,
                        unit = unit,
                        fraction = fraction
                    )
                )
            }
        }

        list
    }

    /**
     * Menghitung total repetisi (Squat & Push-up) atau total km (Running) yang tuntas hari ini.
     */
    private fun getTodayExerciseTotals(): Map<ExerciseType, Float> {
        val db = dbHelper.readableDatabase
        val result = mutableMapOf<ExerciseType, Float>()

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDayMs = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDayMs = cal.timeInMillis

        // Reps untuk Squat dan Pushup hari ini
        val repCursor = db.rawQuery(
            """
            SELECT e.code, SUM(w.reps)
            FROM workout_sessions w
            JOIN exercises e ON w.exercise_id = e.id
            WHERE w.started_at BETWEEN ? AND ? AND e.kind = 'REPS'
            GROUP BY e.code
            """.trimIndent(),
            arrayOf(startOfDayMs.toString(), endOfDayMs.toString())
        )
        repCursor.use { c ->
            while (c.moveToNext()) {
                val code = ExerciseType.valueOf(c.getString(0))
                val totalReps = c.getFloat(1)
                result[code] = totalReps
            }
        }

        // Jarak dalam km untuk Running hari ini
        val runCursor = db.rawQuery(
            """
            SELECT SUM(r.distance_m) / 1000.0
            FROM workout_sessions w
            JOIN run_details r ON w.id = r.session_id
            WHERE w.started_at BETWEEN ? AND ?
            """.trimIndent(),
            arrayOf(startOfDayMs.toString(), endOfDayMs.toString())
        )
        runCursor.use { c ->
            if (c.moveToNext()) {
                val totalKm = c.getFloat(0)
                result[ExerciseType.RUNNING] = totalKm
            }
        }

        return result
    }
}
