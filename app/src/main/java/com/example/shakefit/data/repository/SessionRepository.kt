package com.example.shakefit.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.example.shakefit.data.DbHelper
import com.example.shakefit.engine.badge.BadgeEvaluator
import com.example.shakefit.engine.badge.SessionEvaluationInput
import com.example.shakefit.engine.badge.StreakCalculator
import com.example.shakefit.model.BadgeUi
import com.example.shakefit.model.DistanceSource
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.RunEnvironment
import com.example.shakefit.model.RunSplit
import com.example.shakefit.model.SessionMode
import com.example.shakefit.model.SessionSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * SessionRepository mengelola penyimpanan dan pembacaan sesi latihan di SQLite.
 * Seluruh query berjalan di Dispatchers.IO dengan parameter binding (?)..
 */
class SessionRepository(private val dbHelper: DbHelper) {

    /**
     * Menyimpan sesi latihan yang baru selesai (FINISHED) secara atomik.
     * Mengevaluasi badge yang baru terbuka dan rekor baru (F-12c).
     * Mengembalikan SessionSummary yang telah diperkaya dengan sessionId, newBadges, dan isNewRecord.
     */
    suspend fun saveSession(summary: SessionSummary): SessionSummary = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        var insertedSessionId = -1L
        var finalSummary = summary

        try {
            // 1. Kumpulkan konteks evaluasi sebelum data sesi disimpan
            val totalSessionsBefore = db.rawQuery("SELECT COUNT(*) FROM workout_sessions", null).use {
                if (it.moveToNext()) it.getInt(0) else 0
            }

            var previousBestReps: Int? = null
            db.rawQuery(
                """
                SELECT MAX(w.reps)
                FROM workout_sessions w
                JOIN exercises e ON w.exercise_id = e.id
                WHERE e.code = ?
                """.trimIndent(),
                arrayOf(summary.exercise.name)
            ).use {
                if (it.moveToNext() && !it.isNull(0)) previousBestReps = it.getInt(0)
            }

            var previousBestDistanceMeters: Float? = null
            var previousBestPaceSecPerKm: Int? = null
            if (summary.exercise == ExerciseType.RUNNING) {
                db.rawQuery(
                    """
                    SELECT MAX(distance_m), MIN(avg_pace_sec_per_km)
                    FROM run_details
                    WHERE avg_pace_sec_per_km > 0
                    """.trimIndent(),
                    null
                ).use {
                    if (it.moveToNext()) {
                        if (!it.isNull(0)) previousBestDistanceMeters = it.getFloat(0)
                        if (!it.isNull(1)) previousBestPaceSecPerKm = it.getInt(1)
                    }
                }
            }

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val todayStartMs = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val todayEndMs = cal.timeInMillis

            val repsTodayBefore = db.rawQuery(
                "SELECT COALESCE(SUM(reps), 0) FROM workout_sessions WHERE started_at BETWEEN ? AND ?",
                arrayOf(todayStartMs.toString(), todayEndMs.toString())
            ).use {
                if (it.moveToNext()) it.getInt(0) else 0
            }

            // 2. Simpan sesi ke workout_sessions
            val exerciseId = when (summary.exercise) {
                ExerciseType.SQUAT -> 1
                ExerciseType.PUSHUP -> 2
                ExerciseType.RUNNING -> 3
            }

            val sessionValues = ContentValues().apply {
                put("exercise_id", exerciseId)
                put("mode", summary.mode.name)
                put("started_at", summary.startedAtEpochMs)
                put("duration_sec", summary.durationSec)
                if (summary.reps != null) put("reps", summary.reps) else putNull("reps")
                if (summary.avgTempoMs != null) put("avg_tempo_ms", summary.avgTempoMs) else putNull("avg_tempo_ms")
                if (summary.qualityScore != null) put("quality_score", summary.qualityScore) else putNull("quality_score")
                put("calories_est", summary.caloriesEst)
                put("note", summary.note)
            }

            insertedSessionId = db.insert("workout_sessions", null, sessionValues)

            // Jika sesi Running, simpan run_details dan run_splits
            if (summary.exercise == ExerciseType.RUNNING && insertedSessionId > 0) {
                val environment = if (summary.distanceSource == DistanceSource.STEPS_ESTIMATE) {
                    RunEnvironment.TREADMILL
                } else {
                    RunEnvironment.OUTDOOR
                }

                val runValues = ContentValues().apply {
                    put("session_id", insertedSessionId)
                    put("environment", environment.name)
                    put("distance_m", summary.distanceMeters ?: 0f)
                    if (summary.avgPaceSecPerKm != null) put("avg_pace_sec_per_km", summary.avgPaceSecPerKm) else putNull("avg_pace_sec_per_km")
                    if (summary.bestPaceSecPerKm != null) put("best_pace_sec_per_km", summary.bestPaceSecPerKm) else putNull("best_pace_sec_per_km")
                    if (summary.avgCadenceSpm != null) put("avg_cadence_spm", summary.avgCadenceSpm) else putNull("avg_cadence_spm")
                    put("distance_source", (summary.distanceSource ?: DistanceSource.GPS).name)
                }
                db.insert("run_details", null, runValues)

                // Simpan splits
                for (split in summary.splits) {
                    val splitValues = ContentValues().apply {
                        put("session_id", insertedSessionId)
                        put("km_index", split.kmIndex)
                        put("duration_sec", split.durationSec)
                        put("pace_sec_per_km", split.paceSecPerKm)
                    }
                    db.insert("run_splits", null, splitValues)
                }
            }

            // 3. Hitung streak terkini (termasuk sesi yang baru saja diinsert hari ini)
            val currentStreakDays = StreakCalculator.calculateStreak { startMs, endMs ->
                db.rawQuery(
                    "SELECT COUNT(*) FROM workout_sessions WHERE started_at BETWEEN ? AND ?",
                    arrayOf(startMs.toString(), endMs.toString())
                ).use {
                    it.moveToNext() && it.getInt(0) > 0
                }
            }

            // 4. Evaluasi badge & rekor baru (F-12c)
            val evalOutput = BadgeEvaluator.evaluate(
                SessionEvaluationInput(
                    exercise = summary.exercise,
                    mode = summary.mode,
                    reps = summary.reps,
                    distanceMeters = summary.distanceMeters,
                    avgPaceSecPerKm = summary.avgPaceSecPerKm,
                    totalSessionsBefore = totalSessionsBefore,
                    repsTodayBefore = repsTodayBefore,
                    currentStreakDays = currentStreakDays,
                    previousBestReps = previousBestReps,
                    previousBestDistanceMeters = previousBestDistanceMeters,
                    previousBestPaceSecPerKm = previousBestPaceSecPerKm
                )
            )

            val nowMs = System.currentTimeMillis()
            val newBadges = mutableListOf<BadgeUi>()

            for (code in evalOutput.unlockedBadgeCodes) {
                val cv = ContentValues().apply {
                    put("unlocked_at", nowMs)
                }
                val updatedRows = db.update("badges", cv, "code = ? AND unlocked_at IS NULL", arrayOf(code))
                if (updatedRows > 0) {
                    val (title, description) = BadgeEvaluator.getBadgeMetadata(code)
                    newBadges.add(
                        BadgeUi(
                            code = code,
                            title = title,
                            description = description,
                            unlockedAtEpochMs = nowMs
                        )
                    )
                }
            }

            db.setTransactionSuccessful()

            finalSummary = summary.copy(
                sessionId = insertedSessionId,
                newBadges = newBadges,
                isNewRecord = evalOutput.isNewRecord
            )
        } finally {
            db.endTransaction()
        }

