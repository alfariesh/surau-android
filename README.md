# Surau

**Surau** adalah aplikasi Android untuk membaca Al-Qur'an dan (segera) kitab-kitab Islam,
dibangun sepenuhnya dengan Kotlin dan Jetpack Compose di atas fondasi arsitektur
[Now in Android](https://github.com/android/nowinandroid) (Apache 2.0).

Backend tersedia di `https://api.surau.org` (base path `/v1`).

## Fitur (Milestone 1)

- **Quran reader** — 114 surah dengan teks Hafs resmi KFGQPC (font Uthmanic HAFS dibundel),
  terjemahan Indonesia (Kemenag, dapat diganti), tiga mode baca (Arab & Terjemahan /
  Terjemahan / Arab), ukuran huruf Arab dapat diatur, daftar Juz, dan pencarian server-side.
- **Guest-first** — Quran dapat dibaca tanpa akun. Posisi baca tersimpan lokal (offline-first,
  Room sebagai source of truth) dan otomatis tergabung ke akun saat login (yang terbaru menang).
- **Akun** — daftar, verifikasi email via OTP, login (JWT access 15 menit + refresh token
  berrotasi dengan single-flight refresh), lupa/atur ulang kata sandi, logout.
- **Material 3 Expressive** — tema emerald & gold, `MaterialExpressiveTheme` + motion scheme
  expressive, komponen expressive (LoadingIndicator, ButtonGroup, dsb.) terisolasi di
  `core:designsystem`.

## Arsitektur

Mengikuti pola Now in Android:

- **Modularisasi** `core:*` / `feature:*` dengan split `api`/`impl` per fitur dan
  convention plugins di `build-logic` (plugin id `surau.*`).
- **UI** Jetpack Compose + Navigation3 (abstraksi `core:navigation`), ViewModel +
  `StateFlow<UiState>` (sealed interface).
- **Data** offline-first: Room (`core:database`) sebagai source of truth, Proto DataStore untuk
  preferensi & sesi (token tidak ikut backup), Retrofit + kotlinx-serialization
  (`core:network`) dengan klien publik (HTTP cache, ETag) dan klien terautentikasi
  (Bearer + refresh on 401).
- **DI** Hilt; **sync** WorkManager merekonsiliasi posisi baca saat app start.

## Menjalankan

Prasyarat: JDK 17+ (test screenshot/Robolectric butuh JDK 21+, repo ini dikembangkan dengan
Temurin 25), Android SDK (compileSdk 37).

```sh
./gradlew :app:installDebug      # build & install (default backend: https://api.surau.org/v1/)
./gradlew testDebugUnitTest      # unit + screenshot test (Roborazzi)
./gradlew recordRoborazziDebug   # rekam ulang golden screenshot
```

Backend dapat dialihkan via `local.properties`:

```properties
BACKEND_URL=http://10.0.2.2:8080/v1/
```

## Lisensi & atribusi

- Kode dilisensikan Apache License 2.0 (lihat [LICENSE](LICENSE)); sebagian besar fondasi
  berasal dari proyek Now in Android, hak cipta The Android Open Source Project.
- Font **KFGQPC Uthmanic Script HAFS** © King Fahd Glorious Quran Printing Complex.
- Font **Plus Jakarta Sans** (SIL Open Font License).
