package com.example.shakefit.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * SensorDataSource membungkus Android SensorManager.
 * Membaca data mentah Accelerometer, Gyroscope, dan Proximity.
 * Menggunakan callbackFlow: listener otomatis didaftarkan saat collect
 * dan WAJIB dilepas saat flow dibatalkan/selesai (hemat baterai).
 */
class SensorDataSource(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val proximity: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    fun hasAccelerometer(): Boolean = accelerometer != null
    fun hasGyroscope(): Boolean = gyroscope != null
    fun hasProximity(): Boolean = proximity != null

    fun getProximityMaxRange(): Float? = proximity?.maximumRange

    /**
     * Membaca akselerometer mentah (x, y, z dalam m/s²).
     * Listener langsung dilepas saat koleksi dibatalkan.
     */
    fun getAccelerometerFlow(samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_UI): Flow<Triple<Float, Float, Float>> = callbackFlow {
        if (accelerometer == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.values.size >= 3) {
                        trySend(Triple(it.values[0], it.values[1], it.values[2]))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, samplingPeriodUs)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /**
     * Membaca giroskop mentah (rad/s pada sumbu x, y, z).
     * Listener langsung dilepas saat koleksi dibatalkan.
     */
    fun getGyroscopeFlow(samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_UI): Flow<Triple<Float, Float, Float>> = callbackFlow {
        if (gyroscope == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.values.size >= 3) {
                        trySend(Triple(it.values[0], it.values[1], it.values[2]))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, gyroscope, samplingPeriodUs)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /**
     * Membaca sensor proximity mentah (jarak dalam cm).
     * Listener langsung dilepas saat koleksi dibatalkan.
     */
    fun getProximityFlow(samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_UI): Flow<Float> = callbackFlow {
        if (proximity == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.values.isNotEmpty()) {
                        trySend(it.values[0])
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, proximity, samplingPeriodUs)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
