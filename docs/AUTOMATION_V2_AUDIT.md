# Audit dan referensi Automation V2

Baseline repository: `89c9b8c46eaf9f5ab36bf3750269591a1b5c8a2c` (branch kerja awal `work`), working tree bersih. Audit meliputi inventaris seluruh file tracked, README, SMART_MAPPING, WORKFLOW_WEB, IMPLEMENTATION_STATUS, TEST_MATRIX, alur MainActivity/overlay/accessibility/mapping/workflow/browser/AI/capture, records Room dan migrasi, instrumentasi, fixture serta CI.

## Baseline aktual 28 Oktober 2026

`./scripts/cloud-build.sh testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`: BUILD SUCCESSFUL, **220 tes JVM, 0 failure/error/skipped**. `node scripts/test-workflow-adapter.cjs`: PASS pada Chromium offline. Peluncuran awal tanpa eskalasi sandbox gagal karena socket lokal Gradle/Chromium dibatasi; command yang sama berhasil setelah eskalasi eksekusi. Ini bukan perubahan konfigurasi TLS/keamanan aplikasi.

## Temuan dan implementasi

| Komponen existing | Temuan | Perubahan V2 |
|---|---|---|
| Discovery native | Hanya clickable button, metadata belum bertipe | Input/toggle/tab/scroll, window/parent/struktur, penapisan field sensitif dan grant app native |
| Resolver | Boolean/null menggabungkan target hilang, kembar dan scope salah | Enam status eksplisit, kandidat berubah hanya untuk review |
| Studio | Editor JSON/urutan/mapping ada; IF/wait/recorder belum terhubung | Editor kondisi/timeout/retry observasi, IF/ELSE maju, recorder draft Room, duplikasi dan dry-run binding DB |
| Runner | Checkpoint uncertain ada; verifikasi belum seragam, Stop bergantung pembungkus VM | Fase/evidence, human verification untuk accepted native action, cancellation child action, review rerun |
| Browser | Activity baru bisa mengulang begin command; tombol pertama dapat dipilih saat ambigu | Owner command native, guard dispatch JS, selector unik, approval akun/proyek/aksi, project identity, handoff resmi |
| Flow | Bukti upload dapat berasal dari gambar baru umum | Preview harus berlabel input/frame; proyek CURRENT/NEW; output video harus baru |
| AI | Provider/Agent Lab/Vision existing tersedia | Assistance opt-in dengan preview metadata dan diagnosis tanpa tool execution |
| Performa | Bubble/frame telemetry sudah tersedia | Pengukuran scan/node/heap dan historical ANR; snapshot tetap on-demand background |
| Media | Pembatalan bisa menunggu blocking HTTP read | Call network terikat cancellation sampai body selesai, dengan tes socket lokal |
| Penyimpanan | Room versi 2 dengan JSON records dapat dipakai ulang | Tidak ada migrasi destruktif/schema baru; tes migrasi nyata v1→v2 dan recorder/reopen ditambahkan ke JVM |
| CI/signing | Pipeline optimized ada, signing debug runner tidak stabil lintas run | Pipeline dipertahankan, lint report artifact; opsi signing key pemilik melalui secrets, fallback debug diungkap |

## Referensi dan lisensi yang diperiksa

Tidak ada source/dependency/aset disalin dari keempat repository. Implementasi memakai Android APIs dan engine BYGA sendiri. Lisensi copyleft tidak diasumsikan kompatibel karena repository BYGA belum menetapkan lisensi distribusi yang dapat dievaluasi untuk penggabungan kode tersebut.

| Repository / SHA saat diperiksa | Lisensi | Pendekatan yang dipelajari |
|---|---|---|
| [droidrun/mobilerun-portal](https://github.com/droidrun/mobilerun-portal/tree/94ecc89d02ac86f08076d7d354b612d3dd54541f) | AGPL-3.0-or-later | Root/window selection dan bounded traversal; `AccessibilityTraversalGuard.kt` serta struktur tree builder |
| [tokenarc/open-jarvis](https://github.com/tokenarc/open-jarvis/tree/04873998933830ce582018fe4664ebce5ec64d2f) | MIT | `SelfHealingExecutor.kt`: observe/recovery; BYGA tetap tidak mengadopsi retry action berdasarkan saran model |
| [Nain57/Smart-AutoClicker](https://github.com/Nain57/Smart-AutoClicker/tree/699bff818519bc418addb7f0bdad5fbfb6c3f720) | GPL-3.0 (header berkas: or later) | `GestureExecutor.kt`, cancellation/timeout/lifecycle, pemilihan visual dan flow control; tidak mengadopsi workaround injeksi gesture/proteksi |
| [ghorbelhamdi/buddy-android](https://github.com/ghorbelhamdi/buddy-android/tree/3578b89db131a814d05dcd89ce4b64269d8386de) | Apache-2.0 | `Guard.java`: approval satu tindakan dan scope package/window; BYGA memakai permit dan approval existing sendiri |

LICENSE dan README masing-masing serta berkas teknis di atas dibaca melalui koneksi GitHub sebelum implementasi. Referensi bukan bukti kompatibilitas BYGA atau alasan memperluas akses sensitif.

## Batas yang tetap terbuka

UI native tambahan eksperimental per app/versi; deteksi data sensitif best effort. Tidak merekam semua sentuhan, tidak mengontrol browser generik, tidak ada cookie sharing atau bypass CAPTCHA/login. Native click/scroll/back belum memiliki verifier bisnis otomatis umum. AI tidak menjalankan replanning lintas aplikasi. Login/generate/SAF/MediaStore/overlay/One UI dan performa Galaxy A06 masih NEEDS_REAL_DEVICE. Hasil cloud final dicatat pada TEST_MATRIX dan laporan QA, terpisah dari hasil historis.
