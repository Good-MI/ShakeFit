package com.example.shakefit.ui.screens.repsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shakefit.data.repository.ProfileRepository
import com.example.shakefit.data.repository.SessionRepository
import com.example.shakefit.data.repository.SettingsRepository
import com.example.shakefit.data.sensor.SensorDataSource
import com.example.shakefit.engine.calculator.MovementQualityCalculator
import com.example.shakefit.engine.detector.PushupRepDetector
import com.example.shakefit.engine.detector.SquatRepDetector
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.RepSessionEvent
import com.example.shakefit.model.RepSessionUiState
import com.example.shakefit.model.SessionMode
import com.example.shakefit.model.SessionPhase
import com.example.shakefit.model.SessionSummary
import com.example.shakefit.util.FeedbackManager
import com.example.shakefit.util.Tuning
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * RepSessionViewModel: Mesin sesi latihan Squat dan Push-up.
 * Sesuai kontrak ui-contract.md bagian 3.2 dan 5.
 * Mengintegrasikan skor kualitas gerakan (F-12a), feedback getar/bunyi (F-12b),
 * mode tantangan 60 detik (F-12d), dan estimasi kalori berbasis profil (F-12e).
 */
class RepSessionViewModel(
    initialExercise: ExerciseType,
    private val sensorDataSource: SensorDataSource,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val feedbackManager: FeedbackManager
) : ViewModel() {

    private val squatDetector = SquatRepDetector()
    private val pushupDetector = PushupRepDetector()

    private val _uiState = MutableStateFlow(
        RepSessionUiState(
            exercise = initialExercise,
            phase = SessionPhase.IDLE,
            mode = SessionMode.FREE,
            countdownRemaining = 5,
            reps = 0,
            elapsedSec = 0,
            remainingSec = null,
            lastRepIntervalMs = null,
            liveQualityScore = null,
            targetReps = null,
            targetReached = false,
            positionHint = getPositionHint(initialExercise),
            summary = null,
            error = null
        )
    )
    val uiState: StateFlow<RepSessionUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var timerJob: Job? = null
    private var sensorJob: Job? = null
    private var gyroJob: Job? = null

    private var startedAtEpochMs: Long = 0L
    private val repIntervals = mutableListOf<Long>()

    fun onEvent(event: RepSessionEvent) {
        when (event) {
            is RepSessionEvent.Start -> startSession(event.exercise, event.mode)
            RepSessionEvent.Pause -> pauseSession()
            RepSessionEvent.Resume -> resumeSession()
            RepSessionEvent.Finish -> finishSession()
            RepSessionEvent.Cancel -> cancelSession()
            RepSessionEvent.DismissSummary -> dismissSummary()
        }
    }

    private fun startSession(exercise: ExerciseType, mode: SessionMode) {
        cancelAllJobs()
        val posHint = getPositionHint(exercise)

        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            val countdown = settings.countdownSec
            squatDetector.setSensitivity(settings.squatSensitivity)
            pushupDetector.setSensitivity(settings.pushupSensitivity)

            squatDetector.reset()
            pushupDetector.reset()
            repIntervals.clear()

            _uiState.update {
                it.copy(
                    exercise = exercise,
                    mode = mode,
                    phase = SessionPhase.COUNTDOWN,
                    countdownRemaining = countdown,
                    reps = 0,
                    elapsedSec = 0,
                    remainingSec = if (mode == SessionMode.CHALLENGE_60S) 60 else null,
                    lastRepIntervalMs = null,
                    liveQualityScore = null,
                    targetReached = false,
                    positionHint = posHint,
                    summary = null,
                    error = null
                )
            }

            // Jalankan countdown
            countdownJob = launch {
                for (sec in countdown downTo 1) {
                    _uiState.update { it.copy(countdownRemaining = sec) }
                    feedbackManager.onCountdownTick(settings.soundOn, settings.vibrationOn)
                    delay(1000L)
                }
                feedbackManager.onCountdownGo(settings.soundOn, settings.vibrationOn)
                // Masuk ke fase ACTIVE
                transitionToActive()
            }
        }
    }

    private fun transitionToActive() {
        startedAtEpochMs = System.currentTimeMillis()
        _uiState.update { it.copy(phase = SessionPhase.ACTIVE) }

        startTimer()
        startSensorMonitoring()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val currentState = _uiState.value
                val newElapsed = currentState.elapsedSec + 1
                val newRemaining = if (currentState.mode == SessionMode.CHALLENGE_60S) {
                    (60 - newElapsed).coerceAtLeast(0)
                } else null

                _uiState.update { state ->
                    state.copy(elapsedSec = newElapsed, remainingSec = newRemaining)
                }

                // Auto finish jika waktu 60 detik habis (F-12d)
                if (currentState.mode == SessionMode.CHALLENGE_60S && newRemaining == 0) {
                    finishSession()
                    break
                }
            }
        }
    }

    private fun startSensorMonitoring() {
        stopSensorMonitoring()
        val exercise = _uiState.value.exercise

        if (exercise == ExerciseType.SQUAT) {
            // Monitor Accelerometer
            sensorJob = viewModelScope.launch {
                sensorDataSource.getAccelerometerFlow()
                    .catch { }
                    .collect { (x, y, z) ->
                        val result = squatDetector.processAccelerometer(x, y, z, System.currentTimeMillis())
                        if (result != null) {
                            handleRepCounted(result.repNumber, result.intervalMs)
                        }
                    }
            }
            // Monitor Gyroscope (opsional validator)
            if (sensorDataSource.hasGyroscope()) {
                gyroJob = viewModelScope.launch {
                    sensorDataSource.getGyroscopeFlow()
                        .catch { }
                        .collect { (gx, gy, gz) ->
                            squatDetector.processGyroscope(gx, gy, gz, System.currentTimeMillis())
                        }
                }
            }
        } else if (exercise == ExerciseType.PUSHUP) {
            val maxRange = sensorDataSource.getProximityMaxRange() ?: 5.0f

            // Monitor Proximity
            sensorJob = viewModelScope.launch {
                sensorDataSource.getProximityFlow()
                    .catch { }
                    .collect { dist ->
                        val result = pushupDetector.processProximity(dist, maxRange, System.currentTimeMillis())
                        if (result != null) {
                            handleRepCounted(result.repNumber, result.intervalMs)
                        }
                    }
            }
            // Monitor Accelerometer untuk kestabilan lantai
            gyroJob = viewModelScope.launch {
                sensorDataSource.getAccelerometerFlow()
                    .catch { }
                    .collect { (x, y, z) ->
                        pushupDetector.processAccelerometer(x, y, z, System.currentTimeMillis())
                    }
            }
        }
    }

    private fun stopSensorMonitoring() {
        sensorJob?.cancel()
        sensorJob = null
        gyroJob?.cancel()
        gyroJob = null
    }

    private fun handleRepCounted(repNum: Int, intervalMs: Long) {
        repIntervals.add(intervalMs)

        // Hitung skor kualitas gerakan dari ritme interval antar repetisi (F-12a)
        val qualityScore = MovementQualityCalculator.calculateScore(repIntervals)

        var newlyReachedTarget = false
        _uiState.update { state ->
            val targetReached = state.targetReps != null && repNum >= state.targetReps
            if (targetReached && !state.targetReached) {
                newlyReachedTarget = true
            }
            state.copy(
                reps = repNum,
                lastRepIntervalMs = intervalMs,
                liveQualityScore = qualityScore,
                targetReached = targetReached
            )
        }

        // Feedback getar dan bunyi (F-12b)
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            if (newlyReachedTarget) {
                feedbackManager.onTargetOrFinished(settings.soundOn, settings.vibrationOn)
            } else {
                feedbackManager.onRepCounted(settings.soundOn, settings.vibrationOn)
            }
        }
    }

    private fun pauseSession() {
        timerJob?.cancel()
        timerJob = null
        stopSensorMonitoring() // Wajib lepas listener sensor saat PAUSED
        _uiState.update { it.copy(phase = SessionPhase.PAUSED) }
    }

    private fun resumeSession() {
        _uiState.update { it.copy(phase = SessionPhase.ACTIVE) }
        startTimer()
        startSensorMonitoring()
    }

    private fun finishSession() {
        cancelAllJobs()
        val currentState = _uiState.value

        val avgTempo = if (repIntervals.isNotEmpty()) repIntervals.average().toInt() else null

        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            feedbackManager.onTargetOrFinished(settings.soundOn, settings.vibrationOn)

            // Hitung estimasi kalori dengan berat badan pengguna dari ProfileRepository (F-12e)
            val profile = profileRepository.getProfile()
            val userWeight = profile?.weightKg ?: Tuning.DEFAULT_WEIGHT_KG
            val caloriesPerRep = if (currentState.exercise == ExerciseType.SQUAT) {
                Tuning.CALORIES_PER_SQUAT_REP
            } else {
                Tuning.CALORIES_PER_PUSHUP_REP
            }
            val caloriesEst = currentState.reps * caloriesPerRep * (userWeight / Tuning.DEFAULT_WEIGHT_KG)

            val summary = SessionSummary(
                sessionId = 0L,
                exercise = currentState.exercise,
                mode = currentState.mode,
                startedAtEpochMs = startedAtEpochMs,
                durationSec = currentState.elapsedSec,
                reps = currentState.reps,
                avgTempoMs = avgTempo,
                qualityScore = currentState.liveQualityScore,
                distanceMeters = null,
                avgPaceSecPerKm = null,
                bestPaceSecPerKm = null,
                avgCadenceSpm = null,
                distanceSource = null,
                splits = emptyList(),
                caloriesEst = caloriesEst,
                note = "",
                newBadges = emptyList(),
                isNewRecord = false
            )

            val finalSummary = sessionRepository.saveSession(summary)

            _uiState.update {
                it.copy(
                    phase = SessionPhase.FINISHED,
                    summary = finalSummary
                )
            }
        }
    }

    private fun cancelSession() {
        cancelAllJobs()
        _uiState.update {
            it.copy(
                phase = SessionPhase.IDLE,
                reps = 0,
                elapsedSec = 0,
                summary = null
            )
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
        stopSensorMonitoring()
    }

    override fun onCleared() {
        super.onCleared()
        cancelAllJobs()
    }

    companion object {
        fun getPositionHint(exercise: ExerciseType): String = when (exercise) {
            ExerciseType.SQUAT -> "Saku depan celana, HP tegak, layar menghadap badan. Tidak dipegang"
            ExerciseType.PUSHUP -> "HP rata di lantai, layar menghadap atas, bagian atas HP (dekat sensor proximity) tepat di bawah dada"
            ExerciseType.RUNNING -> "Saku depan celana atau armband. Tidak digenggam"
        }
    }
}
