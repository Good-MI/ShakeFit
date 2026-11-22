# UI CONTRACT — ShakeFit (Kontrak Backend <-> UI)

> Versi 0.2 — 20 Nov 2026 (posisi HP terkunci; tidak ada perubahan struktur UiState/Event dari 0.1). Jembatan antara backend (Claude/Antigravity) dan UI (Gemini).
> UI hanya membaca `StateFlow<UiState>` dan mengirim `Event`. UI tidak menyentuh database, sensor, atau lokasi.
> Perubahan kontrak harus disetujui user karena dua pihak bergantung padanya.
> Nama package: placeholder `com.example.shakefit`. Bahasa teks UI: Indonesia (placeholder, belum final).

---

## 1. Tipe Dasar

```kotlin
enum class ExerciseType { SQUAT, PUSHUP, RUNNING }
enum class Gender { MALE, FEMALE }
enum class RunEnvironment { OUTDOOR, TREADMILL }
enum class DistanceSource { GPS, STEPS_ESTIMATE }
enum class SessionPhase { IDLE, COUNTDOWN, ACTIVE, PAUSED, FINISHED }
enum class SessionMode { FREE, CHALLENGE_60S, GOAL }
enum class GpsStatus { OFF, SEARCHING, READY, WEAK, PERMISSION_DENIED, DISABLED }

data class UiError(val code: String, val message: String)   // message sudah teks yang boleh ditampilkan
```

## 2. Aturan Format (util milik backend, dipakai UI)

| Fungsi | Contoh |
|---|---|
| `formatPace(secPerKm: Int?): String` | 345 -> `"5:45 /km"`, null -> `"--:--"` |
| `formatDuration(sec: Int): String` | 3725 -> `"1:02:05"`, 95 -> `"01:35"` |
| `formatDistanceKm(meters: Float): String` | 5230 -> `"5.23 km"` |
| `formatCalories(kcal: Float): String` | `"~120 kkal"` (selalu berawalan "~", karena estimasi) |

Pace dihitung dan disimpan dalam **detik per km** (Int). UI tidak menghitung pace sendiri.

## 3. UiState per Layar

### 3.1 HomeUiState
```kotlin
data class HomeUiState(
    val greetingName: String?,                 // nickname, null jika belum ada
    val exercises: List<ExerciseCard>,
    val todayGoals: List<GoalProgress>,
    val streakDays: Int,
    val isLoading: Boolean,
    val error: UiError?
)
data class ExerciseCard(
    val type: ExerciseType,
    val name: String,
    val sensorAvailable: Boolean,
    val unavailableReason: String?             // terisi jika sensorAvailable=false
)
data class GoalProgress(
    val goalId: Long, val type: ExerciseType,
    val current: Float, val target: Float, val unit: String,   // "REPS" | "KM"
    val fraction: Float                          // 0f..1f
)
```

### 3.2 RepSessionUiState (Squat / Push-up)
```kotlin
data class RepSessionUiState(
    val exercise: ExerciseType,
    val phase: SessionPhase,
    val mode: SessionMode,
    val countdownRemaining: Int,               // detik, valid saat COUNTDOWN
    val reps: Int,
    val elapsedSec: Int,
    val remainingSec: Int?,                    // hanya CHALLENGE_60S
    val lastRepIntervalMs: Long?,
    val liveQualityScore: Int?,                // 0..100, null sebelum cukup data
    val targetReps: Int?,
    val targetReached: Boolean,
    val positionHint: String,                  // petunjuk posisi HP (teks terkunci, lihat bagian 9)
    val summary: SessionSummary?,              // terisi saat FINISHED
    val error: UiError?
)
```

### 3.3 RunSessionUiState (Running)
```kotlin
data class RunSessionUiState(
    val phase: SessionPhase,
    val environment: RunEnvironment,
    val countdownRemaining: Int,
    val elapsedSec: Int,
    val distanceMeters: Float,
    val distanceSource: DistanceSource,        // STEPS_ESTIMATE => UI tampilkan label "perkiraan"
    val currentPaceSecPerKm: Int?,
    val avgPaceSecPerKm: Int?,
    val cadenceSpm: Int?,
    val caloriesEst: Float,
    val splits: List<RunSplit>,
    val gpsStatus: GpsStatus,                  // OFF saat TREADMILL
    val needsLocationPermission: Boolean,
    val needsNotificationPermission: Boolean,
    val isBackgroundTracking: Boolean,
    val summary: SessionSummary?,
    val error: UiError?
)
data class RunSplit(val kmIndex: Int, val durationSec: Int, val paceSecPerKm: Int)
```

