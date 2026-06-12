# Surau — Roadmap Pengembangan (Auth + Quran)

> Status: **Milestone 1 selesai** (branch `m1-foundation`, 11 commit) — auth dasar + Quran reader
> terverifikasi E2E terhadap `https://api.surau.org` pada build debug & release (R8).
> Scope roadmap ini: **seluruh fitur auth dan Quran sampai tuntas. Kitab/Books sengaja di-skip**
> dan baru direncanakan setelah semua milestone di bawah selesai.

---

## Cara kerja setiap milestone (baca dulu)

**Branch & commit**
- Satu milestone = satu branch: `m1.5-stabilisasi`, `m2-audio`, `m3-bookmark`, dst. Merge ke `main` setelah DoD terpenuhi.
- Satu sub-bagian = satu commit. Repo harus selalu compile + test hijau di setiap commit.

**Perintah standar** (JDK 25 wajib untuk test — Robolectric SDK 36+):
```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew assembleDebug testDebugUnitTest      # verifikasi inti
./gradlew recordRoborazziDebug                 # rekam ulang golden screenshot bila UI berubah
./gradlew :app:installDebug                    # uji manual di emulator/device
./gradlew :app:dependencyGuardBaseline :app:updateReleaseBadging   # bila dependency/manifest berubah
```

**Definition of Done (berlaku semua milestone)**
1. `assembleDebug` + `testDebugUnitTest` hijau; golden Roborazzi diperbarui bila UI berubah.
2. Fitur diuji manual di emulator terhadap backend live.
3. `assembleRelease` terinstal & fitur berjalan (cek R8/keep rules).
4. Baseline dependency-guard & badging di-regenerate bila berubah.
5. String UI berbahasa Indonesia di `values/` (EN menyusul di M6).

**Referensi penting**
- Kontrak backend: `/Users/macmini/Downloads/surau-backend/docs/swagger.yaml` (juga `internal/controller/restapi/v1/` untuk shape persis).
- Pola yang sudah ada: ikuti `feature/quran` (api/impl split, entry provider, VM + sealed UiState), repo offline-first di `core/data`, fake di `core/data-test`.
- Konvensi error: tangkap `SurauApiException` (`isRateLimited` → countdown, kode `AUTH_*`), `IOException` → offline. Reuse `AuthSubmitState` untuk form auth.

---

## M1.5 — Stabilisasi & Pelunasan Utang Teknis (kecil, kerjakan pertama)

Tujuan: repo benar-benar "production-grade" sebelum fitur besar. Semua item independen — bisa dicicil.

### 1.5.1 Lint & format
- Jalankan `./gradlew :app:lintRelease lint` (selama M1 lint selalu di-skip). Perbaiki semua error; ubah baseline hanya jika memang false-positive.
- `./gradlew spotlessApply` lalu commit hasil format.

### 1.5.2 Jalankan instrumented test di emulator
- `./gradlew connectedDebugAndroidTest` (ada 3 skenario `NavigationTest`). Belum pernah dieksekusi di device — kemungkinan perlu `waitUntil` untuk node `welcome:guest` (data store async). Perbaiki flakiness dengan `composeTestRule.waitUntil { ... }`.

### 1.5.3 Ikon launcher legacy (API 23–25)
- `app/src/main/res/mipmap-*/ic_launcher(.round).png` masih render NiA lama. Regenerate dari adaptive icon (Android Studio → New > Image Asset, atau render manual 48–192dp) dengan latar emerald `#006D4A` + mark putih. Jangan lupa varian `debug` tidak punya mipmap sendiri (hanya tint) — cukup main.

### 1.5.4 Deep link reset password
- Manifest `app/src/main/AndroidManifest.xml`: intent-filter `autoVerify` untuk `https://surau.org/reset-password` (+ scheme path `?token=`).
- `MainActivity`: baca `intent.data` → bila ada token, set state awal → `SurauApp` push `ResetPasswordNavKey(token)` (pola sama dengan `shouldShowWelcome`).
- Server: deploy `/.well-known/assetlinks.json` di `surau.org` berisi SHA-256 cert release (koordinasi dengan backend; tanpa ini link tetap berfungsi via chooser).

