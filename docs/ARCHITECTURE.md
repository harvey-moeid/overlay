# Arsitektur yang dibuat

Seluruh orkestrasi di Android; tidak ada server pribadi, analytics, atau login pihak ketiga.

- `MainActivity.kt`: Compose Material 3 navy/cyan, tab navigasi, permission disclosure, prompt/copy/share, test input, editor lokal, profil akun, konfigurasi provider, pengingat, monitor, diagnostik manual, privasi.
- `overlay/OverlayService.kt`: foreground `specialUse`, WindowManager maskot robot transparan 56–96dp (default 72dp), drag/dock, panel prompt focusable hanya saat dibuka, notification Stop, long-press Stop, cleanup/rotation.
- `accessibility/SafeAccessibilityService.kt`: tidak mengumpulkan teks event. Hanya package BYGA dan selector kolom uji, password dikecualikan, rate limit. Click/scroll/gesture pihak ketiga BLOCKED. Bukan accessibility tool untuk disabilitas.
- `workflow/Workflow.kt`: typed action enum, schema validation, hash persetujuan, policy akun/retry/scheduler, ledger in-memory. Executor PoC di Activity **hanya** COPY_TEXT/DELAY/STOP; approval tidak dipersist. Job dibatalkan saat Activity dihancurkan. Room checkpoint tidak memicu replay otomatis. Ledger bukan jaminan durable at-most-once; tidak ada aksi publish yang dieksekusi.
- `data/Storage.kt`: Room records berdasarkan jenis/id, DataStore preference, AES-GCM dengan Keystore dan AAD provider id; backup aplikasi dinonaktifkan. Schema v1/v2 diekspor; migrasi 1→2 menambahkan tabel agent dan menjaga records lama. Record payload JSON saat ini belum model relational lengkap PRD.
- `ai/Providers.kt`: adapter teks OpenAI-compatible (OpenAI/OpenRouter/Groq/Hugging Face/Mistral/NVIDIA NIM/custom), Cohere Chat v2, Anthropic Messages, Gemini generateContent; katalog saran + model discovery on-demand. HTTPS, timeout 30s, redirect/retry transport dimatikan, respons dibatasi 1MiB, error hanya kode. Health test eksplisit di UI. Fake dan fallback maksimal tiga adapter tersedia; routing fallback, role dan budgets belum terhubung UI. Vision belum diimplementasi.
- `media/Handoffs.kt`: clipboard, ACTION_SEND teks/media dengan URI grant, app launch intent. Media dipilih melalui SAF; status share hanya HANDED_OFF. DirectDownload memeriksa HTTPS, MIME, signature awal dan streaming <=50MB, menolak redirect, menulis MediaStore pending lalu memverifikasi file sebelum selesai (Android 10+).
- `scheduler/ReminderWorker.kt`: WorkManager pengingat sekali/periodik (>=15 menit); menyimpan WAITING_FOR_USER dan notifikasi. Tidak menjalankan UI di background, tidak menjanjikan exact time.

Penyimpanan provider hanya menyertakan metadata non-secret. Key tidak ada pada database, log, backup atau build. Tidak mengekspor data otomatis. Layar Android dapat dihentikan/di-recreate; belum ada recovery otomatis berisiko.

Belum terimplementasi: macro recording, AI workflow import, branching executor, native download controller, MediaProjection/Vision, posting automation, verifikasi identitas akun otomatis, dependency/priority queue. UI menjelaskan batasan ini.

Ekstensi agent v2: lihat [AGENT_ARCHITECTURE.md](AGENT_ARCHITECTURE.md). Modul v1 di atas tetap dipertahankan.
