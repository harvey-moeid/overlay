# Automation V2 — 0.7.0 (28 Oktober 2026)

Implementasi terhubung: typed discovery + resolver status + native app grant, input/scroll/back/wait mapping, guided recorder Room, IF/ELSE dan retry observasi, dry-run database binding, evidence/approval/cancellation runner, history per run, guard duplicate dispatch browser, manual account/project approval, proyek NEW/CURRENT, serta AI assistance opt-in tanpa eksekusi model. Tidak ada schema Room baru atau data lama yang dihapus.

Parsial: verifikasi hasil native click/scroll/back masih meminta pemeriksaan manusia; identitas berubah memerlukan pemetaan ulang; dukungan UI situs asli, app tambahan dan performa Samsung belum diukur. Recorder tidak merekam semua touch; AI bukan autonomous agent pihak ketiga. Browser resmi merupakan handoff manual. Release update lintas CI memerlukan key pemilik yang sama.

Lihat [audit dan referensi lisensi](AUTOMATION_V2_AUDIT.md), [panduan V2](WORKFLOW_STUDIO_V2.md), [tes aktual terbaru](TEST_MATRIX.md), dan [penerimaan perangkat](REAL_DEVICE_ACCEPTANCE.md). Status historis di bawah tetap untuk baseline, bukan klaim fitur V2 belum ada.

---

> Pembaruan 0.6.0: maskot BG/byga, Smart Mapping deteksi/tunjuk, library dan CLICK_MAPPING tersedia. Klik terikat scope dan versi; posisi manual selalu dikonfirmasi. [Panduan dan bukti](SMART_MAPPING.md). Pengujian target asli belum dijalankan.

> Pembaruan 0.5.0: pemilih aplikasi terinstal dan adapter workflow ChatGPT/Flow tersedia, termasuk unggah dan unduh media. Build/fixture tidak membuktikan login atau kompatibilitas situs asli. Lihat [panduan workflow web](WORKFLOW_WEB.md). Status historis Studio di bawah tetap menjadi baseline untuk modul lain.

# Status terhadap PRD — 0.4.0 Studio

Tidak ada klaim final 99,9%. IMPLEMENTED berarti ada kode/alur yang terhubung; keberhasilan runtime hanya jika tercatat pada TEST_MATRIX. Semua aplikasi pihak ketiga tetap memerlukan uji fisik per versi.

| Lingkup PRD | Implementasi saat ini | Gap / batas |
|---|---|---|
| Overlay & text gestures | Robot transparan, drag/dock, panel text scrollable, clipboard/share, reviewed focused-field input single-use, notification/long-press Stop, resize/reduce motion | Gesture map/double-tap custom belum ada; Samsung perlu uji |
| Manual target input | Review teks/akun/package, permit 10 detik single-use, field fokus yang editable/non-password, rate-limit dan verifikasi nilai | Experimental untuk whitelist BYGA/ChatGPT/Claude/TikTok/Instagram; tidak mengirim/post. Per app NEEDS_REAL_DEVICE |
| Permission engine | Disclosure, overlay/accessibility/notification settings, lifecycle refresh, restricted-settings guidance | Pencabutan izin/capture real-device perlu diuji |
| Provider hub | 10 types, encrypted key, model live/suggestion/manual, text adapters, health | Role routing dan dollar cost cap belum ada; tiap API butuh key/kuota |
| AI content/conversation | Multi-turn Chat, history, opt-in fallback, request cap, cancel, draft handoff | Tidak streaming; context 8 pesan/24k karakter; capability Vision dikonfirmasi pengguna per model |
| Workflows | Library/template/editor/reorder/JSON/dry run/hash review, 7 action adapters, pause/resume/stop/checkpoint | DSL lain, branching/macro/cross-app selectors belum tersedia |
| Downloads | HTTPS streaming, MIME/signature, 50MB, scoped MediaStore verification | Native app download manual; login/redirect/DRM diblokir |
| Social drafts | URI picker/image preview, caption/hashtags, local library, package+account review, targeted sharesheet | Tidak mempublikasikan/menyimpulkan PUBLISHED, no platform API |
| Accounts | Typed public aliases, legacy read, guided official launch, 5-minute manual confirmation, edit/delete/login status | Tidak mendeteksi akun aktif otomatis atau menyimpan sesi/password |
| Scheduler | Multiple one-time WIB/periodic, priority list, manual dependency, history/pause/delete, bounded retry | Hanya pengingat WAITING_FOR_USER; tidak exact minute/OS priority/auto UI |
| Screen state | Safe-field observation agent, scoped one-shot MediaProjection, local preview/delete, image analysis opt-in dan second consent | Protocol Vision OpenAI-compatible/Anthropic/Gemini tersedia; API nyata perlu key/model yang sesuai. Cohere Vision, coordinate actions dan continuous capture tidak tersedia |
| Experimental agent | Default OFF, task scope, policy, reobserve/verifier, stop, local tool harness, provider plan draft | Agent tidak mengontrol aplikasi pihak ketiga; dynamic AI replanning belum umum |
| Skills & learning | Versioned draft/test/review/approve/enable/disable, strict import/export | Hanya safe-field plan; automatic learning/recording belum tersedia |
| Memory | Manual opt-in, keyword search/edit/forget/export, isolated task history | Bukan auto memory app lain; retention automation belum umum |
| Monitoring | Workflow StateFlow, Room execution/checkpoint history, agent state, stop controls | Tidak ada fake telemetry atau klaim semua platform selesai |
| Event hub | User taps, reminder workers, own download completion in UI | App foreground/download automatic skill subscriptions belum ada |
| Privacy/data | AES-GCM key, no backup/analytics/backend, private records/cache, explicit export/import guards | Database teks biasa app-private; perangkat terkompromi masih berisiko |
| CI/APK | Unit tests, debug build/lint, upload APK and reports | CI tidak menggantikan device acceptance atau release signing |

Rilis ini memperluas alur yang benar-benar dapat dipakai, dengan fitur yang belum diimplementasi tetap diblokir/diungkap. Lihat TEST_MATRIX untuk hasil aktual terbaru, bukan menggunakan angka persentase kesiapan buatan.