### 3.4 SessionSummary (dipakai layar ringkasan dan detail riwayat)
```kotlin
data class SessionSummary(
    val sessionId: Long, val exercise: ExerciseType, val mode: SessionMode,
    val startedAtEpochMs: Long, val durationSec: Int,
    val reps: Int?, val avgTempoMs: Int?, val qualityScore: Int?,
    val distanceMeters: Float?, val avgPaceSecPerKm: Int?, val bestPaceSecPerKm: Int?,
    val avgCadenceSpm: Int?, val distanceSource: DistanceSource?,
    val splits: List<RunSplit>,
    val caloriesEst: Float,
    val note: String,
    val newBadges: List<BadgeUi>,              // badge yang baru terbuka karena sesi ini
    val isNewRecord: Boolean
)
```

### 3.5 HistoryUiState
```kotlin
data class HistoryUiState(
    val filter: ExerciseType?,                 // null = semua
    val items: List<SessionSummary>,
    val selected: SessionSummary?,             // layar detail
    val isLoading: Boolean, val error: UiError?
)
```

### 3.6 GoalsUiState
```kotlin
data class GoalsUiState(
    val goals: List<GoalItem>, val isLoading: Boolean, val error: UiError?
)
data class GoalItem(
    val id: Long, val type: ExerciseType, val target: Float, val unit: String,
    val active: Boolean, val progress: Float
)
```

### 3.7 StatsUiState
```kotlin
data class StatsUiState(
    val totalSessions: Int, val totalReps: Int, val totalKm: Float,
    val bestRepsByExercise: Map<ExerciseType, Int>,
    val bestPaceSecPerKm: Int?, val longestRunMeters: Float?,
    val last7Days: List<DayStat>,              // 7 item, hari tertua dulu
    val streakDays: Int, val badges: List<BadgeUi>,
    val isLoading: Boolean, val error: UiError?
)
data class DayStat(val dateIso: String, val reps: Int, val km: Float, val sessions: Int)
data class BadgeUi(val code: String, val title: String, val description: String, val unlockedAtEpochMs: Long?)
```

### 3.8 ProfileUiState
```kotlin
data class ProfileUiState(
    val nickname: String, val gender: Gender?,
    val heightCm: Float?, val weightKg: Float?, val birthDateIso: String?,   // yyyy-MM-dd
    val ageYears: Int?,
    val isProfileComplete: Boolean,
    val fieldErrors: Map<String, String>,      // key: "height", "weight", "birthDate", "nickname"
    val saved: Boolean,                        // true sesaat setelah simpan sukses
    val notificationPermissionNeeded: Boolean,
    val error: UiError?
)
```

### 3.9 SettingsUiState
```kotlin
data class SettingsUiState(
    val squatSensitivity: Float, val pushupSensitivity: Float,   // 0f..1f
    val vibrationOn: Boolean, val soundOn: Boolean,
    val countdownSec: Int,                      // 3..10
    val treadmillStepLengthFactor: Float,
    val isLoading: Boolean, val error: UiError?
)
```

### 3.10 SensorDebugUiState (layar uji sensor, boleh disembunyikan di rilis)
```kotlin
data class SensorDebugUiState(
    val accelAvailable: Boolean, val gyroAvailable: Boolean, val proximityAvailable: Boolean,
    val accel: Triple<Float, Float, Float>?, val gyro: Triple<Float, Float, Float>?,
    val proximityValue: Float?, val proximityMaxRange: Float?
)
```

## 4. Event (UI -> ViewModel)

