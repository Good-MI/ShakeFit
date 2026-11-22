# ShakeFit

ShakeFit adalah aplikasi olahraga Android yang berjalan 100% offline. Aplikasi ini memanfaatkan sensor bawaan HP untuk menghitung gerakan **Squat**, **Push-up**, dan melacak **Lari** secara otomatis tanpa perlu alat tambahan (*smartwatch*) atau koneksi internet.

---

## 1. Fitur Aplikasi

- **Hitung Squat Otomatis**: Taruh HP di saku celana depan, aplikasi otomatis menghitung setiap gerakan jongkok.
- **Hitung Push-up Otomatis**: Taruh HP di lantai tepat di bawah dada, aplikasi menghitung setiap kali dada mendekati layar.
- **Pelacak Lari (Outdoor & Treadmill)**:
  - **Outdoor**: Melacak jarak, rute, dan kecepatan (*pace*) pakai GPS.
  - **Treadmill**: Menghitung langkah dan estimasi jarak di dalam ruangan.
  - Tetap berjalan di latar belakang meski layar HP dimatikan.
- **Target Harian**: Buat dan pantau target olahraga harian untuk Squat, Push-up, dan Lari.
- **Riwayat & Statistik**: Menyimpan catatan latihan (durasi, repetisi, kalori), grafik 7 hari terakhir, *streak* harian, rekor pribadi, dan lencana (*badges*).
- **Suara & Getaran**: Memberikan panduan suara hitungan (*Text-to-Speech*), bunyi *beep*, dan getaran setiap repetisi terhitung.
- **Profil & Tes Sensor**: Pengaturan berat/tinggi badan untuk hitung kalori, notifikasi ulang tahun, dan menu khusus untuk mengecek sensor HP secara langsung.

---

## 2. Teknologi dan Arsitektur

- **Bahasa Pemrograman**: Kotlin
- **Tampilan (UI)**: Jetpack Compose dengan Material 3
- **Arsitektur**: MVI (*Unidirectional Data Flow*) dan *Single-Activity* agar alur data rapi dan mudah dirawat.
- **Database**: SQLite bawaan Android (tanpa library pihak ketiga, lebih ringan dan 100% offline).
- **Proses Latar Belakang**:
  - *Foreground Service* (menjaga pelacakan lari tetap aktif saat layar mati).
  - *WorkManager* (pengingat ulang tahun otomatis).
- **Konfigurasi SDK**: Minimum Android 8.0 (SDK 26), Target SDK 37.

---

## 3. Sensor yang Digunakan

| Sensor | Fitur | Fungsi |
| :--- | :--- | :--- |
| **Accelerometer** | Squat | Mendeteksi sudut kemiringan paha dan gerakan naik-turun saat HP di saku (dilengkapi jeda 1,2 detik agar tidak terhitung ganda). |
| | Lari (Treadmill) | Menghitung jumlah langkah kaki dan memperkirakan jarak tempuh. |
| | Push-up | Memastikan HP tetap diam stabil di lantai agar hitungan tidak kacau. |
| **Proximity** | Push-up | Mendeteksi jarak dada saat mendekati dan menjauhi layar HP. |
| **Gyroscope** *(Opsional)* | Squat | Membantu memastikan gerakan jongkok lebih akurat. Jika HP tidak punya sensor ini, aplikasi otomatis memakai Accelerometer saja. |
| **GPS** | Lari Outdoor | Mengukur jarak tempuh, rute, dan kecepatan lari secara akurat. |

---

## 4. Cara Menjalankan Aplikasi

1. **Download / Clone Proyek**:
   ```bash
   git clone https://github.com/Good-MI/ShakeFit.git
   cd ShakeFit
   ```
2. **Buka di Android Studio**:
   - Pilih **File > Open**, lalu buka folder `ShakeFit`.
   - Tunggu sampai proses *Gradle Sync* selesai.
3. **Sambungkan HP Android**:
   - Aktifkan **USB Debugging** di HP (minimal Android 8.0) lalu hubungkan ke laptop pakai kabel USB.
   - *(Disarankan pakai HP fisik agar sensor bisa dites langsung).*
4. **Jalankan Aplikasi**:
   - Klik tombol **Run (▶)** di Android Studio.
   - Izinkan akses **Lokasi** dan **Notifikasi** saat aplikasi pertama kali dibuka.

---

## 5. Cara Menggunakan Aplikasi

1. **Isi Profil**: Buka tab **Profil**, masukkan tinggi dan berat badan, lalu simpan agar hitungan kalori lebih akurat.
2. **Atur Target (Opsional)**: Buka tab **Target** untuk menambah target harian Squat, Push-up, atau Lari.
3. **Mulai Latihan**: Buka tab **Beranda**, pilih jenis olahraga:
   - **Squat**: Tekan mulai, masukkan HP ke saku celana depan, lalu mulai jongkok.
   - **Push-up**: Taruh HP di lantai bawah dada, tekan mulai, lalu lakukan push-up.
   - **Lari**: Pilih mode **Outdoor** (nyalakan GPS) atau **Treadmill**, lalu tekan mulai.
4. **Simpan & Lihat Hasil**: Setelah selesai, tekan tombol **Selesai**. Semua catatan dan grafik perkembangan bisa langsung dilihat di tab **Aktivitas**.

---

## 6. Screenshot Tampilan Aplikasi

*(Hapus baris ini, lalu drag & drop gambar screenshot dari HP langsung ke sini saat edit di GitHub)*
