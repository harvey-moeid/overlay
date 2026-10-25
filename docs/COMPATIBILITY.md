# Kompatibilitas

Target/compile API36 (Android16), min API28. AGP8.11.1, Gradle8.13, Kotlin2.1.20, JDK17 dan Compose BOM2025.04.01. Overlay memakai foreground specialUse; capture satu sesi memakai foreground mediaProjection dan consent sistem.

Tes UI JVM SDK35 memeriksa dashboard dan navigasi Chat; ini bukan tes OEM/Android16. AVD API36 cloud sempat boot, namun instalasi/runner gagal pada service OS sebelum device tests berjalan. Tidak ada hasil lulus perangkat Android14/15/16 atau Samsung One UI. Rincian: QA_ANDROID.md.

| Tujuan | Jalur saat ini | Status interaksi perangkat |
|---|---|---|
| ChatGPT | Launcher/share, reviewed focused-field input, scoped capture | NEEDS_REAL_DEVICE |
| Claude | Launcher/share, reviewed focused-field input, scoped capture | NEEDS_REAL_DEVICE |
| Google Flow | Browser resmi, login manual | NOT_TESTED; input/capture browser BLOCKED |
| TikTok | Launcher, draft/media sharesheet, reviewed input/capture | NEEDS_REAL_DEVICE |
| Instagram | Launcher, draft/media sharesheet, reviewed input/capture | NEEDS_REAL_DEVICE |
| Package tambahan | Peluncuran manual tunduk package visibility | NOT_TESTED; input/capture BLOCKED |

Input manual hanya pada whitelist, field fokus yang ditinjau sebelum membuka panel, editable/non-password, permit single-use 10 detik dan verifikasi nilai. NOT_FOUND/BLOCKED/UNKNOWN bukan sukses. Tidak ada selector/kontrol agent pihak ketiga yang diklaim kompatibel. Akun aktif tetap dikonfirmasi manual; posting/send tetap dilakukan pengguna di aplikasi tujuan.

Android dapat mencabut izin, membatasi FGS/baterai/overlay, menunda WorkManager, menyembunyikan overlay atau melindungi FLAG_SECURE. Accessibility tidak menjamin mendeteksi semua data sensitif; pengguna wajib meninjau sebelum input/capture/berbagi. Package regional dan format field dapat berbeda. Izin notifikasi Android13+ disarankan untuk pengingat dan preview capture. Sideload dapat memerlukan Izinkan setelan terbatas melalui Info aplikasi resmi. Semua jalur ini memerlukan QA perangkat; Opsi Pengembang tidak memberi izin tambahan otomatis kepada aplikasi.
