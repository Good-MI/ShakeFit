package com.example.shakefit.model

// === 3.1 HomeUiState ===
data class HomeUiState(
    val greetingName: String?,
    val exercises: List<ExerciseCard>,
    val todayGoals: List<GoalProgress>,
    val streakDays: Int,
    val isLoading: Boolean,
    val error: UiError?
)

data class ExerciseCard(
    val type: ExerciseType,
    val name: String,
    val sensorAvailable: Boolean,
    val unavailableReason: String?
)

data class GoalProgress(
    val goalId: Long,
    val type: ExerciseType,
    val current: Float,
    val target: Float,
    val unit: String,
    val fraction: Float
)

// === 3.2 RepSessionUiState ===
data class RepSessionUiState(
    val exercise: ExerciseType,
    val phase: SessionPhase,
    val mode: SessionMode,
    val countdownRemaining: Int,
    val reps: Int,
    val elapsedSec: Int,
    val remainingSec: Int?,
    val lastRepIntervalMs: Long?,
    val liveQualityScore: Int?,
    val targetReps: Int?,
    val targetReached: Boolean,
    val positionHint: String,
    val summary: SessionSummary?,
    val error: UiError?
)

// === 3.3 RunSessionUiState ===
data class RunSessionUiState(
    val phase: SessionPhase,
    val environment: RunEnvironment,
    val countdownRemaining: Int,
    val elapsedSec: Int,
    val distanceMeters: Float,
    val distanceSource: DistanceSource,
    val currentPaceSecPerKm: Int?,
    val avgPaceSecPerKm: Int?,
    val cadenceSpm: Int?,
    val caloriesEst: Float,
    val splits: List<RunSplit>,
    val gpsStatus: GpsStatus,
    val needsLocationPermission: Boolean,
    val needsNotificationPermission: Boolean,
    val isBackgroundTracking: Boolean,
    val summary: SessionSummary?,
    val error: UiError?
)

data class RunSplit(val kmIndex: Int, val durationSec: Int, val paceSecPerKm: Int)

// === 3.4 SessionSummary ===
data class SessionSummary(
    val sessionId: Long,
    val exercise: ExerciseType,
    val mode: SessionMode,
    val startedAtEpochMs: Long,
    val durationSec: Int,
    val reps: Int?,
    val avgTempoMs: Int?,
    val qualityScore: Int?,
    val distanceMeters: Float?,
    val avgPaceSecPerKm: Int?,
    val bestPaceSecPerKm: Int?,
    val avgCadenceSpm: Int?,
    val distanceSource: DistanceSource?,
    val splits: List<RunSplit>,
    val caloriesEst: Float,
    val note: String,
    val newBadges: List<BadgeUi>,
    val isNewRecord: Boolean
)

// === 3.5 HistoryUiState ===
data class HistoryUiState(
    val filter: ExerciseType?,
    val items: List<SessionSummary>,
    val selected: SessionSummary?,
    val isLoading: Boolean,
    val error: UiError?
)

// === 3.6 GoalsUiState ===
data class GoalsUiState(
    val goals: List<GoalItem>,
    val isLoading: Boolean,
    val error: UiError?
)

data class GoalItem(
    val id: Long,
    val type: ExerciseType,
    val target: Float,
    val unit: String,
    val active: Boolean,
    val progress: Float
)

// === 3.7 StatsUiState ===
data class StatsUiState(
    val totalSessions: Int,
    val totalReps: Int,
    val totalKm: Float,
    val bestRepsByExercise: Map<ExerciseType, Int>,
    val bestPaceSecPerKm: Int?,
    val longestRunMeters: Float?,
    val last7Days: List<DayStat>,
    val streakDays: Int,
    val badges: List<BadgeUi>,
    val isLoading: Boolean,
    val error: UiError?
)

data class DayStat(val dateIso: String, val reps: Int, val km: Float, val sessions: Int)
data class BadgeUi(val code: String, val title: String, val description: String, val unlockedAtEpochMs: Long?)

// === 3.8 ProfileUiState ===
data class ProfileUiState(
    val nickname: String,
    val gender: Gender?,
    val heightCm: Float?,
    val weightKg: Float?,
    val birthDateIso: String?,
    val ageYears: Int?,
    val isProfileComplete: Boolean,
    val fieldErrors: Map<String, String>,
    val saved: Boolean,
    val notificationPermissionNeeded: Boolean,
    val error: UiError?
)

// === 3.9 SettingsUiState ===
data class SettingsUiState(
    val squatSensitivity: Float,
    val pushupSensitivity: Float,
    val vibrationOn: Boolean,
    val soundOn: Boolean,
    val countdownSec: Int,
    val treadmillStepLengthFactor: Float,
    val isLoading: Boolean,
    val error: UiError?
)

// === 3.10 SensorDebugUiState ===
data class SensorDebugUiState(
    val accelAvailable: Boolean,
    val gyroAvailable: Boolean,
    val proximityAvailable: Boolean,
    val accel: Triple<Float, Float, Float>?,
    val gyro: Triple<Float, Float, Float>?,
    val proximityValue: Float?,
    val proximityMaxRange: Float?
)
