package com.example.shakefit.model

sealed interface HomeEvent {
    data class SelectExercise(val type: ExerciseType) : HomeEvent
    object Refresh : HomeEvent
    object SilentRefresh : HomeEvent
}

sealed interface RepSessionEvent {
    data class Start(val exercise: ExerciseType, val mode: SessionMode) : RepSessionEvent
    object Pause : RepSessionEvent
    object Resume : RepSessionEvent
    object Finish : RepSessionEvent
    object Cancel : RepSessionEvent
    object DismissSummary : RepSessionEvent
}

sealed interface RunSessionEvent {
    data class Start(val environment: RunEnvironment) : RunSessionEvent
    object Pause : RunSessionEvent
    object Resume : RunSessionEvent
    object Finish : RunSessionEvent
    object Cancel : RunSessionEvent
    data class CalibrateTreadmill(val actualDistanceMeters: Float) : RunSessionEvent
    object PermissionResultChanged : RunSessionEvent
    object DismissSummary : RunSessionEvent
}

sealed interface HistoryEvent {
    data class SetFilter(val type: ExerciseType?) : HistoryEvent
    data class Open(val sessionId: Long) : HistoryEvent
    data class UpdateNote(val sessionId: Long, val note: String) : HistoryEvent
    data class Delete(val sessionId: Long) : HistoryEvent
    object CloseDetail : HistoryEvent
}

sealed interface GoalsEvent {
    data class Add(val type: ExerciseType, val target: Float) : GoalsEvent
    data class Update(val id: Long, val target: Float) : GoalsEvent
    data class SetActive(val id: Long, val active: Boolean) : GoalsEvent
    data class Delete(val id: Long) : GoalsEvent
}

sealed interface ProfileEvent {
    data class Save(
        val nickname: String,
        val gender: Gender,
        val heightCm: Float,
        val weightKg: Float,
        val birthDateIso: String
    ) : ProfileEvent
    object ConsumeSaved : ProfileEvent
}

sealed interface SettingsEvent {
    data class SetSensitivity(val type: ExerciseType, val value: Float) : SettingsEvent
    data class SetVibration(val on: Boolean) : SettingsEvent
    data class SetSound(val on: Boolean) : SettingsEvent
    data class SetCountdown(val sec: Int) : SettingsEvent
}
