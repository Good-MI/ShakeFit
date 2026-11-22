# PRD — ShakeFit (Backend & Fitur)

> Versi 0.2 — 20 Nov 2026. Fokus: fitur, sistem, data, perilaku. **Bukan** dokumen UI/UX (itu `design.md` oleh Gemini).
> Dokumen terkait: `restriction.md` (aturan AI, WAJIB dibaca dulu), `ui-contract.md` (kontrak state/event untuk UI), `gemini-ui-prompt.md` (prompt UI), `point.md` (riwayat diskusi).
> Prioritas jika bertentangan: `restriction.md` > `prd.md` > `ui-contract.md` > `point.md`.
> Perubahan 0.2: posisi HP TERKUNCI, WorkManager disetujui, LocationManager bawaan Android final, keputusan terbuka ditutup (sisa: package/bahasa dan nickname).

---

## 1. Ringkasan Produk

**ShakeFit** adalah aplikasi Android (Kotlin, Jetpack Compose, MVVM, SQLite) yang menghitung latihan otomatis memakai sensor HP:
**squat** dan **push-up** (hitung repetisi) serta **running** (jarak, pace, cadence). Semua data disimpan lokal di SQLite.

Tujuan tugas: aplikasi yang fungsinya benar-benar jalan (bukan hanya tampilan), memenuhi ketentuan:
Compose, MVVM, SQLite, minimal 2 sensor Android (Motion / Position / Environment).

## 2. Perangkat Target

- Infinix Hot 70 (varian biasa/4G), Android 16 menurut spesifikasi web. Sensor yang tercatat: light, proximity, accelerometer, compass, gyroscope.
- **minSdk 26**. compileSdk/targetSdk mengikuti default template Android Studio terbaru (tidak dikunci di dokumen ini).
- Semua uji akhir dilakukan di HP asli.
- Tidak ada Step Counter/Step Detector sensor di daftar spesifikasi, jadi **langkah dihitung sendiri dari accelerometer**.

## 3. Pemenuhan Ketentuan Sensor

| Sensor | Kategori | Dipakai untuk | Status |
|---|---|---|---|
| Accelerometer | Motion | Squat, push-up (pendukung), cadence running, langkah treadmill | WAJIB |
| Proximity | Position | Push-up (siklus dekat-jauh) | WAJIB |
| Gyroscope | Motion | Validasi squat (kurangi hitungan palsu) | OPSIONAL, ada fallback tanpa gyro |

Minimal 2 sensor terpenuhi oleh **accelerometer (Motion) + proximity (Position)**.
GPS/Location **bukan** sensor dari 3 kategori tugas; dipakai sebagai sumber jarak, bukan untuk memenuhi syarat.

## 4. Daftar Fitur

Prioritas: **M** = wajib MVP, **S** = sebaiknya, **C** = jika waktu ada.

### F-01 Profil Pengguna (M)
- Data: nickname (opsional), gender (MALE/FEMALE), tinggi (cm), berat (kg), tanggal lahir.
- Satu profil per instalasi (tabel satu baris). Operasi: buat, ubah, lihat. Validasi rentang wajar (tinggi, berat) dan tanggal lahir tidak di masa depan.
- Dipakai untuk: estimasi kalori, estimasi panjang langkah (treadmill), notifikasi ulang tahun.
- Profil belum diisi: aplikasi tetap jalan; kalori dan treadmill memakai nilai default yang ditandai "perkiraan kasar".
- Acceptance: data profil bertahan setelah app ditutup; validasi menolak input tidak valid dengan pesan error di state.

### F-02 Katalog Latihan (M)
- Tiga latihan di-seed saat database dibuat: SQUAT, PUSHUP, RUNNING.
- Setiap latihan punya status ketersediaan sensor (cek `getDefaultSensor() != null`), dikirim ke UI beserta alasannya bila tidak tersedia.

### F-03 Mesin Sesi Latihan (M)
State: `IDLE -> COUNTDOWN -> ACTIVE <-> PAUSED -> FINISHED`.
- Countdown sebelum mulai (durasi bisa diatur di pengaturan) agar user sempat menaruh HP sesuai posisi terkunci (bagian 8).
- Satu sesi aktif pada satu waktu.
- Saat FINISHED, sesi disimpan otomatis ke SQLite, lalu ringkasan sesi dikirim ke UI.
- Sesi yang terputus (app dimatikan paksa) tidak merusak database; sesi tak selesai boleh dibuang (tidak wajib dipulihkan di MVP).
- Layar dijaga menyala selama sesi repetisi.

