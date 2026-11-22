package com.example.shakefit

import com.example.shakefit.engine.detector.SquatRepDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SquatRepDetectorTest {

    private lateinit var detector: SquatRepDetector

    @Before
    fun setUp() {
        detector = SquatRepDetector()
    }

    /**
     * Membantu simulasi satu siklus squat normal pada interval waktu tertentu.
     */
    private fun simulateSquatCycle(
        startMs: Long,
        durationMs: Long = 1200L,
        withGyro: Boolean = false
    ) {
        val steps = 30
        val stepMs = durationMs / steps

        for (i in 0..steps) {
            val t = startMs + (i * stepMs)
            val progress = i.toFloat() / steps // 0.0 -> 1.0

            // Simulasi paha: tegak (Y ≈ -9.8) -> horizontal (Y ≈ -2.0, Z ≈ -9.5) -> tegak (Y ≈ -9.8)
            val sine = Math.sin(progress * Math.PI).toFloat() // 0.0 di awal/akhir, 1.0 di puncak squat

            val y = -9.8f + (7.8f * sine)
            val z = -(9.5f * sine)
            val x = 0.2f

            if (withGyro) {
                // Kecepatan sudut rotasi paha
                val gyroSpeed = if (progress in 0.1f..0.9f) 0.8f else 0.05f
                detector.processGyroscope(gx = gyroSpeed, gy = 0.1f, gz = 0f, timestampMs = t)
            }

            detector.processAccelerometer(x = x, y = y, z = z, timestampMs = t)
        }
    }

    @Test
    fun testNormalSquat_DetectedAccurately() {
        // Inisialisasi posisi berdiri diam
        for (i in 0..10) {
            detector.processAccelerometer(x = 0f, y = -9.8f, z = 0f, timestampMs = (i * 50).toLong())
        }
        assertEquals(0, detector.currentReps)

        // Lakukan 1 siklus squat dengan jeda waktu normal
        simulateSquatCycle(startMs = 1000L, durationMs = 1500L, withGyro = true)

        assertEquals(1, detector.currentReps)
    }

    @Test
    fun testMultipleSquats_CountConsecutively() {
        // Inisialisasi awal
        for (i in 0..10) {
            detector.processAccelerometer(x = 0f, y = -9.8f, z = 0f, timestampMs = (i * 50).toLong())
        }

        // Rep 1
        simulateSquatCycle(startMs = 1000L, durationMs = 1200L)
        assertEquals(1, detector.currentReps)

        // Rep 2 (dimulai setelah jeda wajar)
        simulateSquatCycle(startMs = 3000L, durationMs = 1200L)
        assertEquals(2, detector.currentReps)

        // Rep 3
        simulateSquatCycle(startMs = 5000L, durationMs = 1200L)
        assertEquals(3, detector.currentReps)
    }

    @Test
    fun testSmallNoise_DoesNotTriggerRep() {
        // Simulasi getaran/noise kecil saat jalan santai (tidak ada perubahan sudut kemiringan paha)
        for (i in 0..100) {
            val t = (i * 50).toLong()
            val noiseY = -9.8f + (Math.sin(i * 0.5).toFloat() * 0.4f)
            val noiseX = Math.cos(i * 0.5).toFloat() * 0.3f
            detector.processAccelerometer(x = noiseX, y = noiseY, z = 0f, timestampMs = t)
        }

        assertEquals(0, detector.currentReps)
    }

    @Test
    fun testMinimumInterval_RejectsTooFastRep() {
        // Inisialisasi awal
        for (i in 0..10) {
            detector.processAccelerometer(x = 0f, y = -9.8f, z = 0f, timestampMs = (i * 50).toLong())
        }

        // Rep 1 selesai pada t ≈ 2200ms
        simulateSquatCycle(startMs = 1000L, durationMs = 1200L)
        assertEquals(1, detector.currentReps)

        // Rep 2 dipaksa selesai hanya 300ms setelahnya (kurang dari Tuning.SQUAT_MIN_REP_INTERVAL_MS = 600L)
        simulateSquatCycle(startMs = 2300L, durationMs = 200L)

        // Harus tetap 1 karena rep kedua diabaikan akibat jeda minimum
        assertEquals(1, detector.currentReps)
    }

    @Test
    fun testFallbackWithoutGyroscope_WorksNormally() {
        // Inisialisasi awal
        for (i in 0..10) {
            detector.processAccelerometer(x = 0f, y = -9.8f, z = 0f, timestampMs = (i * 50).toLong())
        }

        // Tanpa memanggil processGyroscope sama sekali
        simulateSquatCycle(startMs = 1000L, durationMs = 1400L, withGyro = false)

        // Wajib tetap terhitung 1 rep (fallback aman saat sensor gyro tidak ada di HP)
        assertEquals(1, detector.currentReps)
    }
}
