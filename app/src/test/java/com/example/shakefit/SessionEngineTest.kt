package com.example.shakefit

import com.example.shakefit.engine.running.TreadmillCalibrator
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.ui.screens.repsession.RepSessionViewModel
import com.example.shakefit.util.Tuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEngineTest {

    @Test
    fun testPositionHints_MatchLockedSpecification() {
        // ui-contract.md bagian 9 & restriction.md bagian 7 (TERKUNCI)
        assertEquals(
            "Saku depan celana, HP tegak, layar menghadap badan. Tidak dipegang",
            RepSessionViewModel.getPositionHint(ExerciseType.SQUAT)
        )
        assertEquals(
            "HP rata di lantai, layar menghadap atas, bagian atas HP (dekat sensor proximity) tepat di bawah dada",
            RepSessionViewModel.getPositionHint(ExerciseType.PUSHUP)
        )
        assertEquals(
            "Saku depan celana atau armband. Tidak digenggam",
            RepSessionViewModel.getPositionHint(ExerciseType.RUNNING)
        )
    }

    @Test
    fun testGoalFractionCalculation() {
        val target = 50f
        val currentNormal = 35f
        val fractionNormal = (currentNormal / target).coerceIn(0f, 1f)
        assertEquals(0.70f, fractionNormal, 0.001f)

        val currentExceeded = 65f
        val fractionExceeded = (currentExceeded / target).coerceIn(0f, 1f)
        assertEquals(1.0f, fractionExceeded, 0.001f)

        val currentZero = 0f
        val fractionZero = (currentZero / target).coerceIn(0f, 1f)
        assertEquals(0.0f, fractionZero, 0.001f)
    }

    @Test
    fun testCalorieFormulas_PerExercise() {
        // Squat: 50 reps
        val squatReps = 50
        val squatCalories = squatReps * Tuning.CALORIES_PER_SQUAT_REP
        assertEquals(16.0f, squatCalories, 0.01f)

        // Pushup: 40 reps
        val pushupReps = 40
        val pushupCalories = pushupReps * Tuning.CALORIES_PER_PUSHUP_REP
        assertEquals(11.6f, pushupCalories, 0.01f)

        // Running: 5 km, 70 kg
        val distKm = 5.0f
        val weightKg = 70.0f
        val runCalories = distKm * weightKg * Tuning.CALORIES_PER_KM_PER_KG
        assertEquals(362.6f, runCalories, 0.01f)
    }

    @Test
    fun testTreadmillCalibrator_BoundsSafety() {
        val heightCm = 170f

        // Jarak wajar
        val normalFactor = TreadmillCalibrator.calculateCalibratedStepFactor(1000f, 1200, heightCm)
        assertTrue(normalFactor in 0.40f..0.60f)

        // Nilai input anomali sangat besar (misal 50000m untuk 10 langkah) harus di-clamp ke batas atas (0.85f)
        val maxClamped = TreadmillCalibrator.calculateCalibratedStepFactor(50000f, 10, heightCm)
        assertEquals(0.85f, maxClamped, 0.001f)

        // Nilai input anomali sangat kecil (misal 1m untuk 5000 langkah) harus di-clamp ke batas bawah (0.20f)
        val minClamped = TreadmillCalibrator.calculateCalibratedStepFactor(1f, 5000, heightCm)
        assertEquals(0.20f, minClamped, 0.001f)
    }
}
