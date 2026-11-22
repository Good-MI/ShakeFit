package com.example.shakefit.engine.running

import com.example.shakefit.model.DistanceSource
import com.example.shakefit.model.RunEnvironment
import com.example.shakefit.model.RunSplit
import com.example.shakefit.util.Tuning
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * RunTracker: Engine pelacak lari untuk mode OUTDOOR (GPS) dan TREADMILL (Langkah).
 * Kelas Kotlin murni tanpa dependensi Android framework (bisa diuji via JUnit lokal).
 *
 * Fitur:
 * 1. Akumulasi jarak:
 *    - OUTDOOR: Jarak dari koordinat GPS via formula Haversine murni, dengan filter akurasi buruk (>25m)
 *      dan lonjakan kecepatan tidak masuk akal (>15 m/s).
 *    - TREADMILL: Jarak = langkah x panjang langkah (tinggi badan x faktor kalibrasi).
 * 2. Perhitungan Pace (dalam detik per km):
 *    - currentPaceSecPerKm: Pace jendela waktu bergulir 15 detik.
 *    - avgPaceSecPerKm: Total durasi dibagi total km.
 *    - bestPaceSecPerKm: Pace tercepat yang pernah dicapai.
 * 3. Split per 1 km (RunSplit: kmIndex, durationSec, paceSecPerKm).
 * 4. Cadence (SPM) dihitung dari akselerometer pada kedua mode.
 * 5. Estimasi kalori berdasarkan berat badan dan jarak lari.
 */
