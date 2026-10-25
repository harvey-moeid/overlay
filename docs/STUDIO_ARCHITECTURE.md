# Rilis 0.4.0 — arsitektur Studio

## UI
`ui/DesignSystem.kt` mendefinisikan tema, tipografi, warna, card, hero, status pill dan destination registry. MainActivity memakai Scaffold dengan status/stop toolbar, bottom navigation dan workspace sheet. SaveableStateHolder menjaga draft field sederhana antar tab. Status bubble berasal dari StateFlow aktual OverlayRuntime. Settings/Providers/Agent lama tetap tersedia dalam design system baru.

## AI
`AiRepository` membaca konfigurasi provider dan key Keystore hanya untuk permintaan yang dipicu pengguna. `BudgetLedger` memakai Room transaction untuk reservasi request harian WIB sehingga dua request bersamaan tidak melampaui cap. Fallback hanya jika pengguna memilihnya, maksimal tiga destination tersimpan. Ini request cap, bukan perkiraan biaya dolar. `HttpProvider` kini memakai OkHttp asynchronous call yang dibatalkan saat coroutine dibatalkan. ChatViewModel menjaga request/riwayat pada pergantian tab/configuration; Stop membatalkan operasi aktif. Screenshot/memory tidak otomatis masuk prompt Chat.

## Workflow
`WorkflowRunner` pure Kotlin, terpisah dari Activity: schema/hash approval → device readiness → checkpoint uncertain → act → checkpoint confirmed. Output disimpan privat untuk dependent steps dan resume. Ketidakpastian/versi berubah/missing output memblokir resume. REQUEST_USER_APPROVAL berjangka 5 menit dan tidak dapat dilewati oleh JSON/model. Runner memakai mutex satu workflow. Langkah UI memerlukan foreground/unlocked; network-safe preparation tidak meluncurkan UI. Launch Intent, copy, direct download dan generate text adalah adapter nyata. Aksi post/send/delete/tap arbitrary/branch/macro belum tersedia. Tidak mengklaim global durable exactly-once di semua aplikasi.

## Profil & sosial
Akun di `Record(kind=account)` memakai typed codec dan migrasi baca kompatibel legacy. Label/alias bukan password. USER_CONFIRMED adalah pernyataan pengguna 5 menit yang terikat package; edit/import meresetnya. PublishingStudio menyimpan draft dan content URI grant SAF; tidak mengopi file media ke backend. Handoff memeriksa profile/package/age/review lagi pada dialog final dan memastikan target dapat menerima format intent. Status hanya HANDED_OFF, bukan hasil publikasi app tujuan.

## Scheduler
ScheduleConfig dan ScheduleWorker tersimpan per UUID; WorkManager one-time/periodic. Priority mengurutkan antrean di UI, bukan janji OS. Dependency menunggu state USER_COMPLETED yang pengguna tandai; retry maksimum lima kali dengan backoff 15 menit lalu notifikasi manual. Workflow yang dihapus diblokir. Scheduled worker tidak menjalankan aksi UI, AI atau posting otomatis.

## Capture
CapturePolicy membatasi package; service memeriksa active package, lock dan field sensitif best effort melalui Accessibility yang diaktifkan pengguna. Android 14+ meminta default-display session; Android consent tetap diperlukan setiap sesi. FGS mediaProjection terpisah, callback stop dan timeout, satu frame di ImageReader, size <=1280px lebar dan <=2048px tinggi, cache app-private/FileProvider, preview maksimal lima per halaman dan explicit second consent sebelum share. ManualInputPermit memisahkan jalur native input manusia dari tool AI: immutable text+target, review, 10 detik dan single-use, focused field only, tidak klik tombol dan tidak menerima perintah JSON/model. Panel bubble ditutup sebelum context/field checks lalu nilai dibandingkan untuk SUCCESS; alternatif clipboard tetap tersedia.

Tidak ada capture agent kontinu. VisionReview mengirim satu gambar JPEG yang diperkecil hanya setelah model ditandai mendukung Vision, review dan consent kedua. Payload OpenAI-compatible/Anthropic/Gemini ditangani native; Cohere Vision diblokir. Hasil hanya teks, tidak dijadikan aksi tool. Android dapat menghitamkan layar FLAG_SECURE; pemeriksaan heuristik bukan jaminan redaksi seluruh informasi pribadi. Pengguna wajib meninjau. Stop mencabut sesi, frame tidak menjadi long-term memory.

## Data
Schema Room tetap v2 karena tambahan workspace memakai tabel records existing; tidak ada destructive migration. Account/workflow legacy dibaca dengan codec kompatibel. Export whitelist hanya workflow/prompt/account/app, maksimum 100 entri. Import strict format/schema/size, UUID baru, account confirmation reset, tidak membawa approval/key/chat/capture/schedule. Semua import inert sampai pengguna review dan menjalankan. SecretStore tidak dipanggil oleh export.

## Batas
Lihat IMPLEMENTATION_STATUS.md. Multi-app adaptive agent, Cohere Vision, monetary budgets, automated account detection, native download automation, event hub dan autonomous publishing belum diselesaikan. Tidak mengklaim persentase kesiapan tanpa acceptance perangkat. UI/backend test cloud bukan bukti ChatGPT/Claude/Flow/TikTok/Instagram bekerja pada versi perangkat pengguna.
