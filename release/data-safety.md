# Google Play — Data Safety form (jawaban siap-isi)

Play Console → **Policy → App content → Data safety**. Isi sesuai di bawah. Semua jawaban
ini mencerminkan perilaku aplikasi yang sudah diverifikasi (tanpa SDK iklan/analitik;
izin hanya INTERNET, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE[_MEDIA_PLAYBACK]).

## Bagian 1 — Ringkasan
| Pertanyaan | Jawaban |
|---|---|
| Apakah app mengumpulkan/membagikan data pengguna yang termasuk tipe wajib-deklarasi? | **Ya** (lihat Bagian 2) |
| Apakah semua data pengguna dienkripsi saat transit? | **Ya** (HTTPS untuk semua trafik) |
| Apakah Anda menyediakan cara pengguna meminta penghapusan data? | **Ya** — hapus akun di dalam app (*Profil → Kelola akun → Hapus akun*) + email `privasi@surau.org` |
| URL penghapusan akun (diminta Play) | Sediakan `https://surau.org/hapus-akun` **atau** pakai email di atas |

## Bagian 2 — Tipe data yang dikumpulkan
> Untuk SEMUA baris: **Dibagikan ke pihak ketiga = Tidak**. **Diproses sementara saja = Tidak**
> (data disimpan di server). Data disimpan di server kami sendiri (`api.surau.org`).

| Tipe data | Dikumpulkan? | Wajib/Opsional | Tujuan |
|---|---|---|---|
| **Personal info → Email address** | Ya | Opsional¹ | Account management; App functionality |
| **Personal info → Name** (nama tampilan) | Ya | Opsional | Account management; App functionality |
| **Personal info → Other info** (negara, zona waktu) | Ya | Opsional | App functionality |
| **App activity → App interactions** (progres baca, khatam, streak) | Ya | Opsional | App functionality |
| **App activity → Other user-generated content** (catatan & tag penanda) | Ya | Opsional | App functionality |

¹ *Opsional* karena app bisa dipakai sebagai tamu tanpa akun; email baru diperlukan bila pengguna memilih membuat akun.

### TIDAK dikumpulkan / TIDAK dideklarasikan
- ❌ Location (presisi/perkiraan) — tidak ada izin lokasi.
- ❌ Financial info, Health, Messages, Photos/Videos, Audio files, Files/docs, Calendar, Contacts.
- ❌ **Device or other IDs / Advertising ID** — tidak ada SDK iklan, tidak ada AD_ID.
- ❌ **App info & performance (crash logs, diagnostics)** — tidak ada telemetri/crash SDK (keputusan M6.5).

### Catatan: IP & user-agent sesi
Server mencatat alamat IP + ringkasan perangkat untuk fitur **"Perangkat & sesi"** dan
keamanan akun. Penggunaan khusus-keamanan seperti ini umumnya termasuk *security
exemption* Google dan tidak perlu dideklarasikan sebagai tipe data terpisah. Bila ingin
sangat konservatif, Anda boleh mendeklarasikannya, tetapi tidak diwajibkan. Form Data
Safety juga tidak punya tipe "IP address" tersendiri.

## Bagian 3 — Praktik keamanan
- ✅ Data dienkripsi saat transit (HTTPS).
- ✅ Token penyegar dienkripsi saat disimpan di perangkat (Android Keystore / Tink AEAD).
- ✅ Pengguna dapat meminta penghapusan data (in-app + email).
- ✅ Berkomitmen mengikuti Families Policy? **Tidak menargetkan anak-anak** (audiens umum) — lihat `content-rating.md` & Target audience.
