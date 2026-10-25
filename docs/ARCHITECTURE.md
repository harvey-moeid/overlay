# Arsitektur Studio 0.4.0

Seluruh orkestrasi di Android; tidak ada server pribadi, analytics atau penyimpanan login pihak ketiga. Rincian modul dan lifecycle: [STUDIO_ARCHITECTURE.md](STUDIO_ARCHITECTURE.md). Batas implementasi terhadap PRD: [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md).

- MainActivity/DesignSystem: Compose Material3, navigasi tetap, state per halaman, status izin/service aktual dan STOP global.
- OverlayService: foreground specialUse, maskot transparan/drag/dock, panel Quick Studio, notification/long-press Stop. Input manusia dibatasi permit singkat dan satu field terverifikasi; bukan otomasi kirim/post.
- SafeAccessibilityService: metadata package/fokus dan pengaman field, scoped manual ACTION_SET_TEXT; agent hanya tool harness milik BYGA. Tidak ada click/gesture agent pihak ketiga.
- ChatViewModel/AiRepository: conversation lokal terbatas, provider pilihan/consent, fallback opt-in dan shared transactional request count. Provider HTTPS async cancellable, tanpa retry/redirect otomatis; native text/image protocols sesuai capability. Monetary cap belum ada.
- WorkflowRunner/WorkflowViewModel: typed JSON, review hash, single-run mutex, approvals, tujuh action adapters, checkpoint uncertain dan pause/resume/stop. Aksi DSL belum didukung tetap BLOCKED.
- CaptureService/VisionReview: MediaProjection satu frame dengan izin sistem per sesi, foreground mediaProjection, target/lock/sensitive checks best effort, cache privat dan preview berpaginasi. Upload gambar perlu consent kedua. Vision output teks tidak dieksekusi sebagai tool.
- Accounts/Publishing/Handoffs: konfirmasi akun manual berumur pendek, SAF URI grants, draft library dan sharesheet tujuan eksplisit; hasil hanya HANDED_OFF.
- ScheduleWorker: pengingat sekali/periodik WorkManager, dependensi completion manual dan notifikasi WAITING_FOR_USER. Tidak menjalankan AI/UI otomatis atau menjanjikan exact time.
- Storage/ConfigTransfer: Room schema2 dengan migrasi1→2, records dan tabel agent, DataStore preference; AES-GCM Android Keystore khusus API keys. Backup aplikasi dinonaktifkan. Export/import whitelist mengecualikan secrets, chat, capture dan active grants; import tidak memberi persetujuan eksekusi.

Dokumen agent khusus: [AGENT_ARCHITECTURE.md](AGENT_ARCHITECTURE.md). Agent experimental tetap OFF default dan tidak mendapat kemampuan pihak ketiga dari jalur input manual. Database teks bersifat app-private, bukan seluruhnya terenkripsi; cache tidak dikirim otomatis. Pengujian dan batas runtime: [TEST_MATRIX.md](TEST_MATRIX.md), [QA_ANDROID.md](QA_ANDROID.md).
