# DESIGN SPECIFICATION (FINAL) — ShakeFit

> **Versi:** 2.0 (Final — Diselaraskan dengan `ui-contract.md` v0.2 & `prd.md` v0.2)
> **Platform & Stack:** Android (minSdk 26) — Jetpack Compose (Material 3), MVVM
> **Target Perangkat Uji:** Infinix Hot 70 (Android 16)
> **Bahasa UI:** Indonesia (via `stringResource`, placeholder sesuai kontrak)
> **Package Dasar:** `com.example.shakefit`

---

## 1. Arah Desain (Design Direction)

ShakeFit mengusung pendekatan **Adaptive Material 3 — Expressive & Gamified**: keterbacaan tinggi saat bergerak, struktur navigasi ringkas, dan momen selebrasi yang membangun kebiasaan olahraga harian.

| Dimensi | Keputusan Final |
|---|---|
| **Tema Warna** | **Adaptive Material 3 (Light & Dark)** mengikuti sistem perangkat, kontras tinggi untuk penggunaan *indoor* maupun *outdoor*. |
| **Navigasi Utama** | **4-Tab Bottom Navigation Bar** (`NavigationBar` M3): **Beranda**, **Target**, **Aktivitas** (Segmen Riwayat & Statistik), dan **Profil** (Pengaturan & Uji Sensor di dalamnya). |
| **Layar Sesi Aktif** | **Progress-Centric / Visual Ring** — berpusat pada *circular progress ring* dan indikator kualitas gerakan yang dinamis. |
| **Petunjuk Posisi HP** | **Compact Expandable Banner** wajib tampil di fase `IDLE` sebelum sesi dimulai, menggunakan teks terkunci tanpa perubahan. |
| **Karakter & Selebrasi** | **Expressive & Gamified** — warna aksen hidup, animasi pegas (*spring*), selebrasi visual untuk *Target Reached*, *Streak*, *New Record*, dan *Badge* baru. |

---

## 2. Batas Arsitektur Ketat (Zero Logic Leakage Policy)

Untuk menjamin pemisahan murni antara UI (Compose) dan Backend (MVVM/Repository/Sensor/SQLite), **seluruh Composable wajib mematuhi aturan berikut**:

1. **Dilarang Mengakses Sensor, DB, Lokasi, atau Service:**
   - Tidak ada impor atau pemanggilan `SensorManager`, `LocationManager`, `SQLiteOpenHelper`, `WorkManager`, `Vibrator`, `MediaPlayer`, maupun `ForegroundService` di layer UI.
   - Umpan balik getar/suara saat repetisi (`F-12b`) dan notifikasi ulang tahun (`F-13`) sepenuhnya dipicu oleh backend. UI hanya merender state dan mengirim event.
2. **Dilarang Menghitung Metrik Domain di UI:**
   - UI **tidak boleh** menghitung *pace*, jarak, estimasi kalori, skor kualitas gerakan, panjang langkah treadmill, usia (`ageYears`), *streak*, maupun status *badge*.
   - Semua pemformatan angka domain **wajib** memanggil fungsi util milik backend dari `ui-contract.md`:
     - `formatPace(secPerKm: Int?): String` (contoh: `"5:45 /km"`, `null -> "--:--"`)
     - `formatDuration(sec: Int): String` (contoh: `"1:02:05"`, `"01:35"`)
     - `formatDistanceKm(meters: Float): String` (contoh: `"5.23 km"`)
     - `formatCalories(kcal: Float): String` (contoh: `"~120 kkal"`, selalu berawalan `"~"`)
   - Kalkulasi yang diperbolehkan di UI hanyalah pemetaan tampilan murni (mis. rasio sudut *progress ring* `0f..1f` atau konversi satuan input form km ke meter saat mengirim `CalibrateTreadmill`).
3. **Stateless Screen + Thin Route Wrapper:**
   - Setiap layar dipisah menjadi:
     - `NamaRoute(viewModel: XxxViewModel, ...)` — wrapper tipis yang hanya memanggil `collectAsStateWithLifecycle()` lalu meneruskan ke `NamaScreen`. **UI tidak membuat implementasi ViewModel.**
     - `NamaScreen(state: XxxUiState, onEvent: (XxxEvent) -> Unit, modifier: Modifier = Modifier)` — murni stateless. (Khusus `StatsScreen` dan `SensorDebugScreen` tanpa `onEvent` sesuai kontrak Bagian 5).
