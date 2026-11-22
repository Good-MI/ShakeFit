# RESTRICTION — ShakeFit (Aturan Wajib untuk AI Coding Agent)

> Versi 0.2 — 20 Nov 2026 (WorkManager disetujui, LocationManager final, posisi HP terkunci). BACA FILE INI PALING PERTAMA sebelum menulis satu baris kode pun.
> Prioritas jika bertentangan: `restriction.md` > `prd.md` > `ui-contract.md` > `point.md`.
> Aturan di file ini tidak boleh diubah, dilonggarkan, atau "ditafsirkan ulang" oleh AI. Hanya user yang boleh mengubahnya.

---

## 1. Aturan Emas: BINGUNG = BERHENTI DAN BERTANYA

AI **DILARANG** menebak. Jika salah satu kondisi di bawah terjadi, AI **WAJIB BERHENTI MENULIS KODE** dan menjalankan protokol di bagian 2:

- Instruksi user ambigu, ada dua tafsiran atau lebih.
- Dua dokumen saling bertentangan.
- Ada hal yang dibutuhkan tapi tidak tertulis di dokumen (nama package, versi library, nilai ambang, perilaku edge case).
- AI butuh library, izin, atau dependency yang tidak ada di daftar yang diizinkan (bagian 4).
- Hasil uji di HP asli berbeda dari asumsi (contoh: proximity hanya biner, accelerometer terlalu noisy).
- AI tidak yakin API Android/Compose yang dipakai masih valid untuk versi terbaru.
- AI tergoda untuk "mengisi kekosongan" dengan data/logika karangan sendiri.

**Dilarang keras saat bingung:**
- Menulis kode "sementara", "kira-kira", atau "nanti diperbaiki".
- Mengarang nilai ambang sensor, koefisien kalori, atau rumus lalu menyajikannya seolah fakta.
- Diam-diam memilih salah satu opsi lalu lanjut.
- Menulis kode panjang dulu baru bertanya.
- Menyembunyikan ketidakpastian di dalam komentar kode.

## 2. Protokol Berhenti: 5 Pertanyaan Wajib

Saat bingung, AI menulis **tepat 5 pertanyaan** untuk user, memakai format ini, lalu **berhenti total** dan menunggu jawaban. Tidak ada kode baru sebelum dijawab.

```
STOP — AI BINGUNG, BUTUH JAWABAN USER

Konteks: (1-2 kalimat: sedang mengerjakan apa, bagian mana yang membingungkan)

1. [Apa yang tidak jelas?] — Opsi: A / B / (C). Rekomendasi AI: ... Alasan singkat: ...
2. [Dampak ke fitur/data mana bila salah pilih?] — ...
3. [Info apa yang kurang dan dari mana AI bisa mendapatkannya?] — ...
4. [Apakah perlu mengubah dokumen (prd/restriction/ui-contract)? Bagian mana?] — ...
5. [Apa yang AI lakukan setelah dijawab, dan apa yang sudah/belum disentuh?] — ...

Status kode: tidak ada perubahan baru sejak pertanyaan ini / file X sudah diubah sampai bagian Y.
```

Aturan tambahan protokol:
- Maksimal 5 pertanyaan. Jangan memecah jadi 20 pertanyaan kecil; gabungkan yang sejenis.
- Setiap pertanyaan wajib berisi opsi dan rekomendasi, supaya user bisa menjawab cepat ("1A, 2B...").
- Setelah user menjawab, AI mencatat keputusan ke `DECISIONS.md` (tanggal, pertanyaan, jawaban) sebelum lanjut.
- Jika jawaban user mengubah aturan di dokumen, AI mengusulkan teks revisi dokumen dan menunggu persetujuan.

## 3. Aturan Kejujuran Kode

1. **Tidak ada data sensor palsu** di jalur normal aplikasi. Data dummy hanya boleh di `@Preview`, unit test, atau layar debug yang diberi label jelas.
2. **Setiap konstanta tebakan** (ambang sensor, koefisien kalori, panjang langkah, rumus skor) WAJIB:
   - ditaruh di satu file konstanta (`Tuning.kt`),
   - diberi komentar `// ASUMSI: perlu dikalibrasi di HP asli`,
   - dilaporkan ke user di akhir tugas.
