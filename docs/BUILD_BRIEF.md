# PROMPT UNTUK CODEX CLOUD — BUILD BYGA SMART OVERLAY

Bertindak sebagai lead Android engineer, AI integration engineer, QA engineer, dan CI/CD engineer. Implementasikan proyek Android Kotlin native bernama **BYGA Smart Overlay** di repositori GitHub yang sedang terbuka di Codex Cloud.

## Dokumen utama

Gunakan `BYGA_SMART_OVERLAY_PRD_v1.0.md` sebagai sumber kebenaran untuk 22 keputusan produk. Jika file PRD belum ada di repo, buat `docs/PRD.md` berdasarkan deskripsi lengkap di bawah. Jangan mengubah prioritas produk atau menjadikan Cloudflare/VPS sebagai backend: **arsitektur Full Android Local**. Jangan hanya membuat UI mockup atau PRD; hasil utama harus **source code Android yang nyata, tests, workflow GitHub Actions, dan README untuk memperoleh APK**.

## Sasaran dan kendala wajib

- APK Android pribadi, **tanpa root/ADB/Shizuku**, berbahasa Indonesia, portrait-first, visual premium dark navy `#080D19`, blue `#246BFD`, cyan glow. Bubble melayang terasa halus dengan spring/pulse/dock, terinspirasi animasi Codex tanpa meniru merek/aset Codex.
- Target Android 16 secara khusus, jaga kompatibilitas Android modern sesuai versi Gradle/SDK/AGP yang tersedia. Pilih dependensi stabil dan dapat dikompilasi; jangan mengarang versi yang tidak ada.
- Fokus aplikasi: ChatGPT, Claude, Google Flow (website atau app yang tersedia), TikTok, Instagram; dukung aplikasi lain melalui profil yang dapat ditambah pengguna.
- Semua orkestrasi berjalan di APK; permintaan OpenAI, Anthropic, Gemini, OpenRouter, Groq dan custom AI via HTTPS langsung dari HP. Jangan meng-hardcode API key.
- Keamanan: simpan API key menggunakan Android Keystore + AES-GCM untuk encryption-at-rest, konfigurasi biasa via DataStore, data workflow via Room. Tidak perlu biometric vault atau server.
- Command input **text + quick actions/gestures**; tidak perlu voice.
- Gunakan Android Clipboard/Intent ACTION_SEND/Chooser sebagai fallback resmi. Accessibility service hanya jika pengguna aktifkan dengan sadar, untuk pembacaan UI dan menjalankan **langkah deterministik yang telah di-review pengguna**, bukan agen AI yang membuat dan mengeksekusi aksi sendiri tanpa batas.
- Screenshot Vision via MediaProjection on-demand dengan persetujuan sistem per sesi yang dipersyaratkan, plus persetujuan eksplisit sebelum screenshot dikirim ke provider AI. Jangan bypass FLAG_SECURE, password, OTP, autentikasi, privacy prompts atau layar yang memblokir capture.
- Eksekusi workflow harus punya allowlist aksi, pemeriksaan package/akun aktif, approval untuk side effects, timeout, retry aman, checkpoints, pause/resume/stop. Posting, kirim pesan dan hapus tidak boleh blind-retry, dan **posting wajib konfirmasi pengguna tepat sebelum publikasi**.
- Multi-account menggunakan profil per aplikasi dan bantuan pindah akun melalui fitur resmi aplikasi. Jangan implementasi clone/login bypass/menyimpan password. Sebelum posting, verifikasi akun tujuan; bila belum bisa, `WAITING_FOR_USER`.
- Scheduler one-time/recurring/sequential/prioritas. WorkManager untuk background-safe tasks. Saat layar terkunci, langkah UI harus ditunda dan notifikasi meminta tindakan pengguna; tidak auto-unlock, tidak membangunkan aplikasi diam-diam, tidak janjikan exact minute.
- Build via **GitHub Actions debug APK artifacts**. Tidak perlu Cloudflare, Docker backend, Render, Firebase, GitHub Releases wajib, atau Play Store deployment.

## Implementasi bertahap (kerjakan sebanyak mungkin dalam satu task tanpa mengklaim tes perangkat yang tidak dilakukan)