4. **Tanpa Library Tambahan:**
   - Hanya menggunakan Jetpack Compose, Material 3, dan API standar Android/Lifecycle yang sudah tersedia di project. Grafik 7 hari, *progress ring*, ilustrasi posisi HP, dan efek selebrasi digambar memakai Compose `Canvas` dan ikon bawaan Material.
5. **Teks UI (`stringResource` vs State Backend):**
   - Semua label statis UI wajib lewat `stringResource(R.string.*)`.
   - Teks dinamis dari backend (`UiError.message`, `ExerciseCard.name`, `ExerciseCard.unavailableReason`, `RepSessionUiState.positionHint`, `BadgeUi.title/description`, `ProfileUiState.fieldErrors`) ditampilkan apa adanya dari `UiState`.
6. **Data Dummy Hanya di `@Preview`:**
   - Setiap layar wajib menyertakan `@Preview` untuk kondisi **Loading**, **Kosong (Empty)**, **Normal**, **Error**, dan **Kondisi Khusus**. Data dummy dilarang keras berada di jalur kode produksi.

---

## 3. Struktur Paket & File UI

```text
com.example.shakefit.ui
├── theme/
│   ├── Color.kt         // Token warna Light, Dark, ExerciseType, Semantic
│   ├── Type.kt          // Skala tipografi + tabular figures (tnum)
│   ├── Shape.kt         // Skala radius sudut Material 3
│   └── Theme.kt         // ShakeFitTheme(darkTheme = isSystemInDarkTheme())
├── components/
│   ├── PositionHintBanner.kt     // Compact Expandable Banner (Posisi HP Terkunci)
│   ├── VisualProgressRing.kt     // Expressive Circular Progress Ring (Canvas)
│   ├── SessionSummaryContent.kt  // Reusable untuk Rep/Run FINISHED & History Detail
│   └── StatePlaceholders.kt      // Loading, EmptyState, InlineErrorBanner
└── screens/
    ├── home/           // HomeRoute, HomeScreen + Previews
    ├── repsession/     // RepSessionRoute, RepSessionScreen + Previews
    ├── runsession/     // RunSessionRoute, RunSessionScreen + Previews
    ├── goals/          // GoalsRoute, GoalsScreen + Previews
    ├── activity/       // ActivityScreen (container Segmented Tab Riwayat & Statistik)
    ├── history/        // HistoryRoute, HistoryScreen + Previews
    ├── stats/          // StatsRoute, StatsScreen + Previews
    ├── profile/        // ProfileRoute, ProfileScreen + Previews
    ├── settings/       // SettingsRoute, SettingsScreen + Previews
    └── sensordebug/    // SensorDebugRoute, SensorDebugScreen + Previews
```

---

## 4. Design System & Token Visual (Material 3)

### 4.1 Palet Warna (`Color.kt`)

| Token Peran | Light Mode | Dark Mode | Penggunaan |
|---|---|---|---|
| `primary` | `#FF5722` | `#FF8A65` | CTA utama (Mulai), *active ring*, tab aktif |
| `onPrimary` | `#FFFFFF` | `#3E1000` | Teks/ikon di atas `primary` |
| `primaryContainer` | `#FFDBD0` | `#662009` | *Tonal badge*, sorotan kartu aktif |
| `secondary` | `#00897B` | `#4DB6AC` | Aksi sekunder, grafik 7 hari, GPS Ready |
| `tertiary` | `#FFB300` | `#FFD54F` | *Streak*, *New Record*, *Badge* terbuka |
| `tertiaryContainer` | `#FFF3CD` | `#4D3800` | Kartu selebrasi *New Record* & *New Badges* |
| `background` | `#F8F9FA` | `#0F1115` | Latar belakang utama layar |
| `surface` | `#FFFFFF` | `#171A21` | Permukaan kartu (*card*), `NavigationBar` |
| `surfaceVariant` | `#EEF1F5` | `#232834` | *Track* progress ring, chip tidak aktif, banner |
| `error` | `#D32F2F` | `#EF5350` | Error validasi, `UiError`, GPS error/denied |
| `outline` | `#D0D7DE` | `#323949` | Garis pemisah tipis, border sekunder |

