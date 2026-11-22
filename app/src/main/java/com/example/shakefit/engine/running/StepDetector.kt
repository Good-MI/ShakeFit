package com.example.shakefit.engine.running

import com.example.shakefit.util.Tuning
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * StepDetector: Pendeteksi langkah dari data mentah Accelerometer.
 * Kelas Kotlin murni tanpa dependensi android.*
 *
 * Posisi HP: Saku depan celana atau armband.
 *
 * Mendeteksi benturan langkah (heel strike / push off) dengan:
 * 1. Menghitung magnitudo akselerasi 3 sumbu.
 * 2. Memisahkan baseline gravitasi via low-pass filter.
 * 3. Deteksi puncak (peak detection) dengan hysteresis (Tuning.STEP_ACCEL_THRESHOLD)
 *    dan jeda minimum antar langkah (Tuning.STEP_MIN_INTERVAL_MS = 250ms).
 * 4. Menghitung cadence (langkah/menit) berdasarkan jendela waktu bergulir 12 detik.
 */
class StepDetector(
    private val accelThreshold: Float = Tuning.STEP_ACCEL_THRESHOLD,
    private val minIntervalMs: Long = Tuning.STEP_MIN_INTERVAL_MS
) {

    var totalSteps: Int = 0
        private set

    private var baselineMag = 9.8f
    private var isPeak = false
    private var lastStepTimestampMs: Long = 0L

    // Riwayat timestamp langkah untuk kalkulasi cadence bergulir
    private val recentStepTimestamps = ArrayDeque<Long>()
    private val cadenceWindowMs = 12_000L // 12 detik jendela waktu

    fun reset() {
        totalSteps = 0
        baselineMag = 9.8f
        isPeak = false
        lastStepTimestampMs = 0L
        recentStepTimestamps.clear()
    }

    /**
     * Memproses data akselerometer mentah (x, y, z dalam m/s²).
     * Mengembalikan true jika satu langkah baru terdeteksi.
     */
    fun processAccelerometer(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        val rawMag = sqrt(x * x + y * y + z * z)

        // Baseline gravitasi via low-pass filter
        val alpha = 0.9f
        baselineMag = alpha * baselineMag + (1f - alpha) * rawMag

        // Akselerasi dinamis hentakan langkah
        val dynamicAccel = rawMag - baselineMag

        var stepDetected = false

        if (!isPeak && dynamicAccel > accelThreshold) {
            isPeak = true
        } else if (isPeak && dynamicAccel < (accelThreshold * 0.4f)) {
            // Puncak terlampaui dan sinyal turun kembali (hysteresis)
            isPeak = false

            val interval = if (lastStepTimestampMs == 0L) minIntervalMs else (timestampMs - lastStepTimestampMs)
            if (interval >= minIntervalMs) {
                totalSteps++
                lastStepTimestampMs = timestampMs
                recentStepTimestamps.addLast(timestampMs)
                stepDetected = true
            }
        }

        // Bersihkan timestamp di luar jendela cadence
        while (recentStepTimestamps.isNotEmpty() && (timestampMs - recentStepTimestamps.first()) > cadenceWindowMs) {
            recentStepTimestamps.removeFirst()
        }

        return stepDetected
    }

    /**
     * Menghitung cadence saat ini dalam langkah per menit (SPM).
     */
    fun getCadenceSpm(currentTimestampMs: Long): Int? {
        // Bersihkan entri lama terlebih dahulu
        while (recentStepTimestamps.isNotEmpty() && (currentTimestampMs - recentStepTimestamps.first()) > cadenceWindowMs) {
            recentStepTimestamps.removeFirst()
        }

        val count = recentStepTimestamps.size
        if (count < 2) return null

        val durationMs = currentTimestampMs - recentStepTimestamps.first()
        if (durationMs < 2000L) return null

        val spm = (count * 60_000f / durationMs).roundToInt()
        // Rentang cadence manusia yang wajar saat lari (40 .. 240 spm)
        return spm.coerceIn(40, 240)
    }
}
