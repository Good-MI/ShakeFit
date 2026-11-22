package com.example.shakefit.data.repository

import com.example.shakefit.data.DbHelper
import com.example.shakefit.engine.badge.BadgeEvaluator
import com.example.shakefit.engine.badge.StreakCalculator
import com.example.shakefit.model.BadgeUi
import com.example.shakefit.model.DayStat
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.StatsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * StatsRepository mengagregasi data statistik latihan dari SQLite (F-11).
 * Berjalan di Dispatchers.IO.
 */
class StatsRepository(private val dbHelper: DbHelper) {

    suspend fun getStats(): StatsUiState = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase

        // 1. Total Sesi
        var totalSessions = 0
        val sessionCursor = db.rawQuery("SELECT COUNT(*) FROM workout_sessions", null)
        sessionCursor.use { c ->
            if (c.moveToNext()) totalSessions = c.getInt(0)
        }

        // 2. Total Reps
        var totalReps = 0
        val repsCursor = db.rawQuery("SELECT SUM(reps) FROM workout_sessions WHERE reps IS NOT NULL", null)
        repsCursor.use { c ->
            if (c.moveToNext()) totalReps = c.getInt(0)
        }

        // 3. Total Km
        var totalKm = 0f
        val kmCursor = db.rawQuery("SELECT SUM(distance_m) / 1000.0 FROM run_details", null)
        kmCursor.use { c ->
            if (c.moveToNext()) totalKm = c.getFloat(0)
        }

        // 4. Rekor Terbaik Reps per Exercise
        val bestRepsMap = mutableMapOf<ExerciseType, Int>()
        val bestRepsCursor = db.rawQuery(
            """
            SELECT e.code, MAX(w.reps)
            FROM workout_sessions w
            JOIN exercises e ON w.exercise_id = e.id
            WHERE w.reps IS NOT NULL
            GROUP BY e.code
            """.trimIndent(),
            null
        )
        bestRepsCursor.use { c ->
            while (c.moveToNext()) {
                val code = ExerciseType.valueOf(c.getString(0))
                bestRepsMap[code] = c.getInt(1)
            }
        }

        // 5. Best Pace & Longest Run
        var bestPaceSecPerKm: Int? = null
        var longestRunMeters: Float? = null
        val runStatsCursor = db.rawQuery(
            """
            SELECT MIN(best_pace_sec_per_km), MAX(distance_m)
            FROM run_details
            WHERE best_pace_sec_per_km > 0
            """.trimIndent(),
            null
        )
        runStatsCursor.use { c ->
            if (c.moveToNext()) {
                if (!c.isNull(0)) bestPaceSecPerKm = c.getInt(0)
                if (!c.isNull(1)) longestRunMeters = c.getFloat(1)
            }
        }

        // 6. Ringkasan 7 Hari Terakhir (7 item, hari tertua dulu)
        val last7Days = get7DaysStats()

        // 7. Streak Days (hari berturut-turut ada minimal 1 sesi)
        val streak = calculateStreakDays()

        // 8. Badges
        val badges = getBadges()

        StatsUiState(
            totalSessions = totalSessions,
            totalReps = totalReps,
            totalKm = totalKm,
            bestRepsByExercise = bestRepsMap,
            bestPaceSecPerKm = bestPaceSecPerKm,
            longestRunMeters = longestRunMeters,
            last7Days = last7Days,
            streakDays = streak,
            badges = badges,
            isLoading = false,
            error = null
        )
    }

    suspend fun getStreakDays(): Int = withContext(Dispatchers.IO) {
        calculateStreakDays()
    }

    private fun get7DaysStats(): List<DayStat> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<DayStat>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Generate 7 hari dari (hari ini - 6 hari) s.d hari ini
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0 until 7) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startMs = calendar.timeInMillis
            val dateIso = dateFormat.format(calendar.time)

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endMs = calendar.timeInMillis

            var sessionsCount = 0
            var repsCount = 0
            var kmCount = 0f

            val c = db.rawQuery(
                """
                SELECT 
                    COUNT(w.id),
                    COALESCE(SUM(w.reps), 0),
                    COALESCE(SUM(r.distance_m) / 1000.0, 0)
                FROM workout_sessions w
                LEFT JOIN run_details r ON w.id = r.session_id
                WHERE w.started_at BETWEEN ? AND ?
                """.trimIndent(),
                arrayOf(startMs.toString(), endMs.toString())
            )
            c.use {
                if (it.moveToNext()) {
                    sessionsCount = it.getInt(0)
                    repsCount = it.getInt(1)
                    kmCount = it.getFloat(2)
                }
            }

            list.add(
                DayStat(
                    dateIso = dateIso,
                    reps = repsCount,
                    km = kmCount,
                    sessions = sessionsCount
                )
            )

            // Maju 1 hari untuk iterasi berikutnya
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return list
    }

    private fun calculateStreakDays(): Int {
        val db = dbHelper.readableDatabase
        return StreakCalculator.calculateStreak { startMs, endMs ->
            db.rawQuery(
                "SELECT COUNT(*) FROM workout_sessions WHERE started_at BETWEEN ? AND ?",
                arrayOf(startMs.toString(), endMs.toString())
            ).use {
                it.moveToNext() && it.getInt(0) > 0
            }
        }
    }

    private fun getBadges(): List<BadgeUi> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<BadgeUi>()

        val cursor = db.rawQuery("SELECT code, unlocked_at FROM badges ORDER BY id ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val code = c.getString(0)
                val unlockedAt = if (c.isNull(1)) null else c.getLong(1)

                val (title, description) = when (code) {
                    "FIRST_SESSION" -> Pair("Langkah Pertama", "Menyelesaikan sesi latihan pertama")
                    "FIRST_10_REPS" -> Pair("Pemanasan Bagus", "Mencapai 10 repetisi dalam satu sesi")
                    "STREAK_3" -> Pair("Tiga Hari Beruntun", "Olahraga 3 hari berturut-turut")
                    "STREAK_7" -> Pair("Satu Pekan Disiplin", "Olahraga 7 hari berturut-turut")
                    "REPS_100_DAY" -> Pair("Ratusan Rep", "Mencapai total 100 repetisi dalam 1 hari")
                    "RUN_1KM" -> Pair("Pelari 1K", "Menyelesaikan lari sejauh minimal 1 km")
                    "RUN_5KM" -> Pair("Pelari 5K", "Menyelesaikan lari sejauh minimal 5 km")
                    else -> Pair(code, "Lencana pencapaian")
                }

                list.add(
                    BadgeUi(
                        code = code,
                        title = title,
                        description = description,
                        unlockedAtEpochMs = unlockedAt
                    )
                )
            }
        }

        return list
    }
}
