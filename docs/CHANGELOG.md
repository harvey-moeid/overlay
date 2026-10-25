# Changelog

## 0.2.0-experimental — 25 Oktober 2026

### IMPLEMENTED (kode + build, bukan bukti perangkat)
- PRD v2 asli disimpan utuh di PRD_v2.md; keputusan dan modul v1 dipertahankan.
- Opt-in Agent Experimental, default OFF; goal/koreksi per task, local draft dan strict JSON AI proposal melalui provider yang pengguna pilih.
- Immutable grant per sesi, scope review, native tool policy, monotonic expiry, step cap, bounded reobserve, observe/act/verify, one UI run mutex, pause/resume/stop/revoke.
- Adapter nyata untuk safe EditText BYGA: set_text + verify_state, package/foreground/keyguard/permission checks. Tool lain tidak dapat dieksekusi.
- Timeline WIB redacted, local task history/delete, skills draft/test/review/approve/enable/disable/version/import/export, memory opt-in CRUD/search/forget/export, Compatibility Lab catatan manual.
- Room schema v2 + additive migration 1→2 menjaga records v1. Migration/memory/codec instrumentation test ditambahkan.
- Emergency Stop aplikasi dan Stop bubble/notification mencabut active agent. CI kini menjalankan lintDebug selain unit test/build.

### VERIFIED_LOCALLY
`./scripts/cloud-build.sh testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` selesai **exit 0**.

**58 unit tests passed**, 0 failures/errors/skipped: 30 agent/policy/lifecycle/concurrency, 19 workflow, 4 AI fallback, 5 media policy. APK aplikasi dan APK instrumentation terkompilasi. Android Lint tidak memiliki error (warning nonblocking tetap ada). Signature debug APK diverifikasi menggunakan apksigner.

### EXPERIMENTAL / NOT TESTED
Uji HP/emulator belum dijalankan. Instrumentation tests migration/Room/Keystore hanya dikompilasi. Provider API nyata tidak dipanggil dalam cloud. Semua P14–P29 tetap NEEDS_REAL_DEVICE/BLOCKED/NOT_IMPLEMENTED sesuai matriks; CI bukan bukti kontrol aplikasi.

### BLOCKED / NOT_IMPLEMENTED
M0 aplikasi pihak ketiga belum memiliki bukti fisik: adapter agent dibatasi ke BYGA. Screenshot/Vision, raw gestures, account-aware multi-app automation, dynamic AI replanning, event hub/agent scheduler, automatic skill learning, monetary cost cap dan publishing/send/delete/purchase agent tidak tersedia. Scheduler lama tetap pengingat dengan WAITING_FOR_USER.

GitHub Actions outcome dicatat terpisah pada run commit terkait; status lokal di atas tidak mengklaim CI telah berjalan.

### CI setup correction
Run pertama berhenti sebelum build karena default setup-android meminta legacy SDK package `tools` yang tidak tersedia. Workflow sekarang hanya memasang `platform-tools` dan mematok command-line tools `13114758` yang telah dipakai lokal. Build/test aplikasi lokal tetap lulus; run koreksi menjadi bukti CI tersendiri.
