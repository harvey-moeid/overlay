# BYGA Smart Overlay — Experimental Mobile AI Agent (PRD v2.0)

**Tanggal:** 25 Oktober 2026  
**Status:** Rancangan v2.0 berdasarkan keputusan final #1–#22 dan permintaan baru **Experimental Agent** pada 25 Oktober 2026; **belum diimplementasikan maupun dibuktikan pada perangkat**  
**Target:** APK Android pribadi/sideload, tanpa root, tanpa backend pribadi, AI cloud via API langsung dari APK, CI build di GitHub Actions  
**Bahasa antarmuka:** Bahasa Indonesia (internasionalisasi dapat disiapkan)  
**Metode pengembangan:** PoC terlebih dahulu, iteratif, fitur dibatasi oleh hasil verifikasi Android dan aplikasi tujuan.

## Catatan revisi v2.0

- **Keputusan #1–#22 dari v1.0 tetap menjadi baseline.** Semua kemampuan lama (overlay, aplikasi/akun, AI provider, download, social drafts, scheduler, visual workflow, GitHub Actions) dipertahankan.
- **Keputusan ekstensi #23: Experimental AI Agent.** Agent dapat mengamati UI, memilih dari tool yang diizinkan, mengeksekusi aksi dalam cakupan tugas yang disetujui, memverifikasi hasil, dan mengusulkan revisi rencana. *Tidak* berarti akses bebas tanpa batas.
- Dibandingkan v1.0, penambahan utama mencakup **Agent Runtime, percakapan berkelanjutan, Tool Registry, Skills, Memory, Screen State, Dynamic Replanning, Event Triggers, Action Policy, Compatibility Lab, dan evaluasi agent**.
- **Distribusi:** sideload untuk penggunaan pribadi/eksperimental. Ini **bukan** pernyataan bahwa fitur autonomi Accessibility memenuhi kebijakan publikasi Google Play; penggunaan Accessibility untuk agent yang secara otonom memulai, merencanakan, dan mengeksekusi tindakan dilarang oleh kebijakan Google Play untuk aplikasi biasa. Tidak boleh mengklaim pengecualian alat disabilitas bila tidak benar-benar memenuhi syarat.
- **Basis sumber:** isi v1.0 digabung, bukan diganti secara diam-diam. Bagian 16–27 merupakan persyaratan tambahan v2.0.

## 1. Ringkasan produk

BYGA Smart Overlay adalah asisten produktivitas Android berbentuk **floating bubble beranimasi premium (inspirasi gerak Codex, bukan meniru merek/aset)**. Bubble muncul di atas aplikasi seperti ChatGPT, Claude, Google Flow, TikTok dan Instagram, untuk membantu pengguna memilih aplikasi/akun, memasukkan teks, mengunduh media yang dapat diakses, menyiapkan posting sosial, menjalankan workflow yang disetujui, dan menjadwalkan pekerjaan. Versi v2.0 menambahkan **Experimental Agent**: pengguna memberikan tujuan, agent merencanakan langkah, menjalankan tindakan yang diizinkan, memeriksa hasil, dan meminta persetujuan ulang jika perlu memperluas cakupan tugas.

Perangkat melakukan semua orkestrasi lokal dan menghubungi API provider AI cloud **langsung dari APK**, bukan melalui Cloudflare/Render/VPS/backend milik pengguna. Internet tetap diperlukan untuk permintaan AI cloud. Tidak ada klaim bahwa overlay dapat mengontrol semua aplikasi, semua akun, atau proses layar terkunci.

**Prinsip v2.0:** UI automation tetap **user-controlled, observable, pausable, revocable**. Mode aman mengutamakan skrip deterministik. Mode **Experimental Agent** bersifat opt-in dan boleh memilih/revisi tindakan *di dalam scope grant* yang eksplisit (aplikasi, akun, kategori aksi, waktu, jumlah langkah, dan anggaran). Semua tindakan melewati Policy Engine; perubahan cakupan dan aksi sensitif meminta konfirmasi langsung. Tidak ada akses bebas ke seluruh sistem atau jaminan dapat mengendalikan semua aplikasi. Mode eksperimental ini ditargetkan untuk APK pribadi/sideload, bukan menjamin kepatuhan Play Store.

## 2. Tujuan & ukuran keberhasilan

1. Membuka/menutup bubble dari Android dengan interaksi halus, tidak menutupi UI penting, selalu tersedia kendali Stop.
2. Dari bubble: input prompt dan kirim teks ke field yang dikenali dengan izin pengguna; kembali ke aplikasi asal secara aman.
3. Kelola banyak provider AI, model, API key, fallback, dan biaya perkiraan tanpa server sendiri.
4. Simpan/membagikan gambar atau video hanya melalui mekanisme resmi/URL yang dapat diakses dengan izin.
5. Rencanakan dan jalankan workflow **terverifikasi** lintas aplikasi; posting selalu memerlukan konfirmasi langsung.
6. Mengenali aplikasi dan akun aktif secara **best effort**; tidak pernah mempublikasikan ke akun yang belum diverifikasi.
7. Menjalankan scheduler sesuai batasan Android, menyimpan checkpoint dan menunda langkah layar bila terkunci.
8. Berhasil menjalankan build `assembleDebug` dan unit test melalui GitHub Actions; APK tersedia sebagai artifact.
9. Agent memahami instruksi multi-langkah melalui percakapan, mengingat konteks tugas aktif, menampilkan rencana, dan melaporkan bukti hasil.
10. Experimental Agent dapat melakukan **observe -> propose -> policy-check -> act -> verify -> recover** dalam scope tugas yang diberikan; setiap aksi bisa dihentikan.
11. Pengguna dapat menyimpan skill yang berhasil dan memakai kembali dengan review; memori jangka panjang tersedia dengan kontrol hapus/ekspor.
12. Kompatibilitas diputuskan per aplikasi + versi + operasi berdasarkan pengujian fisik, bukan berdasarkan keberhasilan CI semata.

**Indikator PoC:** minimal 1 demonstrasi nyata/fitur inti, per aplikasi yang memungkinkan, dengan hasil PASS/FAIL/BLOCKED, rekaman langkah uji yang tidak mengekspos data rahasia, dan alasan kegagalan yang jelas. Keberhasilan CI **bukan** bukti interaksi di HP asli.

## 3. Keputusan final #1–#22

| # | Keputusan | Spesifikasi disetujui |
|---|---|---|
| 1 | **D — All-in-One Smart Overlay** | Launcher + asisten AI + prompt manager + automation |
| 2 | **A+ — Floating Bubble** | Bubble mengambang dengan animasi halus bergaya Codex |
| 3 | **D — Smart Hybrid Automation** | Manual, semi-otomatis, atau otomatis **pada langkah yang didukung dan disetujui**; publikasi perlu konfirmasi |
| 4 | **A+B — Clipboard & Share + Accessibility Controller** | Tidak mengintegrasikan API aplikasi tujuan pada fase awal; gunakan intent dan aksi UI yang diizinkan |
| 5 | **D — Smart Workflow Engine** | One-tap, macro/rekam aksi yang disetujui, bantuan AI untuk memahami langkah dan error |
| 6 | **D — Smart Multi-Provider AI Orchestrator** | Konfigurasi model, fallback, peran Planner/Analyzer/Validator, usage/biaya |
| 7 | **A+B — Accessibility Reader + Screenshot Vision** | Struktur UI diutamakan; screenshot on-demand dengan izin, tidak memantau layar terus-menerus |
| 8 | **A+B — Native Download + Direct Media Download** | Tombol resmi dan URL langsung yang tersedia; bukan screen recording sebagai metode unduh |
| 9 | **D — Smart Social Publishing Engine** | Compose/share, format lintas platform, antrean, preview, konfirmasi posting |
| 10 | **D — Smart Android Permission Engine** | Wizard izin, cek status, toggles, privasi dan stop darurat |
| 11 | **A — Full Android Local Engine** | Orkestrasi dan data di HP; permintaan API langsung ke provider tanpa server sendiri |
| 12 | **A — Basic Local Storage** | Pengaturan/data lokal; API key dienkripsi memakai Android security primitives; tanpa biometrik wajib |
| 13 | **D — Smart Adaptive App Engine** | Deteksi aplikasi aktif best effort, app profiles, editor target dan workflow |
| 14 | **D — Intelligent Self-Healing Workflow** | Retry terbatas, revalidate selector/state, checkpoint/resume, no double-post |
| 15 | **D — Smart End-to-End Feasibility** | PoC APK + tes pada HP/app sungguhan sebelum klaim kompatibilitas |
| 16 | **B — GitHub Actions APK Builder** | Android build/test otomatis; debug APK melalui GitHub Actions artifacts, tidak mensyaratkan Releases |
| 17 | **A+C — Text + Quick Actions/Gestures** | Tidak perlu voice; perintah teks, tap, long-press, shortcut dan gesture |
| 18 | **D — Advanced Smart Scheduler** | One-time, recurring, sequential, antrean, prioritas, retry, pause, riwayat |
| 19 | **D — Smart Adaptive Execution** | Background hanya pekerjaan yang boleh; interaksi UI ditunda saat HP tidak siap |
| 20 | **D — Smart AI Workflow Studio** | Editor manual + blok visual + AI drafts + template + branching + test step |
| 21 | **D — Smart Live Automation Dashboard** | Status bubble, langkah berjalan, log, provider aktif, pause/resume/stop |
| 22 | **D — Smart Multi-Account Engine** | Per-app account profiles, switch lewat mekanisme resmi, verifikasi akun sebelum tindakan |

