package com.example.shakefit.util

/**
 * Konstanta yang masih bersifat asumsi/tebakan.
 * Semua nilai di file ini perlu dikalibrasi di HP asli (Infinix Hot 70).
 *
 * Sesuai restriction.md bagian 3: setiap konstanta tebakan WAJIB ditaruh di sini,
 * diberi komentar ASUMSI, dan dilaporkan ke user di akhir tugas.
 */
object Tuning {

    // ========== SQUAT ==========
    // ASUMSI: perlu dikalibrasi di HP asli
    /** Ambang akselerasi vertikal minimum untuk mendeteksi satu rep squat (m/s²). */
    const val SQUAT_ACCEL_THRESHOLD_LOW = 1.8f

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Ambang akselerasi vertikal maksimum untuk mendeteksi puncak rep squat (m/s²). */
    const val SQUAT_ACCEL_THRESHOLD_HIGH = 3.0f

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Jeda minimum antar rep squat (ms) untuk menghindari hitungan ganda (debounce). */
    const val SQUAT_MIN_REP_INTERVAL_MS = 1200L

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Faktor smoothing low-pass filter (EMA) untuk meredam jitter/noise akselerometer sebelum kalkulasi. */
    const val SQUAT_ACCEL_SMOOTHING = 0.2f

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Faktor smoothing low-pass filter untuk memisahkan gravitasi pada squat. */
    const val SQUAT_GRAVITY_SMOOTHING = 0.85f

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Ambang batas perubahan kemiringan paha minimum saat squat (derajat). */
    const val SQUAT_MIN_TILT_DEGREE = 30f

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Ambang batas kecepatan sudut gyroscope minimum squat (rad/s) jika sensor ada. */
    const val SQUAT_GYRO_MIN_RAD_S = 0.25f

    // ========== PUSH-UP ==========
    // ASUMSI: perlu dikalibrasi di HP asli
    /** Jeda minimum antar rep push-up (ms). */
    const val PUSHUP_MIN_REP_INTERVAL_MS = 500L

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Ambang batas jarak proximity default untuk push-up (cm). */
    const val PUSHUP_PROXIMITY_THRESHOLD_CM = 3.0f

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Ambang batas gangguan gerakan accelerometer lantai saat push-up (m/s²). */
    const val PUSHUP_MAX_FLOOR_DISTURBANCE = 4.0f

    // ========== RUNNING ==========
    // ASUMSI: perlu dikalibrasi di HP asli
    /** Koefisien panjang langkah relatif terhadap tinggi badan (langkah = tinggi * faktor). */
    const val DEFAULT_STEP_LENGTH_FACTOR = 0.415f

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Ambang akselerasi untuk mendeteksi satu langkah (m/s²). */
    const val STEP_ACCEL_THRESHOLD = 1.2f

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Jeda minimum antar langkah (ms). */
    const val STEP_MIN_INTERVAL_MS = 250L

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Jendela waktu bergulir untuk menghitung pace saat ini (detik). */
    const val PACE_WINDOW_SEC = 15

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Akurasi GPS minimum yang diterima (meter). Lokasi lebih buruk dibuang. */
    const val GPS_MIN_ACCURACY_METERS = 25f

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Kecepatan maksimum masuk akal untuk running (m/s). Lonjakan di atas ini dibuang. */
    const val GPS_MAX_SPEED_MS = 15f

    // ========== KALORI ==========
    // ASUMSI: perlu dikalibrasi — koefisien kasar
    /** Kalori per rep squat berdasarkan berat badan default 70kg. */
    const val CALORIES_PER_SQUAT_REP = 0.32f

    // ASUMSI: perlu dikalibrasi — koefisien kasar
    /** Kalori per rep push-up berdasarkan berat badan default 70kg. */
    const val CALORIES_PER_PUSHUP_REP = 0.29f

    // ASUMSI: perlu dikalibrasi — koefisien kasar
    /** Kalori per km running (faktor * berat dalam kg). */
    const val CALORIES_PER_KM_PER_KG = 1.036f

    // ASUMSI: perlu dikalibrasi
    /** Berat badan default jika profil belum diisi (kg). */
    const val DEFAULT_WEIGHT_KG = 70f

    // ASUMSI: perlu dikalibrasi
    /** Tinggi badan default jika profil belum diisi (cm). */
    const val DEFAULT_HEIGHT_CM = 170f

    // ========== SKOR KUALITAS ==========
    // ASUMSI: perlu dikalibrasi di HP asli
    /** Minimum rep sebelum skor kualitas bisa dihitung. */
    const val QUALITY_MIN_REPS = 3

    // ASUMSI: perlu dikalibrasi di HP asli
    /** Batas koefisien variasi maksimum (CV 50%) yang menghasilkan skor kualitas 0. */
    const val QUALITY_MAX_CV = 0.50f

    // ========== UMUM ==========
    /** Durasi countdown default (detik). */
    const val DEFAULT_COUNTDOWN_SEC = 5

    /** Sensitivitas default (0f..1f), tengah. */
    const val DEFAULT_SENSITIVITY = 0.5f

    /** Interval minimum pembaruan lokasi GPS (ms). */
    const val GPS_UPDATE_INTERVAL_MS = 2000L

    /** Jarak minimum pembaruan lokasi GPS (meter). */
    const val GPS_UPDATE_DISTANCE_M = 3f
}
