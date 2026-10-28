# Bukti uji — Studio 0.4.0

`VERIFIED_LOCALLY`: perintah cloud benar-benar dijalankan. `VERIFIED_BY_CI`: hanya run GitHub sukses terkait SHA. `NEEDS_REAL_DEVICE`: tidak dapat dibuktikan dari build. Hasil emulator berbeda dari HP fisik dan tidak membuktikan kompatibilitas aplikasi lain.

## Validasi perbaikan kontrol workflow 0.6.1 — 27 Oktober 2026

- Keenam tes `WorkflowControlTest` gagal pada kode runner sebelum perbaikan dan lulus setelahnya. Cakupan: pause saat readiness/checkpoint, Stop saat readiness/final checkpoint, tidak membuka approval baru setelah Stop, dan propagasi timeout pemanggil dengan checkpoint uncertain tetap tersimpan.
- `./scripts/cloud-build.sh testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`: **BUILD SUCCESSFUL**. **208/208 tes JVM lulus**, 0 failure/error/skipped. Android Lint: 0 error/fatal, 80 warning nonblocking.
- `node scripts/test-workflow-adapter.cjs`: PASS pada fixture Chromium offline ChatGPT/Flow.
- APK aplikasi dan instrumentation berhasil dibangun; tanda tangan APK debug diverifikasi dengan `apksigner verify` (exit 0). APK aplikasi berada di `app/build/outputs/apk/debug/app-debug.apk`.
- Build/Chromium dijalankan dengan izin eksekusi tambahan karena sandbox membatasi socket lokal. Percobaan build offline awal terhenti karena dependensi UIAutomator belum ada di cache; build normal di atas berhasil mengunduh dependensi dan menyelesaikan seluruh gerbang.
- Tes instrumentation belum dieksekusi pada HP/emulator. Kompatibilitas Samsung dan situs/aplikasi asli tetap NEEDS_REAL_DEVICE.

## Validasi build dan logic

- Perintah: `./scripts/cloud-build.sh testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`.
- Suite: legacy workflow/agent/media/provider, runner Studio, durable checkpoint policy, account handoff, request cap, capture/scheduler policy, safe config import/export, dan native Vision payload.
- APK instrumentation mencakup Room reopen/migration, Keystore roundtrip/delete, atomic budget concurrency serta UI route smoke. Status eksekusi runtime dicatat pada laporan pengiriman setelah command selesai; compiling bukan passing device test.
- Signature debug APK diperiksa dengan apksigner. Debug signing bukan release signing/Play Store readiness.

## Pemeriksaan yang membutuhkan perangkat

| Fitur | Status penerimaan |
|---|---|
| Samsung Android 16 install/permissions/restricted settings | NEEDS_REAL_DEVICE |
| Robot drag/dock/size/panel/keyboard/rotation/revoke/Stop | NEEDS_REAL_DEVICE |
| AI inference tiap provider/model/key/fallback/cancel | NEEDS_REAL_DEVICE + akun/kuota; mock protocol tests tersedia |
| Multi-turn Chat/draft/Prompt Library | Native UI smoke disediakan; end-to-end provider nyata masih NEEDS_REAL_DEVICE |
| Workflow copy/launch/AI/download/approval/pause/checkpoint | Pure runner/policy tests tersedia; adapter Android end-to-end NEEDS_REAL_DEVICE |
| Room transaction budget | Instrumentation test tersedia; lihat hasil aktual command perangkat |
| SAF grant, image/video share ke Instagram/TikTok | NEEDS_REAL_DEVICE; status tidak boleh menjadi PUBLISHED |
| Fresh account confirmation/mismatch/expiry/import reset | Pure logic verified; akun aktif tetap manual, NEEDS_REAL_DEVICE |
| Multiple reminders/dependency/Doze/locked notification | NEEDS_REAL_DEVICE; tidak exact-time atau UI execution otomatis |
| MediaProjection per-session/system denial/keyguard/secure/rotation/cleanup | NEEDS_REAL_DEVICE; no bypass. Unit capture allowlist saja bukan bukti frame |
| Vision opt-in/second consent/HTTP protocol/cancel | Mock payload tests tersedia; full system capture + model nyata NEEDS_REAL_DEVICE |
| Agent/skill safe-field test | NEEDS_REAL_DEVICE; pihak ketiga BLOCKED |
| Memory search/edit/delete/export | Unit/instrumentation fixture tersedia; review UX perangkat diperlukan |
| Config export/import inert/new IDs/no keys/reset account | Unit tests tersedia; import tidak menjalankan workflow |

