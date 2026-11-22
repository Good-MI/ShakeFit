package com.example.shakefit.engine.running

/**
 * Representasi titik koordinat GPS mentah yang independen dari Android Location.
 * Kelas Kotlin murni untuk pemrosesan tracking lari.
 */
data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMs: Long,
    val speedMs: Float? = null
)