**Warna Aksen per `ExerciseType`:**
- `ExerciseType.SQUAT`: `#5C6BC0` (Light) / `#9FA8DA` (Dark) — Indigo
- `ExerciseType.PUSHUP`: `#FF5722` (Light) / `#FF8A65` (Dark) — Coral
- `ExerciseType.RUNNING`: `#00897B` (Light) / `#4DB6AC` (Dark) — Teal

**Warna Semantik Status:**
- **Success (`Target Reached`, `GpsStatus.READY`):** `#2E7D32` (Light) / `#66BB6A` (Dark)
- **Warning (`GpsStatus.SEARCHING`, `GpsStatus.WEAK`, label `"perkiraan"`):** `#ED6C02` (Light) / `#FFA726` (Dark)

### 4.2 Tipografi (`Type.kt`)
Menggunakan `FontFamily.SansSerif` standar (tanpa font eksternal) dan `FontFamily.Monospace` khusus untuk `SensorDebugScreen`. Semua teks metrik dinamis (repetisi, durasi, jarak, pace, kalori, skor) **wajib** menyertakan `fontFeatureSettings = "tnum"` agar angka tidak bergeser saat berubah cepat.

| Gaya | Ukuran / Line Height | Weight | Penggunaan |
|---|---|---|---|
| `HeroMetric` | `56sp` / `64sp` | ExtraBold (`800`) | Angka utama di dalam *Visual Ring* (Reps, Countdown, Jarak) |
| `DisplaySmall` | `32sp` / `40sp` | Bold (`700`) | Metrik sekunder besar di sesi aktif & ringkasan |
| `HeadlineSmall` | `22sp` / `28sp` | Bold (`700`) | Sapaan Beranda, judul selebrasi Summary |
| `TitleMedium` | `16sp` / `24sp` | SemiBold (`600`) | Judul kartu latihan, judul section, label tab |
| `BodyMedium` | `14sp` / `20sp` | Regular (`400`) | Petunjuk posisi HP, deskripsi badge, catatan sesi |
| `LabelMedium` | `12sp` / `16sp` | Medium (`500`) | Unit (`REPS`, `KM`), chip status, label `"perkiraan"` |

### 4.3 Bentuk (`Shape.kt`) & Jarak
- **Corner Radius:** Kartu utama & Bottom Sheet `24.dp`, kartu metrik/item riwayat/banner `16.dp`, input form `14.dp`, tombol CTA utama & chip `RoundedCornerShape(50)` (pill).
- **Spacing Scale:** Padding horizontal layar `16.dp`, jarak antar section `20.dp`, jarak antar item `12.dp`, padding dalam kartu `16.dp`.

### 4.4 Gerak & Gamifikasi (*Motion*)
- **Progress Ring:** `animateFloatAsState` dengan `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)`.
- **Rep Counter Bump:** Skala ringan (`1.0f -> 1.15f -> 1.0f`) saat `reps` bertambah.
- **Target Reached:** Transisi warna *ring* ke hijau/emas + *pill* `"Target Tercapai!"`.
- **Selebrasi Summary:** Efek partikel ringan berbasis Compose `Canvas` dan kartu sorotan bernuansa `tertiaryContainer` saat `isNewRecord == true` atau `newBadges.isNotEmpty()`.

---

## 5. Arsitektur Navigasi & Penanganan Izin

### 5.1 4-Tab Bottom Navigation Bar
Ditampilkan hanya pada 4 destinasi utama:

| Tab | Label | Rute |
|---|---|---|
| 1 | **Beranda** | `HomeRoute` |
| 2 | **Target** | `GoalsRoute` |
| 3 | **Aktivitas** | `ActivityScreen` (Segmented Tab: `HistoryRoute` & `StatsRoute`) |
| 4 | **Profil** | `ProfileRoute` |