### F-04 Penghitung Squat (M)
- Sensor: accelerometer; gyroscope opsional sebagai validator.
- Posisi HP: saku depan celana, tegak (terkunci, bagian 8).
- Pipeline: data mentah -> pisahkan gravitasi/gerak -> fitur gerak vertikal -> deteksi puncak dengan hysteresis dan jeda minimum antar rep -> hitung 1 rep.
- Sensitivitas bisa diatur (kalibrasi, F-08).
- Output per rep: nomor rep, waktu rep, interval dari rep sebelumnya.

### F-05 Penghitung Push-up (M)
- Sensor: proximity (utama), accelerometer (pendukung).
- Posisi HP: rata di lantai, layar menghadap atas, bagian atas HP tepat di bawah dada (terkunci, bagian 8).
- 1 rep = satu siklus dekat -> jauh -> dekat (atau sebaliknya sesuai pengamatan HP).
- **RISIKO:** sensor proximity HP kelas ini bisa hanya memberi nilai dekat/jauh (biner) dengan jangkauan sangat pendek. WAJIB diuji di Hot 70 sebelum algoritma final. Jika gagal, fallback ke accelerometer (diputuskan bersama user, bukan diam-diam oleh AI). Posisi HP tidak boleh diubah AI sendiri.

### F-06 Pelacak Running (M)
- Mode lingkungan: **OUTDOOR** (GPS) dan **TREADMILL** (tanpa GPS).
- Posisi HP: saku depan celana atau armband, tidak digenggam (terkunci, bagian 8).
- Metrik: durasi, jarak (km), **pace saat ini**, **pace rata-rata**, **split per km**, **cadence** (langkah/menit), estimasi kalori.
- Pace disimpan dan dihitung dalam **detik per km**; format menit:detik per km dilakukan lewat util format (lihat `ui-contract.md`).
- OUTDOOR: jarak dari rangkaian lokasi GPS lewat **`LocationManager` bawaan Android (keputusan final)**. Titik rute **tidak disimpan** (hanya ringkasan). Lokasi dengan akurasi buruk atau lonjakan kecepatan tidak masuk akal dibuang (ambang bisa diatur, nilai awal ditentukan saat uji lapangan).
- Pace saat ini memakai jendela waktu bergulir (panjang jendela bisa diatur; nilai awal disetel saat uji lapangan).
- TREADMILL: jarak = jumlah langkah x panjang langkah. Langkah dihitung dari accelerometer; panjang langkah diturunkan dari tinggi user dengan koefisien yang **bisa dikalibrasi** user (misalnya memasukkan jarak aktual treadmill setelah sesi). Akurasi lebih rendah dari GPS dan UI harus diberi tahu (flag `distanceSource`).
- Split per km: dihitung saat jarak melewati kelipatan 1 km, ditampilkan langsung dan disimpan ringkas.
- Status GPS (mencari sinyal, siap, lemah, izin ditolak, GPS mati) dikirim ke UI.
- Auto-pause: tidak wajib (C).

### F-07 Tracking Background untuk Running (S, keputusan: dikerjakan karena kesulitan sedang)
Jawaban atas pertanyaan user: **bisa**, tingkat kesulitan **sedang**, bukan sulit.
- Foreground service bertipe location, dijalankan hanya saat user menekan mulai dengan app terlihat.
- Butuh: izin lokasi (runtime), izin notifikasi (Android 13+), izin foreground service bertipe location (Android 14+).
- Notifikasi persisten menampilkan durasi dan jarak dengan aksi jeda/selesai.
- State sesi hidup di komponen berlingkup aplikasi yang dipakai bersama service dan ViewModel, bukan di ViewModel saja, supaya sesi tidak mati saat UI ditutup.
- **RISIKO:** beberapa skin Android (termasuk XOS) bisa mematikan app di background secara agresif. Wajib diuji di Hot 70; kemungkinan user harus mematikan optimasi baterai untuk app ini.
- Sesi squat/push-up tidak memakai service (layar dijaga menyala).

### F-08 Pengaturan dan Kalibrasi (M)
- Sensitivitas deteksi rep (per latihan), getar on/off, bunyi on/off, durasi countdown.
- Kalibrasi koefisien langkah treadmill.
- Disimpan di tabel `settings`.

### F-09 Riwayat (M)
- Daftar sesi (terbaru dulu), filter per latihan.
- Detail sesi (termasuk detail running dan split).
- **Edit catatan sesi** dan **hapus sesi** (CRUD lengkap).

### F-10 Target Harian (M)
- Target per latihan: repetisi (squat, push-up) atau km (running).
- Tambah, ubah, hapus, aktif/nonaktif.
- Progres hari ini dihitung dari sesi hari itu.

### F-11 Statistik (M)
- Total sesi, total rep, total km, rekor, ringkasan 7 hari terakhir. Semua dihitung dari SQLite.

