package com.example.shakefit.model

/**
 * Model data profil pengguna (tabel user_profile).
 * Satu profil per instalasi (id = 1).
 */
data class UserProfile(
    val id: Int = 1,
    val nickname: String?,
    val gender: Gender?,
    val heightCm: Float?,
    val weightKg: Float?,
    val birthDateIso: String?, // yyyy-MM-dd
    val lastBirthdayNotifiedYear: Int?
) {
    val isComplete: Boolean
        get() = gender != null && heightCm != null && weightKg != null && !birthDateIso.isNullOrBlank()
}
