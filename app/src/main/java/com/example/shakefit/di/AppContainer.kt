package com.example.shakefit.di

import android.content.Context
import com.example.shakefit.data.DbHelper
import com.example.shakefit.data.location.LocationDataSource
import com.example.shakefit.data.repository.GoalRepository
import com.example.shakefit.data.repository.ProfileRepository
import com.example.shakefit.data.repository.SessionRepository
import com.example.shakefit.data.repository.SettingsRepository
import com.example.shakefit.data.repository.StatsRepository
import com.example.shakefit.data.sensor.SensorDataSource
import com.example.shakefit.engine.running.RunSessionCoordinator
import com.example.shakefit.util.FeedbackManager

/**
 * Container DI manual sesuai restriction.md (tanpa Hilt/Dagger/Koin).
 * Menyimpan instance DataSource, Helper, dan Repository yang di-share di seluruh aplikasi.
 */
class AppContainer(val context: Context) {

    val dbHelper: DbHelper = DbHelper(context)
    val sensorDataSource: SensorDataSource = SensorDataSource(context)
    val locationDataSource: LocationDataSource = LocationDataSource(context)
    val feedbackManager: FeedbackManager = FeedbackManager(context)

    val sessionRepository: SessionRepository = SessionRepository(dbHelper)
    val goalRepository: GoalRepository = GoalRepository(dbHelper)
    val statsRepository: StatsRepository = StatsRepository(dbHelper)
    val settingsRepository: SettingsRepository = SettingsRepository(dbHelper)
    val profileRepository: ProfileRepository = ProfileRepository(dbHelper)

    val runSessionCoordinator: RunSessionCoordinator = RunSessionCoordinator(
        context = context,
        sensorDataSource = sensorDataSource,
        locationDataSource = locationDataSource,
        sessionRepository = sessionRepository,
        settingsRepository = settingsRepository,
        profileRepository = profileRepository,
        feedbackManager = feedbackManager
    )
}
