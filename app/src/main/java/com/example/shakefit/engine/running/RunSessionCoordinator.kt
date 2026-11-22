package com.example.shakefit.engine.running

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.shakefit.data.location.LocationDataSource
import com.example.shakefit.data.repository.ProfileRepository
import com.example.shakefit.data.repository.SessionRepository
import com.example.shakefit.data.repository.SettingsRepository
import com.example.shakefit.data.sensor.SensorDataSource
import com.example.shakefit.model.DistanceSource
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.GpsStatus
import com.example.shakefit.model.RunEnvironment
import com.example.shakefit.model.RunSessionEvent
import com.example.shakefit.model.RunSessionUiState
import com.example.shakefit.model.SessionMode
import com.example.shakefit.model.SessionPhase
import com.example.shakefit.model.SessionSummary
import com.example.shakefit.service.RunningForegroundService
import com.example.shakefit.util.FeedbackManager
import com.example.shakefit.util.Tuning
import com.example.shakefit.util.formatDistanceKm
import com.example.shakefit.util.formatDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * RunSessionCoordinator: Komponen berlingkup aplikasi (singleton di AppContainer)
 * yang mengoordinasikan loop aktif sesi lari, timer, sensor, lokasi, dan sinkronisasi
 * antara RunningForegroundService dan RunSessionViewModel (F-07).
 */
