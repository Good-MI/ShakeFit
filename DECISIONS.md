# DECISIONS — ShakeFit

Catatan keputusan arsitektur, dependensi, dan produk sesuai aturan `restriction.md`.

---

## 20 November 2026

### Keputusan Terbuka PRD v0.2 (Dikonfirmasi User)
1. **Package Name**: `com.example.shakefit`
2. **Bahasa UI**: Bahasa Indonesia (via strings/kontrak)
3. **Field Nickname**: Tetap digunakan di tabel `user_profile` dan `ProfileUiState` untuk ucapan ulang tahun (F-13) dan sapaan beranda (F-01).

### Dependensi Fase 1
- `androidx.navigation:navigation-compose:2.9.0` (diizinkan: Navigation Compose).
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.11.1` (diizinkan: Lifecycle ViewModel Compose).
- `androidx.compose.material:material-icons-core` (diizinkan: Material Compose via BOM untuk navigasi dasar).
- `minSdk` disetel ke 26 (sesuai spesifikasi `prd.md` dan `restriction.md`).

### Fase 2: Sensor & Location Data Source
- `SensorDataSource` menggunakan `SensorManager` bawaan dengan `callbackFlow` yang memanggil `unregisterListener` saat flow dibatalkan.
- `LocationDataSource` menggunakan `LocationManager` bawaan dengan `removeUpdates` saat flow dibatalkan.
- `SensorDebugViewModel` meng-expose `uiState: StateFlow<SensorDebugUiState>` murni sesuai kontrak, dengan state tambahan `gpsDebugState` untuk data uji GPS mentah.
- Izin lokasi diminta *just-in-time* saat tombol "Mulai Uji GPS" ditekan di layar debug.

### Fase 3: Implementasi RepDetector (Squat & Push-up)
- Kelas murni Kotlin di package `com.example.shakefit.engine.detector` tanpa dependensi Android framework (`android.*`).
- `SquatRepDetector`: Menggunakan low-pass filter untuk memisahkan gravitasi dan akselerasi linier vertikal, mendeteksi perubahan sudut kemiringan paha (tilt) posisi HP tegak di saku depan celana, serta mendukung gyroscope opsional sebagai validator rotasi (fallback aman jika gyro null).
- `PushupRepDetector`: Menggunakan Proximity sebagai sensor utama (siklus NEAR -> FAR, mendukung sensor continuous maupun biner 0cm vs maxRange) dan Accelerometer sebagai pendukung untuk menolak rep palsu jika HP di lantai diangkat/digoyang.
- Mendukung penyesuaian sensitivitas (0f..1f) dan validasi jeda minimum antar rep (`Tuning.SQUAT_MIN_REP_INTERVAL_MS = 600L`, `Tuning.PUSHUP_MIN_REP_INTERVAL_MS = 500L`).
- Seluruh ambang batas tebakan ditaruh di `Tuning.kt` dan ditandai `ASUMSI`.

### Fase 4: Implementasi RunTracker (Outdoor GPS & Treadmill)
- Kelas murni Kotlin di package `com.example.shakefit.engine.running` tanpa dependensi `android.*`.
- Mode Outdoor: Akumulasi jarak menggunakan formula Haversine murni dari titik `GpsPoint`, memfilter titik dengan akurasi >25m dan lonjakan kecepatan >15 m/s. Titik rute tidak disimpan (hanya ringkasan).
- Mode Treadmill: `StepDetector` berbasis accelerometer mendeteksi hentakan langkah (peak detection + hysteresis), jarak = total langkah x (tinggi badan x faktor kalibrasi).
- Perhitungan Pace: Pace saat ini (jendela waktu bergulir 15 detik), pace rata-rata (durasi total / jarak total), dan pace terbaik (dalam detik/km).
- Split 1 km: Mencatat `RunSplit` secara otomatis setiap kali jarak melewati kelipatan 1000m.
- Cadence: Dihitung bergulir (SPM) dari timestamp langkah accelerometer.
- Estimasi Kalori: `jarak_km * berat_badan * 1.036f` (koefisien kasar dari `Tuning.kt`).
- Kalibrasi Treadmill: `TreadmillCalibrator.calculateCalibratedStepFactor` menghitung koefisien baru berdasarkan jarak aktual yang dimasukkan user.

### Fase 5: Mesin Sesi Latihan, SQLite Repository, & ViewModel
- Seluruh repository (`SessionRepository`, `GoalRepository`, `StatsRepository`, `SettingsRepository`) dieksekusi di `Dispatchers.IO` dengan parameter binding `?` dan `PRAGMA foreign_keys = ON`.
- `RepSessionViewModel`: Mengelola transisi state `IDLE -> COUNTDOWN -> ACTIVE <-> PAUSED -> FINISHED`, pelepasan listener sensor saat PAUSED/FINISHED/Cancel, serta teks posisi HP terkunci sesuai `ui-contract.md` bagian 9.
- `RunSessionViewModel`: Mengintegrasikan `RunTracker`, `LocationDataSource`, dan `SensorDataSource`. Listener lokasi dan akselerometer dilepas saat PAUSED/FINISHED/Cancel.
- `HomeViewModel`, `HistoryViewModel`, `GoalsViewModel`, `StatsViewModel`, dan `SettingsViewModel` diimplementasikan dengan kontrak StateFlow dan Event persis tanpa perubahan struktur.
- `AppContainer` dan `ViewModelFactory` diperbarui untuk inject seluruh dependensi secara manual tanpa framework DI eksternal.

### Fase 6: Profil Pengguna & Notifikasi Ulang Tahun (WorkManager)
- Dependensi `androidx.work:work-runtime-ktx:2.10.0` ditambahkan (disetujui di `restriction.md` dan `prd.md`).
- `ProfileRepository`: Menangani penyimpanan profil di tabel `user_profile` (id=1), serta update `last_birthday_notified_year`.
- `BirthdayChecker`: Kelas murni Kotlin yang menangani logika tanggal lahir, usia, kasus 29 Februari (dirayakan 28 Februari pada tahun non-kabisat), proteksi sekali per tahun, dan generator template teks panjang BigTextStyle lokal tanpa internet.
- `BirthdayNotificationWorker`: Worker harian terjadwal via WorkManager ditambah pengecekan cadangan saat aplikasi dibuka di `ShakeFitApplication`. Memeriksa izin `POST_NOTIFICATIONS` di Android 13+ secara aman tanpa crash.
- `ProfileViewModel`: Mengelola `ProfileUiState` dan event `Save` serta `ConsumeSaved` dengan validasi rentang tinggi (50-250cm), berat (20-300kg), tanggal lahir tidak di masa depan, dan nickname maks 30 karakter.

### Fase 7: Tracking Background untuk Running (Foreground Service)
- `RunningForegroundService`: Terdaftar di `AndroidManifest.xml` dengan `android:foregroundServiceType="location"` sesuai regulasi Android 14+. Menggunakan `FOREGROUND_SERVICE_TYPE_LOCATION` saat `startForeground()`.
- Notifikasi persisten menampilkan durasi berjalan, jarak lari (km), dan status Dijeda/Aktif, dengan pending intent untuk kembali ke aplikasi serta action intent untuk Jeda / Lanjut / Selesai langsung dari notification drawer.
- `RunSessionCoordinator`: Komponen berlingkup aplikasi (singleton di `AppContainer`) yang mengelola siklus hidup sesi lari, timer, `RunTracker`, dan listener sensor/GPS. Menghindari duplikasi tracking atau hilangnya state saat pengguna beralih aplikasi atau layar dimatikan.
- Mengambil data tinggi dan berat badan terbaru dari `ProfileRepository` (fallback ke konstanta `Tuning.kt` jika profil belum diisi).
- Pelepasan listener sensor & lokasi: Listener dipastikan dilepas saat sesi dalam status `PAUSED`, `FINISHED`, atau `Cancel`.
- `RunSessionViewModel`: Disederhanakan untuk mendelegasikan state flow dan dispatch event ke `RunSessionCoordinator`, mematuhi kontrak `RunSessionUiState` dan `RunSessionEvent` tanpa perubahan API.

### Fase 8: Fitur Kreatif (F-12a sampai F-12e)
- `MovementQualityCalculator`: Kalkulator murni Kotlin (0..100) berbasis koefisien variasi (CV = stdDev / mean) dari interval repetisi. Bernilai null jika < 3 repetisi (`Tuning.QUALITY_MIN_REPS = 3`), dan bernilai 0 jika variasi interval melebihi ambang batas (`Tuning.QUALITY_MAX_CV = 0.50f`).
- `FeedbackManager`: Util audio dan haptic feedback menggunakan `ToneGenerator` (nada bip sistem) dan `Vibrator`/`VibratorManager` bawaan tanpa file audio atau akses internet. Menghormati preferensi `soundOn` dan `vibrationOn` di `SettingsRepository`.
- `BadgeEvaluator` & `StreakCalculator`: Evaluator murni Kotlin independen untuk mengecek 7 badge awal (`FIRST_SESSION`, `FIRST_10_REPS`, `STREAK_3`, `STREAK_7`, `REPS_100_DAY`, `RUN_1KM`, `RUN_5KM`) dan rekor baru (`isNewRecord`) secara atomik di SQLite saat sesi `FINISHED`.
- `StreakCalculator`: Menghitung streak hari beruntun dari SQLite secara deterministik, dengan toleransi hari kemarin jika hari ini belum ada sesi.
- Mode Tantangan 60 Detik (`SessionMode.CHALLENGE_60S`): `RepSessionViewModel` menghitung mundur `remainingSec` dari 60 hingga 0 detik saat `ACTIVE`, lalu memicu `finishSession()` secara otomatis ketika waktu habis.
- Estimasi Kalori: `RepSessionViewModel` membaca berat badan pengguna dari `ProfileRepository` (fallback `Tuning.DEFAULT_WEIGHT_KG`) untuk penyesuaian kalori repetisi secara proporsional.

### Fase 9: Audit Kepatuhan, Keamanan Resource & Kesiapan Integrasi UI
- **Audit restriction.md**: Bebas izin `INTERNET`, bebas library terlarang (tanpa Room, Hilt, Koin, Retrofit, FusedLocation), 100% data sensor asli dari hardware.
- **Audit Lifecycle**: Seluruh sensor listener, GPS location updates, coroutine job, dan audio generator dilepas bersih saat `PAUSED`, `FINISHED`, `Cancel`, atau `onCleared`.
- **Audit Threading**: Semua query SQLite berjalan di `Dispatchers.IO` dengan parameter binding `?` dan `PRAGMA foreign_keys = ON`.
- **Kesiapan UI**: Seluruh 9 ViewModel meng-expose `uiState: StateFlow<T>` dan fungsi `onEvent(E)` persis sesuai spesifikasi `ui-contract.md`, siap di-binding langsung oleh tim UI Compose.

### Fase UI 1: Pondasi Tema (ShakeFitTheme) & Layar Beranda (HomeScreen)
- **Zero Logic Leakage Policy**: `HomeScreen` 100% stateless dan murni merender `HomeUiState`. Tidak ada pemanggilan sensor, database, lokasi, atau komputasi domain di Composable.
- **Sistem Desain Material 3**:
  - `Color.kt`: Palet warna Light & Dark sporty adaptif, termasuk warna aksen latihan (`SQUAT` Indigo, `PUSHUP` Coral, `RUNNING` Teal), warna status semantik, dan warna gamifikasi (tertiary/gold).
  - `Type.kt`: Tipografi tegas berbasis `FontFamily.SansSerif` dengan `fontFeatureSettings = "tnum"` untuk metrik dinamis.
  - `Shape.kt`: Sudut membulat modern 16dp–24dp untuk kartu, pill 50% untuk chip/badge.
  - `Theme.kt`: `ShakeFitTheme` adaptif otomatis Light & Dark tanpa dynamic color wallpaper agar warna identitas ShakeFit tetap konsisten.
- **HomeScreen**:
  - Pola `HomeRoute` (tipis) + `HomeScreen` (stateless).
  - Menampilkan Header sapaan personal/default dan lencana Streak gamified.
  - Kartu target hari ini (LazyRow) dengan visual progress bar/ring dan empty state ramah jika belum ada target.
  - Daftar 3 latihan utama (`ExerciseCard`) dengan penanganan kartu aktif (klik -> navigasi) dan nonaktif (`sensorAvailable == false`, warning banner inline, disabled).
  - Penanganan state loading dan inline error banner dengan tombol retry "Coba Lagi".
  - Teks statis 100% terdaftar di `strings.xml`.
  - Preview lengkap untuk kondisi Normal (Light & Dark), Loading, Empty Goals, Sensor Unavailable, dan Error.

### Fase UI 2: Layar Sesi Repetisi (RepSessionScreen) & Komponen Reusable SessionSummary
- **SessionSummaryContent** (`com.example.shakefit.ui.components`):
  - Komponen reusable untuk `RepSessionScreen`, `RunSessionScreen`, dan Riwayat Latihan Detail.
  - Adaptif terhadap jenis latihan: menampilkan Reps & Quality Score untuk Squat/Push-up, atau Jarak & Pace untuk Running.
  - Menampilkan perayaan gamifikasi: kartu emas saat `isNewRecord == true`, serta daftar lencana baru saat `newBadges.isNotEmpty()`.
- **RepSessionScreen** (`com.example.shakefit.ui.screens.repsession`):
  - Pola `RepSessionRoute` (tipis) + `RepSessionScreen` (100% stateless).
  - `PositionHintBanner`: Banner posisi HP terkunci (`state.positionHint`) yang expandable (terbuka di `IDLE`, otomatis ringkas saat `ACTIVE`/`COUNTDOWN`).
  - `KeepScreenOn`: Menjaga layar tetap menyala saat `COUNTDOWN` dan `ACTIVE` via `DisposableEffect` (PRD F-03).
  - `VisualProgressRing`: Menggambar cincin progres melingkar memakai Canvas Compose dengan angka metrik tabular figures (`tnum`).
  - Transisi visual lengkap:
    - `IDLE`: Pemilihan mode (`FREE`, `CHALLENGE_60S`, `GOAL`) dan tombol Mulai.
    - `COUNTDOWN`: Angka hitung mundur tegas di tengah dan tombol Batal.
    - `ACTIVE`: Cincin progres, angka repetisi, pill selebrasi target tercapai, bar durasi/kualitas, tombol Jeda & Selesai.
    - `PAUSED`: Status Dijeda, tombol Lanjut, Selesai, dan Batal.
    - `FINISHED`: Menampilkan `SessionSummaryContent` dengan aksi `DismissSummary`.
  - Previews lengkap: IDLE, COUNTDOWN, ACTIVE, ACTIVE Target Reached, PAUSED, dan FINISHED.

### Fase UI 3: Layar Sesi Lari (RunSessionScreen — Outdoor GPS & Treadmill)
- **RunSessionRoute** (`com.example.shakefit.ui.screens.runsession`):
  - Menangani izin runtime just-in-time (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, dan `POST_NOTIFICATIONS` Android 13+) via `rememberLauncherForActivityResult`.
  - Memetakan hasil izin ke `RunSessionEvent.PermissionResultChanged` tanpa crash jika ditolak.
- **RunSessionScreen** (100% stateless):
  - Header dengan aksen warna Teal (`RunningAccentLight`/`RunningAccentDark`), status fase, chip tracking latar belakang aktif (`isBackgroundTracking`), dan badge status GPS (`GpsStatus`) dengan warna semantik (Ready/Searching/Weak/Disabled/PermissionDenied/Off).
  - Fase `IDLE`: Pemilihan lingkungan lari (`RunEnvironment.OUTDOOR` vs `RunEnvironment.TREADMILL`) dengan kartu deskripsi masing-masing.
  - Fase `COUNTDOWN`: Hitung mundur besar tabular `tnum` + tombol Batal.
  - Fase `ACTIVE` & `PAUSED`:
    - Angka jarak format besar (`formatDistanceKm`) dengan label "Perkiraan" wajib jika mode Treadmill (`DistanceSource.STEPS_ESTIMATE`).
    - Grid metrik sekunder: Durasi, Pace saat ini, Pace rata-rata, Cadence (spm), dan Kalori.
    - Tabel ringkas split 1 km (`kmIndex`, durasi split, pace split).
    - Kontrol sesi: Jeda & Selesai (saat Active); Lanjut, Selesai, dan Batal (saat Paused).
  - Fase `FINISHED` & Kalibrasi:
    - Reusable `SessionSummaryContent` untuk ringkasan metrik jarak, pace, durasi, kalori, dan lencana.
    - Khusus mode Treadmill: Menyediakan tombol dan `AlertDialog` "Kalibrasi Jarak Treadmill" untuk menyetel jarak aktual (mengirim `RunSessionEvent.CalibrateTreadmill`).
  - Previews lengkap: IDLE, ACTIVE Outdoor (GPS Ready & Splits), ACTIVE Treadmill (Perkiraan), PAUSED, dan FINISHED.

### Fase UI 4: Layar Target Harian (GoalsScreen) & Layar Aktivitas (HistoryScreen + StatsScreen)
- **GoalsScreen** (`com.example.shakefit.ui.screens.goals`):
  - Pola `GoalsRoute` (tipis) + `GoalsScreen` (100% stateless).
  - Menampilkan daftar `GoalItem` dengan warna aksen latihan masing-masing (`SQUAT`, `PUSHUP`, `RUNNING`), teks target & unit, progress bar capaian harian, dan toggle `Switch` aktif/nonaktif (`GoalsEvent.SetActive`).
  - Dialog Tambah Target (`AddGoalDialog`): Memilih jenis latihan dan angka target -> mengirim `GoalsEvent.Add(type, target)`.
  - Dialog Ubah Target (`EditGoalDialog`): Mengubah angka target yang ada -> mengirim `GoalsEvent.Update(id, target)`.
  - Dialog Konfirmasi Hapus (`AlertDialog`): Menghapus target -> mengirim `GoalsEvent.Delete(id)`.
  - Floating Action Button ("Tambah Target") dan kartu Empty State ramah dengan tombol CTA.
  - Previews lengkap: Normal, Kosong, dan Loading.
- **HistoryScreen** (`com.example.shakefit.ui.screens.history`):
  - Pola `HistoryRoute` (tipis) + `HistoryScreen` (100% stateless).
  - Filter Chips interaktif di bagian atas: "Semua" (null), "Squat", "Push-up", "Running" -> mengirim `HistoryEvent.SetFilter`.
  - Daftar kartu ringkasan riwayat: Menampilkan aksen latihan, tanggal & waktu (`SimpleDateFormat`), metrik utama (Reps & Quality Score atau Jarak & Pace), tanda "Perkiraan" (jika `STEPS_ESTIMATE`), durasi, kalori, badge "Rekor Baru", dan indikator catatan.
  - Klik kartu -> mengirim `HistoryEvent.Open(sessionId)`.
  - Mode Detail Sesi (`selected != null`): Menggunakan `SessionSummaryContent` dengan TopAppBar tombol kembali (`CloseDetail`), editor catatan interaktif dengan tombol "Simpan Catatan" (`UpdateNote`), dan tombol hapus dengan dialog konfirmasi (`Delete`).
  - Previews lengkap: Normal, Detail Terbuka, Kosong, dan Loading.
- **StatsScreen** (`com.example.shakefit.ui.screens.stats`):
  - Pola `StatsRoute` (tipis) + `StatsScreen` (100% stateless, read-only sesuai kontrak).
  - Banner Streak harian berturut-turut dengan aksen warna `tertiaryContainer` (gold).
  - Kartu Ringkasan Total: 3 kartu metrik horizontal (Total Sesi, Total Reps, Total Jarak).
  - Grafik Batang 7 Hari Terakhir (`ActivityBarChart`): Murni digambar menggunakan Compose `Canvas` tanpa library pihak ketiga. Menampilkan 7 batang dengan tinggi dinamis berdasarkan intensitas harian, rounded corners, dan label tanggal di bawahnya.
  - Rekor Pribadi: Grid 2x2 untuk Squat terbanyak, Push-up terbanyak, Pace terbaik, dan Lari terjauh.
  - Galeri Lencana: Menampilkan daftar seluruh lencana awal. Lencana yang terbuka (`unlockedAtEpochMs != null`) berwarna emas/vibrant dengan ikon bintang dan tanggal terbuka; lencana terkunci redup (`alpha = 0.55f`) dengan ikon gembok (`Icons.Filled.Lock`).
  - Previews lengkap: Normal, Kosong, dan Loading.
- **ActivityScreen** (`com.example.shakefit.ui.screens.activity`):
  - Wadah Tab 3 Bottom Navigation ("Aktivitas") yang menyatukan `HistoryRoute` dan `StatsRoute` menggunakan `PrimaryTabRow` (Material 3).
- **SessionSummaryContent Enhancement**:
  - Diperbarui secara modular dengan parameter opsional `onUpdateNote` dan `onDelete` untuk mendukung mode detail riwayat secara seamless tanpa memecah kontrak penggunaan di `RepSessionScreen` dan `RunSessionScreen`.
  - Ditambahkan rendering kartu tabel split 1 km (`kmIndex`, durasi, pace) jika `summary.splits.isNotEmpty()`.
- **Navigasi ShakeFitNavHost**:
  - Tab 2 (`NavRoutes.GOALS`) dihubungkan ke `GoalsRoute`.
  - Tab 3 (`NavRoutes.ACTIVITY`) dihubungkan ke `ActivityScreen`.

### Fase UI 5 (Final): Layar Profil, Layar Pengaturan, & Poles Akhir Navigasi
- **ProfileScreen** (`com.example.shakefit.ui.screens.profile`):
  - Pola `ProfileRoute` (tipis) + `ProfileScreen` (100% stateless).
  - Izin notifikasi ulang tahun *just-in-time* via `ActivityResultContracts.RequestPermission(Manifest.permission.POST_NOTIFICATIONS)` di Android 13+ saat `state.notificationPermissionNeeded == true` dengan kartu ajakan ramah.
  - Header profil: Avatar inisial / ikon, nickname (atau "Belum diatur"), usia terhitung, dan status kelengkapan profil.
  - Kartu info ramah jika profil belum lengkap (PRD F-01): menjelaskan estimasi kalori kasar tanpa memblokir penggunaan aplikasi.
  - Formulir Profil: Nickname, pemilihan gender (`Gender.MALE`/`Gender.FEMALE`), tinggi badan (cm), berat badan (kg), tanggal lahir format `yyyy-MM-dd` dengan dialog pemilih tanggal `DatePickerDialog` Material 3, serta pesan error inline per field dari `state.fieldErrors`.
  - Tombol simpan profil -> `ProfileEvent.Save(...)`. Saat `state.saved == true`, menampilkan `Snackbar` sukses dan mengonsumsi event via `ProfileEvent.ConsumeSaved`.
  - Akses Pengaturan melalui ikon gerigi di TopAppBar.
  - Previews lengkap: Profil Lengkap, Profil Kosong, dan Error Validasi.
- **SettingsScreen** (`com.example.shakefit.ui.screens.settings`):
  - Pola `SettingsRoute` (tipis) + `SettingsScreen` (100% stateless).
  - TopAppBar dengan navigasi tombol kembali ke layar sebelumnya.
  - Bagian Sensitivitas Sensor: Slider sensitivitas gerakan Squat dan Push-up (0f..1f) dengan persentase real-time -> `SettingsEvent.SetSensitivity`.
  - Bagian Feedback & Timer: Toggle Switch Getaran (`SettingsEvent.SetVibration`), Bunyi Bip (`SettingsEvent.SetSound`), dan FilterChip durasi hitung mundur 3/5/10 detik (`SettingsEvent.SetCountdown`).
  - Bagian Kalibrasi & Developer: Tampilan read-only faktor kalibrasi treadmill (`state.treadmillStepLengthFactor`) dan tombol cepat membuka `SensorDebugRoute`.
  - Preview Default.
- **Poles Akhir ShakeFitNavHost**:
  - 100% placeholder layar (`PlaceholderScreen` & `SettingsPlaceholderScreen`) telah digantikan oleh layar riil Compose.
  - Bottom Navigation Bar (4 tab: Beranda, Target, Aktivitas, Profil) hanya aktif di 4 layar utama, dan otomatis tersembunyi pada seluruh layar full-screen (`REP_SESSION`, `RUN_SESSION`, `SETTINGS`, `SENSOR_DEBUG`).