### 1.5.5 Signing release sungguhan
- Buat keystore (`keytool -genkeypair`), simpan di luar repo.
- `app/build.gradle.kts`: `signingConfigs.release` membaca path/password dari `local.properties`/env; hapus fallback debug-signing. Dokumentasikan di README.

### 1.5.6 Test tambahan yang masih bolong
- `DefaultAuthRepositoryTest` (fake `SurauAuthApi`/`SurauUserApi`): login persist sesi → introspect enrich; logout selalu clear lokal walau API gagal.
- `OfflineFirstQuranRepositoryTest`: `ensureSurahCached` fetch sekali lalu serve cache (hitung panggilan API); offline + cache ada → tidak throw; offline tanpa cache → `IOException`.
- Screenshot test `SettingsScreen` (guest & authenticated) di `feature/settings/impl/src/test`.

### 1.5.7 CI hijau
- Push branch → pastikan `.github/workflows/Build.yaml` lulus (task sudah di-rename ke variant tunggal, tapi belum pernah dieksekusi). Sesuaikan JDK runner ke 21/25 (Robolectric!) bila masih 17.

### 1.5.8 Merge & push
- Merge `m1-foundation` → `main`, push. Tag `v0.1.0`.

---

## M2 — Audio Murottal (fitur terbesar berikutnya)

Tujuan: dengar murottal per ayat/surah dengan highlight ayat berjalan, pilihan qari, dan kontrol dari notifikasi. Arsitektur M1 sudah menyiapkan: model `Recitation`, kolom `recitationId` di `UserData` + proto, endpoint di `SurauQuranApi.recitations()`.

### 2.1 Data layer audio
- **Room** (`core:database`): `RecitationEntity` (id, displayName, reciterName, style, isDefault, fetchedAt) + DAO + bump versi DB ke 2 **dengan migration** (`Migration(1,2)` + test `MigrationTest` pakai schema JSON yang sudah di-export).
- **DTO/endpoint** (`core:network`): tambah `GET quran/surahs/{id}/audio?recitation_id=` → `SurahAudioManifestDto { surah_id, recitation{...}, mode, tracks[{track_key, ayah_number, url|public_url, duration_ms, segments[{ayah_key, timestamp_from_ms, timestamp_to_ms}]}], missing_ayah_keys[] }`. Verifikasi field persis di `internal/controller/restapi/v1/response/quran.go`.
- **Repo** (`core:data`): `QuranAudioRepository` — `observeRecitations()` (cache Room, refresh 7 hari), `resolveRecitationId(preferredId)` (pref → isDefault → first), `suspend fun audioManifest(surahId, recitationId): SurahAudioManifest` (network-only, jangan cache URL CDN yang bisa kedaluwarsa; cache opsional di 2.6).
- Fake di `core:data-test` + unit test repo.

