package com.example.shakefit.util

import java.util.Locale

/**
 * Fungsi-fungsi format sesuai ui-contract.md bagian 2.
 * Dipakai oleh UI untuk menampilkan metrik domain.
 * Semua kalkulasi format ada di sini, UI tidak menghitung sendiri.
 */

/** Format pace dari detik per km ke "M:SS /km". Null -> "--:--". */
fun formatPace(secPerKm: Int?): String {
    if (secPerKm == null || secPerKm <= 0) return "--:--"
    val minutes = secPerKm / 60
    val seconds = secPerKm % 60
    return String.format(Locale.US, "%d:%02d /km", minutes, seconds)
}

/**
 * Format durasi dari total detik.
 * >= 3600 -> "H:MM:SS"
 * < 3600  -> "MM:SS"
 */
fun formatDuration(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

/** Format jarak dari meter ke "X.XX km". */
fun formatDistanceKm(meters: Float): String {
    val km = meters / 1000f
    return String.format(Locale.US, "%.2f km", km)
}

/** Format kalori, selalu berawalan "~" karena estimasi. */
fun formatCalories(kcal: Float): String {
    return String.format(Locale.US, "~%.0f kkal", kcal)
}