```kotlin
sealed interface HomeEvent {
    data class SelectExercise(val type: ExerciseType) : HomeEvent
    object Refresh : HomeEvent
}
sealed interface RepSessionEvent {
    data class Start(val exercise: ExerciseType, val mode: SessionMode) : RepSessionEvent
    object Pause : RepSessionEvent
    object Resume : RepSessionEvent
    object Finish : RepSessionEvent
    object Cancel : RepSessionEvent              // buang sesi, tidak disimpan
    object DismissSummary : RepSessionEvent
}
sealed interface RunSessionEvent {
    data class Start(val environment: RunEnvironment) : RunSessionEvent
    object Pause : RunSessionEvent
    object Resume : RunSessionEvent
    object Finish : RunSessionEvent
    object Cancel : RunSessionEvent
    data class CalibrateTreadmill(val actualDistanceMeters: Float) : RunSessionEvent
    object PermissionResultChanged : RunSessionEvent   // UI memberi tahu hasil dialog izin
    object DismissSummary : RunSessionEvent
}
sealed interface HistoryEvent {
    data class SetFilter(val type: ExerciseType?) : HistoryEvent
    data class Open(val sessionId: Long) : HistoryEvent
    data class UpdateNote(val sessionId: Long, val note: String) : HistoryEvent
    data class Delete(val sessionId: Long) : HistoryEvent
    object CloseDetail : HistoryEvent
}
sealed interface GoalsEvent {
    data class Add(val type: ExerciseType, val target: Float) : GoalsEvent
    data class Update(val id: Long, val target: Float) : GoalsEvent
    data class SetActive(val id: Long, val active: Boolean) : GoalsEvent
    data class Delete(val id: Long) : GoalsEvent
}
sealed interface ProfileEvent {
    data class Save(
        val nickname: String, val gender: Gender, val heightCm: Float,
        val weightKg: Float, val birthDateIso: String
    ) : ProfileEvent
    object ConsumeSaved : ProfileEvent
}
sealed interface SettingsEvent {
    data class SetSensitivity(val type: ExerciseType, val value: Float) : SettingsEvent
    data class SetVibration(val on: Boolean) : SettingsEvent
    data class SetSound(val on: Boolean) : SettingsEvent
    data class SetCountdown(val sec: Int) : SettingsEvent
}
```

## 5. API ViewModel (yang dilihat UI)

| ViewModel | Expose | Terima |
|---|---|---|
| `HomeViewModel` | `uiState: StateFlow<HomeUiState>` | `onEvent(HomeEvent)` |
| `RepSessionViewModel` | `uiState: StateFlow<RepSessionUiState>` | `onEvent(RepSessionEvent)` |
| `RunSessionViewModel` | `uiState: StateFlow<RunSessionUiState>` | `onEvent(RunSessionEvent)` |
| `HistoryViewModel` | `uiState: StateFlow<HistoryUiState>` | `onEvent(HistoryEvent)` |
| `GoalsViewModel` | `uiState: StateFlow<GoalsUiState>` | `onEvent(GoalsEvent)` |
| `StatsViewModel` | `uiState: StateFlow<StatsUiState>` | - |
| `ProfileViewModel` | `uiState: StateFlow<ProfileUiState>` | `onEvent(ProfileEvent)` |
| `SettingsViewModel` | `uiState: StateFlow<SettingsUiState>` | `onEvent(SettingsEvent)` |
| `SensorDebugViewModel` | `uiState: StateFlow<SensorDebugUiState>` | - |

Aturan: Composable layar menerima `(state, onEvent)` sebagai parameter (stateless). ViewModel di-hoist di level navigasi.

## 6. Izin (dikelola UI, hasilnya dilaporkan ke ViewModel)
- UI yang menampilkan dialog izin sistem (location, notifikasi). Backend hanya menandai `needsLocationPermission` / `needsNotificationPermission` di state.
- Setelah hasil dialog, UI mengirim `PermissionResultChanged`.
- Izin diminta saat dibutuhkan (mis. menekan Start running Outdoor), bukan saat app pertama dibuka.

## 7. Notifikasi Ulang Tahun
- Sepenuhnya dikelola backend (teks template lokal, gaya teks panjang; pengecekan harian memakai WorkManager + cek saat app dibuka). UI hanya perlu menyediakan layar Profil dan memberi tahu bila `notificationPermissionNeeded = true`.

## 8. Data Dummy untuk Preview
- Gemini boleh membuat data dummy **hanya di `@Preview`**. Dilarang menaruh data dummy di jalur kode normal.

## 9. Posisi HP (TERKUNCI — disetujui user)
UI wajib menampilkan petunjuk ini (kartu/ilustrasi) sebelum sesi dimulai. Isi tidak boleh diubah Gemini; hanya cara tampilnya yang bebas.

| Latihan | Posisi HP |
|---|---|
| Squat | Saku depan celana, HP tegak, layar menghadap badan. Tidak dipegang |
| Push-up | HP rata di lantai, layar menghadap atas, bagian atas HP (dekat sensor proximity) tepat di bawah dada |
| Running | Saku depan celana atau armband. Tidak digenggam |

## 10. Catatan Perubahan
- 0.2: tambah bagian 9 (posisi HP terkunci), catatan WorkManager di bagian 7.
- 0.1: versi awal.