        finalSummary
    }

    /**
     * Mengambil daftar riwayat sesi latihan (terbaru dulu), mendukung filter latihan.
     */
    suspend fun getHistory(filter: ExerciseType? = null): List<SessionSummary> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<SessionSummary>()

        val query = StringBuilder(
            """
            SELECT 
                w.id, w.exercise_id, e.code AS exercise_code, w.mode, w.started_at, w.duration_sec,
                w.reps, w.avg_tempo_ms, w.quality_score, w.calories_est, w.note,
                r.environment, r.distance_m, r.avg_pace_sec_per_km, r.best_pace_sec_per_km,
                r.avg_cadence_spm, r.distance_source
            FROM workout_sessions w
            JOIN exercises e ON w.exercise_id = e.id
            LEFT JOIN run_details r ON w.id = r.session_id
            """.trimIndent()
        )

        val args = mutableListOf<String>()
        if (filter != null) {
            query.append(" WHERE e.code = ?")
            args.add(filter.name)
        }
        query.append(" ORDER BY w.started_at DESC")

        val cursor = db.rawQuery(query.toString(), args.toTypedArray())
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToSessionSummary(db, c))
            }
        }

        list
    }

    /**
     * Mengambil detail satu sesi lengkap termasuk splits.
     */
    suspend fun getSessionDetail(sessionId: Long): SessionSummary? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT 
                w.id, w.exercise_id, e.code AS exercise_code, w.mode, w.started_at, w.duration_sec,
                w.reps, w.avg_tempo_ms, w.quality_score, w.calories_est, w.note,
                r.environment, r.distance_m, r.avg_pace_sec_per_km, r.best_pace_sec_per_km,
                r.avg_cadence_spm, r.distance_source
            FROM workout_sessions w
            JOIN exercises e ON w.exercise_id = e.id
            LEFT JOIN run_details r ON w.id = r.session_id
            WHERE w.id = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(sessionId.toString()))
        cursor.use { c ->
            if (c.moveToNext()) {
                cursorToSessionSummary(db, c)
            } else {
                null
            }
        }
    }

    /**
     * Memperbarui catatan (note) pada sesi tertentu.
     */
    suspend fun updateNote(sessionId: Long, note: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("note", note)
        }
        db.update("workout_sessions", cv, "id = ?", arrayOf(sessionId.toString()))
    }

    /**
     * Menghapus sesi (cascade delete run_details dan run_splits).
     */
    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("workout_sessions", "id = ?", arrayOf(sessionId.toString()))
    }

    private fun cursorToSessionSummary(db: android.database.sqlite.SQLiteDatabase, c: Cursor): SessionSummary {
        val id = c.getLong(c.getColumnIndexOrThrow("id"))
        val codeStr = c.getString(c.getColumnIndexOrThrow("exercise_code"))
        val exerciseType = ExerciseType.valueOf(codeStr)
        val modeStr = c.getString(c.getColumnIndexOrThrow("mode"))
        val mode = SessionMode.valueOf(modeStr)
        val startedAt = c.getLong(c.getColumnIndexOrThrow("started_at"))
        val durationSec = c.getInt(c.getColumnIndexOrThrow("duration_sec"))

        val reps = if (c.isNull(c.getColumnIndexOrThrow("reps"))) null else c.getInt(c.getColumnIndexOrThrow("reps"))
        val avgTempo = if (c.isNull(c.getColumnIndexOrThrow("avg_tempo_ms"))) null else c.getInt(c.getColumnIndexOrThrow("avg_tempo_ms"))
        val quality = if (c.isNull(c.getColumnIndexOrThrow("quality_score"))) null else c.getInt(c.getColumnIndexOrThrow("quality_score"))
        val calories = c.getFloat(c.getColumnIndexOrThrow("calories_est"))
        val note = c.getString(c.getColumnIndexOrThrow("note")) ?: ""

        val distanceMeters = if (c.isNull(c.getColumnIndexOrThrow("distance_m"))) null else c.getFloat(c.getColumnIndexOrThrow("distance_m"))
        val avgPace = if (c.isNull(c.getColumnIndexOrThrow("avg_pace_sec_per_km"))) null else c.getInt(c.getColumnIndexOrThrow("avg_pace_sec_per_km"))
        val bestPace = if (c.isNull(c.getColumnIndexOrThrow("best_pace_sec_per_km"))) null else c.getInt(c.getColumnIndexOrThrow("best_pace_sec_per_km"))
        val avgCadence = if (c.isNull(c.getColumnIndexOrThrow("avg_cadence_spm"))) null else c.getInt(c.getColumnIndexOrThrow("avg_cadence_spm"))

        val distSourceStr = if (c.isNull(c.getColumnIndexOrThrow("distance_source"))) null else c.getString(c.getColumnIndexOrThrow("distance_source"))
        val distanceSource = distSourceStr?.let { DistanceSource.valueOf(it) }

        // Ambil run_splits jika running
        val splits = mutableListOf<RunSplit>()
        if (exerciseType == ExerciseType.RUNNING) {
            val splitCursor = db.rawQuery(
                "SELECT km_index, duration_sec, pace_sec_per_km FROM run_splits WHERE session_id = ? ORDER BY km_index ASC",
                arrayOf(id.toString())
            )
            splitCursor.use { sc ->
                while (sc.moveToNext()) {
                    splits.add(
                        RunSplit(
                            kmIndex = sc.getInt(0),
                            durationSec = sc.getInt(1),
                            paceSecPerKm = sc.getInt(2)
                        )
                    )
                }
            }
        }

        return SessionSummary(
            sessionId = id,
            exercise = exerciseType,
            mode = mode,
            startedAtEpochMs = startedAt,
            durationSec = durationSec,
            reps = reps,
            avgTempoMs = avgTempo,
            qualityScore = quality,
            distanceMeters = distanceMeters,
            avgPaceSecPerKm = avgPace,
            bestPaceSecPerKm = bestPace,
            avgCadenceSpm = avgCadence,
            distanceSource = distanceSource,
            splits = splits,
            caloriesEst = calories,
            note = note,
            newBadges = emptyList(),
            isNewRecord = false
        )
    }
}
