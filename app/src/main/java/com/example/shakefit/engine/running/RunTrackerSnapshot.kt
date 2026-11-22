package com.example.shakefit.engine.running

import com.example.shakefit.model.DistanceSource
import com.example.shakefit.model.RunEnvironment
import com.example.shakefit.model.RunSplit

/**
 * Snapshot metrik pelacak lari pada satu waktu.
 * Kelas Kotlin murni yang langsung dapat dipetakan ke RunSessionUiState.
 */
data class RunTrackerSnapshot(
    val environment: RunEnvironment,
    val elapsedSec: Int,
    val distanceMeters: Float,
    val distanceSource: DistanceSource,
    val totalSteps: Int,
    val currentPaceSecPerKm: Int?,
    val avgPaceSecPerKm: Int?,
    val bestPaceSecPerKm: Int?,
    val cadenceSpm: Int?,
    val caloriesEst: Float,
    val splits: List<RunSplit>
)
