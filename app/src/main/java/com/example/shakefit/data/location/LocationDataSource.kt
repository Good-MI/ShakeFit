package com.example.shakefit.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.example.shakefit.model.GpsStatus
import com.example.shakefit.util.Tuning
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Data detail mentah lokasi untuk keperluan pengujian GPS.
 */
data class RawLocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracyMeters: Float,
    val speedMs: Float,
    val timestampEpochMs: Long,
    val status: GpsStatus
)

/**
 * LocationDataSource membungkus LocationManager bawaan Android (tanpa Google Play Services).
 * Menghandle izin, status sinyal, dan pembaruan lokasi.
 * Listener wajib dilepas di awaitClose saat flow di-cancel.
 */
class LocationDataSource(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /** Cek apakah izin lokasi diberikan */
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /** Cek apakah provider GPS aktif */
    fun isGpsEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Mengamati update lokasi GPS mentah.
     * Mengembalikan RawLocationData yang berisi metrik dan pemetaan GpsStatus.
     */
    @SuppressLint("MissingPermission")
    fun getLocationFlow(
        minTimeMs: Long = Tuning.GPS_UPDATE_INTERVAL_MS,
        minDistanceM: Float = Tuning.GPS_UPDATE_DISTANCE_M
    ): Flow<RawLocationData> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(
                RawLocationData(
                    latitude = 0.0,
                    longitude = 0.0,
                    altitude = 0.0,
                    accuracyMeters = 0f,
                    speedMs = 0f,
                    timestampEpochMs = System.currentTimeMillis(),
                    status = GpsStatus.PERMISSION_DENIED
                )
            )
            close()
            return@callbackFlow
        }

        if (!isGpsEnabled()) {
            trySend(
                RawLocationData(
                    latitude = 0.0,
                    longitude = 0.0,
                    altitude = 0.0,
                    accuracyMeters = 0f,
                    speedMs = 0f,
                    timestampEpochMs = System.currentTimeMillis(),
                    status = GpsStatus.DISABLED
                )
            )
            close()
            return@callbackFlow
        }

        // Status awal: Mencari sinyal GPS
        trySend(
            RawLocationData(
                latitude = 0.0,
                longitude = 0.0,
                altitude = 0.0,
                accuracyMeters = 0f,
                speedMs = 0f,
                timestampEpochMs = System.currentTimeMillis(),
                status = GpsStatus.SEARCHING
            )
        )

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val accuracy = if (location.hasAccuracy()) location.accuracy else 999f
                // Penentuan status akurasi sesuai ambang batas Tuning
                val status = if (accuracy <= Tuning.GPS_MIN_ACCURACY_METERS) {
                    GpsStatus.READY
                } else {
                    GpsStatus.WEAK
                }

                trySend(
                    RawLocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = if (location.hasAltitude()) location.altitude else 0.0,
                        accuracyMeters = accuracy,
                        speedMs = if (location.hasSpeed()) location.speed else 0f,
                        timestampEpochMs = location.time,
                        status = status
                    )
                )
            }

            override fun onProviderDisabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    trySend(
                        RawLocationData(
                            latitude = 0.0,
                            longitude = 0.0,
                            altitude = 0.0,
                            accuracyMeters = 0f,
                            speedMs = 0f,
                            timestampEpochMs = System.currentTimeMillis(),
                            status = GpsStatus.DISABLED
                        )
                    )
                }
            }

            override fun onProviderEnabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    trySend(
                        RawLocationData(
                            latitude = 0.0,
                            longitude = 0.0,
                            altitude = 0.0,
                            accuracyMeters = 0f,
                            speedMs = 0f,
                            timestampEpochMs = System.currentTimeMillis(),
                            status = GpsStatus.SEARCHING
                        )
                    )
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeMs,
                minDistanceM,
                listener
            )
        } catch (e: Exception) {
            trySend(
                RawLocationData(
                    latitude = 0.0,
                    longitude = 0.0,
                    altitude = 0.0,
                    accuracyMeters = 0f,
                    speedMs = 0f,
                    timestampEpochMs = System.currentTimeMillis(),
                    status = GpsStatus.DISABLED
                )
            )
            close(e)
            return@callbackFlow
        }

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }
}
