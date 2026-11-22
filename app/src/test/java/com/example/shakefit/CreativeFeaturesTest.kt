package com.example.shakefit

import com.example.shakefit.engine.badge.BadgeEvaluator
import com.example.shakefit.engine.badge.SessionEvaluationInput
import com.example.shakefit.engine.badge.StreakCalculator
import com.example.shakefit.engine.calculator.MovementQualityCalculator
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.SessionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CreativeFeaturesTest {

    // ========== F-12a: SKOR KUALITAS GERAKAN ==========

    @Test
    fun testMovementQualityScore_InsufficientData_ReturnsNull() {
        // Kurang dari 3 repetisi wajib menghasilkan null
        assertNull(MovementQualityCalculator.calculateScore(emptyList()))
        assertNull(MovementQualityCalculator.calculateScore(listOf(1000L)))
        assertNull(MovementQualityCalculator.calculateScore(listOf(1000L, 1100L)))
    }

    @Test
    fun testMovementQualityScore_PerfectConsistency_Returns100() {
        // Interval persis sama (CV = 0) -> skor 100
        val perfectIntervals = listOf(1200L, 1200L, 1200L, 1200L, 1200L)
        val score = MovementQualityCalculator.calculateScore(perfectIntervals)
        assertNotNull(score)
        assertEquals(100, score)
    }

    @Test
    fun testMovementQualityScore_ModerateVariation_ReturnsProportionalScore() {
        // Sedikit variasi ritme
        val intervals = listOf(1000L, 1050L, 980L, 1020L)
        val score = MovementQualityCalculator.calculateScore(intervals)
        assertNotNull(score)
        assertTrue("Skor harus tinggi untuk variasi rendah: $score", score!! >= 85)
    }

    @Test
    fun testMovementQualityScore_ErraticVariation_LowOrZero() {
        // Ritme sangat tidak beraturan
        val erraticIntervals = listOf(500L, 2500L, 700L, 3000L)
        val score = MovementQualityCalculator.calculateScore(erraticIntervals)
        assertNotNull(score)
        assertTrue("Skor harus rendah untuk variasi liar: $score", score!! <= 40)
    }

    // ========== F-12c: BADGE EVALUATOR & REKOR BARU ==========

    @Test
    fun testBadgeEvaluator_FirstSession() {
        val input = SessionEvaluationInput(
            exercise = ExerciseType.SQUAT,
            mode = SessionMode.FREE,
            reps = 5,
            distanceMeters = null,
            avgPaceSecPerKm = null,
            totalSessionsBefore = 0,
            repsTodayBefore = 0,
            currentStreakDays = 1,
            previousBestReps = null,
            previousBestDistanceMeters = null,
            previousBestPaceSecPerKm = null
        )

        val output = BadgeEvaluator.evaluate(input)
        assertTrue(output.unlockedBadgeCodes.contains("FIRST_SESSION"))
        assertTrue(output.isNewRecord) // Sesi pertama dengan reps > 0 adalah rekor baru
    }

    @Test
    fun testBadgeEvaluator_First10Reps() {
        val input = SessionEvaluationInput(
            exercise = ExerciseType.PUSHUP,
            mode = SessionMode.FREE,
            reps = 10,
            distanceMeters = null,
            avgPaceSecPerKm = null,
            totalSessionsBefore = 5,
            repsTodayBefore = 0,
            currentStreakDays = 1,
            previousBestReps = 8,
            previousBestDistanceMeters = null,
            previousBestPaceSecPerKm = null
        )

        val output = BadgeEvaluator.evaluate(input)
        assertTrue(output.unlockedBadgeCodes.contains("FIRST_10_REPS"))
        assertTrue(output.isNewRecord)
    }

    @Test
    fun testBadgeEvaluator_Streaks() {
        // Streak 3 hari
        val input3 = SessionEvaluationInput(
            exercise = ExerciseType.SQUAT,
            mode = SessionMode.FREE,
            reps = 5,
            distanceMeters = null,
            avgPaceSecPerKm = null,
            totalSessionsBefore = 10,
            repsTodayBefore = 0,
            currentStreakDays = 3,
            previousBestReps = 20,
            previousBestDistanceMeters = null,
            previousBestPaceSecPerKm = null
        )
        val output3 = BadgeEvaluator.evaluate(input3)
        assertTrue(output3.unlockedBadgeCodes.contains("STREAK_3"))
        assertFalse(output3.unlockedBadgeCodes.contains("STREAK_7"))
        assertFalse(output3.isNewRecord)

        // Streak 7 hari
        val input7 = input3.copy(currentStreakDays = 7)
        val output7 = BadgeEvaluator.evaluate(input7)
        assertTrue(output7.unlockedBadgeCodes.contains("STREAK_3"))
        assertTrue(output7.unlockedBadgeCodes.contains("STREAK_7"))
    }

    @Test
    fun testBadgeEvaluator_Reps100Day() {
        val input = SessionEvaluationInput(
            exercise = ExerciseType.SQUAT,
            mode = SessionMode.FREE,
            reps = 30,
            distanceMeters = null,
            avgPaceSecPerKm = null,
            totalSessionsBefore = 10,
            repsTodayBefore = 75, // 75 + 30 = 105 >= 100
            currentStreakDays = 1,
            previousBestReps = 50,
            previousBestDistanceMeters = null,
            previousBestPaceSecPerKm = null
        )

        val output = BadgeEvaluator.evaluate(input)
        assertTrue(output.unlockedBadgeCodes.contains("REPS_100_DAY"))
    }

    @Test
    fun testBadgeEvaluator_RunDistanceBadges() {
        // Lari 1.5 km
        val input1k = SessionEvaluationInput(
            exercise = ExerciseType.RUNNING,
            mode = SessionMode.FREE,
            reps = null,
            distanceMeters = 1500f,
            avgPaceSecPerKm = 360,
            totalSessionsBefore = 2,
            repsTodayBefore = 0,
            currentStreakDays = 1,
            previousBestReps = null,
            previousBestDistanceMeters = 800f,
            previousBestPaceSecPerKm = 400
        )
        val output1k = BadgeEvaluator.evaluate(input1k)
        assertTrue(output1k.unlockedBadgeCodes.contains("RUN_1KM"))
        assertFalse(output1k.unlockedBadgeCodes.contains("RUN_5KM"))
        assertTrue(output1k.isNewRecord)

        // Lari 5.2 km
        val input5k = input1k.copy(distanceMeters = 5200f)
        val output5k = BadgeEvaluator.evaluate(input5k)
        assertTrue(output5k.unlockedBadgeCodes.contains("RUN_1KM"))
        assertTrue(output5k.unlockedBadgeCodes.contains("RUN_5KM"))
    }

    // ========== F-12c: STREAK CALCULATOR ==========

    @Test
    fun testStreakCalculator_ContinuousDays() {
        val refCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 21, 14, 0, 0)
        }

        // Simulasikan ada sesi di 21 Nov, 20 Nov, 19 Nov (3 hari beruntun)
        val activeDates = setOf("2026-11-21", "2026-11-20", "2026-11-19")
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

        val streak = StreakCalculator.calculateStreak(refCal) { startMs, _ ->
            val dateStr = dateFormat.format(startMs)
            dateStr in activeDates
        }

        assertEquals(3, streak)
    }

    @Test
    fun testStreakCalculator_GracePeriodYesterdayOnly() {
        val refCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 21, 14, 0, 0)
        }

        // Hari ini (21 Nov) belum ada sesi, tapi kemarin (20 Nov) dan 19 Nov ada
        val activeDates = setOf("2026-11-20", "2026-11-19")
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

        val streak = StreakCalculator.calculateStreak(refCal) { startMs, _ ->
            val dateStr = dateFormat.format(startMs)
            dateStr in activeDates
        }

        assertEquals(2, streak)
    }

    @Test
    fun testStreakCalculator_BrokenStreak() {
        val refCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 21, 14, 0, 0)
        }

        // Hari ini (21 Nov) tidak ada, kemarin (20 Nov) tidak ada, ada di 19 Nov
        val activeDates = setOf("2026-11-19")
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

        val streak = StreakCalculator.calculateStreak(refCal) { startMs, _ ->
            val dateStr = dateFormat.format(startMs)
            dateStr in activeDates
        }

        assertEquals(0, streak)
    }

    // ========== F-12d: LOGIKA COUNTDOWN 60 DETIK ==========

    @Test
    fun testChallenge60Seconds_CountdownLogic() {
        // T0: elapsed = 0 -> remaining = 60
        var elapsed = 0
        var remaining = (60 - elapsed).coerceAtLeast(0)
        assertEquals(60, remaining)

        // T30: elapsed = 30 -> remaining = 30
        elapsed = 30
        remaining = (60 - elapsed).coerceAtLeast(0)
        assertEquals(30, remaining)

        // T60: elapsed = 60 -> remaining = 0 (memicu auto finish)
        elapsed = 60
        remaining = (60 - elapsed).coerceAtLeast(0)
        assertEquals(0, remaining)

        // T65 (guard): elapsed = 65 -> remaining tetap 0, tidak negatif
        elapsed = 65
        remaining = (60 - elapsed).coerceAtLeast(0)
        assertEquals(0, remaining)
    }
}
