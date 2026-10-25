# BYGA Smart Overlay — Product Requirements Document (PRD) v1.0

**Tanggal:** 25 Oktober 2026  
**Status:** Spesifikasi disetujui dari sesi tanya-jawab #1–#22; **belum** diimplementasikan atau dibuktikan pada perangkat  
**Target:** APK Android pribadi, tanpa root, tanpa backend pribadi, CI build di GitHub Actions  
**Bahasa antarmuka:** Bahasa Indonesia (internasionalisasi dapat disiapkan)  
**Metode pengembangan:** PoC terlebih dahulu, iteratif, fitur dibatasi oleh hasil verifikasi Android dan aplikasi tujuan.

## 1. Ringkasan produk

BYGA Smart Overlay adalah asisten produktivitas Android berbentuk **floating bubble beranimasi premium (inspirasi gerak Codex, bukan meniru merek/aset)**. Bubble muncul di atas aplikasi seperti ChatGPT, Claude, Google Flow, TikTok dan Instagram, untuk membantu pengguna memilih aplikasi/akun, memasukkan teks, mengunduh media yang dapat diakses, menyiapkan posting sosial, menjalankan workflow yang disetujui, dan menjadwalkan pekerjaan.

Perangkat melakukan semua orkestrasi lokal dan menghubungi API provider AI cloud **langsung dari APK**, bukan melalui Cloudflare/Render/VPS/backend milik pengguna. Internet tetap diperlukan untuk permintaan AI cloud. Tidak ada klaim bahwa overlay dapat mengontrol semua aplikasi, semua akun, atau proses layar terkunci.

**Prinsip:** UI automation adalah **user-controlled, deterministic, observable, pausable, revocable**. AI boleh membantu menyusun rencana, menyarankan selector, membuat konten dan menjelaskan error, tetapi tidak boleh diberi akses tak terbatas untuk secara otonom merencanakan sekaligus mengeksekusi aksi Accessibility pada aplikasi pihak ketiga. Langkah UI hanya berjalan berdasarkan workflow yang ditinjau/disetujui dan dengan guardrails.

## 2. Tujuan & ukuran keberhasilan

1. Membuka/menutup bubble dari Android dengan interaksi halus, tidak menutupi UI penting, selalu tersedia kendali Stop.
2. Dari bubble: input prompt dan kirim teks ke field yang dikenali dengan izin pengguna; kembali ke aplikasi asal secara aman.
3. Kelola banyak provider AI, model, API key, fallback, dan biaya perkiraan tanpa server sendiri.
4. Simpan/membagikan gambar atau video hanya melalui mekanisme resmi/URL yang dapat diakses dengan izin.
5. Rencanakan dan jalankan workflow **terverifikasi** lintas aplikasi; posting selalu memerlukan konfirmasi langsung.
6. Mengenali aplikasi dan akun aktif secara **best effort**; tidak pernah mempublikasikan ke akun yang belum diverifikasi.
7. Menjalankan scheduler sesuai batasan Android, menyimpan checkpoint dan menunda langkah layar bila terkunci.
8. Berhasil menjalankan build `assembleDebug` dan unit test melalui GitHub Actions; APK tersedia sebagai artifact.

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

## 4. Target pengguna, contoh skenario

- **Pembuat konten pribadi**: Buat prompt di ChatGPT/Claude -> buka Google Flow -> input prompt -> download hasil jika tersedia -> siapkan TikTok/Instagram -> tinjau identitas akun -> konfirmasi posting.
- **Pengguna beberapa akun**: Pilih `Flow - Akun 2`; buka menu switch resmi; verifikasi handle/email yang ditampilkan atau minta konfirmasi; jalankan template spesifik akun.
- **Scheduler**: pukul 09.00 membuat konsep caption menggunakan AI dan menyiapkan draft lokal; jika pukul 12.00 perlu membuka TikTok tetapi ponsel terkunci, kirim notifikasi 'Butuh tindakan pengguna' dan lanjutkan setelah unlock/konfirmasi.

## 5. Lingkup, bukan lingkup, dan batasan

### In scope

- APK Android native, bubble overlay draggable/dock, launcher, panel teks, quick actions, izin/diagnostik.
- Provider AI berbasis HTTP adapter, pemilihan model/role, fallback, timeout, rate-limit, masking API key.
- Accessibility untuk membaca/berinteraksi dengan UI yang diizinkan **dengan tindakan/skrip eksplisit yang disetujui pengguna**.
- Screenshot on-demand via API resmi dengan persetujuan sistem, dikirim ke provider AI **hanya setelah pengungkapan dan persetujuan pengiriman data**.
- Download native via tombol resmi atau URL HTTP(S) berizin, dengan Android `DownloadManager`/`MediaStore`/SAF.
- Android Sharesheet, draft caption, hashtag, preview konten, status posting 'menunggu konfirmasi'.
- Workflow editor, rule-based executor, approvals, scheduler, checkpoint, logging lokal.
- Multi-account profiles dan bantuan pindah akun melalui UI resmi yang tersedia.