### 5.2 Alur Navigasi Lengkap
```text
[Tab 1: Beranda]
 ├── Tap Squat / Push-up ──> RepSessionRoute (Full-screen, tanpa Bottom Bar)
 │    └── Phase FINISHED ──> SessionSummaryContent (Dismiss -> kembali ke Beranda)
 └── Tap Running ──> RunSessionRoute (Full-screen, tanpa Bottom Bar)
      └── Phase FINISHED ──> SessionSummaryContent (Dismiss -> kembali ke Beranda)

[Tab 2: Target]
 └── GoalsRoute (Tambah/Edit via ModalBottomSheet)

[Tab 3: Aktivitas]
 ├── Segmen "Riwayat"   ──> HistoryRoute (selected != null -> HistoryDetail via SessionSummaryContent)
 └── Segmen "Statistik" ──> StatsRoute

[Tab 4: Profil]
 └── ProfileRoute
      └── Ikon Gear (Top Bar) ──> SettingsRoute (Full-screen)
           └── Menu "Uji Sensor" (Debug build only) ──> SensorDebugRoute
```

### 5.3 Penanganan Izin *Just-In-Time* (Kontrak Bag. 6 & PRD F-14)
- Izin **tidak pernah** diminta saat aplikasi pertama dibuka.
- UI memantau flag dari `UiState`:
  - `RunSessionUiState.needsLocationPermission` -> luncurkan launcher `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`.
  - `RunSessionUiState.needsNotificationPermission` -> luncurkan launcher `POST_NOTIFICATIONS` (Android 13+ / API 33+).
  - Setelah hasil dialog keluar, UI **wajib** mengirim `RunSessionEvent.PermissionResultChanged`.
  - `ProfileUiState.notificationPermissionNeeded` -> tampilkan banner ramah di `ProfileScreen` dengan tombol untuk memicu dialog `POST_NOTIFICATIONS` (Android 13+).
- Jika user menolak izin, aplikasi tidak boleh crash; UI cukup menampilkan status/pesan dari backend (`gpsStatus == PERMISSION_DENIED` atau `error: UiError`).

---

## 6. Komponen Global & Aturan Khusus

### 6.1 Compact Expandable Banner — Posisi HP (TERKUNCI)
Wajib tampil di fase `IDLE` pada `RepSessionScreen` dan `RunSessionScreen` sebelum tombol Mulai ditekan.
- **Kondisi Compact (Default):** Kartu sudut `16.dp` di bagian atas layar dengan ikon posisi HP, label `"Posisi HP Wajib"`, **teks terkunci tampil utuh** (tidak boleh dipotong/diubah maknanya), dan ikon panah *expand*.
- **Kondisi Expanded:** Membuka ilustrasi vektor Compose sederhana (diagram HP di saku depan celana / rata di lantai di bawah dada / armband) untuk memperjelas orientasi.
- **Sumber Teks Terkunci (Kontrak Bagian 9):**
  - **Squat & Push-up:** Wajib merender `state.positionHint` langsung dari `RepSessionUiState`.
  - **Running:** Karena `RunSessionUiState` tidak memiliki field `positionHint`, UI merender `stringResource` terkunci: *"Saku depan celana atau armband. Tidak digenggam"*.

### 6.2 Mapping Ikon Badge Awal (PRD F-12c)
UI memetakan `BadgeUi.code` ke ikon vektor Material (selalu sediakan *fallback* `Icons.Filled.EmojiEvents` agar kode badge baru dari backend tidak menyebabkan error):
- `FIRST_SESSION`, `FIRST_10_REPS`, `STREAK_3`, `STREAK_7`, `REPS_100_DAY`, `RUN_1KM`, `RUN_5KM`.

### 6.3 Penjagaan Layar Menyala (*KeepScreenOn*, PRD F-03 & F-07)
- Pada `RepSessionScreen`: saat `phase == COUNTDOWN || phase == ACTIVE`, aktifkan `LocalView.current.keepScreenOn = true` lewat `DisposableEffect` dan kembalikan ke `false` saat `onDispose` atau fase lain.
- Pada `RunSessionScreen`: sesi lari berjalan dengan Foreground Service di background (`isBackgroundTracking`), sehingga layar boleh dimatikan user saat berlari tanpa memutus sesi.

---

## 7. Spesifikasi Detail per Layar

