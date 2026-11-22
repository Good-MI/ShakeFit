# ShakeFit

ShakeFit adalah aplikasi pelacak kebugaran dan latihan beban tubuh (*bodyweight workout*) berbasis Android yang beroperasi 100% luring (*offline-first*). Aplikasi ini memanfaatkan sensor bawaan ponsel pintar (*accelerometer*, *proximity*, *gyroscope*, dan *GPS*) untuk menghitung repetisi latihan secara otomatis dan melacak aktivitas lari tanpa memerlukan perangkat *wearable* tambahan maupun koneksi internet.

---

## 1. Fitur-Fitur Aplikasi

* **Pelacak Repetisi Squat Otomatis**: Menghitung repetisi jongkok secara akurat dengan ponsel diletakkan di saku celana depan menggunakan kombinasi sudut kemiringan paha (*tilt angle*) dan akselerasi vertikal.
* **Pelacak Repetisi Push-up Otomatis**: Menghitung repetisi *push-up* saat ponsel diletakkan di lantai tepat di bawah dada dengan memanfaatkan sensor kedekatan (*proximity*).
* **Pelacak Lari (Outdoor & Treadmill)**:
  * **Outdoor**: Pelacakan jarak, rute, kecepatan (*pace* menit/km), dan *split* per kilometer menggunakan GPS dengan filter akurasi tinggi.
  * **Treadmill (Indoor)**: Pelacakan langkah berbasis *step detector* akselerometer dengan faktor kalibrasi panjang langkah personal.
  * **Layanan Latar Belakang (*Foreground Service*)**: Pelacakan lari tetap aktif saat layar mati atau aplikasi diminimalkan.
* **Target Latihan Harian (Goals)**: Pembuatan, pembaruan, dan pemantauan target harian untuk Squat, Push-up, dan Lari dengan visualisasi progres interaktif.
* **Riwayat Latihan Komprehensif (History)**: Pencatatan detail setiap sesi latihan (waktu, durasi, repetisi/jarak, estimasi kalori, ritme tempo, serta catatan pribadi) yang dapat difilter per jenis latihan.
* **Statistik & Gamifikasi (Stats & Badges)**:
  * Ringkasan total latihan, rekor pribadi (*personal records*), dan grafik tren aktivitas 7 hari terakhir berbasis Canvas kustom.
  * Perhitungan *streak* latihan harian dan galeri lencana penghargaan (*badges*).
* **Umpan Balik Multi-Sensoris**: Panduan suara hitungan/milestone (*Text-To-Speech*), *beep sound*, serta getaran (*haptic feedback*) saat repetisi terhitung dan target tercapai.
* **Profil Pengguna & Notifikasi Ulang Tahun**: Personalisasi data fisik (berat dan tinggi badan) untuk perhitungan kalori akurat, serta pengingat ulang tahun otomatis berbasis *WorkManager*.
* **Alat Diagnostik Sensor (Sensor Debug)**: Layar pemantau nilai mentah sensor secara *real-time* untuk mempermudah pengujian hardware ponsel.

---

## 2. Teknologi dan Arsitektur yang Digunakan

* **Bahasa Pemrograman**: Kotlin 2.2.10 murni
* **UI Toolkit**: Jetpack Compose dengan Material 3 (berbasis Compose BOM)
* **Pola Arsitektur**:
  * **MVI / UDF (Unidirectional Data Flow)**: Pemisahan tegas antara State, Event, dan Side Effects.
  * **Thin Route & Stateless Screen**: Layar UI sepenuhnya *stateless* dan modular, dihubungkan via *Route Composable* ke ViewModel.
  * **Single-Activity Architecture**: Navigasi multi-layar dikelola oleh `ShakeFitNavHost` dengan AndroidX Navigation Compose.
* **Penyimpanan Data (Database)**: SQLite murni menggunakan `SQLiteOpenHelper` bawaan Android tanpa ORM/Room pihak ketiga, menjamin performa cepat dan ukuran aplikasi ringan.
* **Dependency Injection**: *Manual Dependency Injection* terpusat melalui `AppContainer` di kelas `ShakeFitApplication`.
* **Asinkron & Reaktivitas**: Kotlin Coroutines (`StateFlow`, `SharedFlow`, `Flow`).
* **Background Processing**:
  * Android Foreground Service (`RunningForegroundService`) dengan integrasi *Notification & WakeLock*.
  * AndroidX WorkManager (`BirthdayNotificationWorker`) untuk tugas terjadwal.
* **Build System & Konfigurasi SDK**:
  * Android Gradle Plugin (AGP): `9.3.3`
  * Gradle: `9.5.0`
  * Minimum SDK: `26` (Android 8.0 Oreo)
  * Target & Compile SDK: `37` (Android 15+)
  * Java Version: OpenJDK 11 / 25 kompatibel

