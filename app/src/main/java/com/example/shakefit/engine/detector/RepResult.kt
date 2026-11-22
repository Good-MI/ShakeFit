package com.example.shakefit.engine.detector

/**
 * Hasil dari setiap rep yang berhasil dideteksi.
 * Kelas Kotlin murni tanpa dependensi Android framework.
 */
data class RepResult(
    val repNumber: Int,
    val timestampMs: Long,
    val intervalMs: Long
)