### 7.1 Beranda (`HomeScreen` — `com.example.shakefit.ui.screens.home`)
- **Kontrak:** `HomeUiState` | `HomeEvent` (`SelectExercise`, `Refresh`)
- **Komponen & Tata Letak:**
  1. **Header Sapaan & Streak Pill:**
     - Kiri: `"Halo, ${greetingName}!"` (jika `greetingName != null`) atau `"Siap bergerak hari ini?"` (jika `null`).
     - Kanan: Pill *Streak* dengan ikon api (`"${streakDays} hari"`), sorotan warna emas jika `streakDays > 0`.
  2. **Section "Target Hari Ini" (`todayGoals`):**
     - `LazyRow` kartu mini berisi *Visual Ring* menggunakan `GoalProgress.fraction` langsung dari state, nama latihan, dan `"${current} / ${target} ${unit}"`.
     - Jika `todayGoals` kosong: kartu ajakan ringkas menuju tab **Target**.
  3. **Section "Mulai Latihan" (`exercises`):**
     - 3 kartu besar untuk `SQUAT`, `PUSHUP`, `RUNNING` dengan warna aksen masing-masing.
     - Jika `sensorAvailable == false`: kartu nonaktif (opacity `0.55f`), tidak bisa diklik, dan menampilkan banner peringatan inline berisi `unavailableReason`.
     - Klik kartu aktif -> `HomeEvent.SelectExercise(card.type)`.
  4. **Interaksi:** Mendukung *Pull-to-Refresh* -> `HomeEvent.Refresh`.
- **Kondisi `@Preview` Wajib:** Loading (`isLoading = true`), Normal (dengan `greetingName`, `todayGoals`, `streakDays > 0`), Kosong (`greetingName = null`, `todayGoals = emptyList()`), Sensor Tidak Tersedia (`sensorAvailable = false` pada Push-up), Error (`UiError`).

### 7.2 Sesi Repetisi (`RepSessionScreen` — `com.example.shakefit.ui.screens.repsession`)
- **Kontrak:** `RepSessionUiState` | `RepSessionEvent` (`Start`, `Pause`, `Resume`, `Finish`, `Cancel`, `DismissSummary`)
- **Fase `IDLE`:**
  - Top Bar (Kembali + nama latihan Squat/Push-up).
  - `PositionHintBanner` menampilkan `state.positionHint`.
  - Pemilih `SessionMode`: **Bebas (`FREE`)**, **Tantangan 60 Detik (`CHALLENGE_60S`)**, **Target (`GOAL`)** (menampilkan `targetReps` jika tidak null).
  - Tombol CTA besar `"Mulai Latihan"` -> `RepSessionEvent.Start(state.exercise, selectedMode)`.
- **Fase `COUNTDOWN`:**
  - *Visual Ring* menyusut + angka `countdownRemaining` (`HeroMetric`).
  - Pengingat singkat `state.positionHint` di bawah ring + tombol `"Batal"` (`RepSessionEvent.Cancel`).
- **Fase `ACTIVE` & `PAUSED`:**
  - Badge mode aktif + badge `"DIJEDA"` saat `PAUSED`.
  - **Visual Progress Ring (tengah):**
    - Isi ring: angka `reps` (`HeroMetric`, animasi *bump*), sub-label `"REPETISI"` atau `"${reps} / ${targetReps}"`, plus pill `"Target Tercapai!"` jika `targetReached == true`.
    - Progres ring: `GOAL` -> rasio `reps` terhadap `targetReps`; `CHALLENGE_60S` -> rasio `remainingSec` terhadap `60`; `FREE` -> *pulse ring*.
  - **Baris Metrik Pendukung:**
    - Waktu: `formatDuration(elapsedSec)` (atau tonjolkan `formatDuration(remainingSec)` pada `CHALLENGE_60S`).
    - Skor Kualitas (`liveQualityScore`): horizontal bar 0–100 (tampilkan `"Mengukur..."` jika `null`).
    - Interval Terakhir (`lastRepIntervalMs`): tampilkan teks ringkas jika tidak `null`.
  - **Kontrol Bawah:**
    - `ACTIVE`: Tombol **Jeda** (`Pause`) & **Selesai** (`Finish`).
    - `PAUSED`: Tombol **Lanjutkan** (`Resume`), **Selesai** (`Finish`), dan **Buang Sesi** (`Cancel` dengan dialog konfirmasi).
