# Surau — Runbook Rilis Play Store (v1.0.0)

Panduan langkah demi langkah untuk merilis **produksi penuh**. Artefak teknis sudah
disiapkan; yang tersisa adalah langkah di Play Console (hanya bisa Anda kerjakan).

---

## 0. Artefak yang sudah siap
| Item | Lokasi / nilai |
|---|---|
| **AAB rilis** (yang di-upload) | `app/build/outputs/bundle/release/app-release.aab` |
| Versi | `versionName 1.0.0`, `versionCode 20620` |
| **Upload key** | `~/.surau/surau-upload.jks`, alias `upload` (RSA 4096, s/d 2053) |
| Upload key SHA-256 | `44:EA:76:91:01:B6:B8:12:43:EE:B0:BA:27:B2:CF:12:93:EB:50:C9:F9:5F:E7:84:8E:7E:A2:9F:6D:38:6F:FB` |
| Upload key SHA-1 | `AF:94:04:5E:E0:44:20:7C:77:D5:4D:2A:A5:D6:53:04:CD:26:28:9A` |
| Privacy policy | `release/privacy-policy.html` → host di `surau.org/privacy` |
| Store listing (ID/EN) | `release/store-listing.md` |
| Data Safety | `release/data-safety.md` |
| Content rating | `release/content-rating.md` |
| Grafik | `release/graphics/` (lihat `graphics/README.md`) |

> 🔐 **CADANGKAN SEKARANG:** salin `~/.surau/surau-upload.jks` + password-nya ke password
> manager / penyimpanan aman OFF-machine. Kehilangan upload key = harus minta reset ke
> Google sebelum bisa update app.

---

## 1. Prasyarat (sekali saja)
1. Akun **Google Play Developer** — biaya pendaftaran **$25** sekali, verifikasi identitas (D.U.N.S. bila organisasi). Bisa makan 1–3 hari untuk verifikasi.
2. **Host privacy policy** di URL publik, mis. `https://surau.org/privacy` (pakai `release/privacy-policy.html`). URL harus live SEBELUM submit.

## 2. Buat aplikasi
Play Console → **Create app**:
- App name: `Surau — Al-Qur'an & Murottal`
- Default language: **Indonesia (id-ID)**
- App or game: **App** · Free or paid: **Free**
- Centang deklarasi (Developer Program Policies, US export laws).

## 3. App content (Policy → App content) — isi semua
| Bagian | Sumber jawaban |
|---|---|
| Privacy policy | URL `surau.org/privacy` |
| Ads | **Tidak ada iklan** |
| App access | Beri **kredensial demo** untuk reviewer (lihat §6) — app punya fitur login |
| Content rating | kuesioner di `content-rating.md` |
| Target audience | 13+ / dewasa (JANGAN pilih kelompok anak) — `content-rating.md` |
| News app | Tidak |
| Data safety | `data-safety.md` |
| Government apps / Financial / Health | Tidak |

## 4. Play App Signing (otomatis untuk app baru)
- Saat membuat rilis produksi pertama, Play **mengaktifkan Play App Signing** secara default.
- Google membuat & menyimpan **app signing key** (kunci penandatangan final). AAB Anda
  ditandatangani dengan **upload key** kita; Google menandatangani ulang untuk distribusi.
- Tak perlu meng-upload app signing key. Upload key kita = sudah dipakai menandatangani AAB.
- ⚠️ **assetlinks.json (deep link reset password):** setelah enrol, ambil SHA-256 **APP
  SIGNING key** dari *Play Console → Test and release → App integrity → App signing*
  (BUKAN SHA-256 upload key di atas). Itu yang dipasang di `surau.org/.well-known/assetlinks.json`.

## 5. Store listing (Grow → Store presence → Main store listing)
- App name, short & full description: salin dari `release/store-listing.md` (ID + EN).
- App icon 512, feature graphic 1024×500, 2–8 screenshot telepon: dari `release/graphics/`.
- App category: **Books & Reference**. Email kontak: isi email Anda. Tags: pilih relevan (Islam, Quran).

## 6. Buat rilis produksi
Test and release → **Production → Create new release**:
1. Lanjutkan dengan **Play App Signing** (Continue).
2. **Upload** `app-release.aab`.
3. Release name: otomatis `20620 (1.0.0)`.
4. Release notes (id-ID):
   ```
   Rilis pertama Surau: baca Al-Qur'an 30 juz, murottal, penanda ayat,
   pelacak khatam & streak, unduh offline. Tanpa iklan.
   ```
   (en-US):
   ```
   First release of Surau: read all 30 juz, recitation audio, verse
   bookmarks, khatam & streak tracking, offline download. No ads.
   ```
5. **App access**: karena ada login, tambahkan akun uji untuk reviewer
   (email + password akun nyata di `api.surau.org`), atau tandai bagian yang butuh login.

## 7. Negara & harga
- Production → **Countries/regions**: pilih negara (mis. Indonesia + worldwide).
- Free (tanpa in-app purchase).

## 8. Review & rollout bertahap
- **Save → Review release → Start rollout to Production.**
- Disarankan **staged rollout** (mulai 10–20%) agar bisa pantau crash sebelum 100%.
- **Pre-launch report** Play akan otomatis menjalankan app di beberapa device — cek hasilnya
  (crash/ANR/aksesibilitas) sebelum menaikkan persentase.
- Review pertama biasanya beberapa jam s/d beberapa hari.

## 9. Pasca-rilis (tanpa telemetri — keputusan M6.5)
- Pantau **Play Console → Quality → Android vitals**: ANR rate, crash rate (sumber utama
  pemantauan karena app sengaja tanpa SDK telemetri/crash).
- Balas review pengguna dari Play Console.

---

## Lampiran — rilis berikutnya
- **versionCode = epoch-day**: `echo $(( $(date -ju -f "%Y-%m-%d %H:%M:%S" "<YYYY-MM-DD> 00:00:00" +%s) / 86400 ))`, perbarui di `app/build.gradle.kts`. Naikkan `versionName` sesuai SemVer.
- Build ulang: `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :app:bundleRelease` → AAB tertandatangani upload key otomatis (kredensial dari `local.properties`).
