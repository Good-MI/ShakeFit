package com.example.shakefit.engine.detector

import com.example.shakefit.util.Tuning
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * SquatRepDetector: Pendeteksi repetisi squat berbasis Accelerometer & Gyroscope (opsional).
 * Kelas Kotlin murni tanpa dependensi Android framework (bisa diuji via JUnit lokal).
 *
 * Posisi HP terkunci:
 * Saku depan celana, HP tegak, layar menghadap tubuh. Tidak dipegang.
 *
 * Pipeline deteksi:
 * 1. Low-pass filter (EMA alpha = 0.2f) untuk meredam getaran/noise kecil di saku celana.
 * 2. Low-pass filter untuk memisahkan gravitasi statis dan akselerasi linier gerak dinamis.
 * 3. Hitung orientasi sudut kemiringan (tilt) paha relatif terhadap vertikal.
 * 4. Hitung proyeksi percepatan linier vertikal searah gravitasi.
 * 5. State machine: STANDING -> DESCENDING -> BOTTOM -> ASCENDING -> STANDING (1 rep).
 * 6. Validasi jeda minimum (Tuning.SQUAT_MIN_REP_INTERVAL_MS = 1200L) dan validasi Gyroscope (jika tersedia).
 */
class SquatRepDetector : RepDetector {

    private enum class SquatPhase {
        STANDING,
        DESCENDING,
        BOTTOM,
        ASCENDING
    }

    private var phase = SquatPhase.STANDING
    override var currentReps: Int = 0
        private set

    private var lastRepTimestampMs: Long = 0L

    // Sensitivitas 0f..1f (default Tuning.DEFAULT_SENSITIVITY = 0.5f)
    private var sensitivity: Float = Tuning.DEFAULT_SENSITIVITY

    // Low-pass filter (EMA) untuk raw accelerometer data
    private var smoothX = 0f
    private var smoothY = 0f
    private var smoothZ = 0f
    private var rawInitialized = false

    // Estimasi gravitasi (Low-Pass Filter)
    private var gravX = 0f
    private var gravY = 0f
    private var gravZ = 0f
    private var gravityInitialized = false

    // Gyroscope tracking (opsional)
    private var lastGyroTimestampMs = 0L
    private var maxGyroInCycle = 0f

    private var maxTiltInCycle = 0f