class RunTracker(
    val environment: RunEnvironment,
    var userWeightKg: Float = Tuning.DEFAULT_WEIGHT_KG,
    var userHeightCm: Float = Tuning.DEFAULT_HEIGHT_CM,
    var treadmillStepLengthFactor: Float = Tuning.DEFAULT_STEP_LENGTH_FACTOR
) {

    var distanceMeters: Float = 0f
        private set

    var elapsedSec: Int = 0
        private set

    val distanceSource: DistanceSource = when (environment) {
        RunEnvironment.OUTDOOR -> DistanceSource.GPS
        RunEnvironment.TREADMILL -> DistanceSource.STEPS_ESTIMATE
    }

    private val stepDetector = StepDetector()
    private val splits = mutableListOf<RunSplit>()

    private var bestPaceSecPerKm: Int? = null
    private var lastSplitCompletedKm = 0
    private var lastSplitElapsedSec = 0

    // Pelacakan GPS (Outdoor)
    private var lastGpsPoint: GpsPoint? = null

    // Jendela waktu bergulir untuk perhitungan pace saat ini
    private data class DistanceRecord(val timestampMs: Long, val cumulativeDistanceMeters: Float)
    private val paceWindow = ArrayDeque<DistanceRecord>()
    private var lastTimestampMs: Long = 0L

    fun reset() {
        distanceMeters = 0f
        elapsedSec = 0
        splits.clear()
        bestPaceSecPerKm = null
        lastSplitCompletedKm = 0
        lastSplitElapsedSec = 0
        lastGpsPoint = null
        paceWindow.clear()
        lastTimestampMs = 0L
        stepDetector.reset()
    }

    /**
     * Memperbarui durasi waktu lari yang telah berjalan.
     */
    fun onTick(currentElapsedSec: Int) {
        this.elapsedSec = currentElapsedSec
    }

    /**
     * Memproses data lokasi GPS (khusus mode OUTDOOR).
     * Mengembalikan true jika titik valid dan diakumulasikan ke jarak,
     * false jika dibuang (akurasi buruk atau lonjakan kecepatan).
     */
    fun onGpsLocation(point: GpsPoint): Boolean {
        if (environment != RunEnvironment.OUTDOOR) return false

        lastTimestampMs = point.timestampMs

        // 1. Filter akurasi buruk (> Tuning.GPS_MIN_ACCURACY_METERS)
        if (point.accuracyMeters > Tuning.GPS_MIN_ACCURACY_METERS) {
            return false
        }

        // 2. Filter lonjakan kecepatan
        val prev = lastGpsPoint
        if (prev != null) {
            val timeDiffSec = (point.timestampMs - prev.timestampMs) / 1000.0
            if (timeDiffSec <= 0.0) return false // Abaikan timestamp duplikat / mundur

            val deltaDist = calculateHaversine(
                prev.latitude, prev.longitude,
                point.latitude, point.longitude
            ).toFloat()

            val speedMs = deltaDist / timeDiffSec.toFloat()
            if (speedMs > Tuning.GPS_MAX_SPEED_MS) {
                return false // Lonjakan kecepatan tidak masuk akal (teleportasi sinyal)
            }

            if (point.speedMs != null && point.speedMs > Tuning.GPS_MAX_SPEED_MS) {
                return false
            }

            // Akumulasi jarak hanya jika ada pergerakan minimal (filter jitter statis)
            if (deltaDist >= 0.5f) {
                distanceMeters += deltaDist
                recordPaceSample(point.timestampMs, distanceMeters)
                checkKmSplits()
            }
        } else {
            // Titik GPS pertama yang valid
            recordPaceSample(point.timestampMs, distanceMeters)
        }

        lastGpsPoint = point
        return true
    }

    /**
     * Memproses data akselerometer (untuk menghitung cadence dan langkah treadmill).
     */
    fun onAccelerometer(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        lastTimestampMs = timestampMs
        val stepDetected = stepDetector.processAccelerometer(x, y, z, timestampMs)

        if (stepDetected && environment == RunEnvironment.TREADMILL) {
            // Hitung panjang langkah (m) = (tinggi dalam m) x faktor kalibrasi
            val effectiveHeight = if (userHeightCm > 0f) userHeightCm else Tuning.DEFAULT_HEIGHT_CM
            val effectiveFactor = if (treadmillStepLengthFactor > 0f) treadmillStepLengthFactor else Tuning.DEFAULT_STEP_LENGTH_FACTOR
            val stepLengthMeters = (effectiveHeight / 100f) * effectiveFactor

            distanceMeters += stepLengthMeters
            recordPaceSample(timestampMs, distanceMeters)
            checkKmSplits()
        }

        return stepDetected
    }

    /**
     * Mencatat sampel jarak ke dalam jendela bergulir untuk menghitung pace saat ini.
     */
    private fun recordPaceSample(timestampMs: Long, cumulativeDistance: Float) {
        paceWindow.addLast(DistanceRecord(timestampMs, cumulativeDistance))

        // Buang data di luar jendela waktu PACE_WINDOW_SEC
        val windowCutoff = timestampMs - (Tuning.PACE_WINDOW_SEC * 1000L)
        while (paceWindow.isNotEmpty() && paceWindow.first().timestampMs < windowCutoff) {
            paceWindow.removeFirst()
        }

        // Perbarui best pace jika pace saat ini valid
        val currentPace = calculateCurrentPace()
        if (currentPace != null && currentPace in 120..1800) { // Rentang 2:00/km s.d 30:00/km
            if (bestPaceSecPerKm == null || currentPace < bestPaceSecPerKm!!) {
                bestPaceSecPerKm = currentPace
            }
        }
    }

    /**
     * Memeriksa apakah jarak telah melampaui kelipatan 1000m untuk mencatat RunSplit.
     */
    private fun checkKmSplits() {
        val currentKm = (distanceMeters / 1000f).toInt()
        while (currentKm > lastSplitCompletedKm) {
            val kmIndex = lastSplitCompletedKm + 1
            val splitDurationSec = elapsedSec - lastSplitElapsedSec
            val splitPaceSecPerKm = splitDurationSec // 1 km ditempuh dalam splitDurationSec detik

            splits.add(
                RunSplit(
                    kmIndex = kmIndex,
                    durationSec = splitDurationSec,
                    paceSecPerKm = splitPaceSecPerKm
                )
            )

            lastSplitCompletedKm = kmIndex
            lastSplitElapsedSec = elapsedSec
        }
    }

    /**
     * Menghitung pace saat ini (detik per km) dari jendela waktu bergulir 15 detik.
     */
    private fun calculateCurrentPace(): Int? {
        if (paceWindow.size < 2) return null

        val oldest = paceWindow.first()
        val newest = paceWindow.last()

        val timeDiffSec = (newest.timestampMs - oldest.timestampMs) / 1000.0
        val distDiffMeters = newest.cumulativeDistanceMeters - oldest.cumulativeDistanceMeters

        // Butuh durasi minimal 4 detik dan perpindahan minimal 4 meter agar tidak pembagian nol
        if (timeDiffSec < 4.0 || distDiffMeters < 4.0f) return null

        val distDiffKm = distDiffMeters / 1000.0
        val pace = (timeDiffSec / distDiffKm).roundToInt()
        return pace.coerceIn(120, 1800)
    }

    /**
     * Menghitung pace rata-rata total (detik per km).
     */
    private fun calculateAvgPace(): Int? {
        val distKm = distanceMeters / 1000.0
        if (distKm < 0.02 || elapsedSec <= 0) return null
        return (elapsedSec / distKm).roundToInt()
    }

    /**
     * Menghitung estimasi kalori lari (kkal).
     */
    private fun calculateCalories(): Float {
        val effectiveWeight = if (userWeightKg > 0f) userWeightKg else Tuning.DEFAULT_WEIGHT_KG
        val distKm = distanceMeters / 1000f
        return distKm * effectiveWeight * Tuning.CALORIES_PER_KM_PER_KG
    }

    /**
     * Mengambil snapshot metrik saat ini untuk diteruskan ke UI / StateFlow.
     */
    fun getSnapshot(): RunTrackerSnapshot {
        return RunTrackerSnapshot(
            environment = environment,
            elapsedSec = elapsedSec,
            distanceMeters = distanceMeters,
            distanceSource = distanceSource,
            totalSteps = stepDetector.totalSteps,
            currentPaceSecPerKm = calculateCurrentPace(),
            avgPaceSecPerKm = calculateAvgPace(),
            bestPaceSecPerKm = bestPaceSecPerKm,
            cadenceSpm = stepDetector.getCadenceSpm(lastTimestampMs),
            caloriesEst = calculateCalories(),
            splits = splits.toList()
        )
    }

    companion object {
        /**
         * Menghitung jarak melengkung pada permukaan bumi antara 2 koordinat (meter)
         * menggunakan formula Haversine murni.
         */
        fun calculateHaversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0 // Radius rata-rata bumi dalam meter
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