### 2.2 Modul `core:media` (playback engine)
- Modul baru `core/media` (plugin `surau.android.library` + hilt). Deps baru di catalog: `androidx.media3:media3-exoplayer`, `media3-session` (cek versi stabil terbaru di Google Maven).
- `PlaybackService : MediaSessionService` — foreground, notifikasi media otomatis dari session, audio focus & becoming-noisy ditangani Media3.
- Manifest modul: permission `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, deklarasi service `androidx.media3.session.MediaSessionService` intent-filter.
- `SurauPlayerController` (singleton Hilt): bungkus `MediaController` async; expose `StateFlow<PlayerUiState>`:
  ```kotlin
  data class PlayerUiState(
      val isPlaying: Boolean,
      val surahId: Int?,
      val currentAyahNumber: Int?,   // dari index MediaItem (1 item = 1 ayat)
      val recitationName: String?,
      val positionMs: Long, val durationMs: Long,
  )
  ```
- API controller: `playSurah(manifest, startAyah)`, `playPause()`, `next()/previous()`, `seekToAyah(n)`, `stop()`. MediaItem per ayat: `mediaId = ayahKey`, metadata judul "Al-Fatihah · 1", artist = nama qari.

### 2.3 UI reader (feature:quran)
- `SurahReaderViewModel`: inject controller + audio repo; aksi `playFromAyah(n)` (fetch manifest → controller); expose `playingAyah: StateFlow<Int?>`.
- `AyahItem`: ikon play kecil di baris badge nomor; highlight container (`secondaryContainer` lembut) bila `ayahNumber == playingAyah`.
- **MiniPlayerBar** (komposabel baru, taruh di `core:designsystem` atau di feature): bar bawah reader saat sesi aktif — play/pause, prev/next ayat, label "Surah · Ayat n", tap → scroll ke ayat. Auto-scroll mengikuti ayat aktif (toggle "ikuti bacaan" simpan di rememberSaveable).
- Pilihan qari: tambahkan daftar recitations di `ReaderSettingsSheet` + section "Qari" di `SettingsScreen` (simpan via `userDataRepository.setRecitationId`, push prefs).

### 2.4 Lifecycle & edge case
- Ganti surah saat playing → stop sesi lama. Ganti qari saat playing → restart ayat aktif dengan manifest baru.
- `missing_ayah_keys` tidak kosong → tetap mainkan yang ada, skip yang hilang (toast/snackbar sekali).
- Offline saat fetch manifest → snackbar error, tanpa crash. Token TIDAK diperlukan (endpoint publik) — pakai klien publik.

### 2.5 Test & verifikasi
- Unit: mapper manifest→MediaItem, resolveRecitationId, PlayerUiState reducer (pakai `media3-test-utils` / fake controller).
- Manual E2E: play Al-Fatihah penuh, kunci layar (notifikasi kontrol jalan), headset cabut → pause.

### 2.6 (Opsional, boleh digeser) Cache audio offline
- `SimpleCache` Media3 (LRU 512MB di `cacheDir/media`) via `CacheDataSource.Factory` — dengar ulang tanpa kuota. Tombol "unduh audio surah" menyusul di M6.

---

## M3 — Bookmark Ayat & Preferensi Baca Lanjutan

Tujuan: tandai ayat (guest-first, sync saat login), kelola dari satu layar; plus toggle transliterasi yang datanya sudah tersimpan sejak M1.

### 3.1 Verifikasi kontrak saved-items
- Baca `internal/entity/personal.go` + `request/reader.go` (`UpsertSavedItem`) — pastikan field (`id`, jenis/ref `ayah_key`, `note`, `tags[]`, `created_at`). Endpoint: `GET/POST /me/saved-items`, `PATCH/DELETE /me/saved-items/{id}`, `GET /me/saved-items/tags`, dan `saved_items` di `GET /me/sync`.

### 3.2 Data layer bookmark (offline-first, pola sama dengan progress)
- Room: `BookmarkEntity(ayahKey PK, note?, tagsJson, createdAt, remoteId?, pendingSync, pendingDelete)` + DAO + **migration DB v2→v3** (atau gabung v2 bila M2 belum merge).
- DTO + `SurauMeApi`: saved-items CRUD.
- `BookmarkRepository` (`core:data`): `observeBookmarks()`, `observeIsBookmarked(ayahKey)`, `toggle(ayahKey)` (lokal dulu), `syncBookmarks()` — push pending (create/delete), pull via `/me/sync` rekonsiliasi by `ayah_key` (remote menang untuk konflik update, lokal pendingDelete menang untuk delete).
- Panggil `syncBookmarks()` dari `SyncUserDataAfterLoginUseCase` + `SyncWorker`.

### 3.3 UI
- Reader: long-press sheet ganti dari share-only → `AyahActionsSheet` (Tandai/Hapus tanda, Salin, Bagikan); indikator bookmark kecil di `AyahItem`.
- Layar **Penanda** (`BookmarksNavKey` di feature:quran api): list ayat tersimpan (teks Arab ringkas + terjemahan + waktu), tap → reader scroll ke ayat, swipe-to-delete. Entry: ikon bookmark di header QuranHome.
- Filter tag sederhana (chips dari `/me/saved-items/tags`) — opsional bila backend shape mendukung.

### 3.4 Transliterasi toggle
- Proto + `UserData`: `showTransliteration: Boolean` (field proto baru no. 26).
- ReaderSettingsSheet + SettingsScreen: switch "Transliterasi latin".
- `AyahItem`: render `populated.transliteration?.text` (italic, di bawah Arab) bila aktif. Data sudah ada di tabel `transliterations`.

### 3.5 Test
- Repo bookmark: toggle guest → pendingSync; login sync → push; delete remote-wins matrix.
- Screenshot: reader dengan transliterasi, layar Penanda.

---

## M4 — Khatam, Streak & Aktivitas

Tujuan: motivasi membaca — pelacak khatam 30 juz, streak harian, heatmap aktivitas. Semua endpoint protected (tampilkan CTA login untuk guest).

### 4.1 Data layer
- DTO + `SurauMeApi`:
  - `POST/GET /me/quran/khatam`, `GET .../history`, `POST .../complete`, `PUT/DELETE .../juz/{n}` → `KhatamCycleDto { id, started_at, completed_at?, notes?, completed_juz[], juz_count, percent }`.
  - `GET /me/activity?days=30` → harian `{date, quran_ayahs_read, ...}`; `GET /me/activity/streak` → `{current_streak_days, longest_streak_days, active_today, ...}`.
  - `GET /me/quran/progress/surahs` (persen per surah).
- `ActivityRepository` (network-first, cache in-memory di VM saja — data kecil & personal; JANGAN cache HTTP karena klien auth tanpa cache).

### 4.2 UI — layar Aktivitas (`ActivityNavKey`, feature:quran atau feature baru `feature:activity`)
- Header streak: angka besar "X hari beruntun" + longest + indikator hari ini.
- Heatmap 4–5 pekan (grid `Canvas`/`LazyVerticalGrid` 7×N, intensitas dari `quran_ayahs_read`).
- Kartu Khatam aktif: progress % + grid 30 juz (checkbox toggle → `PUT/DELETE juz/{n}` optimistik), tombol "Mulai khatam" / "Tandai selesai" (+ catatan opsional), riwayat khatam (list tanggal mulai-selesai).
- Entry point: ikon/section di QuranHome (mis. chip streak kecil di header → buka layar penuh).
- Guest: layar menampilkan ilustrasi + tombol Masuk.

### 4.3 (Opsional) Persen per surah di daftar
- Badge tipis progress di `SurahListItem` untuk user login (`/me/quran/progress/surahs`, fetch sekali per buka home, gabung di `GetSurahListWithLastReadUseCase`).

### 4.4 Test
- VM aktivitas: guest → state LoginRequired; authed → render data fake; toggle juz optimistik + rollback saat error.

---

## M5 — Manajemen Akun Penuh (menuntaskan auth)

Tujuan: semua kemampuan akun backend terpakai. Semua aksi sensitif minta password ulang. Reuse `AuthSubmitState` + komponen `feature:auth`.

### 5.1 Layar "Kelola Akun" (`AccountNavKey` di feature:auth api; entry dari Settings > Akun)
Item: Profil, Ganti kata sandi, Ganti email, Perangkat & sesi, Preferensi email, Keluar dari semua perangkat, Hapus akun.

### 5.2 Profil
- `PATCH /user/profile` (`display_name`, `timezone` auto dari `TimeZoneMonitor`, `country_code`). Form sederhana + simpan. Refresh `UserSession.displayName` di `AuthSessionDataSource` setelah sukses.

### 5.3 Ganti kata sandi
- `POST /auth/change-password {current_password, new_password}` → **respons berisi token pair baru + session_id** (semua sesi lama dicabut). WAJIB: persist pair baru via `AuthSessionDataSource.setSession/updateTokens` — kalau tidak, user langsung ter-logout.

### 5.4 Perangkat & sesi
- `GET /auth/sessions` → list (user_agent diringkas, client_ip, last_used_at, badge "Perangkat ini" bila `is_current`).
- `DELETE /auth/sessions/{id}` per item; `POST /auth/logout-all` (konfirmasi; setelahnya sesi lokal ikut mati → clear + kembali ke guest).

### 5.5 Ganti email
- 2 langkah: `POST /auth/change-email/request {current_password, new_email}` → layar OTP `POST /auth/change-email/verify {otp|token}`. Update email di `AuthSessionDataSource` setelah sukses.

### 5.6 Hapus akun
- `POST /auth/delete-account {current_password}` + dialog konfirmasi dua tahap (ketik "HAPUS"?). Sukses → clear sesi + progress lokal? (keputusan: **pertahankan** progress lokal sebagai guest, hapus hanya identitas) → kembali ke Welcome.

### 5.7 Preferensi email
- `GET/PATCH /user/email-preferences` → toggle marketing/notifikasi (cek shape di backend).

### 5.8 Test
- VM tiap aksi dengan ScriptedAuthRepository-style fake: sukses, password salah (401), rate-limit. Change-password test memastikan token pair baru dipersist.

---

## M6 — Offline Penuh, Kualitas & Internasionalisasi

### 6.1 Unduh semua surah (teks)
- Tombol di Settings > Bacaan: "Unduh seluruh Al-Qur'an (±X MB)".
- `DownloadQuranWorker` (WorkManager unique, constraint network) iterasi 114 × `ensureSurahCached(surahId, sourceId)` + progress (setProgress) → indikator di Settings (persentase, batal). Saat sumber terjemahan diganti → tawarkan unduh ulang.

### 6.2 Pencarian offline (FTS)
- Tabel FTS4/5 `ayahs_fts(text_imlaei_simple, translation_text)`; isi saat unduh penuh (perlu `view=full` agar `text_imlaei_simple` terisi — saat ini reader_minimal tidak memuatnya; simpan kolomnya di `AyahEntity` saat full-fetch).
- `QuranRepository.search`: coba server → `IOException` → fallback FTS lokal (tandai hasil "hasil offline").

### 6.3 Adaptive layout (tablet/foldable)
- `rememberListDetailSceneStrategy` (deps sudah ada): expanded width → Home (list) | Reader (detail) dua panel; Settings dua panel opsional. Uji `Medium_Phone` + resizable emulator.

### 6.4 Bahasa
- Tambah `values-en/strings.xml` untuk seluruh modul (default tetap ID). Per-app language picker di Settings (API `LocaleManager`/AppCompat per-app locales) — opsional.

### 6.5 Telemetri & crash (keputusan produk)
- Pilih: Firebase kembali / Sentry / tanpa telemetri. Implement `AnalyticsHelper` nyata di `core:analytics` (interface sudah ada, tinggal binding) + crash reporter. Opt-in setting privasi.

### 6.6 Hardening token (opsional)
- Bungkus refresh token dengan Keystore AEAD (Tink) sebelum simpan ke DataStore. Layak dikerjakan bila target audit keamanan.

### 6.7 Baseline profile & performa
- Update `benchmarks` journeys: startup → scroll home → buka surah → scroll reader. `./gradlew :app:generateBaselineProfile` → commit `app/src/main/baseline-prof.txt`. Cek jank via macrobenchmark.

---

## M7 — Rilis ke Play Store

1. Keystore produksi + enroll **Play App Signing**; simpan upload key aman.
2. `versionCode` scheme (mis. epoch-day) + `versionName` 1.0.0.
3. Store listing: ikon 512, feature graphic, screenshot (ambil dari emulator; sudah ada pipeline screencap), deskripsi ID/EN.
4. **Privacy policy** di `surau.org/privacy` (wajib: akun email, token sesi, progres baca; tanpa iklan) + Data Safety form.
5. Internal testing track → closed testing → produksi bertahap.
6. Pasca-rilis: pantau ANR/crash (Play vitals atau telemetri M6.5).

---

## Backlog (setelah semua di atas; belum dijadwalkan)

- Kitab/Books (scope besar — endpoint `/books/*` sudah lengkap di backend).
- Mode mushaf per halaman (604 hal., butuh font per-halaman QPC V1/V2 — eksploratif).
- Widget/Glance "Terakhir dibaca" + Quick Settings tile.
- Pengingat harian (notifikasi; perlu modul notifications baru + POST_NOTIFICATIONS).
- Wear OS / Auto.
- Tafsir (cek ketersediaan endpoint backend dulu).