### Phase 0 — Reproducible Android project + functional PoC [PRIORITAS TERTINGGI]
1. Inspeksi repo dan pertahankan file pengguna yang ada. Jika repo kosong, buat proyek Kotlin Android dengan Gradle wrapper, Jetpack Compose Material 3, package contoh `com.byga.smartoverlay` (boleh atur sesuai namespace repo jika sudah ada).
2. Buat navigation + halaman Onboarding/Permissions, Home, Live Monitor, Apps, Accounts, AI Providers, Workflows, Scheduler, Settings. Setiap layar harus usable; fungsi yang belum didukung tunjukkan status `Experimental` atau `Blocked`, bukan tombol mati tanpa penjelasan.
3. Buat `OverlayService` + `WindowManager.TYPE_APPLICATION_OVERLAY`, tutorial `SYSTEM_ALERT_WINDOW`, animated draggable bubble, edge snap, tap expand ke panel, long press quick actions, minimal notification/stop yang benar. Pastikan bubble tak menghalangi interaksi bawah saat collapsed; handle rotation, keyboard, service teardown, foreground service requirements jika diperlukan.
4. Buat `AccessibilityService` deklarasi yang benar, permission disclosure, `canRetrieveWindowContent` dan `canPerformGestures` sesuai kebutuhan. Implementasikan `readAccessibleNodes`, `setText(node)`, `performClick(node)`, `scroll` dengan checks, scope allowlist, rate limiting, structured result `SUCCESS/NOT_FOUND/PERMISSION_DENIED/BLOCKED/UNKNOWN`. Tombol "Test Input" harus memerlukan user action dan membuka layar uji aman bila aplikasi target tidak kompatibel.
5. Implement Android Sharesheet untuk teks dan media URI melalui FileProvider/MediaStore grants yang benar, app launcher intent, basic download URL via Android DownloadManager/MediaStore.
6. Buat halaman **Diagnostics & PoC Matrix** dengan uji manual (overlay, accessibility, field input, share, direct download, native app download, switch account, locked scheduler). Semua status awal `NOT_TESTED` dan hanya diubah melalui aksi manual pengguna atau test nyata.
7. Tulis unit tests untuk pure logic, permission state, workflow transitions, account verification guard, idempotency, AI fallback (mock), scheduler readiness.
8. Buat `.github/workflows/android-apk.yml`: checkout, setup Java+SDK, Gradle wrapper, `testDebugUnitTest assembleDebug`, upload artifact APK. Jangan menyimpan API keys sebagai secrets build atau artefak. Bila lingkungan cloud tidak memiliki SDK, set CI tetap lengkap dan laporkan validasi apa yang benar-benar dijalankan.

### Phase 1 — Multi-provider AI local
- Buat interface `AiProviderAdapter` untuk request text dan vision dengan capability matrix, request timeout, retries terbatas, fallback, usage estimates, provider health test, roles Planner/Screen Analyzer/Content Generator/Validator.
- Buat halaman tambah provider/model/base URL (custom endpoints hanya HTTPS atau localhost debug), API key masked/encrypted via Keystore. Jangan masukkan key dalam manifest, tests, JSON export, GitHub artifacts atau Logcat.
- Gunakan fake provider untuk test dan preview tanpa biaya; user menambahkan key nyata saat aplikasi terinstal.
- AI dapat menyusun **draft workflow** dan membuat konten teks, namun draft AI harus dikonversi ke typed DSL, diperiksa, disetujui pengguna, baru boleh dijalankan.
- Implement MediaProjection minimal yang benar dan opt-in; jangan menampilkan bahwa screenshot sukses jika Android belum memberikan izin.

### Phase 2 — Workflow engine, media dan publishing
- Typed DSL: LAUNCH_APP, FIND_NODE, SET_TEXT, COPY_TEXT, CLICK_NODE, SWIPE, WAIT_FOR_STATE, GENERATE_AI_TEXT, ANALYZE_SCREEN_ON_DEMAND, DOWNLOAD_URL, TAP_NATIVE_DOWNLOAD, SHARE_MEDIA, VERIFY_ACCOUNT, REQUEST_USER_APPROVAL, BRANCH, DELAY, STOP.
- Workflow editor usable: tambah/edit/hapus/urutkan steps, template, konfigurasi selectors, validate, dry run, execute and monitor; jika drag-and-drop kompleks belum selesai, sediakan reorder UI terlebih dahulu dan dokumentasikan keterbatasannya.
- Implement state machine + Room checkpoints + explicit approval gates, no hidden execution. Error recovery bounded/selector rescan, log redacted.
- Download native via UI yang benar-benar menyediakan fungsi tersebut; URL download cek scheme, size, MIME dan izin. Jika tidak bisa, tampil `BLOCKED/UNSUPPORTED`.
- Draft Social Publisher untuk TikTok dan Instagram: pilih media lokal, caption/hashtag, akun tujuan, sharesheet dan pilihan assist via Accessibility yang disetujui. Jangan mengklaim `PUBLISHED` hanya karena share intent berhasil. Konfirmasi pengguna wajib di layar tujuan.

