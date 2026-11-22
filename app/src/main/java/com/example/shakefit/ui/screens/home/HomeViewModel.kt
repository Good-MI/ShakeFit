package com.example.shakefit.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shakefit.data.DbHelper
import com.example.shakefit.data.repository.GoalRepository
import com.example.shakefit.data.repository.StatsRepository
import com.example.shakefit.data.sensor.SensorDataSource
import com.example.shakefit.model.ExerciseCard
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.HomeEvent
import com.example.shakefit.model.HomeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * HomeViewModel mengelola UI State untuk layar Beranda sesuai kontrak ui-contract.md.
 */
class HomeViewModel(
    private val dbHelper: DbHelper,
    private val sensorDataSource: SensorDataSource,
    private val goalRepository: GoalRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            greetingName = null,
            exercises = emptyList(),
            todayGoals = emptyList(),
            streakDays = 0,
            isLoading = true,
            error = null
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SelectExercise -> {
                // Navigasi di-handle oleh Composable wrapper / NavHost
            }
            HomeEvent.Refresh -> {
                loadHomeData(silent = false)
            }
            HomeEvent.SilentRefresh -> {
                loadHomeData(silent = true)
            }
        }
    }

    private fun loadHomeData(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            try {
                // 1. Ambil greetingName dari user_profile
                val nickname = withContext(Dispatchers.IO) {
                    val db = dbHelper.readableDatabase
                    var name: String? = null
                    val cursor = db.rawQuery("SELECT nickname FROM user_profile WHERE id = 1", null)
                    cursor.use {
                        if (it.moveToNext() && !it.isNull(0)) {
                            name = it.getString(0)
                        }
                    }
                    name
                }

                // 2. Cek ketersediaan sensor untuk setiap latihan (F-02)
                val hasAccel = sensorDataSource.hasAccelerometer()
                val hasProximity = sensorDataSource.hasProximity()

                val exerciseCards = listOf(
                    ExerciseCard(
                        type = ExerciseType.SQUAT,
                        name = "Squat",
                        sensorAvailable = hasAccel,
                        unavailableReason = if (!hasAccel) "Sensor Accelerometer tidak tersedia di HP ini" else null
                    ),
                    ExerciseCard(
                        type = ExerciseType.PUSHUP,
                        name = "Push-up",
                        sensorAvailable = hasProximity,
                        unavailableReason = if (!hasProximity) "Sensor Proximity tidak tersedia di HP ini" else null
                    ),
                    ExerciseCard(
                        type = ExerciseType.RUNNING,
                        name = "Lari",
                        sensorAvailable = hasAccel,
                        unavailableReason = if (!hasAccel) "Sensor Accelerometer tidak tersedia di HP ini" else null
                    )
                )

                // 3. Ambil target hari ini
                val todayGoals = goalRepository.getTodayGoalProgress()

                // 4. Ambil streak
                val streak = statsRepository.getStreakDays()

                _uiState.update {
                    it.copy(
                        greetingName = nickname,
                        exercises = exerciseCards,
                        todayGoals = todayGoals,
                        streakDays = streak,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = com.example.shakefit.model.UiError("ERR_HOME_LOAD", e.message ?: "Gagal memuat data")
                    )
                }
            }
        }
    }
}
