package com.example.shakefit.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shakefit.data.repository.SessionRepository
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.HistoryEvent
import com.example.shakefit.model.HistoryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * HistoryViewModel mengelola riwayat sesi latihan (CRUD).
 * Sesuai kontrak ui-contract.md bagian 3.5 dan 5.
 */
class HistoryViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HistoryUiState(
            filter = null,
            items = emptyList(),
            selected = null,
            isLoading = true,
            error = null
        )
    )
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    /**
     * Memuat ulang riwayat latihan dari SQLite menggunakan filter yang aktif saat ini.
     */
    fun refresh(silent: Boolean = false) {
        loadHistory(silent = silent)
    }

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.SetFilter -> {
                _uiState.update { it.copy(filter = event.type) }
                loadHistory(silent = false)
            }
            is HistoryEvent.Open -> {
                loadSessionDetail(event.sessionId)
            }
            is HistoryEvent.UpdateNote -> {
                updateNote(event.sessionId, event.note)
            }
            is HistoryEvent.Delete -> {
                deleteSession(event.sessionId)
            }
            HistoryEvent.CloseDetail -> {
                _uiState.update { it.copy(selected = null) }
            }
        }
    }

    fun loadHistory(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            try {
                val list = sessionRepository.getHistory(_uiState.value.filter)
                _uiState.update {
                    it.copy(
                        items = list,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = com.example.shakefit.model.UiError("ERR_HIST_LOAD", e.message ?: "Gagal memuat riwayat")
                    )
                }
            }
        }
    }

    private fun loadSessionDetail(sessionId: Long) {
        viewModelScope.launch {
            val detail = sessionRepository.getSessionDetail(sessionId)
            _uiState.update { it.copy(selected = detail) }
        }
    }

    private fun updateNote(sessionId: Long, note: String) {
        viewModelScope.launch {
            sessionRepository.updateNote(sessionId, note)
            // Perbarui state selected dan daftar items
            _uiState.update { state ->
                val updatedSelected = state.selected?.copy(note = note)
                val updatedItems = state.items.map {
                    if (it.sessionId == sessionId) it.copy(note = note) else it
                }
                state.copy(selected = updatedSelected, items = updatedItems)
            }
        }
    }

    private fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
            _uiState.update { state ->
                val updatedItems = state.items.filterNot { it.sessionId == sessionId }
                state.copy(selected = null, items = updatedItems)
            }
        }
    }
}