- **Fase `FINISHED`:** Merender `SessionSummaryContent(state.summary)` -> tombol tutup memicu `RepSessionEvent.DismissSummary`.
- **Kondisi `@Preview` Wajib:** `IDLE` (Squat & Push-up), `COUNTDOWN`, `ACTIVE` (mode `GOAL` dengan `targetReached = true`), `ACTIVE` (mode `CHALLENGE_60S`), `PAUSED`, `FINISHED` (dengan `isNewRecord` & `newBadges`), dan `Error`.

### 7.3 Sesi Lari (`RunSessionScreen` — `com.example.shakefit.ui.screens.runsession`)
- **Kontrak:** `RunSessionUiState` | `RunSessionEvent` (`Start`, `Pause`, `Resume`, `Finish`, `Cancel`, `CalibrateTreadmill`, `PermissionResultChanged`, `DismissSummary`)
- **Fase `IDLE`:**
  - Top Bar (Kembali + `"Lari"`).
  - `PositionHintBanner` dengan teks terkunci Running.
  - Segmented Toggle `RunEnvironment`: **Outdoor (`OUTDOOR`)** vs **Treadmill (`TREADMILL`)**.
  - Saat `OUTDOOR`: tampilkan pill status `gpsStatus` (`SEARCHING`, `READY`, `WEAK`, `PERMISSION_DENIED`, `DISABLED`).
  - Saat `TREADMILL`: sembunyikan indikator GPS (`gpsStatus == OFF`), tampilkan catatan singkat bahwa jarak berupa *perkiraan* langkah dan bisa dikalibrasi setelah selesai.
  - Tombol CTA `"Mulai Lari"` -> `RunSessionEvent.Start(selectedEnvironment)`.
- **Fase `COUNTDOWN`:** Visual Ring + `countdownRemaining` + tombol `"Batal"` (`Cancel`).
- **Fase `ACTIVE` & `PAUSED`:**
  - Top Status Bar: Badge `Outdoor`/`Treadmill`, pill `gpsStatus` (khusus `OUTDOOR`), dan indikator kecil bila `isBackgroundTracking == true`.
  - **Visual Progress Ring:** Progres visual per 1 km (`(distanceMeters % 1000f) / 1000f`). Di tengah: `formatDistanceKm(distanceMeters)`. **Wajib** tampilkan pill `"perkiraan"` tepat di bawah jarak bila `distanceSource == DistanceSource.STEPS_ESTIMATE`.
  - **Grid Metrik 2x2:**
    - Durasi: `formatDuration(elapsedSec)`
    - Pace Saat Ini: `formatPace(currentPaceSecPerKm)`
    - Pace Rata-rata: `formatPace(avgPaceSecPerKm)`
    - Kadens & Kalori: `"${cadenceSpm ?: "--"} spm"` & `formatCalories(caloriesEst)`
  - **Strip Split Terakhir:** Jika `splits.isNotEmpty()`, tampilkan baris ringkas split km terakhir (`"KM ${last.kmIndex} • ${formatPace(last.paceSecPerKm)}"`).
  - **Kontrol Bawah:** Jeda/Lanjut, Selesai, Buang Sesi (dengan konfirmasi).
- **Fase `FINISHED`:**
  - Merender `SessionSummaryContent(state.summary)`.
  - Khusus `environment == RunEnvironment.TREADMILL`: tampilkan kartu aksi **"Kalibrasi Jarak Treadmill"** yang membuka dialog input jarak aktual (km), lalu mengirim `RunSessionEvent.CalibrateTreadmill(actualDistanceMeters)`.
  - Tombol tutup memicu `RunSessionEvent.DismissSummary`.
- **Kondisi `@Preview` Wajib:** `IDLE` Outdoor (`GPS READY` & `PERMISSION_DENIED`), `IDLE` Treadmill, `ACTIVE` Outdoor (GPS + splits), `ACTIVE` Treadmill (`STEPS_ESTIMATE` dengan label `"perkiraan"`), `PAUSED`, `FINISHED` Treadmill (dengan opsi kalibrasi), dan `Error`.

