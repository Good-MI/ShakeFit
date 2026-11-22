package com.example.shakefit.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shakefit.data.repository.GoalRepository
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.GoalsEvent
import com.example.shakefit.model.GoalsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * GoalsViewModel mengelola CRUD target harian.
 * Sesuai kontrak ui-contract.md bagian 3.6 dan 5.
 */
class GoalsViewModel(
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GoalsUiState(
            goals = emptyList(),
            isLoading = true,
            error = null
        )
    )
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        loadGoals()
    }

    fun onEvent(event: GoalsEvent) {
        when (event) {
            is GoalsEvent.Add -> addGoal(event.type, event.target)
            is GoalsEvent.Update -> updateGoal(event.id, event.target)
            is GoalsEvent.SetActive -> setActive(event.id, event.active)
            is GoalsEvent.Delete -> deleteGoal(event.id)
        }
    }

    fun refresh(silent: Boolean = false) {
        loadGoals(silent = silent)
    }

    fun loadGoals(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            try {
                val list = goalRepository.getGoals()
                _uiState.update {
                    it.copy(goals = list, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = com.example.shakefit.model.UiError("ERR_GOALS_LOAD", e.message ?: "Gagal memuat target")
                    )
                }
            }
        }
    }

    private fun addGoal(type: ExerciseType, target: Float) {
        viewModelScope.launch {
            goalRepository.addGoal(type, target)
            loadGoals()
        }
    }

    private fun updateGoal(id: Long, target: Float) {
        viewModelScope.launch {
            goalRepository.updateGoal(id, target)
            loadGoals()
        }
    }

    private fun setActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            goalRepository.setActive(id, active)
            loadGoals()
        }
    }

    private fun deleteGoal(id: Long) {
        viewModelScope.launch {
            goalRepository.deleteGoal(id)
            loadGoals()
        }
    }
}
