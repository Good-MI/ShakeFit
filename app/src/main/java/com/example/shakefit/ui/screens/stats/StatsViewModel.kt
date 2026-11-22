package com.example.shakefit.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shakefit.data.repository.StatsRepository
import com.example.shakefit.model.StatsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * StatsViewModel: Menampilkan statistik total, 7 hari terakhir, dan lencana.
 * Sesuai kontrak ui-contract.md bagian 3.7 dan 5 (Read-only, tanpa event).
 */
class StatsViewModel(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StatsUiState(
            totalSessions = 0,
            totalReps = 0,
            totalKm = 0f,
            bestRepsByExercise = emptyMap(),
            bestPaceSecPerKm = null,
            longestRunMeters = null,
            last7Days = emptyList(),
            streakDays = 0,
            badges = emptyList(),
            isLoading = true,
            error = null
        )
    )
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun refresh(silent: Boolean = false) {
        loadStats(silent = silent)
    }

    fun loadStats(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            try {
                val stats = statsRepository.getStats()
                _uiState.value = stats.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = com.example.shakefit.model.UiError("ERR_STATS_LOAD", e.message ?: "Gagal memuat statistik")
                    )
                }
            }
        }
    }
}