### Phase 3 — Accounts + schedules
- Account profiles per aplikasi: label, alias publik, target workflow, `UNKNOWN/VERIFIED/NEEDS_LOGIN`. Guided account switching via menu resmi, bukan clone sessions; account-check step before publishing.
- Schedule builder sekali jalan/recurring/dependencies, WorkManager with OS limits, notification & resume on unlock when safe. Queue + priority + retry and status history.
- Live dashboard status bubble (idle/thinking/working/waiting/success/error) + active step/provider/account + pause/stop, redacted timeline.

## Keamanan & batasan non-negotiable

- Aplikasi bukan AccessibilityTool untuk disabilitas; **jangan klaim flag `isAccessibilityTool=true`** untuk menghindari aturan. Google Play membatasi autonomous plan+execute via Accessibility, sehingga gunakan user-reviewed rule-based scripts yang deterministik; jika rencana Play Store di masa depan, dokumentasikan kebutuhan audit kebijakan.
- Jangan bypass lock screen, secure flag, autentikasi/2FA, DRM, privacy restrictions, anti-bot atau pembatasan platform.
- Jangan membaca informasi aplikasi tanpa allowlist/pengaktifan pengguna; jangan merekam screen terus-menerus; jangan mengekspor screenshot/API key di logs.
- Jangan membangun posting massal tanpa kendali, posting terjadwal diam-diam, atau eksekusi AI yang mempublikasikan tanpa konfirmasi.
- UI harus menjelaskan ketika fitur tidak tersedia karena batasan Android atau aplikasi target, bukan mengklaim telah berhasil.

## Struktur repo disarankan

```text
app/src/main/java/com/byga/smartoverlay/
  ui/                  // Compose screens/components and bubble style
  overlay/             // Overlay service, drag/dock, controls
  accessibility/       // Service + safe typed UI adapter
  media/               // Screenshot permission and download/share
  ai/                  // Provider adapters, orchestrator, budget
  profiles/            // App/account registry and verification
  workflow/            // DSL, editor, state machine, checkpoints
  scheduler/           // WorkManager, queue, notifications
  data/                // Room, DataStore, encrypted key storage
  permissions/         // Wizard and capability checks
  diagnostics/         // Device PoC checklist
.github/workflows/android-apk.yml
README.md
docs/PRD.md
docs/ARCHITECTURE.md
docs/TEST_MATRIX.md
docs/COMPATIBILITY.md
docs/PRIVACY.md
docs/ROADMAP.md
```

## Acceptance checks sebelum kamu mengakhiri task

1. Periksa dan jelaskan actual repo changes; `./gradlew testDebugUnitTest assembleDebug` berhasil **jika build environment mendukung**. Bila gagal, diagnosa dan perbaiki semampunya; jangan mengklaim sukses palsu.
2. GitHub Actions YAML valid dan menjalankan tests/build lalu meng-upload **installable debug APK artifact**.
3. Onboarding, bubble, permission status, manual prompt share, local workflow dan stop button benar-benar terhubung ke backend komponen Android (bukan decorative UI).
4. Unit tests untuk approval/account guard, retries, invalid selectors, network fallback, data persistence dan scheduler behavior.
5. `docs/TEST_MATRIX.md`: pisahkan VERIFIED_BY_CI, NEEDS_REAL_DEVICE, BLOCKED, NOT_IMPLEMENTED. Per aplikasi ChatGPT/Claude/Google Flow/TikTok/Instagram jangan tandai PASS tanpa test perangkat.
6. `README.md` memuat cara buka Actions -> pilih workflow run -> unduh APK artifact -> instal via Android, atur izin overlay/accessibility, lalu langkah PoC untuk Samsung/Android modern.
7. Jelaskan bagian yang belum terimplementasi dan berikan daftar prioritas perubahan berikutnya. Jangan berhenti pada plan; tulis kode nyata secara bertahap, teruskan semua bagian yang dapat diimplementasikan di lingkungan tugas.
8. Bila repo belum memuat PRD, commit teks PRD sesuai keputusan #1–#22, bukan spesifikasi baru yang menyimpang.

## Output balasan akhir Codex

Berikan:
- Ringkasan modul yang **sudah benar-benar dibuat** versus TODO/eksperimental.
- Daftar file yang diubah/ditambah.
- Hasil tes/build beserta command dan exit code (jika tersedia).
- Nama workflow GitHub Actions dan lokasi artifact APK.
- Langkah pemasangan dan uji perangkat nyata.
- Limitasi Android/app target yang masih harus divalidasi.
- SHA/branch atau PR, hanya jika benar-benar tersedia.

Prioritas: **kode yang buildable, aman dan dapat diuji nyata** lebih penting daripada kesan otomatis penuh yang sebenarnya tidak bekerja.
