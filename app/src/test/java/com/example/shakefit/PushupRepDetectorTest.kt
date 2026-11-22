package com.example.shakefit

import com.example.shakefit.engine.detector.PushupRepDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PushupRepDetectorTest {

    private lateinit var detector: PushupRepDetector

    @Before
    fun setUp() {
        detector = PushupRepDetector()
    }

    @Test
    fun testBinaryProximity_CountsRepAccurately() {
        val maxRange = 5.0f // Sensor biner khas (0cm = dekat, 5cm = jauh)

        // Kondisi awal di lantai: badan di atas (jauh)
        var result = detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 0L)
        assertNull(result)
        assertEquals(0, detector.currentReps)

        // Dada mendekat ke lantai (turun)
        result = detector.processProximity(distanceCm = 0.0f, maxRange = maxRange, timestampMs = 500L)
        assertNull(result)
        assertEquals(0, detector.currentReps)

        // Dada naik kembali ke atas (siklus selesai pada t=1200ms)
        result = detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 1200L)
        assertNotNull(result)
        assertEquals(1, detector.currentReps)
        assertEquals(1, result?.repNumber)
    }

    @Test
    fun testContinuousProximity_CountsRepAccurately() {
        val maxRange = 10.0f // Sensor analog/continuous

        // Awal di atas: 8cm
        detector.processProximity(distanceCm = 8.0f, maxRange = maxRange, timestampMs = 0L)

        // Turun ke lantai: 1.5cm
        detector.processProximity(distanceCm = 1.5f, maxRange = maxRange, timestampMs = 600L)

        // Kembali naik: 7.5cm (interval 1400ms > 500ms)
        val result = detector.processProximity(distanceCm = 7.5f, maxRange = maxRange, timestampMs = 1400L)
        assertNotNull(result)
        assertEquals(1, detector.currentReps)
    }

    @Test
    fun testMultiplePushups_ConsecutiveCounting() {
        val maxRange = 5.0f

        // Rep 1
        detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 0L)
        detector.processProximity(distanceCm = 0.0f, maxRange = maxRange, timestampMs = 500L)
        detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 1200L)
        assertEquals(1, detector.currentReps)

        // Rep 2
        detector.processProximity(distanceCm = 0.0f, maxRange = maxRange, timestampMs = 1800L)
        detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 2400L)
        assertEquals(2, detector.currentReps)

        // Rep 3
        detector.processProximity(distanceCm = 0.0f, maxRange = maxRange, timestampMs = 3000L)
        detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 3700L)
        assertEquals(3, detector.currentReps)
    }

    @Test
    fun testMinimumInterval_RejectsRapidTriggers() {
        val maxRange = 5.0f

        // Rep 1 pada t = 1000ms
        detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 0L)
        detector.processProximity(distanceCm = 0.0f, maxRange = maxRange, timestampMs = 500L)
        detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 1000L)
        assertEquals(1, detector.currentReps)

        // Rep 2 terlalu cepat (hanya selang 200ms < 500ms)
        detector.processProximity(distanceCm = 0.0f, maxRange = maxRange, timestampMs = 1100L)
        val result = detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 1200L)

        assertNull(result)
        assertEquals(1, detector.currentReps)
    }

    @Test
    fun testAccelerometerDisturbance_RejectsRepWhenPhoneLiftedOrShaken() {
        val maxRange = 5.0f

        // Posisi awal di lantai
        detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 0L)

        // Proximity membaca dekat
        detector.processProximity(distanceCm = 0.0f, maxRange = maxRange, timestampMs = 500L)

        // Namun accelerometer mendeteksi HP diangkat/dimiringkan/digoyang keras (bukan rata di lantai)
        detector.processAccelerometer(x = 6.0f, y = 7.0f, z = 2.0f, timestampMs = 600L)

        // Proximity membaca jauh kembali
        val result = detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 1200L)

        // Harus ditolak karena ada gangguan lantai (bukan pushup sah)
        assertNull(result)
        assertEquals(0, detector.currentReps)
    }

    @Test
    fun testAccelerometerStableOnFloor_AllowsRep() {
        val maxRange = 5.0f

        detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 0L)
        detector.processProximity(distanceCm = 0.0f, maxRange = maxRange, timestampMs = 500L)

        // Accelerometer normal di lantai (x ≈ 0, y ≈ 0, z ≈ 9.8)
        detector.processAccelerometer(x = 0.1f, y = 0.1f, z = 9.8f, timestampMs = 600L)

        val result = detector.processProximity(distanceCm = 5.0f, maxRange = maxRange, timestampMs = 1200L)

        assertNotNull(result)
        assertEquals(1, detector.currentReps)
    }
}
