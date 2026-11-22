package com.example.shakefit.engine.running

import com.example.shakefit.util.Tuning

/**
 * Utilitas kalibrasi treadmill.
 * Kelas Kotlin murni tanpa dependensi android.*
 */
object TreadmillCalibrator {

    /**
     * Menghitung koefisien panjang langkah baru (treadmillStepLengthFactor)
     * berdasarkan jarak aktual (meter) yang dibaca dari treadmill dan jumlah langkah.
     *
     * Rumus:
     * actualDistanceMeters = totalSteps * (userHeightCm / 100f) * newFactor
     * newFactor = actualDistanceMeters / (totalSteps * (userHeightCm / 100f))
     */
    fun calculateCalibratedStepFactor(
        actualDistanceMeters: Float,
        totalSteps: Int,
        userHeightCm: Float = Tuning.DEFAULT_HEIGHT_CM
    ): Float {
        val heightMeters = (if (userHeightCm > 0f) userHeightCm else Tuning.DEFAULT_HEIGHT_CM) / 100f
        if (totalSteps <= 0 || heightMeters <= 0f || actualDistanceMeters <= 0f) {
            return Tuning.DEFAULT_STEP_LENGTH_FACTOR
        }

        val factor = actualDistanceMeters / (totalSteps * heightMeters)
        // Batasi nilai faktor dalam rentang anatomis wajar manusia (0.20 .. 0.85)
        return factor.coerceIn(0.20f, 0.85f)
    }
}