### F-12 Fitur Kreatif
| ID | Fitur | Prioritas | Aturan |
|---|---|---|---|
| F-12a | Skor kualitas gerakan (0-100) dari konsistensi interval antar rep (variasi rendah = skor tinggi). Rumus final disetel saat implementasi; AI wajib menandai konstanta sebagai asumsi | S | Hanya untuk squat dan push-up |
| F-12b | Feedback getar dan bunyi tiap rep; getar khusus saat target tercapai | S | Bisa dimatikan di pengaturan |
| F-12c | Streak harian (hari berturut-turut ada minimal satu sesi tersimpan) dan badge | S | Badge awal: FIRST_SESSION, FIRST_10_REPS, STREAK_3, STREAK_7, REPS_100_DAY, RUN_1KM, RUN_5KM |
| F-12d | Mode Tantangan 60 detik (rep terbanyak, rekor diambil dari sesi bermode tantangan) | C | Squat dan push-up |
| F-12e | Estimasi kalori dari berat user (running: berdasarkan jarak dan berat; rep: kalori per rep). Koefisien final disetel saat implementasi dan ditandai sebagai perkiraan | S | Selalu ditampilkan sebagai "estimasi" |

Urutan buang jika waktu mepet: F-12d -> F-12e -> F-12c -> F-12b -> F-12a.

### F-13 Notifikasi Ulang Tahun (S)
- Pada tanggal lahir di profil, tampil notifikasi ucapan **Happy Birthday** dengan **teks panjang** (gaya notifikasi teks diperluas).
- Teks dibuat lokal dari beberapa template (tanpa internet), memakai nickname bila ada.
- Hanya sekali per tahun (catat tahun terakhir notifikasi dikirim).
- Pemicu: pengecekan harian terjadwal memakai **WorkManager (DISETUJUI user; AI boleh menambah dependency sendiri sesuai `restriction.md` bagian 4A)** ditambah pengecekan saat app dibuka sebagai cadangan.
- Catatan: WorkManager tidak menjamin waktu tepat (Doze / optimasi baterai XOS), jadi notifikasi bisa agak telat dalam hari yang sama. Ini dapat diterima.
- Butuh izin notifikasi (Android 13+); jika ditolak, app tetap jalan dan state memberi tahu UI.
- Tanggal lahir 29 Februari: dirayakan 28 Februari pada tahun bukan kabisat (aturan perilaku, bisa diubah user).

### F-14 Izin dan Penanganan Error (M)
- Izin diminta saat dibutuhkan, bukan saat pertama kali app dibuka.
- Semua kegagalan (sensor tidak ada, izin ditolak, GPS mati, database gagal) menjadi state yang bisa ditampilkan UI, bukan crash.
- Listener sensor WAJIB dilepas saat sesi selesai, jeda, atau app berhenti.

## 5. Izin Android (rencana)

| Izin | Untuk | Catatan |
|---|---|---|
| ACCESS_FINE_LOCATION (+ COARSE) | GPS running | Runtime |
| FOREGROUND_SERVICE | Service running | Normal |
| FOREGROUND_SERVICE_LOCATION | Service bertipe location | Android 14+ |
| POST_NOTIFICATIONS | Notifikasi service dan ulang tahun | Runtime Android 13+ |
| VIBRATE | Getar | Normal |

Tidak ada izin INTERNET. (Gradle butuh internet saat build untuk mengunduh library; itu tidak berarti aplikasi butuh izin INTERNET.)

## 6. Model Data (SQLite via SQLiteOpenHelper)

```
user_profile(id INTEGER PK CHECK(id=1), nickname TEXT, gender TEXT CHECK IN('MALE','FEMALE'),
             height_cm REAL, weight_kg REAL, birth_date TEXT /*yyyy-MM-dd*/, last_birthday_notified_year INTEGER)

exercises(id INTEGER PK, code TEXT UNIQUE /*SQUAT|PUSHUP|RUNNING*/, name TEXT, kind TEXT /*REPS|RUN*/,
          calories_per_rep REAL, is_active INTEGER)

workout_sessions(id INTEGER PK AUTOINCREMENT, exercise_id INTEGER FK->exercises, mode TEXT /*FREE|CHALLENGE_60S|GOAL*/,
                 started_at INTEGER /*epoch ms*/, duration_sec INTEGER, reps INTEGER NULL,
                 avg_tempo_ms INTEGER NULL, quality_score INTEGER NULL, calories_est REAL, note TEXT)

run_details(session_id INTEGER PK FK->workout_sessions ON DELETE CASCADE, environment TEXT /*OUTDOOR|TREADMILL*/,
            distance_m REAL, avg_pace_sec_per_km INTEGER, best_pace_sec_per_km INTEGER,
            avg_cadence_spm INTEGER, distance_source TEXT /*GPS|STEPS_ESTIMATE*/)

run_splits(id INTEGER PK AUTOINCREMENT, session_id INTEGER FK->workout_sessions ON DELETE CASCADE,
           km_index INTEGER, duration_sec INTEGER, pace_sec_per_km INTEGER)

daily_goals(id INTEGER PK AUTOINCREMENT, exercise_id INTEGER FK->exercises, target_value REAL,
            unit TEXT /*REPS|KM*/, active INTEGER)

badges(id INTEGER PK, code TEXT UNIQUE, unlocked_at INTEGER NULL)

settings(key TEXT PK, value TEXT)
```
- Foreign key diaktifkan. Versi skema dikelola lewat `onCreate`/`onUpgrade`.
- Tabel `run_splits` diisi ringkasan per km, **bukan** titik rute.

