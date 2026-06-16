**بِسْمِ ٱللَّٰهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ**
*In the name of Allah, the Most Gracious, the Most Merciful.*

> Released on **1 Muharram 1448 H (16 June 2026)** — the first day of the new Hijri year. 🌙

I begin with Bismillah, seeking the pleasure of Allah — the One who guided me through every step of building this app.

With a sincere and humble heart, I share the very first public release of **Surau Nusantara for Android**. Surau is not merely an app to read — it is the beginning of a trustworthy home for Islamic knowledge, built on one promise: *never let someone believe an answer they can't verify.* The destination is a connected **wiki** and an **AI you can ask (RAG)** that answers only from sources you can trace back.

This release opens the **first door — the Qur'an**, built on the official Kemenag foundation. Hadith, the classical books (turath) such as *Ihya Ulumuddin*, the connected wiki, and the verified-source AI will follow — one careful, verified step at a time, in shaa Allah.

## ✨ What ships in v1.0.0

### The Qur'an — open to everyone
- **Read freely, no account needed** — reading, audio, and the surah/juz flow are all public, with gentle sign-up prompts, never a wall.
- All **30 juz** in clear Uthmanic Hafs (KFGQPC) Arabic text.
- Toggleable **Indonesian translation** and **latin transliteration**.
- Adjustable font size, light/dark theme, and reading layout.

### Murottal audio (Media3)
- Play **per-ayah or a full surah**, with your choice of qari.
- The current ayah **auto-highlights** and the page **scrolls to follow**.
- **Background playback** with lock-screen and notification controls.

### For those who sign in
- **Bookmark** ayat with rich personal notes and tags.
- **Khatam** tracker across the 30 juz, with history.
- **Daily streak** and a **reading-activity calendar** to keep you consistent.
- **Full account management** in-app — sessions, display name, change email, email preferences, and delete-account-anytime.

### Offline-first
- **Download the entire Qur'an** to read with no internet.
- **Offline ayat search** (full-text) keeps working with no connection.

### Design & theming
- A **HeroUI-based design system** on Jetpack Compose — Inter type, a custom icon set, and a refreshed component library.
- Site-wide **palette × light/dark/system** — switch between *Default / Mouve / Sky*.
- **User-customizable seed-color theming** with live contrast feedback.
- A RetroMusic-style **expandable audio player** and a clean **5-tab navigation**.

### Languages & quality
- Full **Indonesian and English**, with an in-app language picker that can follow the system.
- **Adaptive two-pane layouts** for tablets and large screens.
- Auth tokens **encrypted at rest** (Android Keystore-backed AEAD via Tink).
- Tuned with a **Baseline Profile** for a smoother first run.

### Under the hood
- A **modular Jetpack Compose** architecture (core + feature modules).
- **Network-first sync** backed by an offline Room cache.
- A **signed release build (AAB)** ready for Google Play — `org.surau.app`.
- *In this beta the focus stays on a clean, verified Qur'an. Hadith, the classical books, the wiki, and the verified-source AI are on the road ahead.*

## 🧭 The road ahead (being built, in shaa Allah)
1. **Qur'an** — *this release* ✅
2. **Hadith**
3. **Classical books (turath)** — e.g. *Ihya Ulumuddin*
4. **Connected wiki**
5. **Trustworthy AI (RAG)** — answers grounded only in verified sources

*Each step verified before the next — verify, not volume.*

---
*Alhamdulillah for everything that led to this moment. May Allah make it sincerely for His sake, and a lasting benefit. آمين.*

🤖 Generated with [Claude Code](https://claude.com/claude-code)
