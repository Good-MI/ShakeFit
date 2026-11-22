package com.example.shakefit.engine.calculator

import com.example.shakefit.util.Tuning
import kotlin.math.sqrt

/**
 * MovementQualityCalculator: Kalkulator skor kualitas gerakan (0..100) murni Kotlin (F-12a).
 * Mengukur ritme konsistensi interval antar repetisi (Squat & Push-up).
 * Variasi rendah antar repetisi menghasilkan skor mendekati 100.
 */
object MovementQualityCalculator {

    /**
     * Menghitung skor kualitas gerakan dari daftar interval (ms).
     * Mengembalikan null jika jumlah repetisi belum memenuhi ambang batas minimum (< 3 rep).
     */
    fun calculateScore(intervalsMs: List<Long>): Int? {
        if (intervalsMs.size < Tuning.QUALITY_MIN_REPS) return null

        val avg = intervalsMs.average()
        if (avg <= 0.0) return 100

        // Hitung standar deviasi
        val variance = intervalsMs.map { (it - avg) * (it - avg) }.average()
        val stdDev = sqrt(variance)

        // Koefisien variasi (CV = stdDev / mean)
        val cv = (stdDev / avg).toFloat()

        // Semakin rendah CV, semakin tinggi skor
        // Jika CV >= QUALITY_MAX_CV (default 0.50), skor adalah 0
        val rawScore = (1f - (cv / Tuning.QUALITY_MAX_CV)) * 100f
        return rawScore.toInt().coerceIn(0, 100)
    }
}