3. Tidak boleh ada `TODO()` yang membuat crash, fungsi kosong yang pura-pura sukses, atau `try/catch` yang menelan error tanpa dilaporkan ke state.
4. Jangan klaim "sudah berjalan/sudah diuji" kalau belum dijalankan di HP asli. Tulis jujur: "belum diuji di perangkat".
5. Jangan mengarang API. Kalau tidak yakin nama class/fungsi/parameter, bertanya (protokol bagian 2) atau minta user cek dokumentasi.
6. Jangan mengubah file di luar lingkup tugas. Perubahan di luar lingkup harus diminta persetujuan dulu.
7. Satu tugas = satu lingkup kecil. Selesaikan, ringkas apa yang diubah, baru lanjut. Jangan generate seluruh aplikasi sekaligus.

## 4. Kunci Teknis

### Dilarang
- **Room**, ORM, atau library database lain. Database = `SQLiteOpenHelper` murni.
- **Hilt/Dagger/Koin**. DI manual (`Application` + `ViewModelFactory`).
- **Izin INTERNET** dan semua library jaringan (Retrofit, OkHttp, dll). Aplikasi 100% lokal.
- Library analitik, iklan, crash reporter pihak ketiga.
- Google Play Services Location (FusedLocationProvider) dan library lokasi lain. **KEPUTUSAN FINAL user: lokasi memakai `LocationManager` bawaan Android.** Tidak boleh diganti tanpa bertanya.
- Step Counter / Step Detector sensor (tidak ada di Hot 70; langkah dihitung sendiri dari accelerometer).
- Operasi database atau komputasi berat di main thread.
- Hardcode string UI di ViewModel/Repository. Teks tampilan hanya di layer UI atau `strings.xml`.
- Mendesain atau mengubah tampilan UI. UI adalah tugas Gemini; AI backend hanya menyediakan state, event, dan ViewModel.

### Diizinkan tanpa bertanya
- Kotlin, Jetpack Compose (BOM bawaan template), Navigation Compose, Lifecycle ViewModel Compose, Kotlin Coroutines + Flow.
- API bawaan Android: `SensorManager`, `LocationManager`, `SQLiteOpenHelper`, `NotificationManager`, `Vibrator/VibratorManager`, `ToneGenerator`/`SoundPool`, `Service` foreground.
- JUnit untuk unit test kelas murni (`RepDetector`, `RunTracker`, util).
- **WorkManager** (`androidx.work:work-runtime-ktx`) **DISETUJUI user** khusus untuk pengecekan harian notifikasi ulang tahun (F-13). AI boleh menambahkannya sendiri ke Gradle dengan aturan bagian 4A.

### Wajib tanya dulu sebelum menambah
- Library apa pun di luar daftar "diizinkan" (WorkManager sudah termasuk daftar diizinkan).
- Mengubah `minSdk` (terkunci 26), nama package, atau struktur database.
- Mengganti `LocationManager` dengan library lokasi lain.

### 4A. Aturan Menambah Dependency (Gradle)
1. Hanya library di daftar "diizinkan" yang boleh ditambah AI sendiri. Selain itu: protokol bagian 2.
2. **Versi library tidak boleh dari ingatan AI.** Ambil dari dokumentasi resmi atau saran Android Studio / version catalog. Kalau tidak yakin, berhenti dan bertanya.
3. Gunakan `libs.versions.toml` bila project memakainya; jika tidak, tulis di `build.gradle.kts` modul app. Jangan mengubah plugin/AGP/Kotlin/Compose BOM tanpa bertanya.
4. Setelah menambah dependency, minta user menjalankan **Gradle Sync** (atau jalankan sendiri jika agent bisa) dan laporkan hasilnya jujur. Jangan klaim berhasil sebelum sync sukses.
5. Laporkan di akhir tugas: baris dependency persisnya, versinya, alasannya.
6. Tidak menambah izin `INTERNET` walau build butuh internet untuk mengunduh library (itu urusan Gradle, bukan aplikasi).

## 5. Aturan Arsitektur