**Ekstensi #23 (v2.0): Experimental Agent Mode — D.** Aktif manual, default OFF; per-task scope consent; mendukung perencanaan, seleksi tool, eksekusi terbatas, observasi ulang, replanning dalam scope, checkpoint, penjelasan alasan tindakan, dan eskalasi izin. Tidak membatalkan keputusan #1–#22.

## 4. Target pengguna, contoh skenario

- **Pembuat konten pribadi**: Buat prompt di ChatGPT/Claude -> buka Google Flow -> input prompt -> download hasil jika tersedia -> siapkan TikTok/Instagram -> tinjau identitas akun -> konfirmasi posting.
- **Pengguna beberapa akun**: Pilih `Flow - Akun 2`; buka menu switch resmi; verifikasi handle/email yang ditampilkan atau minta konfirmasi; jalankan template spesifik akun.
- **Scheduler**: pukul 09.00 membuat konsep caption menggunakan AI dan menyiapkan draft lokal; jika pukul 12.00 perlu membuka TikTok tetapi ponsel terkunci, kirim notifikasi 'Butuh tindakan pengguna' dan lanjutkan setelah unlock/konfirmasi.
- **Eksperimen lintas aplikasi**: "Buka Claude, buat ide konten, pindah ke Flow, siapkan prompt video lalu buat draf di TikTok". Agent menjelaskan rencana, meminta scope grant, memvalidasi hasil setiap tahap, dan meminta konfirmasi sebelum mengirim/mempublikasikan.
- **Skill berulang**: setelah pengguna menguji "Buat caption di ChatGPT", simpan sebagai skill lokal; penerapan skill di versi UI lain memerlukan uji ulang jika berubah.
- **Penanganan kegagalan**: saat tombol target tak ditemukan, agent melakukan inspeksi UI sekali lagi, mencoba alternatif yang diperbolehkan, atau berhenti dengan status BLOCKED beserta saran manual.

## 5. Lingkup, bukan lingkup, dan batasan

### In scope

- APK Android native, bubble overlay draggable/dock, launcher, panel teks, quick actions, izin/diagnostik.
- Provider AI berbasis HTTP adapter, pemilihan model/role, fallback, timeout, rate-limit, masking API key.
- Accessibility untuk membaca/berinteraksi dengan UI yang diizinkan **dengan tindakan/skrip eksplisit yang disetujui pengguna**.
- Screenshot on-demand via API resmi dengan persetujuan sistem, dikirim ke provider AI **hanya setelah pengungkapan dan persetujuan pengiriman data**.
- Download native via tombol resmi atau URL HTTP(S) berizin, dengan Android `DownloadManager`/`MediaStore`/SAF.
- Android Sharesheet, draft caption, hashtag, preview konten, status posting 'menunggu konfirmasi'.
- Workflow editor, rule-based executor, approvals, scheduler, checkpoint, logging lokal; ditambah experimental agent yang dapat memilih dan merevisi langkah di dalam scope grant.
- Multi-account profiles dan bantuan pindah akun melalui UI resmi yang tersedia.
- Chat agent multi-turn, task manager, skill library, on-device memory, typed tool registry, action policy, evaluasi kompatibilitas, dan event triggers berizin.
- Accessibility tree lebih dulu; screenshot sekali-sekali jika perlu melalui kemampuan screenshot AccessibilityService atau MediaProjection sesuai dukungan/perizinan. Tidak melakukan pemantauan visual terus-menerus.

### Out of scope v2 (termasuk mode experimental)

