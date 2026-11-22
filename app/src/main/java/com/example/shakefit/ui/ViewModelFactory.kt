package com.example.shakefit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.shakefit.di.AppContainer
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.ui.screens.goals.GoalsViewModel
import com.example.shakefit.ui.screens.history.HistoryViewModel
import com.example.shakefit.ui.screens.home.HomeViewModel
import com.example.shakefit.ui.screens.profile.ProfileViewModel
import com.example.shakefit.ui.screens.repsession.RepSessionViewModel
import com.example.shakefit.ui.screens.runsession.RunSessionViewModel
import com.example.shakefit.ui.screens.sensordebug.SensorDebugViewModel
import com.example.shakefit.ui.screens.settings.SettingsViewModel
import com.example.shakefit.ui.screens.stats.StatsViewModel

/**
 * Factory manual untuk inisialisasi ViewModel dengan DI manual (AppContainer).
 * Mengikuti aturan restriction.md tanpa Hilt/Dagger/Koin.
 */
class ViewModelFactory(
    private val appContainer: AppContainer,
    private val initialExercise: ExerciseType = ExerciseType.SQUAT
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    dbHelper = appContainer.dbHelper,
                    sensorDataSource = appContainer.sensorDataSource,
                    goalRepository = appContainer.goalRepository,
                    statsRepository = appContainer.statsRepository
                ) as T
            }
            modelClass.isAssignableFrom(RepSessionViewModel::class.java) -> {
                RepSessionViewModel(
                    initialExercise = initialExercise,
                    sensorDataSource = appContainer.sensorDataSource,
                    sessionRepository = appContainer.sessionRepository,
                    settingsRepository = appContainer.settingsRepository,
                    profileRepository = appContainer.profileRepository,
                    feedbackManager = appContainer.feedbackManager
                ) as T
            }
            modelClass.isAssignableFrom(RunSessionViewModel::class.java) -> {
                RunSessionViewModel(
                    coordinator = appContainer.runSessionCoordinator
                ) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(
                    sessionRepository = appContainer.sessionRepository
                ) as T
            }
            modelClass.isAssignableFrom(GoalsViewModel::class.java) -> {
                GoalsViewModel(
                    goalRepository = appContainer.goalRepository
                ) as T
            }
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> {
                StatsViewModel(
                    statsRepository = appContainer.statsRepository
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    settingsRepository = appContainer.settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(
                    profileRepository = appContainer.profileRepository,
                    context = appContainer.context
                ) as T
            }
            modelClass.isAssignableFrom(SensorDebugViewModel::class.java) -> {
                SensorDebugViewModel(
                    sensorDataSource = appContainer.sensorDataSource,
                    locationDataSource = appContainer.locationDataSource
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
