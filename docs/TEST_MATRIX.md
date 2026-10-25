# Matriks bukti uji

Kategori: `VERIFIED_LOCALLY` = perintah cloud benar-benar dijalankan; `VERIFIED_BY_CI` hanya boleh diisi setelah ada run GitHub sukses; `NEEDS_REAL_DEVICE` = build bukan bukti perilaku HP; `BLOCKED` = aksi belum diizinkan executor; `NOT_IMPLEMENTED` = kode fitur belum tersedia.

| Pemeriksaan | Bukti / status |
|---|---|
| Android unit tests | Baseline v1 VERIFIED_LOCALLY: 28 test, 0 failure/error/skipped (19 workflow guards, 4 fallback AI, 5 media policies) |
| Debug APK | VERIFIED_LOCALLY: testDebugUnitTest assembleDebug selesai exit 0; bukan bukti instalasi perangkat |
| Instrumentation APK compile | VERIFIED_LOCALLY: assembleDebugAndroidTest exit 0; v1 Room/Keystore tests belum dieksekusi; v2 menambah migration/memory/codec test |
| Android Lint | VERIFIED_LOCALLY: lintDebug exit 0, 0 error; peringatan nonblocking tetap ada (style, metadata versi lama/baru, touch accessibility, RTL) |
| APK signature | VERIFIED_LOCALLY: apksigner verify exit 0, signature v2 valid; instalasi tetap perlu perangkat |
| GitHub Actions workflow | YAML diparse lokal, test/build dan upload artifact dikonfigurasi; run GitHub belum dilakukan, belum VERIFIED_BY_CI |
| Permission lifecycle / bubble panel / keyboard / drag / rotation / Stop | NEEDS_REAL_DEVICE |
| Accessibility test input kolom BYGA | NEEDS_REAL_DEVICE; harus terlihat berubah setelah tombol Test Input |
| Accessibility click/scroll/app pihak ketiga | BLOCKED; tidak ada kontrol pihak ketiga aktif |
| Room reopen / Keystore roundtrip-delete | Instrumentation tests tersedia; NEEDS_REAL_DEVICE untuk eksekusi |
| Clipboard / sharesheet teks dan media URI | NEEDS_REAL_DEVICE |
| Direct media download HTTPS / MIME / magic bytes / 50MB / MediaStore | Policy unit test VERIFIED_LOCALLY; unduhan nyata dan file galeri NEEDS_REAL_DEVICE |
| Native download app tujuan | NOT_IMPLEMENTED (manual melalui tombol resmi) |
| Screenshot / MediaProjection / Vision upload | NOT_IMPLEMENTED; tidak ada capture aktif |
| Provider HTTP health test | NEEDS_REAL_DEVICE + key/model runtime; tidak ada panggilan provider berbayar dilakukan di cloud |
| Fake fallback / cancellation / max providers | VERIFIED_LOCALLY melalui mock adapters; bukan bukti kompatibilitas API nyata |
| Workflow version approval / account guard / retry / locked scheduling policy | VERIFIED_LOCALLY pada pure policy; bukan bukti account detection atau scheduler runtime |
| Room checkpoint + pause/resume/stop UI | NEEDS_REAL_DEVICE; tidak ada auto-resume setelah process death |
| WorkManager reminder + locked screen notification | NEEDS_REAL_DEVICE; tidak ada UI execution background, status WAITING_FOR_USER |
| Account profiles | NEEDS_REAL_DEVICE untuk penyimpanan UI; akun aktif UNKNOWN, publishing BLOCKED |
| Automated publish/send/delete | NOT_IMPLEMENTED; sharesheet hanya HANDED_OFF, pengguna konfirmasi di aplikasi tujuan |

## PoC per aplikasi

| Aplikasi | Overlay | Input/paste manual | Share/download | Switch akun | Posting |
|---|---|---|---|---|---|
| ChatGPT | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED |
| Claude | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED |
| Google Flow | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED |
| TikTok | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED |
| Instagram | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED |

Ikuti langkah PoC README, catat model perangkat, Android/One UI dan versi aplikasi, izin, langkah, hasil, serta alasan BLOCKED. Jangan menyertakan key, OTP, password, screenshot pribadi atau data login. Status manual di Diagnostik dimulai NOT_TESTED dan tidak mengubah bukti CI ini.

Validasi akhir cloud: `./scripts/cloud-build.sh testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` selesai exit **0**. Script mengaktifkan JDK 17 dan SDK 36 di snapshot ini. Build memakai proxy JVM dan system CA trust; TLS/checksum tetap aktif. GitHub CI dan instalasi perangkat belum dijalankan.

Ekstensi v2 dan hasil build terkini tercatat di [EXPERIMENTAL_TEST_MATRIX.md](EXPERIMENTAL_TEST_MATRIX.md) dan [CHANGELOG.md](CHANGELOG.md).

Ekstensi 0.3.0: ProviderTest menambahkan 22 pengujian request/response/model discovery menggunakan mock, sehingga total menjadi 80 unit test. Lihat PROVIDERS.md dan BUBBLE_MASCOT.md. Inference provider berkey nyata dan tampilan/gesture maskot masih NEEDS_REAL_DEVICE.