## 7. Arsitektur (ringkas)

MVVM: `UI (Compose) -> ViewModel (StateFlow<UiState>) -> Repository -> SQLite / SensorDataSource / LocationDataSource`.
- `LocationDataSource` membungkus `LocationManager` bawaan Android (tanpa library lokasi tambahan).
- `RepDetector` per latihan (kelas Kotlin murni, bisa diuji tanpa Android).
- `RunTracker` (jarak, pace, split, cadence) kelas murni yang menerima data lokasi dan langkah.
- Service running dan ViewModel berbagi state lewat komponen berlingkup aplikasi.
- DI manual (tanpa Hilt). Detail aturan ada di `restriction.md`.

## 8. Kunci Posisi HP — **TERKUNCI** (disetujui user, 20 Nov 2026)

| Latihan | Posisi HP |
|---|---|
| Squat | Saku depan celana, HP tegak, layar menghadap tubuh. Tidak dipegang |
| Push-up | HP rata di lantai, layar menghadap atas, bagian atas HP (dekat sensor proximity) tepat di bawah dada |
| Running | Saku depan celana atau armband. Tidak digenggam |

AI tidak boleh mengubah posisi ini tanpa bertanya ke user. Jika uji di HP asli menunjukkan posisi tidak bekerja, AI berhenti dan menjalankan protokol 5 pertanyaan di `restriction.md`.

## 9. Persyaratan Non-Fungsional
- Semua data lokal, tanpa akun dan tanpa internet.
- Operasi database tidak di main thread.
- Listener sensor dan lokasi dilepas saat tidak dipakai (hemat baterai).
- Tidak ada data sensor palsu di jalur normal aplikasi.
- Tidak crash saat izin ditolak, sensor tidak ada, atau GPS mati.

## 10. Rencana Pengerjaan (fase)
1. Setup project, struktur paket, navigasi dasar, `DbHelper` + seed.
2. Layar uji sensor mentah di HP asli (accelerometer, proximity, gyro) dan uji GPS.
3. `RepDetector` squat, lalu push-up (iterasi di HP asli).
4. `RunTracker` outdoor, lalu treadmill.
5. Mesin sesi + penyimpanan + riwayat + target + statistik.
6. Profil + notifikasi ulang tahun (tambah WorkManager di fase ini).
7. Foreground service running.
8. Fitur kreatif sesuai prioritas.
9. Uji menyeluruh di HP asli.

## 11. Kriteria Selesai (Definition of Done) per Fitur
- Berjalan di Infinix Hot 70 asli (bukan hanya emulator).
- Data tersimpan dan terbaca benar setelah app ditutup-dibuka.
- Kasus gagal (izin, sensor, GPS) tidak membuat crash.
- Mengikuti `restriction.md` dan kontrak `ui-contract.md`.

## 12. Rencana Masa Depan (di luar MVP)
- Simpan dan tampilkan rute running di peta (butuh tabel titik rute dan layanan peta).
- Latihan tambahan: sit-up (gyroscope), jumping jack.
- Ekspor riwayat ke CSV.
- Auto-pause running.
- Widget ringkasan harian.
- Mode gelap/terang otomatis mengikuti sensor cahaya (sensor Environment).
- Opsi lokasi lebih akurat (Google Play Services Location) bila uji lapangan menunjukkan `LocationManager` kurang memadai (butuh persetujuan user).

## 13. Keputusan

### Sudah Final
| # | Keputusan | Tanggal |
|---|---|---|
| 1 | Kunci posisi HP (bagian 8) disetujui dan TERKUNCI | 20 Nov 2026 |
| 2 | WorkManager boleh ditambah AI otomatis (khusus notifikasi ulang tahun) | 20 Nov 2026 |
| 3 | Penyimpanan ringkasan saja, tanpa titik rute | 20 Nov 2026 |
| 4 | Lokasi memakai `LocationManager` bawaan Android | 20 Nov 2026 |

### Masih Terbuka
1. Field nickname di profil: ditambahkan untuk ucapan ulang tahun (diasumsikan boleh; belum dikonfirmasi eksplisit).
2. Nama package dan bahasa teks UI belum ditentukan (placeholder: `com.example.shakefit`, bahasa Indonesia).
