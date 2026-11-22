package com.example.shakefit.engine.detector

import com.example.shakefit.util.Tuning
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * PushupRepDetector: Pendeteksi repetisi push-up berbasis Proximity sensor (utama)
 * dan Accelerometer (pendukung untuk validasi kestabilan lantai).
 * Kelas Kotlin murni tanpa dependensi Android framework (bisa diuji via JUnit lokal).
 *
 * Posisi HP terkunci:
 * Rata di lantai, layar menghadap atas, bagian atas HP (dekat sensor proximity) tepat di bawah dada.
 *
 * Logika deteksi:
 * 1. Mulai di posisi UP (dada jauh dari HP).
 * 2. Saat dada mendekat (siklus NEAR, baik pada sensor analog maupun biner), masuk fase DOWN.
 * 3. Saat dada kembali ke posisi atas (FAR), terhitung 1 rep jika:
 *    - Jeda antar rep >= Tuning.PUSHUP_MIN_REP_INTERVAL_MS
 *    - HP tetap stabil di lantai (akselerometer tidak mendeteksi HP diangkat/digoyang).
 */
class PushupRepDetector : RepDetector {

    private enum class PushupPhase {
        UP,
        DOWN
    }

    private var phase = PushupPhase.UP
    override var currentReps: Int = 0
        private set

    private var lastRepTimestampMs: Long = 0L

    // Sensitivitas 0f..1f (default Tuning.DEFAULT_SENSITIVITY = 0.5f)
    private var sensitivity: Float = Tuning.DEFAULT_SENSITIVITY

    // Deteksi gangguan pada HP di lantai via accelerometer
    private var floorDisturbed = false

    override fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity.coerceIn(0f, 1f)
    }

    override fun reset() {
        phase = PushupPhase.UP
        currentReps = 0
        lastRepTimestampMs = 0L
        floorDisturbed = false
    }

    /**
     * Memproses data accelerometer sebagai validator pendukung.
     * Karena HP rata di lantai, accelerometer membaca mendekati x=0, y=0, z=9.8 m/s².
     * Jika HP diangkat, dimiringkan, atau digoyang keras, tandai sebagai gangguan.
     */
    fun processAccelerometer(x: Float, y: Float, z: Float, @Suppress("UNUSED_PARAMETER") timestampMs: Long) {
        val totalMagnitude = sqrt(x * x + y * y + z * z)
        val horizMagnitude = sqrt(x * x + y * y)

        // Deviasi dari gravitasi 9.8 m/s² dan komponen horizontal
        val disturbance = abs(totalMagnitude - 9.8f) + horizMagnitude

        if (disturbance > Tuning.PUSHUP_MAX_FLOOR_DISTURBANCE) {
            floorDisturbed = true
        }
    }

    /**
     * Memproses data sensor Proximity (jarak dalam cm dan nilai maximumRange sensor).
     * Mendukung sensor analog maupun sensor biner (misal Infinix Hot 70 dengan 0 cm vs maxRange).
     */
    fun processProximity(distanceCm: Float, maxRange: Float, timestampMs: Long): RepResult? {
        // Tentukan apakah kondisi sensor berada dalam status "NEAR" (dada dekat lantai)
        val isBinary = maxRange <= 5.0f
        val isNear = if (isBinary) {
            // Pada sensor biner: jika jarak terbaca lebih kecil dari separuh maxRange (biasanya 0.0 cm)
            distanceCm < (maxRange * 0.5f)
        } else {
            // Pada sensor continuous: sesuaikan ambang batas dengan faktor sensitivitas
            val factor = 0.6f + 0.8f * sensitivity
            val threshold = (Tuning.PUSHUP_PROXIMITY_THRESHOLD_CM * factor).coerceAtMost(maxRange * 0.8f)
            distanceCm <= threshold
        }

        var repResult: RepResult? = null

        when (phase) {
            PushupPhase.UP -> {
                // Dada mendekat ke lantai -> masuk fase DOWN
                if (isNear) {
                    phase = PushupPhase.DOWN
                }
            }

            PushupPhase.DOWN -> {
                // Dada menjauh kembali ke posisi atas -> siklus 1 rep selesai
                if (!isNear) {
                    val interval = if (lastRepTimestampMs == 0L) {
                        Tuning.PUSHUP_MIN_REP_INTERVAL_MS
                    } else {
                        timestampMs - lastRepTimestampMs
                    }

                    val isIntervalValid = interval >= Tuning.PUSHUP_MIN_REP_INTERVAL_MS

                    // Validasi kestabilan HP di lantai: jangan hitung jika HP sedang diangkat/digoyang
                    if (isIntervalValid && !floorDisturbed) {
                        currentReps++
                        lastRepTimestampMs = timestampMs
                        repResult = RepResult(
                            repNumber = currentReps,
                            timestampMs = timestampMs,
                            intervalMs = interval
                        )
                    }

                    // Reset status untuk siklus berikutnya
                    phase = PushupPhase.UP
                    floorDisturbed = false
                }
            }
        }

        return repResult
    }
}
