package com.example.shakefit

import com.example.shakefit.engine.running.GpsPoint
import com.example.shakefit.engine.running.RunTracker
import com.example.shakefit.engine.running.StepDetector
import com.example.shakefit.engine.running.TreadmillCalibrator
import com.example.shakefit.model.DistanceSource
import com.example.shakefit.model.RunEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunTrackerTest {

    @Test
    fun testHaversineFormula_CalculatesKnownDistance() {
        // Monas Jakarta (-6.175392, 106.827153) ke Stasiun Gambir (-6.176667, 106.830556)
        // Jarak garis lurus ~400 meter
        val dist = RunTracker.calculateHaversine(
            -6.175392, 106.827153,
            -6.176667, 106.830556
        )
        assertTrue(dist in 380.0..420.0)
    }

    @Test
    fun testOutdoorGps_AccumulatesDistanceAccurately() {
        val tracker = RunTracker(environment = RunEnvironment.OUTDOOR)
        assertEquals(DistanceSource.GPS, tracker.distanceSource)

        // Titik 1
        val p1 = GpsPoint(latitude = -6.200000, longitude = 106.800000, accuracyMeters = 8f, timestampMs = 1000L)
        assertTrue(tracker.onGpsLocation(p1))
        assertEquals(0f, tracker.distanceMeters, 0.01f)

        // Titik 2 (bergeser ~111 meter ke utara: lat -6.199000, dalam waktu 20 detik => ~5.5 m/s)
        val p2 = GpsPoint(latitude = -6.199000, longitude = 106.800000, accuracyMeters = 8f, timestampMs = 21000L)
        assertTrue(tracker.onGpsLocation(p2))

        val snapshot = tracker.getSnapshot()
        assertTrue("Jarak harus terakumulasi ~111 meter", snapshot.distanceMeters in 105f..115f)
        assertEquals(DistanceSource.GPS, snapshot.distanceSource)
    }

    @Test
    fun testOutdoorGps_RejectsPoorAccuracy() {
        val tracker = RunTracker(environment = RunEnvironment.OUTDOOR)

        val p1 = GpsPoint(latitude = -6.200000, longitude = 106.800000, accuracyMeters = 10f, timestampMs = 1000L)
        assertTrue(tracker.onGpsLocation(p1))

        // Titik dengan akurasi 35m (lebih buruk dari ambang Tuning.GPS_MIN_ACCURACY_METERS = 25m)
        val pBad = GpsPoint(latitude = -6.199000, longitude = 106.800000, accuracyMeters = 35f, timestampMs = 15000L)
        assertFalse(tracker.onGpsLocation(pBad))

        // Jarak tetap 0 karena titik kedua dibuang
        assertEquals(0f, tracker.distanceMeters, 0.01f)
    }

    @Test
    fun testOutdoorGps_RejectsSpeedJump() {
        val tracker = RunTracker(environment = RunEnvironment.OUTDOOR)

        val p1 = GpsPoint(latitude = -6.200000, longitude = 106.800000, accuracyMeters = 5f, timestampMs = 1000L)
        assertTrue(tracker.onGpsLocation(p1))

        // Lonjakan koordinat: bergeser 0.01 derajat (~1.1 km) hanya dalam selang 2 detik (> 500 m/s > 15 m/s)
        val pJump = GpsPoint(latitude = -6.190000, longitude = 106.800000, accuracyMeters = 5f, timestampMs = 3000L)
        assertFalse(tracker.onGpsLocation(pJump))

        // Titik teleportasi harus dibuang
        assertEquals(0f, tracker.distanceMeters, 0.01f)
    }

    @Test
    fun testSplits_GeneratedEvery1Km() {
        val tracker = RunTracker(environment = RunEnvironment.OUTDOOR)

        var currentLat = -6.200000
        var currentTimeMs = 1000L
        var elapsedSec = 0

        tracker.onGpsLocation(GpsPoint(currentLat, 106.800000, 5f, currentTimeMs))

        // Simulasikan perpindahan sejauh ~2.5 km
        // Setiap ~0.009 derajat latitude ≈ 1000 meter
        // Lakukan 25 langkah @ 100m (0.0009 derajat, selang 30 detik => pace 5:00/km)
        for (i in 1..25) {
            currentLat += 0.0009
            currentTimeMs += 30_000L
            elapsedSec += 30
            tracker.onTick(elapsedSec)
            tracker.onGpsLocation(GpsPoint(currentLat, 106.800000, 5f, currentTimeMs))
        }

        val snapshot = tracker.getSnapshot()
        assertTrue("Jarak harus melampaui 2000m", snapshot.distanceMeters >= 2000f)
        assertEquals("Harus ada tepat 2 split untuk 2 km yang tuntas", 2, snapshot.splits.size)

        val split1 = snapshot.splits[0]
        assertEquals(1, split1.kmIndex)
        assertTrue("Durasi split 1 harus positif", split1.durationSec > 0)
        assertTrue("Pace split 1 harus positif", split1.paceSecPerKm > 0)

        val split2 = snapshot.splits[1]
        assertEquals(2, split2.kmIndex)
    }

    @Test
    fun testPaceCalculation_AverageAndCurrent() {
        val tracker = RunTracker(environment = RunEnvironment.OUTDOOR)

        // Lari 1000 meter dalam 300 detik (5 menit => pace 300 detik/km atau 5:00 /km)
        tracker.onTick(300)

        var lat = -6.200000
        var t = 1000L
        tracker.onGpsLocation(GpsPoint(lat, 106.800000, 5f, t))

        // Geser ~1000m dalam 300 detik
        for (i in 1..10) {
            lat += 0.0009
            t += 30_000L
            tracker.onGpsLocation(GpsPoint(lat, 106.800000, 5f, t))
        }

        val snapshot = tracker.getSnapshot()
        assertNotNull(snapshot.avgPaceSecPerKm)
        // Pace harus mendekati 300 s/km (5:00 /km)
        assertTrue("Avg pace harus berkisar 280-320 s/km", snapshot.avgPaceSecPerKm in 280..320)
    }

    @Test
    fun testTreadmill_StepDetectionAndDistance() {
        val userHeight = 175f // 1.75m
        val stepFactor = 0.415f
        // Panjang langkah = 1.75 * 0.415 = 0.72625 meter
        val tracker = RunTracker(
            environment = RunEnvironment.TREADMILL,
            userHeightCm = userHeight,
            treadmillStepLengthFactor = stepFactor
        )
        assertEquals(DistanceSource.STEPS_ESTIMATE, tracker.distanceSource)

        // Simulasikan 50 langkah lari via akselerometer
        val stepIntervalMs = 350L // Cadence ~171 spm
        var currentTimeMs = 1000L

        for (step in 1..50) {
            // Fase naik / hentakan langkah
            currentTimeMs += stepIntervalMs / 2
            tracker.onAccelerometer(x = 0.5f, y = 14.0f, z = 2.0f, timestampMs = currentTimeMs)

            // Fase turun
            currentTimeMs += stepIntervalMs / 2
            tracker.onAccelerometer(x = 0.1f, y = 8.0f, z = 0.5f, timestampMs = currentTimeMs)
        }

        val snapshot = tracker.getSnapshot()
        assertEquals(50, snapshot.totalSteps)

        val expectedDistance = 50 * (1.75f * stepFactor)
        assertEquals(expectedDistance, snapshot.distanceMeters, 0.5f)
        assertEquals(DistanceSource.STEPS_ESTIMATE, snapshot.distanceSource)
    }

    @Test
    fun testTreadmillCalibrator_Formula() {
        // Kasus: User tinggi 170cm berlari 1000m di treadmill, terhitung 1200 langkah
        // newFactor = 1000 / (1200 * 1.70) = 1000 / 2040 = 0.4902f
        val actualMeters = 1000f
        val steps = 1200
        val heightCm = 170f

        val calibratedFactor = TreadmillCalibrator.calculateCalibratedStepFactor(
            actualDistanceMeters = actualMeters,
            totalSteps = steps,
            userHeightCm = heightCm
        )

        assertEquals(0.4902f, calibratedFactor, 0.005f)

        // Kasus edge: input 0 langkah atau 0 meter
        val fallback = TreadmillCalibrator.calculateCalibratedStepFactor(0f, 0, heightCm)
        assertEquals(0.415f, fallback, 0.001f)
    }

    @Test
    fun testCaloriesEstimation() {
        val tracker = RunTracker(
            environment = RunEnvironment.TREADMILL,
            userWeightKg = 70f,
            userHeightCm = 170f,
            treadmillStepLengthFactor = 0.415f
        )

        // Lakukan langkah hingga mencapai jarak tertentu
        // 1418 langkah @ 0.7055m ≈ 1000 meter (1 km)
        var t = 1000L
        for (i in 1..1418) {
            t += 175L
            tracker.onAccelerometer(0f, 15f, 0f, t)
            t += 175L
            tracker.onAccelerometer(0f, 8f, 0f, t)
        }

        val snapshot = tracker.getSnapshot()
        assertTrue("Jarak harus ~1000m", snapshot.distanceMeters in 990f..1010f)

        // Rumus kalori: distKm * weightKg * 1.036f
        // ~1.0 km * 70 kg * 1.036f = ~72.5 kkal
        assertTrue("Estimasi kalori harus berkisar 70-75 kkal", snapshot.caloriesEst in 70f..75f)
    }

    @Test
    fun testStepDetector_CadenceCalculation() {
        val detector = StepDetector()

        // Kirim langkah dengan ritme 333ms per langkah (3 langkah per detik = 180 SPM)
        var t = 10_000L
        for (i in 1..25) {
            t += 166L
            detector.processAccelerometer(0f, 14f, 0f, t)
            t += 167L
            detector.processAccelerometer(0f, 8f, 0f, t)
        }

        val cadence = detector.getCadenceSpm(t)
        assertNotNull(cadence)
        assertTrue("Cadence harus mendekati 180 spm (rentang 165..195)", cadence!! in 165..195)
    }
}
