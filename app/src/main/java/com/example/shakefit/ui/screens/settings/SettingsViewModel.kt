package com.example.shakefit.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shakefit.data.repository.SettingsRepository
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.SettingsEvent
import com.example.shakefit.model.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * SettingsViewModel mengelola pengaturan aplikasi (sensitivitas, getar, bunyi, countdown).
 * Sesuai kontrak ui-contract.md bagian 3.9 dan 5.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            squatSensitivity = 0.5f,
            pushupSensitivity = 0.5f,
            vibrationOn = true,
            soundOn = true,
            countdownSec = 5,
            treadmillStepLengthFactor = 0.415f,
            isLoading = true,
            error = null
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetSensitivity -> setSensitivity(event.type, event.value)
            is SettingsEvent.SetVibration -> setVibration(event.on)
            is SettingsEvent.SetSound -> setSound(event.on)
            is SettingsEvent.SetCountdown -> setCountdown(event.sec)
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val settings = settingsRepository.getSettings()
                _uiState.value = settings
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = com.example.shakefit.model.UiError("ERR_SETTINGS_LOAD", e.message ?: "Gagal memuat pengaturan")
                    )
                }
            }
        }
    }

    private fun setSensitivity(type: ExerciseType, value: Float) {
        viewModelScope.launch {
            settingsRepository.setSensitivity(type, value)
            if (type == ExerciseType.SQUAT) {
                _uiState.update { it.copy(squatSensitivity = value) }
            } else {
                _uiState.update { it.copy(pushupSensitivity = value) }
            }
        }
    }

    private fun setVibration(on: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibration(on)
            _uiState.update { it.copy(vibrationOn = on) }
        }
    }

    private fun setSound(on: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSound(on)
            _uiState.update { it.copy(soundOn = on) }
        }
    }

    private fun setCountdown(sec: Int) {
        viewModelScope.launch {
            settingsRepository.setCountdown(sec)
            _uiState.update { it.copy(countdownSec = sec) }
        }
    }
}