### 7.4 Ringkasan Sesi (`SessionSummaryContent` — Komponen Reusable)
- **Data:** `SessionSummary`
- **Isi:**
  1. **Header Selebrasi:** Banner emas `"REKOR BARU!"` jika `isNewRecord == true`, nama latihan, mode, dan tanggal/waktu (`startedAtEpochMs`).
  2. **Badge Baru (`newBadges`):** Kartu sorotan `tertiaryContainer` untuk setiap badge baru (`title`, `description`, ikon).
  3. **Metrik Utama:** Durasi (`formatDuration`), Kalori (`formatCalories`, selalu `"~..."`), ditambah metrik spesifik latihan:
     - Repetisi: `reps`, `qualityScore` (bar 0–100), `avgTempoMs`.
     - Running: `formatDistanceKm(distanceMeters)` (+ label `"perkiraan"` jika `distanceSource == STEPS_ESTIMATE`), `formatPace(avgPaceSecPerKm)`, `formatPace(bestPaceSecPerKm)`, `avgCadenceSpm`.
  4. **Tabel Splits (`splits`):** Daftar per km (`kmIndex`, `formatDuration(durationSec)`, `formatPace(paceSecPerKm)`).
  5. **Catatan (`note`):** Read-only di ringkasan pasca-sesi; dapat diedit (simpan via `HistoryEvent.UpdateNote`) saat dibuka dari Detail Riwayat.

### 7.5 Target (`GoalsScreen` — `com.example.shakefit.ui.screens.goals`)
- **Kontrak:** `GoalsUiState` | `GoalsEvent` (`Add`, `Update`, `SetActive`, `Delete`)
- **Isi:** Daftar kartu `GoalItem` (ikon latihan, progress bar `progress` vs `target` + `unit`, `Switch` aktif/nonaktif, aksi ubah & hapus dengan konfirmasi) serta `ExtendedFloatingActionButton` `"Tambah Target"` yang membuka `ModalBottomSheet` pilih latihan & angka target.
- **Kondisi `@Preview` Wajib:** Loading, Kosong, Normal (campuran aktif/nonaktif & REPS/KM), Error.

### 7.6 Aktivitas — Riwayat (`HistoryScreen` — `com.example.shakefit.ui.screens.history`)
- **Kontrak:** `HistoryUiState` | `HistoryEvent` (`SetFilter`, `Open`, `UpdateNote`, `Delete`, `CloseDetail`)
- **Isi:**
  - Filter chip: Semua (`null`), Squat, Push-up, Running -> `SetFilter`.
  - Daftar `items`: kartu ringkas (ikon, metrik utama, `"perkiraan"` bila `STEPS_ESTIMATE`, durasi, tanda *New Record*). Klik -> `Open(sessionId)`.
  - Ketika `selected != null`: tampilkan layar/overlay Detail Riwayat menggunakan `SessionSummaryContent` dengan input edit catatan (`UpdateNote`), tombol hapus (`Delete` dengan konfirmasi), dan tombol kembali (`CloseDetail`).
- **Kondisi `@Preview` Wajib:** Loading, Kosong, Normal (list campuran), Detail Terbuka (`selected != null`), Error.

### 7.7 Aktivitas — Statistik (`StatsScreen` — `com.example.shakefit.ui.screens.stats`)
- **Kontrak:** `StatsUiState` (Read-only, tanpa event)
- **Isi:**
  1. Ringkasan Total (`totalSessions`, `totalReps`, `totalKm`) + banner `streakDays`.
  2. Grafik Batang 7 Hari Terakhir (`last7Days`, hari tertua di kiri -> terbaru di kanan) digambar dengan Compose `Canvas`.
  3. Kartu Rekor Pribadi (`bestRepsByExercise`, `formatPace(bestPaceSecPerKm)`, `longestRunMeters?.let { formatDistanceKm(it) }`).
  4. Galeri Lencana (`badges`): Grid 2 kolom; terbuka (`unlockedAtEpochMs != null`) berwarna penuh, terkunci (`null`) redup dengan ikon gembok.
- **Kondisi `@Preview` Wajib:** Loading, Kosong (pengguna baru, 0 sesi, semua badge terkunci), Normal (data 7 hari + sebagian badge terbuka), Error.