class RunSessionCoordinator(
    private val context: Context,
    private val sensorDataSource: SensorDataSource,
    private val locationDataSource: LocationDataSource,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val feedbackManager: FeedbackManager = FeedbackManager(context),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private var runTracker: RunTracker = RunTracker(RunEnvironment.OUTDOOR)

    private val _uiState = MutableStateFlow(
        RunSessionUiState(
            phase = SessionPhase.IDLE,
            environment = RunEnvironment.OUTDOOR,
            countdownRemaining = 5,
            elapsedSec = 0,
            distanceMeters = 0f,
            distanceSource = DistanceSource.GPS,
            currentPaceSecPerKm = null,
            avgPaceSecPerKm = null,
            cadenceSpm = null,
            caloriesEst = 0f,
            splits = emptyList(),
            gpsStatus = GpsStatus.OFF,
            needsLocationPermission = false,
            needsNotificationPermission = false,
            isBackgroundTracking = false,
            summary = null,
            error = null
        )
    )
    val uiState: StateFlow<RunSessionUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var timerJob: Job? = null
    private var locationJob: Job? = null
    private var accelJob: Job? = null

    private var startedAtEpochMs: Long = 0L

    fun onEvent(event: RunSessionEvent) {
        when (event) {
            is RunSessionEvent.Start -> startSession(event.environment)
            RunSessionEvent.Pause -> pauseSession()
            RunSessionEvent.Resume -> resumeSession()
            RunSessionEvent.Finish -> finishSession()
            RunSessionEvent.Cancel -> cancelSession()
            is RunSessionEvent.CalibrateTreadmill -> calibrateTreadmill(event.actualDistanceMeters)
            RunSessionEvent.PermissionResultChanged -> checkPermissions()
            RunSessionEvent.DismissSummary -> dismissSummary()
        }
    }

    private fun startSession(environment: RunEnvironment) {
        cancelAllJobs()

        // 1. Cek izin lokasi jika OUTDOOR
        if (environment == RunEnvironment.OUTDOOR && !locationDataSource.hasLocationPermission()) {
            _uiState.update {
                it.copy(
                    needsLocationPermission = true,
                    gpsStatus = GpsStatus.PERMISSION_DENIED
                )
            }
            return
        }

        // 2. Cek izin notifikasi untuk Android 13+ (POST_NOTIFICATIONS)
        val needsNotif = checkNotificationPermissionNeeded()

        scope.launch {
            val settings = settingsRepository.getSettings()
            val countdown = settings.countdownSec

            // Ambil berat & tinggi profil terbaru (fallback ke Tuning jika kosong)
            val profile = profileRepository.getProfile()
            val userWeight = profile?.weightKg ?: Tuning.DEFAULT_WEIGHT_KG
            val userHeight = profile?.heightCm ?: Tuning.DEFAULT_HEIGHT_CM

            runTracker = RunTracker(
                environment = environment,
                userWeightKg = userWeight,
                userHeightCm = userHeight,
                treadmillStepLengthFactor = settings.treadmillStepLengthFactor
            )

            _uiState.update {
                it.copy(
                    phase = SessionPhase.COUNTDOWN,
                    environment = environment,
                    countdownRemaining = countdown,
                    elapsedSec = 0,
                    distanceMeters = 0f,
                    distanceSource = if (environment == RunEnvironment.OUTDOOR) DistanceSource.GPS else DistanceSource.STEPS_ESTIMATE,
                    currentPaceSecPerKm = null,
                    avgPaceSecPerKm = null,
                    cadenceSpm = null,
                    caloriesEst = 0f,
                    splits = emptyList(),
                    gpsStatus = if (environment == RunEnvironment.OUTDOOR) GpsStatus.SEARCHING else GpsStatus.OFF,
                    needsLocationPermission = false,
                    needsNotificationPermission = needsNotif,
                    isBackgroundTracking = false,
                    summary = null,
                    error = null
                )
            }

            countdownJob = launch {
                for (sec in countdown downTo 1) {
                    _uiState.update { it.copy(countdownRemaining = sec) }
                    feedbackManager.onCountdownTick(settings.soundOn, settings.vibrationOn)
                    delay(1000L)
                }
                feedbackManager.onCountdownGo(settings.soundOn, settings.vibrationOn)
                transitionToActive()
            }
        }
    }

    private fun transitionToActive() {
        startedAtEpochMs = System.currentTimeMillis()

        // Mulai Foreground Service bertipe location saat app masuk ACTIVE
        try {
            RunningForegroundService.startService(context)
        } catch (_: Exception) {
            // Safety handling
        }

        _uiState.update {
            it.copy(
                phase = SessionPhase.ACTIVE,
                isBackgroundTracking = true
            )
        }

        startTimer()
        startTracking()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000L)
                val newElapsed = _uiState.value.elapsedSec + 1
                runTracker.onTick(newElapsed)
                val snapshot = runTracker.getSnapshot()

                _uiState.update { state ->
                    state.copy(
                        elapsedSec = newElapsed,
                        distanceMeters = snapshot.distanceMeters,
                        currentPaceSecPerKm = snapshot.currentPaceSecPerKm,
                        avgPaceSecPerKm = snapshot.avgPaceSecPerKm,
                        cadenceSpm = snapshot.cadenceSpm,
                        caloriesEst = snapshot.caloriesEst,
                        splits = snapshot.splits
                    )
                }

                // Update notifikasi persisten foreground service
                RunningForegroundService.updateNotification(
                    context = context,
                    durationStr = formatDuration(newElapsed),
                    distanceStr = formatDistanceKm(snapshot.distanceMeters),
                    isPaused = false
                )
            }
        }
    }

    private fun startTracking() {
        stopTracking()
        val env = _uiState.value.environment

        // Akselerometer untuk kedua mode
        accelJob = scope.launch {
            sensorDataSource.getAccelerometerFlow()
                .catch { }
                .collect { (x, y, z) ->
                    val stepDetected = runTracker.onAccelerometer(x, y, z, System.currentTimeMillis())
                    if (stepDetected && env == RunEnvironment.TREADMILL) {
                        updateUiFromSnapshot()
                    }
                }
        }

        // GPS khusus OUTDOOR
        if (env == RunEnvironment.OUTDOOR) {
            locationJob = scope.launch {
                locationDataSource.getLocationFlow()
                    .catch {
                        _uiState.update { it.copy(gpsStatus = GpsStatus.DISABLED) }
                    }
                    .collect { rawLoc ->
                        _uiState.update { it.copy(gpsStatus = rawLoc.status) }
                        if (rawLoc.status == GpsStatus.READY || rawLoc.status == GpsStatus.WEAK) {
                            val accepted = runTracker.onGpsLocation(
                                GpsPoint(
                                    latitude = rawLoc.latitude,
                                    longitude = rawLoc.longitude,
                                    accuracyMeters = rawLoc.accuracyMeters,
                                    timestampMs = rawLoc.timestampEpochMs,
                                    speedMs = rawLoc.speedMs
                                )
                            )
                            if (accepted) {
                                updateUiFromSnapshot()
                            }
                        }
                    }
            }
        }
    }

    private fun updateUiFromSnapshot() {
        val snap = runTracker.getSnapshot()
        _uiState.update {
            it.copy(
                distanceMeters = snap.distanceMeters,
                currentPaceSecPerKm = snap.currentPaceSecPerKm,
                avgPaceSecPerKm = snap.avgPaceSecPerKm,
                cadenceSpm = snap.cadenceSpm,
                caloriesEst = snap.caloriesEst,
                splits = snap.splits
            )
        }
    }

    private fun stopTracking() {
        locationJob?.cancel()
        locationJob = null
        accelJob?.cancel()
        accelJob = null
    }

    private fun pauseSession() {
        timerJob?.cancel()
        timerJob = null
        stopTracking() // Wajib lepas listener sensor & lokasi saat PAUSED

        _uiState.update { it.copy(phase = SessionPhase.PAUSED) }

        // Update notifikasi menunjukkan status DIJEDA
        RunningForegroundService.updateNotification(
            context = context,
            durationStr = formatDuration(_uiState.value.elapsedSec),
            distanceStr = formatDistanceKm(_uiState.value.distanceMeters),
            isPaused = true
        )
    }

    private fun resumeSession() {
        _uiState.update { it.copy(phase = SessionPhase.ACTIVE) }
        startTimer()
        startTracking()
    }

    private fun finishSession() {
        cancelAllJobs()
        RunningForegroundService.stopService(context)

        val snap = runTracker.getSnapshot()

        val summary = SessionSummary(
            sessionId = 0L,
            exercise = ExerciseType.RUNNING,
            mode = SessionMode.FREE,
            startedAtEpochMs = startedAtEpochMs,
            durationSec = snap.elapsedSec,
            reps = null,
            avgTempoMs = null,
            qualityScore = null,
            distanceMeters = snap.distanceMeters,
            avgPaceSecPerKm = snap.avgPaceSecPerKm,
            bestPaceSecPerKm = snap.bestPaceSecPerKm,
            avgCadenceSpm = snap.cadenceSpm,
            distanceSource = snap.distanceSource,
            splits = snap.splits,
            caloriesEst = snap.caloriesEst,
            note = "",
            newBadges = emptyList(),
            isNewRecord = false
        )

        scope.launch {
            val settings = settingsRepository.getSettings()
            feedbackManager.onTargetOrFinished(settings.soundOn, settings.vibrationOn)

            val finalSummary = sessionRepository.saveSession(summary)

            _uiState.update {
                it.copy(
                    phase = SessionPhase.FINISHED,
                    isBackgroundTracking = false,
                    summary = finalSummary
                )
            }
        }
    }

    private fun cancelSession() {
        cancelAllJobs()
        RunningForegroundService.stopService(context)

        _uiState.update {
            it.copy(
                phase = SessionPhase.IDLE,
                distanceMeters = 0f,
                elapsedSec = 0,
                isBackgroundTracking = false,
                summary = null
            )
        }
    }

    private fun calibrateTreadmill(actualDistanceMeters: Float) {
        val totalSteps = runTracker.getSnapshot().totalSteps
        scope.launch {
            val profile = profileRepository.getProfile()
            val height = profile?.heightCm ?: Tuning.DEFAULT_HEIGHT_CM
            val newFactor = TreadmillCalibrator.calculateCalibratedStepFactor(
                actualDistanceMeters = actualDistanceMeters,
                totalSteps = totalSteps,
                userHeightCm = height
            )
            settingsRepository.setTreadmillStepLengthFactor(newFactor)
        }
    }

    fun checkPermissions() {
        val hasLoc = locationDataSource.hasLocationPermission()
        val needsNotif = checkNotificationPermissionNeeded()
        _uiState.update {
            it.copy(
                needsLocationPermission = !hasLoc,
                needsNotificationPermission = needsNotif,
                gpsStatus = if (hasLoc) GpsStatus.SEARCHING else GpsStatus.PERMISSION_DENIED
            )
        }
    }

    private fun checkNotificationPermissionNeeded(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    private fun dismissSummary() {
        _uiState.update {
            it.copy(
                phase = SessionPhase.IDLE,
                summary = null
            )
        }
    }

    private fun cancelAllJobs() {
        countdownJob?.cancel()
        countdownJob = null
        timerJob?.cancel()
        timerJob = null
        stopTracking()
    }
}