1. MVVM ketat: Composable tidak menyentuh SQLite, SensorManager, atau LocationManager.
2. Alur data: `UI -> ViewModel (StateFlow<UiState>) -> Repository -> SQLite / SensorDataSource / LocationDataSource`.
3. `RepDetector`, `RunTracker`, `ScoreCalculator`, util format = **kelas Kotlin murni** (tanpa import `android.*`) agar bisa diuji unit test.
4. State sesi running hidup di komponen berlingkup aplikasi (bukan di ViewModel saja) supaya service dan UI berbagi state.
5. UiState = `data class` immutable. Event dari UI = `sealed interface`. Detail ada di `ui-contract.md` dan **tidak boleh diubah diam-diam**; perubahan kontrak harus diumumkan ke user karena Gemini bergantung padanya.
6. Semua kegagalan (sensor tidak ada, izin ditolak, GPS mati, DB gagal) menjadi nilai di state, bukan crash dan bukan `Toast` dari ViewModel.
7. Listener sensor dan lokasi **wajib dilepas** saat sesi selesai/jeda/app berhenti.

## 6. Aturan Database
- Skema mengikuti `prd.md` bagian 6. Perubahan skema = naikkan versi + tulis `onUpgrade` + beri tahu user.
- `PRAGMA foreign_keys = ON`.
- Semua query lewat DAO, memakai parameter binding (`?`), tidak ada string concat dari input user.
- Operasi DB berjalan di `Dispatchers.IO`.
- Rute GPS **tidak** disimpan (hanya ringkasan dan split).

## 7. Aturan Posisi HP (TERKUNCI — disetujui user 20 Nov 2026)
- **Squat**: saku depan celana, HP tegak, layar menghadap badan. Tidak dipegang.
- **Push-up**: HP rata di lantai, layar menghadap atas, bagian atas HP (dekat sensor proximity) tepat di bawah dada.
- **Running**: saku depan celana atau armband. Tidak digenggam.

Algoritma deteksi boleh dan harus dibangun berdasarkan posisi ini. AI **tidak boleh mengubah** posisi ini. Jika uji di HP asli menunjukkan posisi tidak berhasil, AI berhenti dan bertanya (protokol bagian 2), bukan mengganti posisi sendiri.

## 8. Risiko yang Wajib Dilaporkan, Bukan Disembunyikan
- **Proximity push-up**: kalau di HP asli ternyata biner/jangkauan terlalu pendek, AI berhenti dan bertanya (opsi fallback accelerometer diputuskan user).
- **Background kill oleh XOS**: kalau tracking running mati saat layar off, laporkan, jangan tambal diam-diam.
- **Akurasi treadmill**: selalu ditandai `STEPS_ESTIMATE` di data dan state.
- **Akurasi GPS**: lokasi buruk dibuang dan statusnya dikirim ke UI.

## 9. Format Laporan Setelah Setiap Tugas
```
SELESAI: (apa yang dibuat, 2-4 kalimat)
FILE DIUBAH: daftar file
ASUMSI BARU: daftar konstanta/keputusan tebakan (atau "tidak ada")
BELUM DIUJI DI HP ASLI: daftar (atau "semua sudah diuji")
RISIKO/CATATAN: ...
LANGKAH BERIKUTNYA YANG DIUSULKAN: ...
```

## 10. Checklist Mandiri AI Sebelum Mengirim Kode
- [ ] Sudah membaca `restriction.md`, `prd.md`, `ui-contract.md`.
- [ ] Tidak ada library/izin di luar daftar diizinkan; dependency baru ikut aturan 4A (versi dari sumber resmi, bukan ingatan).
- [ ] Tidak ada operasi berat di main thread.
- [ ] Listener sensor/lokasi dilepas dengan benar.
- [ ] Semua konstanta tebakan ada di `Tuning.kt` dan ditandai `ASUMSI`.
- [ ] Error menjadi state, bukan crash.
- [ ] Tidak menyentuh UI desain milik Gemini.
- [ ] Laporan format bagian 9 sudah disiapkan.
- [ ] Kalau ada keraguan sekecil apa pun: berhenti, jalankan bagian 2.
