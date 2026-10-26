# Changelog

## 0.6.0-smart-mapping — 26 Oktober 2026

Validasi lokal: 191/191 tes JVM, fixture Chromium offline, build APK/instrumentation dan lint lulus; signature debug v2 valid. Tidak ada perangkat tersambung; interaksi aplikasi asli belum diuji.

- Maskot BG/byga transparan sesuai referensi pada launcher adaptif, header, dashboard, Settings dan bubble 56–128dp.
- Smart Mapping: deteksi, tunjuk tanpa meneruskan sentuhan, sorot target, pertanyaan Simpan/Uji/Batal, library lokal, aktif/nonaktif/edit/hapus dan area latihan.
- Identitas tombol per package/situs/layar; kandidat kembar ditolak untuk replay semantik. Fallback posisi selalu dikonfirmasi dan terikat geometri/orientasi/layout.
- Aksi CLICK_MAPPING memakai fingerprint versi mapping serta permit sekali pakai, origin/window/lock checks, pause/Stop dan checkpoint tanpa retry. CLICK_ACCEPTED bukan bukti hasil Generate/Download.
- [Panduan, batas dan penerimaan](SMART_MAPPING.md). Agent Lab tidak diberi akses raw klik baru; uji akun/HP asli belum dijalankan.

## 0.5.0-workflow — 26 Oktober 2026

Validasi lokal: 154/154 tes JVM, fixture Chromium offline, build APK/instrumentation dan lint lulus. Signature debug v2 valid. Uji perangkat/akun asli belum dijalankan.

- Pemilih aplikasi terinstal dengan pencarian pada editor workflow dan Apps; query Android hanya aplikasi launcher pada profil aktif.
- Browser workflow pada halaman resmi ChatGPT/Flow, template prompt → pengambilan teks/gambar, proyek → unggah gambar → generate → video, serta gambar ChatGPT → Flow. Sesi login lokal terpisah dari aplikasi native.
- SAF gambar, izin baca persisten, upload terikat langkah, pemeriksaan MIME/signature/ukuran, transfer media sesi/blob bertahap dan MediaStore terverifikasi.
- Aksi Kirim/Generate satu kali diikuti observasi; timeout 10 menit, pause/Stop/lock/foreground gates, tanpa resume sesi web sesudah proses mati.
- Login WebView dan perubahan tampilan situs dapat menghalangi alur; belum diuji pada akun/HP asli. Detail, cara pakai dan penerimaan: [WORKFLOW_WEB.md](WORKFLOW_WEB.md).

## 0.4.0-studio — 25 Oktober 2026

### Implementasi
- Dashboard navy/cyan, maskot robot, navigasi Home/Chat/Alur/Media dan semua modul, kartu status aktual, kontrol STOP tetap di header.
- Chat AI multi-turn dan riwayat lokal, provider/model pilihan pengguna, fallback opt-in, cancellation transport, shared atomic daily request cap untuk Chat/Workflow/Vision.
- Workflow library/editor/template/JSON, dry run, hash approval, executor tujuh aksi, pause/resume/stop, checkpoint yang menolak replay aksi belum pasti.
- Prompt library, profil aplikasi/akun, guided switching dan konfirmasi akun kedaluwarsa, draft media/caption/hashtag dan handoff sharesheet dengan review akhir. Handoff tidak dianggap posting berhasil.
- Scheduler multi-pengingat WIB, periodic/dependency/manual completion, history/pause/delete; tetap WAITING_FOR_USER tanpa auto UI.
- MediaProjection satu frame, cache/preview lokal terbatas, pemeriksaan package/lock/field best effort, consent upload terpisah; adapter Vision OpenAI-compatible, Anthropic dan Gemini.
- Input manual kolom fokus melalui bubble: review teks/target/akun, permit single-use 10 detik terikat field, pemeriksaan sensitif dan verifikasi nilai. Tidak ada tombol kirim/post otomatis; agent pihak ketiga tetap diblokir.
- Backup konfigurasi terpilih tanpa key/chat/capture, import dengan ID baru, akun direset dan workflow tanpa persetujuan aktif. Clear logs menjaga template/provider metadata.

### Validasi dan batas
138 tes JVM termasuk UI Compose nyata melalui Robolectric SDK35. Gerbang lokal: `./scripts/cloud-build.sh testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`. Hasil terakhir tercatat di TEST_MATRIX; laporan UI JVM menghasilkan preview dashboard, bukan bukti perangkat Android 16.

Enam instrumentation tests disediakan dan APK tes dikompilasi. Upaya AVD API36 gagal pada service/discovery dan PackageInstaller/StorageManager sebelum pengujian aplikasi; tidak ada device test yang diklaim lulus. Provider inference memakai key nyata dan seluruh alur Accessibility/MediaProjection/target app masih perlu QA perangkat. Lihat QA_ANDROID, IMPLEMENTATION_STATUS dan COMPATIBILITY. Ini debug build, bukan release signing atau klaim final 99,9% PRD.

## 0.3.0-mascot-providers — 25 Oktober 2026

### IMPLEMENTED
- Bubble robot biru terminal RGBA transparan, mengganti huruf B. Drag/dock/panel/Stop dipertahankan, ukuran 56–96dp dengan preview Settings dan DataStore. Click/long-click memakai aksi view aksesibel; tidak ada idle animation loop.
- Tambahan Cohere Chat v2, Hugging Face Inference Providers, Mistral dan NVIDIA NIM; semua provider lama dipertahankan. Adapter Cohere native, tiga provider baru lainnya OpenAI-compatible.
- Pemilih provider yang terlihat seluruhnya, model suggestions + filter/dropdown/manual ID, authenticated model discovery on-demand, label pagination/akses, key dibersihkan saat pindah provider. Respons discovery dari key/provider lama tidak diterapkan ke pilihan baru.
- Model/catalog/http protocol tests menggunakan respons mock dan fake key, termasuk Cohere payload/parser, paths/auth, HF routing ID, model filtering/pagination, errors, empty/oversized response dan tanpa retry/redirect.

### VALIDATION / LIMITS
80 unit tests (22 tes provider baru + 58 sebelumnya). Perintah validasi: `./scripts/cloud-build.sh testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`. Validasi lokal selesai exit 0; Android Lint tanpa error, signature APK valid. Status CI disertakan dalam laporan pengiriman; tests perangkat/instrumentation tidak diklaim dijalankan.

Katalog publik HF/NIM dan SDK/docs resmi dipakai untuk verifikasi kontrak, bukan inference berbayar. Ketersediaan model, kuota/kredit, izin gated model dan tampilan maskot/gesture di HP masih NOT_TESTED. Saran model tidak menjamin akses akun. Tidak ada peningkatan scope agent atau izin Android baru. Referensi: PROVIDERS.md dan BUBBLE_MASCOT.md.

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
