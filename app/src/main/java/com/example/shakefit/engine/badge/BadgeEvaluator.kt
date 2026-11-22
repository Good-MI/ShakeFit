package com.example.shakefit.engine.badge

import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.SessionMode

data class SessionEvaluationInput(
    val exercise: ExerciseType,
    val mode: SessionMode,
    val reps: Int?,
    val distanceMeters: Float?,
    val avgPaceSecPerKm: Int?,
    val totalSessionsBefore: Int,
    val repsTodayBefore: Int,
    val currentStreakDays: Int,
    val previousBestReps: Int?,
    val previousBestDistanceMeters: Float?,
    val previousBestPaceSecPerKm: Int?
)

data class BadgeEvaluationOutput(
    val unlockedBadgeCodes: List<String>,
    val isNewRecord: Boolean
)

/**
 * BadgeEvaluator: Evaluator murni Kotlin untuk mengecek 7 badge awal dan rekor baru (F-12c).
 * Tanpa dependensi Android framework agar mudah diuji secara deterministik di unit test.
 */
object BadgeEvaluator {

    fun evaluate(input: SessionEvaluationInput): BadgeEvaluationOutput {
        val badges = mutableListOf<String>()

        // 1. FIRST_SESSION (Sesi pertama yang berhasil diselesaikan)
        if (input.totalSessionsBefore == 0) {
            badges.add("FIRST_SESSION")
        }

        // 2. FIRST_10_REPS (Minimal 10 repetisi dalam 1 sesi)
        val currentReps = input.reps ?: 0
        if (currentReps >= 10) {
            badges.add("FIRST_10_REPS")
        }

        // 3. STREAK_3 (3 hari berturut-turut berolahraga)
        if (input.currentStreakDays >= 3) {
            badges.add("STREAK_3")
        }

        // 4. STREAK_7 (7 hari berturut-turut berolahraga)
        if (input.currentStreakDays >= 7) {
            badges.add("STREAK_7")
        }

        // 5. REPS_100_DAY (Akumulasi 100 repetisi dalam 1 hari)
        if (input.repsTodayBefore + currentReps >= 100) {
            badges.add("REPS_100_DAY")
        }

        // 6. RUN_1KM (Sesi lari minimal 1 km / 1000m)
        val currentDist = input.distanceMeters ?: 0f
        if (input.exercise == ExerciseType.RUNNING && currentDist >= 1000f) {
            badges.add("RUN_1KM")
        }

        // 7. RUN_5KM (Sesi lari minimal 5 km / 5000m)
        if (input.exercise == ExerciseType.RUNNING && currentDist >= 5000f) {
            badges.add("RUN_5KM")
        }

        // Evaluasi Rekor Baru (isNewRecord)
        var isNewRecord = false
        if (input.exercise == ExerciseType.SQUAT || input.exercise == ExerciseType.PUSHUP) {
            if (currentReps > 0) {
                val prev = input.previousBestReps ?: 0
                if (currentReps > prev) {
                    isNewRecord = true
                }
            }
        } else if (input.exercise == ExerciseType.RUNNING) {
            if (currentDist > 0f) {
                val prevDist = input.previousBestDistanceMeters ?: 0f
                if (currentDist > prevDist) {
                    isNewRecord = true
                }
            }
            val pace = input.avgPaceSecPerKm
            if (pace != null && pace > 0) {
                val prevPace = input.previousBestPaceSecPerKm
                if (prevPace != null && prevPace > 0 && pace < prevPace) {
                    isNewRecord = true
                }
            }
        }

        return BadgeEvaluationOutput(
            unlockedBadgeCodes = badges,
            isNewRecord = isNewRecord
        )
    }

    fun getBadgeMetadata(code: String): Pair<String, String> = when (code) {
        "FIRST_SESSION" -> Pair("Langkah Pertama", "Menyelesaikan sesi latihan pertama")
        "FIRST_10_REPS" -> Pair("Pemanasan Bagus", "Mencapai 10 repetisi dalam satu sesi")
        "STREAK_3" -> Pair("Tiga Hari Beruntun", "Olahraga 3 hari berturut-turut")
        "STREAK_7" -> Pair("Satu Pekan Disiplin", "Olahraga 7 hari berturut-turut")
        "REPS_100_DAY" -> Pair("Ratusan Rep", "Mencapai total 100 repetisi dalam 1 hari")
        "RUN_1KM" -> Pair("Pelari 1K", "Menyelesaikan lari sejauh minimal 1 km")
        "RUN_5KM" -> Pair("Pelari 5K", "Menyelesaikan lari sejauh minimal 5 km")
        else -> Pair(code, "Lencana pencapaian")
    }
}