### Out of scope v1

- Root, Shizuku, ADB permanen, bypass keamanan, silent login, pembacaan OTP/password/2FA, auto-unlock, bot yang bekerja pada lockscreen tanpa persetujuan.
- Mengambil video/gambar tersembunyi, mem-bypass DRM/FLAG_SECURE, menembus akses URL yang butuh login tanpa izin, scraping massal.
- Kontrol bebas penuh atas semua aplikasi tanpa batas; pembuatan akun palsu, spam atau posting massal otomatis.
- Remote backend, API platform sosial langsung (keputusan #4 A+B), fitur suara, integrasi TikTok/Instagram API resmi.
- Menjanjikan multi-session account terpisah atau cloning aplikasi di Android standar.

### Pembatasan nyata

- Overlay membutuhkan `SYSTEM_ALERT_WINDOW` dan tampil **di bawah UI sistem tertentu**; posisi/visibilitas dapat diubah OS.
- Accessibility hanya memperoleh node/aksi yang diekspos aplikasi tujuan. Layout/selector dapat berubah sewaktu-waktu.
- MediaProjection meminta izin untuk **setiap sesi** pada Android modern, dapat berhenti saat screen lock, dan layar sensitif boleh diblokir.
- Tugas background dibatasi Doze/optimization; WorkManager periodik minimal sekitar 15 menit dan tidak menjamin jam tepat; exact alarm terbatas izin/kategori.
- Pembukaan activity dari background dibatasi; jangan membangunkan/membuka kunci perangkat diam-diam.
- Banyak aplikasi membutuhkan otentikasi manual pada pergantian akun. Status akun `UNKNOWN` tidak boleh diasumsikan benar.
- Kebijakan Google Play melarang penggunaan Accessibility untuk agen yang secara otonom memulai, merencanakan dan mengeksekusi tindakan. Produk berfokus sideload pribadi; **tidak** mengklaim layak distribusi Play Store tanpa audit kebijakan, dan harus tetap menghindari misuse.

## 6. Arsitektur teknis

```text
Android APK (Kotlin + Jetpack Compose)
 ├─ UI Shell / Settings / Wizard / Dashboard
 ├─ OverlayService + Bubble Motion Controller
 ├─ App & Account Profile Registry
 ├─ Text Command + Quick Gesture Router
 ├─ Workflow Studio + Deterministic Execution Engine
 │   ├─ Step State Machine + Guardrails + Approval Gates
 │   ├─ AccessibilityAdapter (user-approved actions only)
 │   ├─ Clipboard & Sharesheet Adapters
 │   ├─ Native & URL Download Adapters
 │   └─ Scheduler (WorkManager / notifications)
 ├─ Screen Context
 │   ├─ Accessibility Tree Reader
 │   └─ On-demand MediaProjection (consent + cleanup)
 ├─ Multi-Provider AI
 │   ├─ OpenAI / Anthropic / Google Gemini / OpenRouter / Groq / Custom
 │   ├─ Text/Vision capability registry
 │   ├─ Cost estimates + Health/timeout + Fallback
 │   └─ AI makes content/plans; user approves UI plan before run
 └─ Room Database + DataStore + Encrypted API Secret Store (Android Keystore)
      |
      +-- HTTPS direct to provider AI APIs (only after configuration/consent)
```

**Suggested stack:** Kotlin, Jetpack Compose + Material 3, MVVM/Clean-ish packages (pragmatic), Coroutines/Flow, Retrofit/OkHttp or Ktor (choose one), Room, DataStore, WorkManager, Android Keystore AES-GCM, WindowManager overlay, AccessibilityService, MediaProjection. Avoid large runtime dependencies. Android 16/API 36 must be validated against current compatible build tooling; use a supported SDK/Gradle/AGP pairing found in environment, not assumed versions.

## 7. Fungsional requirements

### FR-01 Animated floating bubble

- Bubble 48–64 dp default, translucent glass/navy #080D19 with electric blue #246BFD and cyan accent, subtle glow and spring expand/collapse.
- Drag with edge snap/dock; minimize; close/dismiss; double tap opens text; long press opens actions; avoid overlap with system navigation/status and touch targets.
- States: idle, listening-for-text, thinking, executing, awaiting-user, success, error, paused. No microphone permission because voice not selected.
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
- Roles: Planner (suggest steps), Content Generator, Screen Describer, Validator; providers may overlap. AI cannot trigger raw unchecked accessibility action.
- Direct HTTPS; keys never in repo/BuildConfig/log, encrypted at rest with Keystore-based symmetric encryption. Risk of key extraction from compromised devices must be stated.
- Connectivity test with redacted errors; quotas estimated, not asserted exact if provider does not return usage.

### FR-05 Screen understanding + execution

- Read accessible UI semantics (package, window, node content description, text, clickability, bounds, IDs) only for allowlisted apps and while user-enabled.
- Input via `ACTION_SET_TEXT` where supported or clipboard/user-confirmed paste fallback; tap via node action preferentially, gesture dispatch only when explicitly defined and validated.
- Screenshots via MediaProjection consent for each capture session, never hidden; immediately discard by default, explicitly ask before sharing to remote AI. Hide/shut down overlay as needed to avoid self-capture.
- Handle FLAG_SECURE/no-node/permission denied as **BLOCKED**; no bypass.
- Sensitive windows (password, OTP, banking, authenticator, system settings requiring authorization) are prohibited from AI capture/action; ask user to complete manually.

### FR-06 Workflow Studio

- Steps typed and versioned: `LAUNCH_APP`, `SET_TEXT`, `COPY_TEXT`, `PASTE_TEXT`, `FIND_NODE`, `CLICK_NODE`, `SWIPE`, `WAIT_FOR_STATE`, `GENERATE_AI_TEXT`, `ANALYZE_SCREEN_ON_DEMAND`, `DOWNLOAD_URL`, `TAP_NATIVE_DOWNLOAD`, `SHARE_MEDIA`, `VERIFY_ACCOUNT`, `REQUEST_USER_APPROVAL`, `DELAY`, `BRANCH`, `STOP`.
- All tool actions pass schema validation (package allowlist, field selectors, timeouts, max step count, scope and user approval); imported AI-generated JSON is inert until reviewed and approved.
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
2. **Home:** status bubble, active app/account, quick actions, AI message input, current workflow.
3. **Providers:** add endpoint/API key/model, priority order, test connection, usage estimate.
4. **Applications:** app profiles, selector capabilities, package/web launch, manual app selection.
5. **Accounts:** grouped by app, switch guided, verify account, map workflows, never show saved passwords.
6. **Workflow Studio:** template gallery, AI-draft -> user review -> approve -> run; visual steps and conditional branches.
7. **Scheduler:** calendar/list, recurring interval, timezone, dependencies, pending-user queue.
8. **Media & Social:** file picker/download state, preview caption/platform, confirmation sheet.
9. **Monitor:** live timeline, model, activity, pause/resume/stop, checkpoints, redacted logs.
10. **Settings:** accessibility/overlay status, data privacy, battery policy tips, export safe config, app info.

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
| Accessibility automation policy | bounded deterministic workflows, user approvals, no fully autonomous plan+execute; Play review if publishing publicly |
| Cloud builds lack actual UI device test | separate PASS/FAIL/BLOCKED matrix, no fake test claims |

## 15. Definition of Done (initial MVP)

1. Repo has reproducible Android project, CI workflow emits installable debug APK artifact.
2. A user can grant/revoke permissions and display/move/dismiss overlay.
3. A user can input and share a text prompt; Accessibility text action supported where compatible.
4. Provider manager can add encrypted key and test one provider (if user configures credentials), with safe fake provider unit tests in CI.
5. At least one local workflow template is editable, reviewable and stoppable; no hidden autonomous execution.
6. Scheduler persists tasks and correctly enters WAITING_FOR_USER on blocked UI conditions.
7. Account manager stores multiple account profiles and refuses high-risk action when destination account unverified.
8. No secrets in repo/log, permission/off-switch works, README and test matrix accurately state limitations.
9. Features not implementable or not tested remain explicitly flagged EXPERIMENTAL / BLOCKED / NOT TESTED.

## 16. Referensi resmi

- Android overlay: https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW
- Android accessibility: https://developer.android.com/guide/topics/ui/accessibility/service
- Android MediaProjection: https://developer.android.com/media/grow/media-projection
- Android background restrictions: https://developer.android.com/guide/components/activities/secure-bal
- WorkManager periodic minimum 15 minutes: https://developer.android.com/reference/androidx/work/PeriodicWorkRequest
- Google Play Accessibility API policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Codex Cloud: https://help.openai.com/en/articles/20001545-using-codex-cloud

---

**Implementation note:** This document is an approved requirements baseline, not evidence that third-party app interaction has been demonstrated. A personal sideload APK is not automatically exempt from security, privacy, or third-party service terms.
