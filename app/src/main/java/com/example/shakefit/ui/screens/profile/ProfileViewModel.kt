package com.example.shakefit.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shakefit.data.repository.ProfileRepository
import com.example.shakefit.engine.birthday.BirthdayChecker
import com.example.shakefit.model.Gender
import com.example.shakefit.model.ProfileEvent
import com.example.shakefit.model.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ProfileViewModel mengelola profil pengguna (F-01) dan notifikasi ulang tahun (F-13).
 * Sesuai kontrak ui-contract.md bagian 3.8 dan 5.
 */
class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            nickname = "",
            gender = null,
            heightCm = null,
            weightKg = null,
            birthDateIso = null,
            ageYears = null,
            isProfileComplete = false,
            fieldErrors = emptyMap(),
            saved = false,
            notificationPermissionNeeded = checkNotificationPermissionNeeded(),
            error = null
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.Save -> saveProfile(
                nickname = event.nickname,
                gender = event.gender,
                heightCm = event.heightCm,
                weightKg = event.weightKg,
                birthDateIso = event.birthDateIso
            )
            ProfileEvent.ConsumeSaved -> {
                _uiState.update { it.copy(saved = false) }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val profile = profileRepository.getProfile()
                if (profile != null) {
                    val age = BirthdayChecker.calculateAgeYears(profile.birthDateIso)
                    _uiState.update {
                        it.copy(
                            nickname = profile.nickname ?: "",
                            gender = profile.gender,
                            heightCm = profile.heightCm,
                            weightKg = profile.weightKg,
                            birthDateIso = profile.birthDateIso,
                            ageYears = age,
                            isProfileComplete = profile.isComplete,
                            notificationPermissionNeeded = checkNotificationPermissionNeeded(),
                            fieldErrors = emptyMap(),
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = com.example.shakefit.model.UiError("ERR_PROFILE_LOAD", e.message ?: "Gagal memuat profil")
                    )
                }
            }
        }
    }

    private fun saveProfile(
        nickname: String,
        gender: Gender,
        heightCm: Float,
        weightKg: Float,
        birthDateIso: String
    ) {
        val errors = mutableMapOf<String, String>()

        // 1. Validasi nickname (opsional, maks 30 karakter)
        if (nickname.trim().length > 30) {
            errors["nickname"] = "Nama panggilan maksimal 30 karakter"
        }

        // 2. Validasi tinggi badan (rentang wajar 50 - 250 cm)
        if (heightCm !in 50f..250f) {
            errors["height"] = "Tinggi badan harus antara 50 - 250 cm"
        }

        // 3. Validasi berat badan (rentang wajar 20 - 300 kg)
        if (weightKg !in 20f..300f) {
            errors["weight"] = "Berat badan harus antara 20 - 300 kg"
        }

        // 4. Validasi tanggal lahir (format & tidak di masa depan)
        if (!BirthdayChecker.isBirthDateValid(birthDateIso)) {
            errors["birthDate"] = "Format tanggal lahir salah atau berada di masa depan"
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, saved = false) }
            return
        }

        viewModelScope.launch {
            try {
                profileRepository.saveProfile(
                    nickname = nickname,
                    gender = gender,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    birthDateIso = birthDateIso
                )

                val age = BirthdayChecker.calculateAgeYears(birthDateIso)
                _uiState.update {
                    it.copy(
                        nickname = nickname.trim(),
                        gender = gender,
                        heightCm = heightCm,
                        weightKg = weightKg,
                        birthDateIso = birthDateIso.trim(),
                        ageYears = age,
                        isProfileComplete = true,
                        fieldErrors = emptyMap(),
                        saved = true,
                        notificationPermissionNeeded = checkNotificationPermissionNeeded(),
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        saved = false,
                        error = com.example.shakefit.model.UiError("ERR_PROFILE_SAVE", e.message ?: "Gagal menyimpan profil")
                    )
                }
            }
        }
    }

    private fun checkNotificationPermissionNeeded(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }
}
