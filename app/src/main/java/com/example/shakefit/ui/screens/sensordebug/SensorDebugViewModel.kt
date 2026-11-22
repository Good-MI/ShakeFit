package com.example.shakefit.ui.screens.sensordebug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shakefit.data.location.LocationDataSource
import com.example.shakefit.data.location.RawLocationData
import com.example.shakefit.data.sensor.SensorDataSource
import com.example.shakefit.model.GpsStatus
import com.example.shakefit.model.SensorDebugUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State pengujian GPS untuk layar debug.
 * Dibuat terpisah agar kontrak SensorDebugUiState tidak berubah.
 */
data class GpsDebugUiState(
    val isTesting: Boolean = false,
    val hasPermission: Boolean = false,
    val isGpsEnabled: Boolean = false,
    val status: GpsStatus = GpsStatus.OFF,
    val rawData: RawLocationData? = null,
    val message: String? = null
)

/**
 * ViewModel untuk layar uji sensor mentah & GPS.
 * Meng-expose uiState persis sesuai kontrak ui-contract.md bagian 3.10 & 5.
 */
class SensorDebugViewModel(
    private val sensorDataSource: SensorDataSource,
    private val locationDataSource: LocationDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SensorDebugUiState(
            accelAvailable = sensorDataSource.hasAccelerometer(),
            gyroAvailable = sensorDataSource.hasGyroscope(),
            proximityAvailable = sensorDataSource.hasProximity(),
            accel = null,
            gyro = null,
            proximityValue = null,
            proximityMaxRange = sensorDataSource.getProximityMaxRange()
        )
    )
    val uiState: StateFlow<SensorDebugUiState> = _uiState.asStateFlow()

    private val _gpsDebugState = MutableStateFlow(
        GpsDebugUiState(
            hasPermission = locationDataSource.hasLocationPermission(),
            isGpsEnabled = locationDataSource.isGpsEnabled(),
            status = GpsStatus.OFF
        )
    )
    val gpsDebugState: StateFlow<GpsDebugUiState> = _gpsDebugState.asStateFlow()

    private var accelJob: Job? = null
    private var gyroJob: Job? = null
    private var proximityJob: Job? = null
    private var gpsJob: Job? = null

    init {
        startSensorMonitoring()
    }

    fun startSensorMonitoring() {
        if (accelJob == null && sensorDataSource.hasAccelerometer()) {
            accelJob = viewModelScope.launch {
                sensorDataSource.getAccelerometerFlow()
                    .catch { }
                    .collect { values ->
                        _uiState.update { it.copy(accel = values) }
                    }
            }
        }

        if (gyroJob == null && sensorDataSource.hasGyroscope()) {
            gyroJob = viewModelScope.launch {
                sensorDataSource.getGyroscopeFlow()
                    .catch { }
                    .collect { values ->
                        _uiState.update { it.copy(gyro = values) }
                    }
            }
        }

        if (proximityJob == null && sensorDataSource.hasProximity()) {
            proximityJob = viewModelScope.launch {
                sensorDataSource.getProximityFlow()
                    .catch { }
                    .collect { value ->
                        _uiState.update { it.copy(proximityValue = value) }
                    }
            }
        }
    }

    fun stopSensorMonitoring() {
        accelJob?.cancel()
        accelJob = null
        gyroJob?.cancel()
        gyroJob = null
        proximityJob?.cancel()
        proximityJob = null
    }

    fun onPermissionResult(isGranted: Boolean) {
        val gpsEnabled = locationDataSource.isGpsEnabled()
        _gpsDebugState.update {
            it.copy(
                hasPermission = isGranted,
                isGpsEnabled = gpsEnabled,
                status = if (!isGranted) GpsStatus.PERMISSION_DENIED else if (!gpsEnabled) GpsStatus.DISABLED else GpsStatus.SEARCHING
            )
        }
        if (isGranted && _gpsDebugState.value.isTesting) {
            startGpsTest()
        }
    }

    fun startGpsTest() {
        gpsJob?.cancel()
        val hasPerm = locationDataSource.hasLocationPermission()
        val gpsEnabled = locationDataSource.isGpsEnabled()

        if (!hasPerm) {
            _gpsDebugState.update {
                it.copy(
                    isTesting = true,
                    hasPermission = false,
                    status = GpsStatus.PERMISSION_DENIED,
                    message = "Izin lokasi belum diberikan"
                )
            }
            return
        }

        if (!gpsEnabled) {
            _gpsDebugState.update {
                it.copy(
                    isTesting = true,
                    isGpsEnabled = false,
                    status = GpsStatus.DISABLED,
                    message = "GPS/Lokasi HP dalam keadaan nonaktif"
                )
            }
            return
        }

        _gpsDebugState.update {
            it.copy(
                isTesting = true,
                hasPermission = true,
                isGpsEnabled = true,
                status = GpsStatus.SEARCHING,
                message = "Mencari sinyal GPS..."
            )
        }

        gpsJob = viewModelScope.launch {
            locationDataSource.getLocationFlow()
                .catch { e ->
                    _gpsDebugState.update {
                        it.copy(
                            status = GpsStatus.DISABLED,
                            message = "Gagal membaca GPS: ${e.message}"
                        )
                    }
                }
                .collect { data ->
                    _gpsDebugState.update {
                        it.copy(
                            status = data.status,
                            rawData = data,
                            message = when (data.status) {
                                GpsStatus.READY -> "Sinyal GPS Akurat"
                                GpsStatus.WEAK -> "Sinyal GPS Lemah"
                                GpsStatus.SEARCHING -> "Mencari sinyal GPS..."
                                GpsStatus.DISABLED -> "GPS Nonaktif"
                                GpsStatus.PERMISSION_DENIED -> "Izin Lokasi Ditolak"
                                GpsStatus.OFF -> "GPS Mati"
                            }
                        )
                    }
                }
        }
    }

    fun stopGpsTest() {
        gpsJob?.cancel()
        gpsJob = null
        _gpsDebugState.update {
            it.copy(
                isTesting = false,
                status = GpsStatus.OFF,
                rawData = null,
                message = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSensorMonitoring()
        stopGpsTest()
    }
}