### 7.8 Profil (`ProfileScreen` — `com.example.shakefit.ui.screens.profile`)
- **Kontrak:** `ProfileUiState` | `ProfileEvent` (`Save`, `ConsumeSaved`)
- **Isi:**
  - Top Bar dengan ikon **Pengaturan** (`Settings`) menuju `SettingsRoute`.
  - Banner permintaan izin notifikasi ulang tahun jika `notificationPermissionNeeded == true`.
  - Kartu status kelengkapan profil (`isProfileComplete`): jika `false`, tampilkan info ramah bahwa aplikasi tetap bisa dipakai dengan estimasi kalori/treadmill *perkiraan kasar* (selaras PRD F-01).
  - Form: `nickname` (opsional), `gender` (`MALE`/`FEMALE`), `heightCm`, `weightKg`, `birthDateIso` (M3 `DatePickerDialog` format `yyyy-MM-dd`), menampilkan usia `ageYears` dari state.
  - Error per field membaca `fieldErrors["nickname" | "height" | "weight" | "birthDate"]`.
  - Tombol `"Simpan Profil"` -> `ProfileEvent.Save(...)`. Saat `saved == true`, tampilkan `Snackbar` sukses lalu panggil `ProfileEvent.ConsumeSaved`.
- **Kondisi `@Preview` Wajib:** Profil Kosong (`isProfileComplete = false`), Profil Lengkap + `notificationPermissionNeeded = true`, Validation Errors (`fieldErrors` terisi), Saved Toast/Error.

### 7.9 Pengaturan (`SettingsScreen` — `com.example.shakefit.ui.screens.settings`)
- **Kontrak:** `SettingsUiState` | `SettingsEvent` (`SetSensitivity`, `SetVibration`, `SetSound`, `SetCountdown`)
- **Isi:**
  - Slider `squatSensitivity` & `pushupSensitivity` (`0f..1f`).
  - Switch `vibrationOn` & `soundOn`.
  - Pemilih `countdownSec` (`3..10` detik).
  - Informasi read-only `treadmillStepLengthFactor` (terkalibrasi dari sesi treadmill).
  - Menu `"Uji Sensor (Debug)"` menuju `SensorDebugRoute` — hanya ditampilkan jika flag debug aktif (`showDebugOption: Boolean`).
- **Kondisi `@Preview` Wajib:** Loading, Normal (dengan & tanpa opsi Debug), Error.

### 7.10 Uji Sensor (`SensorDebugScreen` — `com.example.shakefit.ui.screens.sensordebug`)
- **Kontrak:** `SensorDebugUiState` (Read-only, tanpa event)
- **Isi:**
  - Status chip ketersediaan `accelAvailable`, `gyroAvailable`, `proximityAvailable`.
  - Kartu angka real-time (`FontFamily.Monospace`, tabular): `accel` (X/Y/Z), `gyro` (X/Y/Z), `proximityValue` & `proximityMaxRange` beserta bar visual.
- **Kondisi `@Preview` Wajib:** Semua Sensor Tersedia (data aktif), Sebagian Sensor Tidak Tersedia (`gyroAvailable = false`, nilai `null`).

---

## 8. Checklist Audit Final (Kontrak & PRD)

- [x] Nol akses sensor, SQLite, lokasi, WorkManager, atau service di layer Composable.
- [x] Nol perhitungan pace, jarak, kalori, skor kualitas, atau usia di UI; semua memakai util backend.
- [x] Seluruh layar memakai pola stateless `NamaScreen` + wrapper tipis `NamaRoute(viewModel)`.
- [x] Tanpa penambahan library di luar standar Compose/Material 3.
- [x] Posisi HP terkunci (Bagian 9 Kontrak / Bagian 8 PRD) wajib tampil utuh di fase `IDLE`.
- [x] Label `"perkiraan"` wajib tampil saat `distanceSource == DistanceSource.STEPS_ESTIMATE`.
- [x] `KeepScreenOn` hanya aktif di `RepSessionScreen` (`COUNTDOWN`/`ACTIVE`); Running mengandalkan Foreground Service.
- [x] Izin lokasi & notifikasi diminta *just-in-time* dan dilaporkan balik via `PermissionResultChanged`.
- [x] Semua teks statis memakai `stringResource`; data dummy hanya di `@Preview`.