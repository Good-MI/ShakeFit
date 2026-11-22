package com.example.shakefit.ui.screens.runsession

import androidx.lifecycle.ViewModel
import com.example.shakefit.engine.running.RunSessionCoordinator
import com.example.shakefit.model.RunSessionEvent
import com.example.shakefit.model.RunSessionUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * RunSessionViewModel: Mesin sesi lari (Outdoor & Treadmill).
 * Sesuai kontrak ui-contract.md bagian 3.3 dan 5.
 *
 * Mengikuti arsitektur F-07: Pengelolaan state dan active tracking didelegasikan
 * ke singleton RunSessionCoordinator di AppContainer, sehingga sesi lari tetap
 * berjalan secara konsisten dan terkoordinasi bersama RunningForegroundService
 * saat aplikasi berpindah layar atau berada di latar belakang.
 */
class RunSessionViewModel(
    private val coordinator: RunSessionCoordinator
) : ViewModel() {

    val uiState: StateFlow<RunSessionUiState> = coordinator.uiState

    fun onEvent(event: RunSessionEvent) {
        coordinator.onEvent(event)
    }
}