- Root, Shizuku, ADB permanen, bypass keamanan, silent login, pembacaan OTP/password/2FA, auto-unlock, bot yang bekerja pada lockscreen tanpa persetujuan.
- Mengambil video/gambar tersembunyi, mem-bypass DRM/FLAG_SECURE, menembus akses URL yang butuh login tanpa izin, scraping massal.
- Kontrol bebas penuh atas semua aplikasi tanpa batas; pembuatan akun palsu, spam atau posting massal otomatis.
- Remote backend, API platform sosial langsung (keputusan #4 A+B), dan integrasi TikTok/Instagram API resmi. **Voice tetap tidak diaktifkan pada target awal** sesuai keputusan #17; opsional fase lanjut, setelah izin microphone terpisah.
- Menjanjikan multi-session account terpisah atau cloning aplikasi di Android standar.

### Pembatasan nyata

- Overlay membutuhkan `SYSTEM_ALERT_WINDOW` dan tampil **di bawah UI sistem tertentu**; posisi/visibilitas dapat diubah OS.
- Accessibility hanya memperoleh node/aksi yang diekspos aplikasi tujuan. Layout/selector dapat berubah sewaktu-waktu.
- MediaProjection meminta izin untuk **setiap sesi** pada Android modern, dapat berhenti saat screen lock, dan layar sensitif boleh diblokir.
- Tugas background dibatasi Doze/optimization; WorkManager periodik minimal sekitar 15 menit dan tidak menjamin jam tepat; exact alarm terbatas izin/kategori.
- Pembukaan activity dari background dibatasi; jangan membangunkan/membuka kunci perangkat diam-diam.
- Banyak aplikasi membutuhkan otentikasi manual pada pergantian akun. Status akun `UNKNOWN` tidak boleh diasumsikan benar.
- Kebijakan Google Play melarang penggunaan Accessibility untuk agen aplikasi biasa yang secara otonom memulai, merencanakan, dan mengeksekusi tindakan; kebijakan memiliki pengecualian sempit untuk alat aksesibilitas yang benar-benar memenuhi syarat. Fokus **sideload pribadi**, bukan menjanjikan distribusi Play Store atau kemampuan mengabaikan pengamanan Android.

## 6. Arsitektur teknis

```text
Android APK (Kotlin + Jetpack Compose) — seluruh orkestrasi di perangkat
 ├─ UI: Home, Chat Agent, Bubble, Live Monitor, Skill Library, Workflow Studio
 ├─ App/Account Profiles + Permissions Wizard + Scheduler
 ├─ BYGA Agent Core (eksperimental, opt-in)
 │   ├─ Goal/Task Manager + Conversation Context + Agent State Machine
 │   ├─ Planner / Critic-Validator / Replanner (AI provider cloud)
 │   ├─ Tool Registry + JSON Schema + Action Policy + Scope Grants
 │   ├─ Skills Registry + Skill Versioning + Recovery Playbooks
 │   ├─ Memory Manager (short-term / opt-in long-term)
 │   ├─ Observation & State Diff + Step Result Verification
 │   └─ Budget/Time/Step Limits + User Approval + Emergency Stop
 ├─ Executor (deterministik + AI-selected within granted scope)
 │   ├─ Android Intent / App Launcher / Deep Link
 │   ├─ Accessibility Tree, Node Actions, Scoped Gestures
 │   ├─ Accessibility Screenshot (supported API), MediaProjection fallback
 │   ├─ Clipboard / Sharesheet / Download / SAF / MediaStore
 │   └─ Wait-State / Verify Account / Checkpoint / Retry Guards
 ├─ Event Hub (explicit user workflows and supported events only)
 ├─ Provider Hub (OpenAI, Anthropic, Google, OpenRouter, Groq, custom)
 ├─ Local DB: Room + DataStore + App-private files + Android Keystore
 └─ Audit / Diagnostics / Compatibility Test Recorder
         |
         +-- direct HTTPS to user-configured AI provider (no own backend)
```


**Suggested stack:** Kotlin, Jetpack Compose + Material 3, MVVM/Clean-ish packages (pragmatic), Coroutines/Flow, Retrofit/OkHttp or Ktor (choose one), Room, DataStore, WorkManager, Android Keystore AES-GCM, WindowManager overlay, AccessibilityService, MediaProjection. Avoid large runtime dependencies. Android 16/API 36 must be validated against current compatible build tooling; use a supported SDK/Gradle/AGP pairing found in environment, not assumed versions.

## 7. Fungsional requirements

### FR-01 Animated floating bubble

- Bubble 48–64 dp default, translucent glass/navy #080D19 with electric blue #246BFD and cyan accent, subtle glow and spring expand/collapse.
- Drag with edge snap/dock; minimize; close/dismiss; double tap opens text; long press opens actions; avoid overlap with system navigation/status and touch targets.
- States: idle, listening-for-text, thinking, planning, observing, executing, verifying, awaiting-user, success, error, paused. Tidak ada izin mikrofon pada baseline karena voice belum dipilih.
- Use `TYPE_APPLICATION_OVERLAY` with explicit permission, non-intrusive transparent region and sensible touch flags. No full-screen invisible touch overlay.
- Persistent notification/foreground service only when technically required and with correct declared service type; Stop control always accessible.

### FR-02 Commands + launcher

- Launch only user-selected installed target apps with package launch intent / supported deep link; browsers may open configured URLs.
- Prompt editor multiline; saved templates, copy/paste and Android Sharesheet fallback.
- Gesture map is editable with defaults and conflict prevention.

### FR-03 App detection and profiles

- Allowlist of supported packages/URLs; event-driven foreground package detection through user-enabled AccessibilityService where possible.
- Per-app metadata: display name, package/URI, desired entry screen, selectors and fallback description, permission capability matrix, workflow templates.
- Manual select profile if detection is unavailable; never secretly enumerate unrelated apps.

### FR-04 AI provider manager

- Add/edit/delete provider, endpoint where allowed, model, API key, request timeout, role assignment, priority/fallback, cost caps.
- Support text API first; vision only on models that support image input. Feature-detect errors; no fabricated model availability.
- Roles: Planner (suggest steps), Content Generator, Screen Describer, Validator, Recovery Critic; providers may overlap. Semua tool call AI wajib lolos validasi skema, scope grant, allowlist, pemeriksaan state, dan approval policy—AI tidak dapat langsung mengeksekusi Accessibility mentah tanpa pemeriksaan.
- Direct HTTPS; keys never in repo/BuildConfig/log, encrypted at rest with Keystore-based symmetric encryption. Risk of key extraction from compromised devices must be stated.
- Connectivity test with redacted errors; quotas estimated, not asserted exact if provider does not return usage.

### FR-05 Screen understanding + execution

- Read accessible UI semantics (package, window, node content description, text, clickability, bounds, IDs) only for allowlisted apps and while user-enabled.
- Input via `ACTION_SET_TEXT` where supported or clipboard/user-confirmed paste fallback; tap via node action preferentially, gesture dispatch only when explicitly defined and validated.
- Screenshot on-demand mengutamakan `AccessibilityService.takeScreenshot()` (API 30+) atau `takeScreenshotOfWindow()` (API 34+) jika kemampuan dan akses tersedia; MediaProjection sebagai jalur alternatif dengan persetujuan sesi yang diwajibkan sistem. Tangkap hanya saat perlu; buang sesudah penggunaan secara default; beri disclosure dan minta persetujuan sebelum mengirim data layar ke AI cloud. Jangan mencoba membaca konten `FLAG_SECURE`.
- Handle FLAG_SECURE/no-node/permission denied as **BLOCKED**; no bypass.
- Sensitive windows (password, OTP, banking, authenticator, system settings requiring authorization) are prohibited from AI capture/action; ask user to complete manually.

### FR-06 Workflow Studio

- Steps typed and versioned: `LAUNCH_APP`, `SET_TEXT`, `COPY_TEXT`, `PASTE_TEXT`, `FIND_NODE`, `CLICK_NODE`, `SWIPE`, `WAIT_FOR_STATE`, `GENERATE_AI_TEXT`, `ANALYZE_SCREEN_ON_DEMAND`, `DOWNLOAD_URL`, `TAP_NATIVE_DOWNLOAD`, `SHARE_MEDIA`, `VERIFY_ACCOUNT`, `REQUEST_USER_APPROVAL`, `DELAY`, `BRANCH`, `STOP`.
- All tool actions pass schema validation (package allowlist, field selectors, timeouts, max step count, scope and user approval). AI-generated workflow tidak aktif secara default; pertama kali harus disetujui. Dalam Experimental Agent, tool calls dapat dipilih secara dinamis **hanya dalam scope grant aktif**. Tindakan yang membutuhkan scope baru harus meminta persetujuan lagi.
- Visual editor with drag/reorder, form-configured node action, branching, test one step, dry run, preview steps and estimated usage. Templates for Flow->Social.
- Macro recorder logs explicitly opted-in UI semantics/actions and excludes sensitive windows; no recording passwords.
- Resume from persisted checkpoint with app/account/state verification; at-most-once guarantee where possible via idempotency tokens/step completion markers; **never blind-retry POST/PUBLISH/SEND/DELETE**.

### FR-07 Media downloads

- Native download: locate/guide usage of app-provided Download/Save button if accessible; confirm saved file exists before declaring success.
- Direct URL: HTTP(S), valid accessible file and user authorization; size limit, timeout, MIME validation, filename sanitization, downloads notification, failures visible.
- Save via MediaStore (images/videos) or SAF file picker, compliant with scoped storage; gallery sync when Android supports.
- No screen recording/substitute file falsely labeled original.

### FR-08 Social publisher

- Compose a per-platform post draft: caption, hashtags, media selection, aspect-ratio checklist, destination profile.
- Use Android Sharesheet/intent to open platform, then optional deterministic Accessibility-assist; UI confirmations for account, media, and final publish.
- For 'publish', user must explicitly approve each target platform/account immediately before final action; no silent scheduled posting.
- Statuses `DRAFT`, `READY`, `WAITING_FOR_USER`, `HANDED_OFF`, `USER_CONFIRMED`, `UNKNOWN`, `FAILED`; never claim `PUBLISHED` solely because share intent launched.
- Include retry protection to avoid double posting; prevent cross-account leaks.

### FR-09 Multi-account manager

- Logical account profiles per app: unique ID, label, optional public username/email alias (**not password**), app, associated workflows, target account rules, last confirmed at, verification source/confidence.
- `Switch Account`: open official account switch UI or guide user step-by-step; automation uses pre-approved deterministic macro only if safe; prompt for manual login/2FA when needed.
- Verify active account using accessible, user-visible handle/email/selected item or explicit user confirmation; `UNKNOWN` blocks publish and prompts the user.
- Workflow binds target pair `(appId, accountProfileId)`; scheduler queue cannot silently inherit last active account; verification repeated before critical action.
- No cloning, parallel isolated sessions, bypass sessions or credential/password storage.

### FR-10 Scheduler + background execution

- Schedule models: one-off, recurring, time window, local timezone Asia/Jakarta default with edit, sequential dependencies, priority, retry, pause and history.
- WorkManager for deferrable work; AlarmManager only when legitimately suitable and permissions support, and user explicitly needs tighter timing. Do not promise exact-time every minute.
- Prepare prompts/files and make permitted network calls in background within OS constraints. When UI action needed but keyguard active/foreground mismatch, transition to `WAITING_FOR_USER` and post notification.
- After unlock/user tap: revalidate app/account/step before resuming. Avoid silent foreground app launches.

### FR-11 Monitoring and recovery

- Bubble color/motion status; dashboard with current workflow, step list, last state, model/provider used, timeout/error, cost estimate, last checkpoint.
- Buttons Pause, Resume, Stop (cancel foreground jobs where possible), Retry only safe steps.
- Capped retry exponential backoff + selector lookup strategy; timeouts and step count ceilings; battery/data budget.
- Local telemetry opt-in and redacted; no screenshots, API keys, app private text in logs by default.

### FR-12 Permissions and privacy

- Friendly permission wizard with rationale before Android settings: overlay special access, AccessibilityService, notification permission where required, screen capture prompt, media access via scoped storage.
- All features degrade predictably without permissions; individual toggles and emergency stop revoke internal execution, not system-level permission covertly.
- Prominent disclosure when reading other apps/screenshots and when transmitting data to cloud AI; consent options per session/role; allow deletion of prompts/logs.
- Keystore-based encryption for secret values; protect exported components, minimize network permissions, no analytics by default.

## 8. Data model (minimal)

- `AiProviderConfig(id, type, baseUrl, model, role, priority, enabled, encryptedSecretRef, timeoutMs, dailyBudget)`
- `AppProfile(id, packageName?, url?, displayName, capabilityMatrix, selectorPresetVersion)`
- `AccountProfile(id, appProfileId, label, publicIdentifier?, lastVerifiedAt?, verificationStatus, note?)`
- `Workflow(id, name, version, status, approvedVersion, stepsJson, accountBinding?, createdAt)`
- `WorkflowStep(id, workflowId, sequence, type, payloadJson, retryPolicy, requiresApproval)`
- `Schedule(id, workflowId, scheduleSpec, zoneId, enabled, nextRunAt, priority)`
- `Execution(id, workflowId, scheduleId?, status, currentStep, lastError?, startedAt, updatedAt)`
- `Checkpoint(executionId, stepId, appPackage, verifiedAccountId?, stateHash?, approvalToken?, timestamp)`
- `MediaItem(id, uri, mime, filename, source, obtainedAt, verified)`
- `AuditEvent(id, executionId?, eventType, summaryRedacted, timestamp)`

Use Room schema migrations and indexes for workflow/schedule/execution; DataStore for UI preferences. Never place API keys inside exported backup/plain database columns.

## 9. Experience / screens

1. **Onboarding:** intro, scope disclosure, permission wizard, Start Bubble, diagnostic.
2. **Home:** status bubble, active app/account, quick actions, AI message input, current workflow. **Chat Agent:** thread tujuan berkelanjutan, lampiran terbatas yang dipilih pengguna, plan preview, tombol approve-scope, step-by-step evidence.
3. **Providers:** add endpoint/API key/model, priority order, test connection, usage estimate.
4. **Applications:** app profiles, selector capabilities, package/web launch, manual app selection.
5. **Accounts:** grouped by app, switch guided, verify account, map workflows, never show saved passwords.
6. **Workflow Studio:** template gallery, AI-draft -> user review -> approve -> run; visual steps and conditional branches.
7. **Scheduler:** calendar/list, recurring interval, timezone, dependencies, pending-user queue.
8. **Media & Social:** file picker/download state, preview caption/platform, confirmation sheet.
9. **Monitor:** live timeline, model, activity, pause/resume/stop, checkpoints, redacted logs.
10. **Settings:** accessibility/overlay status, data privacy, battery policy tips, export safe config, app info.
11. **Agent Inspector:** tool yang akan dijalankan, alasan, target aplikasi/akun, izin yang aktif, screenshot sharing indicator, token/budget/remaining steps, dan keputusan policy.
12. **Skills & Memory:** katalog skill terpasang, buat/tes/import/export aman, pengaturan memori, lihat/hapus/disable pembelajaran otomatis.
13. **Compatibility Lab:** per-app/package/version, daftar aksi yang sudah terbukti, log PASS/FAIL/BLOCKED, evidence redacted.

**Visual system:** premium dark-first, navy `#080D19`, electric blue `#246BFD`, cyan glow; reduced motion support; low-end-friendly rendering; avoid high-frequency background animations.

## 10. Non-functional requirements

- Reliability: detect permission revoked, app crash, API timeout, workflow canceled; never silently mark success.
- Performance: no constant screenshots, no busy-loop accessibility events, do not degrade UI of target app; minimize overlay overdraw.
- Security: Keystore AES-GCM encryption; no hardcoded credentials; user-scoped consent and action allowlist; TLS; sanitized request logging.
- Privacy: transparent opt-in data egress to chosen provider. Capture off by default and short-lived in memory.
- Compatibility: phone portrait-first; responsive landscape; test Android 14–16 when devices available; primary acceptance device Android 16 without root.
- Maintainability: modular adapters and DI, unit testable business logic; data migrations and clear failure modes.
- Accessibility: content descriptions, large touch targets, contrast, reduce motion.
- Internationalization: Bahasa Indonesia first; preserve strings in resources.

## 11. PoC acceptance matrix (release gate)

| ID | Scenario nyata | Acceptance | Status awal |
|---|---|---|---|
| P01 | Install APK dari GitHub Actions artifact | Android installer accepts debug APK, app opens | NOT TESTED |
| P02 | Bubble muncul di atas target app | Tap/drag/dock/close; system settings grant overlay | NOT TESTED |
| P03 | Accessibility enable/disable | Semantics read only if granted, status updated | NOT TESTED |
| P04 | Open target app + paste into safe field | Correct app and field, no unintended taps | NOT TESTED |
| P05 | Screenshot consent + vision request | Correct session consent, sensitive screen blocked | NOT TESTED |
| P06 | Native download where available | File existence confirmed, otherwise BLOCKED | NOT TESTED |
| P07 | URL image/video download | Correct allowed URL, MIME and saved file | NOT TESTED |
| P08 | Share media to TikTok/Instagram | Correct platform opens/draft, not claimed posted | NOT TESTED |
| P09 | Multi-account guided switch | Target visible and verified or WAITING_FOR_USER | NOT TESTED |
| P10 | Locked-screen scheduled workflow | Background-safe portions only, UI waits/notification | NOT TESTED |
| P11 | AI provider fallback | Failure handled, no secret in log, next provider tested | NOT TESTED |
| P12 | Crash/resume + posting guard | No duplicate post or cross-account leak | NOT TESTED |
| P13 | Android unit tests + CI | Gradle test/assembleDebug pass, artifact uploaded | NOT TESTED |

**Go/No-Go:** (1) prove overlay + input on at least one real target app, (2) permission lifecycle works, (3) no unsafe unintended actions, (4) fallback for blocked app documented; otherwise keep impacted functionality experimental/disabled. All matrix statuses must reflect actual test evidence.

## 12. Delivery phases

### Phase 0 — foundation + PoC (first deliverable)
- Kotlin Android repo + Compose design system + permission wizard + bubble service + basic AccessibilityService + manual paste/share + diagnostic screen + GitHub Actions debug APK.
- Document real-device test script and limitation. Do not assert success in ChatGPT/Flow/TikTok/Instagram without user-side test.

### Phase 1 — local AI / app profiles
- Add encrypted provider key storage, model registry, request client and fallback; app profiles; one-shot screenshot and consent.

### Phase 2 — workflows / downloader / social drafts
- Typed action DSL, workflow studio, templates, URL/native download, sharesheet/post approval, checkpoints/monitor.

### Phase 3 — multi-account / scheduler / hardening
- Guided account switching, account validation before posting, recurrence/queue, retry, background-safe scheduling, privacy hardening, instrumentation.

### Phase 4 — actual compatibility
- Device tests per app and account, update selectors/fallback docs, disable unsupported automation paths rather than silently promising them.

### Phase 5 — Experimental Agent Core (v2)
- Chat threads, planner with structured outputs, scoped task approval, tool registry, observation/act/verify loop, first 1–2 app adapters, monitor.

### Phase 6 — Memory, Skills, Replanning (v2)
- Short/long-term local memory, skill library/import/export, compatible selector fallback, revision tracking, recovery playbooks, event trigger safeguards.

### Phase 7 — Compatibility Lab & hardening (v2)
- Device test matrix across target apps/versions, fault injection, performance/battery tests, privacy audits, emergency-stop recovery, signed APK option.

## 13. GitHub / CI requirements

- Native Android Gradle project with checked-in Gradle wrapper and Android SDK documentation.
- Workflow `.github/workflows/android-apk.yml` on push/PR/manual dispatch: checkout, setup JDK compatible with AGP, Android SDK setup, Gradle cache, `./gradlew testDebugUnitTest assembleDebug`, upload `app/build/outputs/apk/debug/*.apk` as artifact.
- Use supported setup actions; permissions minimal. No provider API keys in CI (unit tests use fake adapters); no release/signing secret needed for debug artifact.
- `README.md`: setup, where to add key at runtime, permission troubleshooting, download APK via Actions -> run -> Artifacts, PoC test steps.
- `docs/COMPATIBILITY.md`, `docs/PRIVACY.md`, `docs/TEST_MATRIX.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, and sample non-secret data.

## 14. Risks & mitigation

| Risiko | Mitigasi |
|---|---|
| Android/app UI changes | semantic selector first, versioned profile, manual fallback, PoC validation |
| Authentication/account mismatch | explicit selected account + on-screen verification + confirmation gate |
| Unintended publication / duplicate post | only explicit final user confirmation, at-most-once markers, status UNKNOWN on uncertainty |
| Keystore still extractable on compromised device | private-device warning, least privileges, no logs/backup of secrets |
| Excessive AI fees | per-model budget, request timeout, max retries, estimated usage and stop threshold |
| Screenshot private information | permission each session, sensitive screen block, opt-in cloud send, ephemeral storage |
| Battery and OS background restrictions | event-driven listeners, WorkManager, no forced unlock, handoff to notification |
| Accessibility automation policy | APK pribadi/sideload sebagai eksperimen; tidak klaim patuh Google Play; kurangi scope, minta persetujuan jelas, audit semua aksi, jangan bypass pengamanan Android |
| Cloud builds lack actual UI device test | separate PASS/FAIL/BLOCKED matrix, no fake test claims |

## 15. Definition of Done (initial MVP)

1. Repo has reproducible Android project, CI workflow emits installable debug APK artifact.
2. A user can grant/revoke permissions and display/move/dismiss overlay.
3. A user can input and share a text prompt; Accessibility text action supported where compatible.
4. Provider manager can add encrypted key and test one provider (if user configures credentials), with safe fake provider unit tests in CI.
5. At least one local workflow template is editable, reviewable and stoppable; Experimental Agent **OFF secara default** dan setiap tugas memerlukan persetujuan scope serta tombol stop.
6. Scheduler persists tasks and correctly enters WAITING_FOR_USER on blocked UI conditions.
7. Account manager stores multiple account profiles and refuses high-risk action when destination account unverified.
8. No secrets in repo/log, permission/off-switch works, README and test matrix accurately state limitations.
9. Features not implementable or not tested remain explicitly flagged EXPERIMENTAL / BLOCKED / NOT TESTED.
10. Pada milestone v2, agent menyelesaikan minimal satu tugas multi-langkah nyata memakai siklus observe/act/verify dengan scope grant dan log bukti; bila gagal tampilkan penyebab, bukan status berhasil palsu.
11. Memori dan skill dapat dilihat, dinonaktifkan, dan dihapus pengguna; tidak menyimpan rahasia secara terselubung.
12. P14–P29 (pengujian tambahan v2) memiliki hasil nyata, bukan diklaim PASS dari build cloud.

## 16. Kesenjangan v1.0 yang wajib ditutup untuk mode agent

| Gap | Fitur sudah ada pada v1 | Tambahan v2 yang diperlukan | Urgensi |
|---|---|---|---|
| G01 Runtime agent | Workflow engine deterministik | Goal, observations, action decision, policy, verifier, replanner, state machine | P0 |
| G02 Conversational task | Input teks & quick actions | Multi-turn chat dan task context terpisah | P0 |
| G03 Tool registry | Android action adapters | Skema typed tool, kemampuan, precondition, side-effect, guard, audit | P0 |
| G04 Adaptive perception | Accessibility + MediaProjection | Accessibility screenshots API, snapshot/diff, confidence, sensitive-field filters | P0 |
| G05 Skill learning | Macro/editor workflow | Skill lifecycle (draft/test/approve/version/reuse/disable) | P1 |
| G06 Long-term memory | Room untuk konfigurasi/history | Explicit remembered facts/skill outcomes, relevance retrieval, user deletion | P1 |
| G07 Replanning | Capped retry/self-healing | Evaluate expected/actual states, recovery plan bounded in scope | P0 |
| G08 Event-driven | Timer/scheduler | User-defined event subscriptions; reliable pause on locked/no-foreground | P1 |
| G09 Inspector & explainability | Monitoring dasar | Observe/plan/tool/policy/verify timeline, budgets, decision reasons | P0 |
| G10 Compatibility Lab | PoC matrix 13 butir | App-version-specific test packs, regression, fallback, fault injection | P0 |

**Arti prioritas:** P0 untuk pembuktian agent, P1 setelah Agent Core stabil, P2 opsional. Tidak ada klaim sebuah gap sudah terimplementasi hanya karena tertulis di PRD.

## 17. FR-13 Experimental Agent Core — goal-to-action runtime

### 17.1 Perilaku

1. Pengguna memberi **goal** dalam bahasa Indonesia, misalnya: "Buka ChatGPT dan buatkan 3 variasi caption".
2. Agent membentuk `GoalSpec` berisi target, aplikasi yang diizinkan, hasil yang diharapkan, risiko, dan batasan.
3. Planner menghasilkan `PlanDraft` dengan langkah, tool kandidat, expected state, kriteria keberhasilan, dan estimasi biaya.
4. UI menampilkan **scope grant**; tombol Jalankan baru tersedia setelah pengguna menyetujui.
5. Agent membuat `Observation` dari aplikasi aktif (Accessibility UI tree first, screenshot on-demand second), memvalidasi package/akun, dan memilih **tool yang sudah disetujui**.
6. Policy Engine memeriksa tiap `ToolCall`; aksi dibatalkan jika app/akun/status izin tidak cocok, token grant habis, budget terlampaui, atau permintaan menyentuh data sensitif.
7. Executor melakukan satu aksi, lalu `Verifier` mengecek state dan evidence nyata. Tidak menganggap `tap() == success` sebagai bukti tujuan tercapai.
8. Jika mismatch, `Recovery Planner` boleh mengganti selector atau urutan **hanya dalam scope**; jika tidak aman, pause `WAITING_FOR_USER`.
9. Selesai hanya setelah expected outcome terverifikasi atau konfirmasi pengguna. Simpan ringkasan task, bukti minimal yang disunting (redacted), dan cost estimates.

### 17.2 State machine

`CREATED -> PLANNING -> WAITING_FOR_SCOPE -> OBSERVING -> EXECUTING -> VERIFYING -> (OBSERVING | RECOVERING | COMPLETED | WAITING_FOR_USER | BLOCKED | FAILED | CANCELLED)`

- `WAITING_FOR_SCOPE`: tidak ada aksi UI yang boleh berjalan.
- `OBSERVING`: baca UI secara event-driven dan sesuai izin; tidak melakukan polling screenshot terus-menerus.
- `EXECUTING`: satu tool call pada satu waktu per perangkat untuk mencegah tabrakan UI; concurrency hanya untuk tugas non-UI aman.
- `RECOVERING`: batas percobaan; jika app/account/window berubah mendadak, stop.
- `COMPLETED`: ditentukan oleh verifier/hasil yang bisa diamati; status `UNKNOWN` bila hasil tak dapat dipastikan.

### 17.3 Batas default eksperimental (pengaturan awal, bukan batas OS)

- `experimental_agent_enabled=false` setelah instalasi; pengguna mengaktifkan eksplisit.
- Scope per tugas, **bukan** izin selamanya: aplikasi/akun, tool types, masa berlaku (mis. 10 menit), max steps (mis. 30), max AI requests (mis. 12), max replans (mis. 3), dan plafon estimasi biaya.
- Pengguna bisa mengubah batas sebelum mulai; perubahan menaikkan hak akses saat berjalan harus mendapatkan grant baru.
- Pause setelah app meninggalkan allowlist, akun berubah, screen terkunci, izin dicabut, layar aman muncul, atau foreground context hilang.
- Disable eksekusi sentuhan ketika overlay/Accessibility belum sehat; tombol **Stop Emergency** menghentikan antrean dan membatalkan sesi aktif secara best effort.

### 17.4 Definisi API internal

`AgentController.start(goal, scopeGrant)`, `observe()`, `plan()`, `evaluateToolCall(call)`, `executeApproved(call)`, `verify()`, `recover()`, `pause(reason)`, `resume(afterRevalidation)`, `stop()`.

**Acceptance:** Goal multi-step terkonversi ke rencana dan dieksekusi dalam grant; tidak ada eksekusi tool di luar grant; dapat di-stop sebelum langkah berikutnya; verifikasi akhir memberi PASS/FAIL/UNKNOWN disertai evidence minimal.

## 18. FR-14 Perception & Screen State Engine

1. **Primary observation:** `AccessibilityService` events, active package, active window, `AccessibilityNodeInfo` (resource ID bila ada, text content yang boleh dibaca, contentDescription, clickable, bounds, enabled, scrollable).
2. **Secondary observation:** `takeScreenshot()` (API 30+) atau `takeScreenshotOfWindow()` (API 34+) jika kemampuan screenshot service dideklarasikan dan akses diberikan. Screenshot digunakan ketika UI tree tidak cukup; perhatikan cooldown/error API.
3. **Alternative capture:** MediaProjection hanya sesuai persetujuan Android per sesi yang diperlukan. Tidak membuat trik bypass untuk meniadakan dialog persetujuan.
4. **State snapshot:** `UiSnapshot(packageName, windowId, appVersion, accountStatus, treeDigest, screenshotRef?, timestamp)`; snapshot hash untuk membandingkan perubahan tanpa menyimpan konten pribadi secara otomatis.
5. **State diff:** identifikasi perubahan teks/node/layar setelah tindakan; debounce agar tidak keliru membaca transisi loading. Gunakan timeouts, tidak fixed-delay buta sebagai kebenaran.
6. **Vision model:** caption/region/target proposal; hasil berupa *kandidat*, bukan instruksi tap langsung. Hanya memproses gambar jika pengguna menyetujui pengiriman cloud. Pembungkusan data harus minim; hindari OTP, password, chat privat, finansial, dan sistem keamanan.
7. **Selector preference:** Accessibility node semantic > stable resource ID > supported view hierarchy > koordinat screenshot terverifikasi. Kandidat koordinat wajib cek device rotation/layout dan verifikasi ulang sebelum sentuhan.
8. **Unknown/secure screens:** beri `BLOCKED_SECURE`, `NEED_CONSENT`, atau `UNOBSERVABLE`, bukan menebak tindakan. Tindakan pada dialog permission/settings sensitif memerlukan manusia.
9. **Compatibility fallback:** node-only mode, user-guided mode, text clipboard/share mode ketika screenshot tidak tersedia.

**Acceptance:** capture gagal tidak memicu tap tebakan; overlay tidak tertangkap saat window capture mendukung; tidak ada screenshot mentah tersimpan dalam log secara default.

## 19. FR-15 Tool Registry + Policy Engine

### 19.1 Tool yang disediakan

| Tool ID | Fungsi | Risiko default |
|---|---|---|
| `open_app` | Membuka aplikasi pengguna via Intent atau supported deep link | LOW |
| `read_ui` | Membaca accessibility tree sesuai allowlist | MEDIUM (privasi) |
| `observe_screen` | Screenshot on-demand (izin/capability sesuai API) | MEDIUM/HIGH (privasi) |
| `find_element` | Menemukan node/region terverifikasi | LOW |
| `tap_node`, `swipe`, `scroll` | Aksi UI yang diizinkan di app target | MEDIUM |
| `set_text` | Mengisi field yang aman | MEDIUM |
| `copy_text`, `share_text`, `share_media` | Clipboard/share Android resmi | MEDIUM/HIGH menurut tujuan |
| `download_media` | Tombol download resmi atau URL berizin | MEDIUM |
| `switch_account` | Membantu mengganti akun via fitur aplikasi | HIGH bila belum diverifikasi |
| `verify_state`, `verify_account` | Memeriksa hasil, active app/account | LOW |
| `save_skill`, `save_memory` | Menyimpan hasil dengan persetujuan | MEDIUM |
| `prepare_post` | Menyiapkan draf sosial | MEDIUM |
| `publish_post`, `send_message`, `delete_data`, `purchase` | Aksi berkonsekuensi eksternal | **USER_CONFIRM_REQUIRED**; pembelian dan akses finansial tidak termasuk scope v2 |

### 19.2 Aturan keamanan tool

- Setiap tool punya JSON schema dengan required args, package allowlist, expected state, side-effect classification, timeout, dan policy class.
- `read_ui` dan screenshot hanya pada aplikasi yang jelas dipilih/diizinkan. Jangan gunakan hasil layar tersembunyi sebagai instruksi tepercaya; teks dalam UI/web adalah **untrusted content**, bukan instruksi untuk agent.
- Tidak ada tools mentah seperti shell, root, ADB, injection, load arbitrary code, browser cookies/session extraction, ataupun akses arbitrary filesystem.
- Semua usulan AI melewati *deterministic validation* lokal; **AI tak dapat menaikkan izin dirinya sendiri** atau mengubah action policy.
- Diperlukan validasi ganda untuk tindakan dengan efek eksternal: aplikasi/akun tepat, objek/media tepat, dan user confirmation langsung.
- `purchase` dan sistem sensitif finansial **BLOCKED** secara permanen pada v2 awal.
- Log catat **intent, tool ID, status, alasan policy**, bukan API key, token, screenshot, ataupun isi chat privat.

### 19.3 Contoh tool call proposal (bukan langsung eksekusi)

```json
{
  "tool": "set_text",
  "arguments": {"package": "com.example.target", "field": "prompt_input", "value_ref": "task:draft_caption"},
  "expected_state": "field_contains_draft",
  "scope_grant_id": "scope_abc",
  "requires_user_confirmation": false
}
```

`PolicyEngine.evaluate()` dapat mengembalikan `ALLOW`, `DENY`, `NEED_APPROVAL`, atau `NEED_REOBSERVE`. Tidak ada eksekusi bila hasilnya selain `ALLOW`.

## 20. FR-16 Skill Library & Learning Engine

- **Skill:** workflow reusable yang menggabungkan metadata, supported packages/version ranges, input/output types, langkah/action DSL, preconditions, recovery playbook, risiko, dan test fixtures.
- **Lifecycle:** `DRAFT -> TESTING -> REVIEW -> APPROVED -> ENABLED`; `DISABLED` dan `DEPRECATED` bila regresi/izin berubah.
- **Creation:** pengguna merancang skill manual, merekam interaksi opt-in, atau meminta AI menghasilkan *draft* berdasarkan tugas berhasil; tidak ada auto-install/auto-enable skill hasil AI.
- **Selection:** katalog skills diberi tag nama aplikasi/akun/kebutuhan; rank berdasarkan kecocokan konteks, bukan mengeksekusi semuanya sekaligus.
- **Learning:** setelah task, simpan perubahan selector/recovery sebagai *saran versi skill baru*, bukan overwrite diam-diam; kelulusan test dan approval wajib.
- **Editing:** visual steps, parameterized input, guard conditions, timeout/retry, dry run, rollback terbatas untuk langkah reversibel; tidak klaim semua aksi bisa di-undo.
- **Export/import:** file JSON versioned; import harus sanitasi dan tampilkan capabilities/risiko, tidak boleh berisi executable code atau API keys.
- **Contoh:** `DraftCaptionChatGPT`, `FlowPromptAssistant`, `DownloadAvailableResult`, `PrepareTikTokDraft`, `SwitchOfficialAccount`, `VerifyTargetAccount`.

**Acceptance:** skill versi A tetap bekerja meski versi B ditolak; skill impor tidak aktif sebelum review; skill gagal karena perubahan UI dinonaktifkan atau perlu re-test.

## 21. FR-17 Memory & Conversation Engine

### 21.1 Jenis memori

- **Working Memory:** goal, last observation summary, pending approvals, active tool, previous step outputs. Hapus atau arsip ketika task berakhir.
- **Task History:** ringkasan eksekusi, outcome, link lokal ke media yang dapat diakses (tanpa mengopi data privat tanpa izin), error/recovery. Gunakan untuk debugging.
- **Long-Term Memory (opt-in):** preferensi umum yang pengguna pilih untuk disimpan; daftar skill favorit; preferensi format caption dan app/account aliases; tidak menyimpan password, OTP, sesi browser, atau isi pribadi dari aplikasi secara otomatis.
- **Semantic Retrieval:** awalnya keyword/FTS Room untuk hemat sumber daya; embedding/vector index lokal opsional setelah terbukti memenuhi batas performa. Jangan wajibkan model LLM on-device pada ponsel ringan.
- **Conversation:** thread chat yang terhubung ke `TaskRun`; saat thread baru, tidak mengasumsikan izin dari thread lama. AI dapat merujuk ringkasan memori yang pengguna izinkan, bukan mengeksekusi aksi masa lalu tanpa grant baru.

### 21.2 Privasi & lifecycle

- Memori baru yang diusulkan AI ditampilkan dengan alasan; pengguna bisa `Save`, `Edit`, `Reject`, atau `Forget`.
- Tombol `Hapus riwayat`, `Hapus memori`, dan `Ekspor data` dengan penjelasan konsekuensinya; hapus lokal tidak otomatis menghapus konten yang sudah dikirim ke provider cloud.
- Retensi ringkasan dan log dapat diatur; screenshot tidak menjadi bagian long-term memory default.
- Data sensitif terenkripsi sesuai threat model; API key tetap khusus Android Keystore. Fitur Android Auto Backup harus mengecualikan secret dan materi berisiko.
- Prompt outbound ke AI cloud menampilkan/menjelaskan kategori data yang dikirim; minimisasi data/pseudonimisasi secara best effort.

**Acceptance:** pengguna dapat mencari, melihat, mengoreksi, menghapus seluruh memori lokal; setelah penghapusan, retrieval lokal tidak mengembalikan entri itu.

## 22. FR-18 Adaptive Replanning, Recovery, Event Hub

### 22.1 Bounded replanning

1. Bandingkan `expected_state` vs `actual_state` setelah setiap aksi.
2. Bila mismatch: re-observe, periksa apakah aplikasi/akun berubah, lakukan fallback yang didefinisikan skill.
3. Jika tetap gagal: minta AI mengusulkan rencana alternatif terstruktur; Policy Engine menguji scope, risiko, dan budget.
4. Perubahan selector/urutan yang tetap *di dalam grant* dan tanpa side effects baru boleh dijalankan; keluar scope butuh approval baru.
5. Setelah replans mencapai maksimum atau task timeout: STOP dan tampilkan `BLOCKED` beserta penyebab dan langkah manual.
6. Jangan pernah blind retry `PUBLISH`, `SEND`, `DELETE`, transaksi, atau operasi eksternal yang tak idempotent.

### 22.2 Event-triggered tasks

- Event sumber v2: jadwal WorkManager, user tap/quick action, app foreground berubah (dengan Accessibility yang diaktifkan pengguna dan allowlist), pengunduhan selesai yang diprakarsai sendiri, dan `TaskRun` selesai.
- Event-based trigger disimpan sebagai `IF condition THEN propose/run approved skill`; background boleh menyiapkan konten lokal dan notifikasi, **tidak** memaksa UI terbuka di lockscreen.
- Hindari memantau semua notifikasi/pesan/aplikasi terus-menerus; tidak menambah Notification Listener atau capture suara di MVP.
- Pasang debounce, cooldown, concurrency key, dan anti-loop agar event tidak berulang memicu agent tanpa akhir.
- **Scheduler tidak bisa menghidupkan AI agent yang menyentuh UI secara diam-diam saat HP terkunci.** Lanjutkan dengan notifikasi saat UI perlu tindakan pengguna.

## 23. FR-19 Experimental Permissions & Safety Modes

### 23.1 Mode operasi

| Mode | Default | Perilaku |
|---|---|---|
| `ASSISTED` | ON | User memicu dan menyetujui langkah/workflow; UI automation deterministik |
| `EXPERIMENTAL_SCOPED` | OFF | Agent dapat merencanakan/replan dan memilih tool untuk tujuan aktif dalam scope grant berjangka |
| `BACKGROUND_PREP` | OFF | Hanya tugas non-UI aman yang diizinkan Android, seperti menyiapkan draf dan request AI berizin |

Tidak ada mode `UNRESTRICTED_FULL_CONTROL` atau `SILENT_LOCKSCREEN_AUTOMATION`.

### 23.2 Approval tiers

- **Tier 0 (read-only lokal):** status aplikasi/plan/task sendiri dan metadata non-sensitif; sesuai izin yang diberikan.
- **Tier 1 (reversible scoped actions):** membuka app, scroll, isi draft di field yang diperbolehkan — cukup scope approval awal yang jelas.
- **Tier 2 (external/share/account switching):** sharesheet, download ke galeri, pindah akun, kirim data yang dipilih ke provider cloud — perlu aksi/consent tambahan bila cakupan awal belum memadai.
- **Tier 3 (irreversible or significant):** kirim pesan, publikasi, hapus data — **konfirmasi manusia segera sebelum aksi final**, package, akun, isi, dan tujuan terlihat.
- **Blocked:** password, OTP, 2FA, pembayaran, bank, perubahan izin sistem tanpa instruksi manual, rekam panggilan, bypass layar terlindungi, pengambilan informasi privat tanpa izin, spam/mass posting.

### 23.3 Threat model minimum

- Prompt injection lewat teks UI/website: treat as data, never as system/developer instruction. Tool call harus relevan dengan goal pengguna dan divalidasi lokal.
- AI hallucination atas objek yang tidak ada: re-observe, node verification, stop-on-uncertainty.
- Data exfiltration: outbound request allowlist ke provider yang dipilih, redaction/consent, no arbitrary URLs/tool-generated fetch in agent core.
- Account mix-up: re-verifikasi akun sebelum aksi eksternal, termasuk setelah switching atau resume.
- Device compromised: data mungkin terekspos walau Keystore digunakan; jangan mengklaim mustahil diekstraksi.
- Fat-finger/action drift: node semantic prioritas; tap koordinat dibatasi, UI fokus dan orientasi diverifikasi ulang.
- Service disabled: pause segera dan tampilkan diagnostic, jangan mencoba mengaktifkan layanan sendiri.

## 24. FR-20 Agent Chat, Floating UX & Monitoring

### 24.1 Antarmuka utama

- **Bubble minimal**: status ring bercahaya, icon animasi ringan, drag/snap, tap untuk membuka mini chat, long-press opsi cepat (Pause/Stop/Open Dashboard).
- **Chat Agent**: percakapan per tugas, goal edit, prompt templates, auto-suggest skill, tool call preview, plan visualization, attachment pilihan pengguna, tampilan evidence dan alasan gagal.
- **Scope sheet**: ringkasan yang mudah dibaca: aplikasi/akun, data yang dibaca/dikirim, langkah tindakan yang boleh, waktu aktif, cost/step limit, aksi yang selalu butuh konfirmasi.
- **Live Activity timeline**: `Goal -> Plan -> Screen Observation -> Tool -> Verification -> Replan -> Result` dengan timestamp WIB, elapsed time, provider AI, perkiraan biaya, dan outcome.
- **Inspector**: layar `Why this action?`, selector reasoning (ringkas, bukan chain-of-thought internal provider), permission policy decision, screenshot sharing indicator, status akun terverifikasi.
- **Pause/Resume/Stop** di bubble, notification (jika diperlukan), dan dashboard; Stop tetap terlihat jika overlay gagal (buka app utama).

### 24.2 Performa perangkat Android ringan

- Compose UI ringan, tidak memuat scene 3D atau animasi high-FPS terus-menerus; mode reduce motion/battery saver.
- Screenshot resized/compressed on-demand setelah redaction; buang bitmap lama sesegera mungkin; tidak simpan frame/video live.
- Maksimal satu active UI agent run; network calls bisa async dengan batas concurrency.
- Hindari menjalankan LLM lokal besar wajib; gunakan provider cloud langsung seperti baseline, dengan indikator konektivitas dan quota.

## 25. Model data baru & kontrak internal

Tambahkan migrasi Room dari model lama, jangan hapus tabel v1.0. Nama tabel contoh:

| Entity | Kolom penting | Catatan |
|---|---|---|
| `AgentGoal` | id, text, createdAt, status, targetAppIds | Goal asli pengguna |
| `AgentThread` | id, title, lastMessageAt, relatedTaskId | Multi-turn conversation |
| `AgentMessage` | id, threadId, role, contentRef, redacted | Hindari menyimpan rahasia |
| `ScopeGrant` | id, goalId, packageIds, accountIds, allowedToolIds, expiresAt, maxSteps, maxCost, status | Wajib dibuat user |
| `AgentPlan` | id, goalId, revision, stepsJson, reviewedAt | Versioned draft |
| `AgentObservation` | id, runId, snapshotDigest, packageId, screenState, timestamp | Metadata saja default |
| `AgentToolCall` | id, runId, toolId, argsRedacted, policyDecision, status | Auditable |
| `AgentEvaluation` | id, runId, actualOutcome, expectedOutcome, evidenceRedacted, status | Verifier |
| `Skill` | id, version, name, manifestJson, status, compatibleApps | Typed reusable skill |
| `SkillTestRun` | id, skillId, packageVersion, result, summary | Regression |
| `MemoryEntry` | id, type, textRef, source, consent, expiresAt, deletedAt | Opt-in long-term |
| `EventRule` | id, eventType, filterJson, skillId, cooldown, enabled | Explicit trigger |
| `CompatibilityResult` | id, deviceOs, package, appVersion, toolId, result, testedAt | Evidence-based |

### Contoh scope grant (untuk menunjukkan kontrak, bukan implementasi final)

```json
{
  "goal": "Buka ChatGPT dan siapkan tiga caption",
  "packages": ["com.openai.chatgpt"],
  "accounts": ["chatgpt_personal"],
  "allowed_tools": ["open_app", "read_ui", "find_element", "tap_node", "set_text", "verify_state"],
  "expires_in_seconds": 600,
  "max_steps": 30,
  "max_replans": 3,
  "allow_publish": false,
  "allow_send_message": false,
  "allow_sensitive_windows": false,
  "require_confirmation_for_external_actions": true
}
```

Pastikan `ScopeGrant` ditandatangani/diikat secara lokal terhadap sesi/aplikasi, tidak dapat diedit lewat prompt model, dan dicek saat tiap aksi. Nilai example package/account bukan klaim ketersediaan atau kompatibilitas app.

## 26. PoC/acceptance tambahan v2 — setiap hasil awal NOT TESTED

| ID | Kasus uji fisik / fault injection | Hasil yang dituntut | Status awal |
|---|---|---|---|
| P14 | Mode Experimental tidak diaktifkan | Tidak ada agent action; hanya assisted | NOT TESTED |
| P15 | Goal multi-step di app target | Plan, scope review, action, verify lengkap | NOT TESTED |
| P16 | Scope grant kedaluwarsa | Tool berikutnya ditolak, task paused | NOT TESTED |
| P17 | AI mengusulkan aksi di app lain | Policy DENY atau minta grant baru, tanpa tap | NOT TESTED |
| P18 | Selector tidak ditemukan | Reobserve + fallback terbatas atau BLOCKED | NOT TESTED |
| P19 | UI loading lambat | Wait-for-state; tidak spam tap/retry buta | NOT TESTED |
| P20 | App switching/unverified account | Pause dan verifikasi ulang | NOT TESTED |
| P21 | Screenshot ditolak/FLAG_SECURE | BLOCKED atau node-only fallback; no bypass | NOT TESTED |
| P22 | Screenshot mengandung isi sensitif | Tidak otomatis dikirim ke provider AI | NOT TESTED |
| P23 | Prompt injection tertulis pada halaman target | Tidak menjadi instruksi/tool call berizin | NOT TESTED |
| P24 | Pencet Stop saat tengah workflow | Tidak menjalankan tool selanjutnya; batalkan best effort | NOT TESTED |
| P25 | Matikan Accessibility/pindah orientasi | Pause/revalidate; tidak tap posisi lama | NOT TESTED |
| P26 | Buat skill dari task berhasil | Draft -> tes -> approve -> enable | NOT TESTED |
| P27 | Hapus memori tersimpan | Retrieval lokal tidak memakai entri terhapus | NOT TESTED |
| P28 | Publikasi / pengiriman | Konfirmasi segera sebelum final; anti double-send | NOT TESTED |
| P29 | Biaya/request cap / provider error | Stop/fallback terukur, tanpa membocorkan secret | NOT TESTED |

**Kriteria release Experimental (minimal):** P01–P05 lulus pada satu aplikasi target, P14–P19, P21–P25, P28–P29 lulus; yang lain dapat ditandai experimental/blocked jika fitur terkait tidak diaktifkan, dan harus dinyatakan jujur. Untuk mengklaim kompatibel dengan aplikasi tertentu (ChatGPT/Claude/Flow/TikTok/Instagram), lakukan tes terpisah pada app/versi/account target; kegagalan satu target tidak menunda aplikasi dasar jika fitur target itu dinonaktifkan.

## 27. Rencana implementasi & paket keluaran v2

### M0: Proof of Control (wajib sebelum agent penuh)
- Bangun APK debug via CI, uji permission/overlay/UI tree/open app/typed input di **perangkat fisik**, simpan catatan app version dan hasil.
- Jika UI aplikasi yang diinginkan menghalangi interaksi, beri label BLOCKED dan gunakan intent/share/manual handoff; jangan menyiasati batas keamanan.

### M1: Agent runtime yang aman
- Goal/chat + scope grant + tool registry/schema validator + 1 app adapter + observe/act/verify + timeline + stop.
- Demo: perintah teks untuk membuat draf di aplikasi yang telah lulus PoC; selesaikan tanpa aksi di luar grant.

### M2: Vision, self-healing, replanning
- Tambahkan accessibility screenshot API, UI state diff, model vision opsional, error classifier, fallback selector, bounded replan, account/package validation.
- Demo: gangguan UI/loading menghasilkan reobserve/recover atau BLOCKED yang jelas.

### M3: Skills, memory, events
- Skill library/versioning, memory opt-in/CRUD, event hub, scheduler integration, anti-loop, profile binding.
- Demo: skill tersimpan dipakai ulang setelah konfirmasi, dapat dimatikan/dihapus.

### M4: Multi-app compatibility + hardening
- Test real app/version matrix, unauthorized action tests, privacy audit, cost/memory/battery profile, graceful degrade.
- Dokumen `docs/AGENT_ARCHITECTURE.md`, `docs/TOOL_POLICY.md`, `docs/SCOPE_GRANTS.md`, `docs/SKILLS.md`, `docs/MEMORY.md`, `docs/EXPERIMENTAL_TEST_MATRIX.md`, `docs/PLAY_POLICY_LIMITATIONS.md`.

### Instruksi bagi Codex Cloud (implementasi)

1. Anggap dokumen ini sebagai target desain, **jangan klaim fitur berhasil bila hanya skeleton/mocked**.
2. Pertahankan keputusan #1–#22 dan modul lama. Gunakan feature flag `experimentalAgentEnabled=false` pada instalasi awal.
3. Prioritaskan PoC Android: APK dapat dipasang; pengguna dapat memberikan izin; overlay dapat berinteraksi pada satu target kompatibel.
4. Agent AI hanya boleh menghasilkan *typed proposed tool call*; evaluasi izin dan state harus dilakukan di native Kotlin, bukan dipercayakan sepenuhnya kepada LLM.
5. Jangan menambah backend, mengandalkan root, ADB, Shizuku, atau background hack. Provider API menggunakan konfigurasi dari APK via HTTPS.
6. Gunakan stub/fake provider untuk unit/instrumentation tests dan mock UI test target. Jika tidak bisa menjalankan tes fisik di Codex Cloud, tulis status `NOT TESTED` beserta prosedur pengujian HP.
7. Laporkan daftar fitur `IMPLEMENTED`, `EXPERIMENTAL`, `BLOCKED`, `NOT TESTED` di changelog; tidak menamai stub sebagai selesai.
8. CI tetap menghasilkan debug APK artifact, `testDebugUnitTest`, dan lint/kt compilation checks bila lingkungan mendukung.
9. Berikan dokumentasi izin sensitif, saran penggunaan pribadi, serta perbedaan sideload vs Google Play policy; tidak mencap aplikasi sebagai alat aksesibilitas disabilitas bila tidak memenuhi syarat.

## 28. Referensi resmi

- Android overlay: https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW
- Android accessibility: https://developer.android.com/guide/topics/ui/accessibility/service
- Accessibility screenshot methods (API 30/34): https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- Android MediaProjection: https://developer.android.com/media/grow/media-projection
- Android background restrictions: https://developer.android.com/guide/components/activities/secure-bal
- WorkManager periodic minimum 15 minutes: https://developer.android.com/reference/androidx/work/PeriodicWorkRequest
- Google Play Accessibility API policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Codex Cloud: https://help.openai.com/en/articles/20001545-using-codex-cloud

---

**Implementation note v2.0:** Eksperimental adalah **pilihan desain**, bukan bukti kemampuan atau pembebasan dari batas Android, privasi, kontrak pihak ketiga, atau kebijakan distribusi. Semua status uji awal tetap NOT TESTED. Tidak ada kode/APK yang sudah dibangun lewat pembuatan PRD ini.
