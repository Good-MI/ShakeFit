package com.example.shakefit.engine.detector

/**
 * Interface dasar untuk pendeteksi repetisi latihan.
 * Kelas Kotlin murni (tanpa dependensi Android framework).
 */
interface RepDetector {
    val currentReps: Int
    fun setSensitivity(sensitivity: Float)
    fun reset()
}