## Aplikasi target

ChatGPT, Claude, Google Flow, TikTok, Instagram: **NOT_TESTED** pada versi HP pengguna. Tidak ada PASS yang dibuat berdasarkan screenshot referensi, model mock atau GitHub build. Login, switching akun dan publikasi dilakukan manual melalui fitur resmi. Native download automation, arbitrary UI clicks/gestures, multi-app adaptive agent dan AI dynamic replanner umum tetap BLOCKED/NOT_IMPLEMENTED.

## Script penerimaan fisik

1. Install, grant/revoke overlay/accessibility/notification. Uji Stop dari toolbar, bubble dan notification saat request/approval berjalan.
2. Tambah provider/model sesuai akun. Uji teks biasa, HTTP failure, cap, consent/fallback dan cancel. Jangan membagikan key sebagai bukti.
3. Chat → draft → pilih media/akun → konfirmasi fresh → handoff. Uji account mismatch/expiry; jangan klaim post sukses dari intent.
4. Simpan workflow, edit membatalkan hash approval. Uji checkpoint selesai/uncertain, pause/stop, app background/locked, approval timeout, import draft tanpa scope.
5. Jadwal one-time/recurring/dependency: foreground tetap tidak dibuka otomatis; notifikasi/antrean menjelaskan WAITING_FOR_USER.
6. Capture target yang diizinkan dengan consent baru; tolak consent, rotate, lock dan uji layar terlindungi. Review lalu batalkan/share/Vision dengan consent kedua. Capture tidak boleh dikirim otomatis.
7. Catat device/OS/app version/operation/results di Diagnostics/Compatibility Lab dengan bukti yang tidak memuat rahasia. P14–P29 pada EXPERIMENTAL_TEST_MATRIX tetap relevan.

## Hasil rilis Studio yang diamati

- Validasi lokal penuh: **138 tes**, **0 failure/error/skipped** pada suite logic + UI JVM; gerbang command lengkap test/build/instrumentation APK compilation/lint lulus pada iterasi tercatat. Assertion UI JVM meliputi dashboard tampil, navigasi Chat, empty provider state dan Stop terlihat; native Canvas menghasilkan preview. Ini menggunakan Robolectric SDK 35, bukan perangkat Android 16.
- Enam instrumentation test Android disediakan dan APK test dikompilasi, tetapi **belum dieksekusi**: emulator AOSP API36 tanpa KVM mengalami discovery/PackageManager/storage-system failures sebelum test method berjalan. Upaya direct install menghasilkan NullPointerException StorageManager di PackageInstaller. Itu blocker infrastruktur emulator, bukan PASS dan bukan bukti bug aplikasi. Emulator QA dihentikan setelah diagnosis.
- Unit/native-schema coverage bertambah untuk ManualInputPermit (8 guard tests). Input pihak ketiga tetap NEEDS_REAL_DEVICE dan hasil harus dicatat per app/versi. Tidak ada klaim target kompatibel hanya dari policy test.
- Preview `docs/screenshots/studio-home.png` adalah render actual Compose view pada JVM SDK35/native Canvas. Bukan foto atau rekaman HP.
- Hasil GitHub Actions tersedia pada run SHA pengiriman. Repo tidak memuat API key atau release signing secret; debug APK signature diverifikasi lokal.