    override fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity.coerceIn(0f, 1f)
    }

    override fun reset() {
        phase = SquatPhase.STANDING
        currentReps = 0
        lastRepTimestampMs = 0L
        rawInitialized = false
        smoothX = 0f
        smoothY = 0f
        smoothZ = 0f
        gravityInitialized = false
        gravX = 0f
        gravY = 0f
        gravZ = 0f
        lastGyroTimestampMs = 0L
        maxGyroInCycle = 0f
        maxTiltInCycle = 0f
    }

    /**
     * Memproses data gyro (opsional validator).
     * Jika tidak dipanggil/gyro null, detector tetap bekerja normal tanpa gyro.
     */
    fun processGyroscope(gx: Float, gy: Float, gz: Float, timestampMs: Long) {
        val angularSpeed = sqrt(gx * gx + gy * gy + gz * gz)
        lastGyroTimestampMs = timestampMs
        if (angularSpeed > maxGyroInCycle) {
            maxGyroInCycle = angularSpeed
        }
    }

    /**
     * Memproses data akselerometer (x, y, z dalam m/s²).
     * Mengembalikan RepResult jika satu rep squat valid selesai, atau null jika belum.
     */
    fun processAccelerometer(x: Float, y: Float, z: Float, timestampMs: Long): RepResult? {
        // 0. Low-Pass Filter (EMA) untuk meredam jitter/noise di saku celana
        if (!rawInitialized) {
            smoothX = x
            smoothY = y
            smoothZ = z
            rawInitialized = true
        } else {
            val alpha = Tuning.SQUAT_ACCEL_SMOOTHING
            smoothX = alpha * smoothX + (1f - alpha) * x
            smoothY = alpha * smoothY + (1f - alpha) * y
            smoothZ = alpha * smoothZ + (1f - alpha) * z
        }

        val rawNorm = sqrt(smoothX * smoothX + smoothY * smoothY + smoothZ * smoothZ)
        if (rawNorm < 0.1f) return null

        // 1. Inisialisasi / Update Gravitasi (Low-Pass Filter)
        if (!gravityInitialized) {
            gravX = smoothX
            gravY = smoothY
            gravZ = smoothZ
            gravityInitialized = true
        } else {
            val alpha = Tuning.SQUAT_GRAVITY_SMOOTHING
            gravX = alpha * gravX + (1f - alpha) * smoothX
            gravY = alpha * gravY + (1f - alpha) * smoothY
            gravZ = alpha * gravZ + (1f - alpha) * smoothZ
        }

        val gNorm = sqrt(gravX * gravX + gravY * gravY + gravZ * gravZ)
        if (gNorm < 0.1f) return null

        // 2. Percepatan linier vertikal dinamis
        val linX = smoothX - gravX
        val linY = smoothY - gravY
        val linZ = smoothZ - gravZ
        val vertAccel = (linX * gravX + linY * gravY + linZ * gravZ) / gNorm

        // 3. Orientasi kemiringan instan paha (tilt angle) relatif terhadap sumbu Y
        // Saat berdiri tegak di saku: |smoothY| / rawNorm ≈ 1.0 (tilt ≈ 0°)
        // Saat jongkok: paha miring menuju horizontal sehingga |smoothY| / rawNorm mengecil (tilt > 30°)
        val currentTiltDeg = (acos((abs(smoothY) / rawNorm).coerceIn(0f, 1f)) * (180f / Math.PI)).toFloat()

        // 4. Adaptasi ambang batas berdasarkan sensitivitas (0f..1f)
        val factor = (1.5f - sensitivity)
        val minTilt = Tuning.SQUAT_MIN_TILT_DEGREE * factor
        val accelThresholdLow = Tuning.SQUAT_ACCEL_THRESHOLD_LOW * factor

        // Debounce / Cooldown pasca repetisi: abaikan pantulan gerakan awal tepat setelah rep selesai
        val inCooldown = lastRepTimestampMs != 0L && (timestampMs - lastRepTimestampMs) < (Tuning.SQUAT_MIN_REP_INTERVAL_MS / 2)

        var repResult: RepResult? = null

        // 5. State Machine Deteksi Repetisi Squat
        when (phase) {
            SquatPhase.STANDING -> {
                if (!inCooldown && (currentTiltDeg >= minTilt * 0.5f || vertAccel < -accelThresholdLow)) {
                    phase = SquatPhase.DESCENDING
                    maxTiltInCycle = currentTiltDeg
                }
            }

            SquatPhase.DESCENDING -> {
                if (currentTiltDeg > maxTiltInCycle) {
                    maxTiltInCycle = currentTiltDeg
                }
                if (currentTiltDeg >= minTilt || (currentTiltDeg >= minTilt * 0.85f && vertAccel > accelThresholdLow)) {
                    phase = SquatPhase.BOTTOM
                } else if (currentTiltDeg <= minTilt * 0.3f && maxTiltInCycle < minTilt * 0.6f) {
                    // Gerakan batal/goyangan kecil yang kembali tegak sebelum mencapai dasar
                    phase = SquatPhase.STANDING
                    maxTiltInCycle = 0f
                }
            }

            SquatPhase.BOTTOM -> {
                if (currentTiltDeg > maxTiltInCycle) {
                    maxTiltInCycle = currentTiltDeg
                }
                // Hysteresis: mulai naik saat sudut berkurang jelas dari puncak atau dorongan akselerasi naik
                if (currentTiltDeg < maxTiltInCycle - 8f || vertAccel > accelThresholdLow) {
                    phase = SquatPhase.ASCENDING
                }
            }

            SquatPhase.ASCENDING -> {
                // Kembali ke posisi berdiri tegak (hysteresis ketat <= minTilt * 0.45f)
                val isStandingUp = currentTiltDeg <= minTilt * 0.45f
                if (isStandingUp) {
                    // Validasi: gerakan harus mencapai kedalaman squat yang cukup
                    val depthReached = maxTiltInCycle >= minTilt

                    // Validasi jeda minimum antar rep (minimal 1200 ms)
                    val interval = if (lastRepTimestampMs == 0L) {
                        Tuning.SQUAT_MIN_REP_INTERVAL_MS
                    } else {
                        timestampMs - lastRepTimestampMs
                    }
                    val isIntervalValid = interval >= Tuning.SQUAT_MIN_REP_INTERVAL_MS

                    // Validasi gyroscope (jika data gyro aktif masuk dalam 2 detik terakhir)
                    val isGyroActive = (timestampMs - lastGyroTimestampMs) < 2000L
                    val isGyroValid = if (isGyroActive) {
                        maxGyroInCycle >= (Tuning.SQUAT_GYRO_MIN_RAD_S * factor)
                    } else {
                        true // Fallback aman jika tanpa gyro
                    }

                    if (depthReached && isIntervalValid && isGyroValid) {
                        currentReps++
                        lastRepTimestampMs = timestampMs
                        repResult = RepResult(
                            repNumber = currentReps,
                            timestampMs = timestampMs,
                            intervalMs = interval
                        )
                    }

                    // Reset siklus
                    phase = SquatPhase.STANDING
                    maxTiltInCycle = 0f
                    maxGyroInCycle = 0f
                }
            }
        }

        return repResult
    }
}