---

## 3. Sensor yang Digunakan beserta Fungsinya

| Sensor | Peran & Fitur Terkait | Implementasi Teknis |
| :--- | :--- | :--- |
| **Accelerometer** (`Sensor.TYPE_ACCELEROMETER`) | **Squat** | Menerapkan *Exponential Moving Average (EMA)* dengan koefisien $\alpha = 0.2$ untuk meredam *jitter* di saku. Menghitung sudut kemiringan paha (*thigh tilt*) terhadap proyeksi gravitasi dan mendeteksi siklus gerak penuh melalui *finite state machine* (`STANDING` $\rightarrow$ `DESCENDING` $\rightarrow$ `BOTTOM` $\rightarrow$ `ASCENDING` $\rightarrow$ `STANDING`). Dilengkapi *debounce* 1200 ms untuk mencegah *overcounting*. |
| | **Lari (Treadmill & Cadence)** | Mendeteksi entakan langkah kaki menggunakan algoritma *peak-to-peak threshold*, menghitung kadens langkah per menit (*cadence* SPM), dan mengestimasi jarak tempuh. |
| | **Push-up (Stabilitas Lantai)** | Memantau kestabilan posisi ponsel di lantai. Jika ponsel terbentur atau bergeser secara tidak wajar (> 4.0 m/s²), repetisi akan dibatalkan untuk menghindari manipulasi sensor. |
| **Proximity Sensor** (`Sensor.TYPE_PROXIMITY`) | **Push-up** | Membaca jarak objek di atas layar ponsel. Siklus repetisi dihitung berdasarkan transisi jarak (*FAR* $\rightarrow$ *NEAR* $\rightarrow$ *FAR*) saat dada mendekat ke layar (< 3.0 cm) dengan validasi kestabilan waktu. |
| **Gyroscope** (`Sensor.TYPE_GYROSCOPE`) *(Opsional)* | **Squat** | Bertindak sebagai validator sekunder kecepatan sudut rotasi paha ($\ge 0.25$ rad/s). Jika ponsel tidak memiliki gyroscope, sistem secara otomatis beralih (*fallback*) ke accelerometer penuh tanpa kendala. |
| **GPS / Lokasi** (`LocationManager`) | **Lari Outdoor** | Mengukur perpindahan titik koordinat geografis (*latitude/longitude*) secara presisi menggunakan formula *Haversine*, menerapkan filter akurasi (< 25 m), serta menyaring lonjakan kecepatan (*speed filter* < 15 m/s). |

---

## 4. Cara Menjalankan Aplikasi

### Prasyarat:
* Komputer dengan OS Windows, macOS, atau Linux.
* **Android Studio Ladybug (2024.2.1)** atau versi yang lebih baru.
* Android SDK Platform 37 dan JDK 11 atau lebih baru telah terpasang.
* Perangkat fisik Android dengan OS minimal Android 8.0 (Oreo) yang memiliki fitur Accelerometer (direkomendasikan HP fisik agar sensor gerak dan lokasi dapat diuji langsung).

### Langkah-Langkah:

1. **Clone Repositori**:
   ```bash
   git clone <URL_REPOSITORY_ANDA>
   cd ShakeFit
   ```

2. **Buka Proyek di Android Studio**:
   * Jalankan Android Studio.
   * Pilih menu **File** > **Open...** lalu arahkan ke direktori proyek `ShakeFit`.
   * Tunggu proses *Gradle Sync* dan pengunduhan dependensi selesai.

3. **Kompilasi & Jalankan Unit Test (Opsional)**:
   Untuk memverifikasi logika detektor sensor dan kalkulasi:
   * Melalui terminal:
     ```bash
     ./gradlew testDebugUnitTest
     ```
     *(Pada Windows: `.\gradlew.bat testDebugUnitTest`)*

4. **Hubungkan Perangkat Fisik**:
   * Aktifkan **Developer Options** dan **USB Debugging** pada ponsel Android Anda.
   * Hubungkan ponsel ke komputer menggunakan kabel data USB.
   * Pastikan nama perangkat Anda terdeteksi pada daftar target perangkat di toolbar Android Studio.

5. **Build dan Jalankan Aplikasi**:
   * Klik tombol hijau **Run 'app'** (`Shift + F10`) di Android Studio.
   * Aplikasi akan dikompilasi, dipasang (*installed*), dan langsung terbuka di ponsel fisik Anda.
   * Berikan izin runtime (Notifikasi dan Lokasi) saat diminta agar seluruh fitur pelacakan berjalan optimal.