Gerbang akhir Studio 0.4.0: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` selesai exit 0 (BUILD SUCCESSFUL). JUnit: 138/138, 0 failure/error/skipped. Lint: 0 error/fatal, 37 warning nonblocking (termasuk dependency updates, KTX/i18n dan accessibility lint); warning tidak diklaim telah seluruhnya dibersihkan. Signature debug APK diverifikasi terpisah.


## Validasi 0.5.0-workflow — 26 Oktober 2026

- Gerbang penuh testDebugUnitTest / assembleDebug / assembleDebugAndroidTest / lintDebug: BUILD SUCCESSFUL.
- **154/154 tes JVM lulus**, 0 failure/error/skipped. Native UI JVM mencakup template dan editor langkah aplikasi; dialog pemilih launcher berada pada instrumentation yang belum dieksekusi.
- Android Lint: 0 error/fatal, 69 warning nonblocking. APK versi 0.5.0-workflow/code 5, signature debug v2 terverifikasi dengan apksigner.
- Fixture Chromium offline untuk adapter ChatGPT/Flow: PASS. Prompt multiline/literal, hasil lama, upload readiness, satu kali dispatch dan video baru diperiksa. Tidak menghubungi akun situs asli.
- Instrumentation APK dikompilasi; uji perangkat, login WebView, native file chooser, generasi akun nyata dan unduhan sesi/blob ke galeri tetap NEEDS_REAL_DEVICE. Sesi Google dapat menolak login browser tertanam.
- [Panduan workflow web](WORKFLOW_WEB.md), [preview Compose JVM](screenshots/workflow-studio.png).


## Validasi 0.6.0-smart-mapping — 26 Oktober 2026

- Gerbang penuh `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`: **BUILD SUCCESSFUL**.
- **191/191 tes JVM**, 0 failure/error/skipped. Tambahan 26 policy/runner mapping, 10 adapter Accessibility Robolectric, 1 route/latihan Compose.
- Tes mencakup tombol berpindah, label/parent/role berubah, duplikasi, scope situs/aplikasi salah, lock/password, permit expired/single-use, serta checkpoint tanpa retry. Fake node/service bukan aplikasi ChatGPT/Chrome asli.
- UI JVM memeriksa logo BG, route Smart Mapping, counter latihan dan scroll per halaman; [preview dari Compose](screenshots/smart-mapping-studio.png).
- Fixture DOM Chromium offline ChatGPT/Flow: PASS. Lint: 0 error/fatal, 79 warning nonblocking. Signature APK debug v2 terverifikasi; versionCode 6.
- `adb devices -l`: tidak ada emulator/HP tersambung. APK instrumentation dikompilasi; device tests **belum dijalankan**. Interaksi overlay/point/gesture, OEM, URL Chrome dan akun ChatGPT/Flow tetap NEEDS_REAL_DEVICE.
- [Penggunaan dan batas Smart Mapping](SMART_MAPPING.md). Tidak ada klaim kompatibilitas 99,9% dari hasil build/fixture ini.

## Automation V2 — 0.7.0 / 28 Oktober 2026

Baseline commit `89c9b8c`: 220/220 tes JVM dan fixture offline PASS sebelum perubahan. Hasil V2: **270/270 tes JVM**, **0 failure/error/skipped** pada gerbang `./scripts/cloud-build.sh testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` (BUILD SUCCESSFUL). Tes mencakup Kotlin logic/state machine, Accessibility Robolectric SDK35, Room migration/reopen serta Compose UI. Android lint: **0 error/fatal, 93 warning**; warning belum seluruhnya dibersihkan. [Ringkasan terstruktur](qa/automation-v2-cloud.json).

Tambahan pengujian nyata:

- Tombol bergeser, resource ID/struktur berubah, duplikasi, disabled, window/package salah dan snapshot stale; permit input terikat operasi/teks.
- Dialog/panel sistem aktif di atas aplikasi memblokir discovery/klik/Kembali; dekorasi sistem yang tidak aktif tidak memblokir snapshot.
- Discovery input tanpa isi field, checkbox/tab/container scroll, foreign-package child, password/OTP termasuk resource `otp_code`; set-text diterima tanpa nilai sesuai ditolak. Fixture node Robolectric bukan bukti native Binder aplikasi asli.
- Transisi launch→wait saat target loading atau foreground sebelumnya berbeda memakai observasi scope terbatas; Stop menghentikan observasi berikutnya tanpa dispatch.
- IF/ELSE maju, schema/timeout/retry, dry-run mapping hilang/diubah, evidence accepted vs uncertain, Stop saat aksi aktif, timeout generate satu dispatch, provider/network gagal tanpa retry, process death/checkpoint dan review rerun berbiaya.
- Room SQLite asli pada Robolectric: migrasi 1→2 mempertahankan mapping/workflow/checkpoint, recorder draft/reopen dan revoke grant. Tes instrumentation Keystore existing tetap memerlukan Android runtime.
- Compose: route lama, area latihan, recorder dan editor IF/ELSE dapat dijangkau pada layout HP; tombol aksi dibungkus sesuai lebar layar.
- HTTP MockWebServer lokal: Stop menutup call saat menunggu headers dan saat membaca response body, tanpa membiarkan pembatalan menjadi hasil sukses.

`node scripts/test-workflow-adapter.cjs`: **PASS**, Chromium offline. Tambahan kontrak duplicate begin tidak mengklik ulang, selector Send/Generate ambigu menunggu, proyek CURRENT tidak membuat proyek baru, origin/proyek berubah ditolak, upload tanpa file belum selesai, serta hasil lama tidak dipakai. Ini bukan pengujian login/akun/generasi situs asli.

`python3 -m unittest discover -s scripts -p 'test_instrumentation_result.py'`: **4/4 PASS**. YAML workflow dan shell signing diperiksa sintaksnya.

Cloud tidak memiliki perangkat tersambung (`adb devices -l` kosong) atau `/dev/kvm`. APK instrumentation **dikompilasi, belum dijalankan lokal**. UI native tambahan, izin OEM, gesture overlay, rotasi/capture, WebView login, SAF/MediaStore Android nyata, provider/account nyata serta frame timing Galaxy A06: **NEEDS_REAL_DEVICE**. Gunakan [checklist penerimaan](REAL_DEVICE_ACCEPTANCE.md). Riwayat hasil lama di atas tidak mewakili run V2.

Build optimized R8/resource shrinking serta hasil GitHub Actions dicatat pada laporan pengiriman setelah command/run selesai. Private signing key tidak berada di artifact; signature konsisten untuk update memerlukan key pemilik yang sama.

### Pengamatan CI emulator pertama V2

[Run 38 / fc0de00](https://github.com/harvey-moeid/overlay/actions/runs/40736087330) membangun dan mengunggah APK optimized serta lulus fixture browser. Android 15: **9/9 instrumentation PASS**. Android 16: **7/9 PASS, 2 FAIL** pada assertion dialog app picker dan counter latihan setelah sentuhan; tidak diklaim PASS. Tes performa, Room/Keystore/budget lulus. Emulator API36 mencatat 478 frame, p95 66ms, max 177ms, 0 frozen; API35 471 frame, p95 71ms, 0 frozen. Angka emulator bukan performa Galaxy A06.

Helper UI test diperketat agar target berada di area konten di antara toolbar dan navigasi serta bounds stabil sebelum satu klik. Assertion hasil tetap wajib; tidak ditutupi retry klik. Screenshot/hierarchy spesifik kegagalan ditambahkan. Hasil verifikasi revisi harness dicatat pada run CI dan laporan PR #6, terpisah dari hasil gagal di atas.
