# BYGA Smart Overlay

PoC Android Kotlin/Compose berbahasa Indonesia, portrait-first, navy/blue/cyan. Semua orkestrasi di HP; tidak ada backend pribadi, root, ADB atau Shizuku untuk penggunaan aplikasi. Baseline 22 keputusan: [PRD asli](docs/PRD.md). Ekstensi Experimental Agent: [PRD v2 asli](docs/PRD_v2.md).

**Status:** fondasi Phase 0 dan subset Phase 1–3. Bukan asisten otonom penuh. Kontrol UI pihak ketiga, screenshot/Vision, kontrol native download dan publishing otomatis belum tersedia. Kompatibilitas aplikasi target belum dibuktikan pada HP.

## Experimental Agent v2

Tab **Agent Experimental** menambah draft goal/koreksi, scope per sesi, policy gates, observe→act→verify pada kolom uji BYGA, timeline WIB, pause/resume/stop, skill lifecycle, memory opt-in dan Compatibility Lab. Flag awal **OFF**. Grant tidak diwariskan ke thread/skill/jadwal.

Mulai dari izin overlay + Accessibility, aktifkan flag, buat draft lokal tanpa biaya, tinjau scope lalu jalankan. Agent harus mengisi **kolom uji di layar Agent** dan membaca hasilnya sebelum COMPLETED. Ini belum membuktikan kontrol aplikasi pihak ketiga. Planner provider opsional hanya mengirim goal/koreksi yang diketik setelah Anda menekan tombol; tidak ada capture atau pengiriman UI tree/memory. Jangan tulis rahasia dalam goal.

Lihat [arsitektur/status agent](docs/AGENT_ARCHITECTURE.md), [tool policy](docs/TOOL_POLICY.md), [scope](docs/SCOPE_GRANTS.md), [skills](docs/SKILLS.md), [memory](docs/MEMORY.md), [uji v2](docs/EXPERIMENTAL_TEST_MATRIX.md) dan [batas distribusi](docs/PLAY_POLICY_LIMITATIONS.md). V2 penuh belum selesai: multi-app agent, Vision, dynamic AI replanning, event hub dan monetary cost cap masih BLOCKED/NOT_IMPLEMENTED.

## Memperoleh APK melalui GitHub Actions

1. Push perubahan source ini ke GitHub (workflow belum dijalankan hanya dengan membuat file lokal).
2. Buka **Actions → Android Debug APK**, pilih run yang sukses, atau jalankan **Run workflow**.
3. Di **Artifacts**, unduh **BYGA-Smart-Overlay-debug**, ekstrak ZIP dan ambil `app-debug.apk`.
4. Transfer ke HP Android, buka APK, izinkan instalasi dari sumber tersebut bila diminta, lalu instal. Debug APK ditandatangani debug key build; update dari runner berbeda mungkin memerlukan uninstall dan menghapus data. Bukan release APK.
5. Buka BYGA → Mulai. Baca pengungkapan, atur izin **tampil di atas aplikasi**, opsional **Accessibility**, dan **notifikasi**. Beberapa Android/Samsung meminta Allow restricted settings untuk aplikasi sideload; ikuti pengaturan resmi perangkat, jangan bypass proteksi.

## Yang terhubung

- Bubble foreground: drag/dock, tap membuka panel prompt, salin/share, long press/notification Stop, cleanup. Collapsed window hanya 56dp; panel keyboard terbuka hanya setelah tap.
- Home: prompt lokal, simpan/muat, clipboard/sharesheet, kolom uji Accessibility aman milik BYGA.
- Apps: launcher ChatGPT/Claude/TikTok/Instagram, Flow via browser dan package tambahan manual.
- Accounts: profil lokal dengan status UNKNOWN; tidak menyimpan password dan belum dapat memverifikasi akun aktif.
- Providers: metadata Room, API key AES-GCM Android Keystore, health request teks langsung HTTPS. OpenAI/OpenRouter/Groq/custom kompatibel chat completions, Anthropic Messages, Gemini generateContent. Pilih model yang benar-benar tersedia di akun. Key hanya diisi saat runtime; jangan taruh di CI/repo.
- Workflows: tambah/edit/hapus/urutkan COPY_TEXT/DELAY/STOP, simpan/muat Room, dry run, review hash versi, run/pause/resume/stop, checkpoint tanpa replay otomatis.
- Scheduler: pengingat sekali/berulang WorkManager; WAITING_FOR_USER, bukan eksekusi UI background atau janji waktu tepat.
- Media: SAF image/video picker + caption dari Home, ACTION_SEND dengan URI grants. Unduh HTTPS langsung <=50MB dengan MIME/signature validation ke MediaStore (Android 10+). HANDED_OFF bukan PUBLISHED.
- Diagnostics: status awal NOT_TESTED dan pencatatan manual terpisah dari test otomatis.

## Build lokal

Prasyarat: JDK **17 lengkap** (`javac` tersedia), Android SDK platform **36**, Build Tools **36.0.0**, lisensi SDK diterima. AGP dapat memasang Build Tools 35 yang menjadi default-nya. Gradle wrapper 8.13 dan checksum disertakan; AGP 8.11.1/Kotlin 2.1.20.

```sh
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
# Atau isi sdk.dir pada local.properties (diabaikan Git).
./gradlew testDebugUnitTest assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. Laporan unit test: `app/build/reports/tests/testDebugUnitTest/index.html`.

Cloud ini mempertahankan JDK/SDK/cache di `/workspace/toolchains`, bukan di Git. Gunakan `scripts/cloud-build.sh` untuk mengaktifkan toolchain dan proxy JVM/CA cloud yang telah diuji. Mesin biasa dan CI tidak perlu proxy cloud tersebut.

Instrumentation test Room/Keystore tersedia di `app/src/androidTest`; memerlukan emulator/perangkat uji terhubung, tidak dieksekusi hanya dengan unit test:

```sh
./gradlew connectedDebugAndroidTest
```

## PoC pada HP (Samsung / Android 16)

1. Mulai bubble, buka aplikasi target. Verifikasi bubble, drag ke kedua tepi, rotasi, keyboard panel, tap, salin, bagikan dan Stop notifikasi/tahan bubble. Ulangi setelah mencabut izin.
2. Aktifkan Accessibility secara sadar, kembali Home. Tekan **Test Input** pada layar kolom aman; cek hasil field nyata. Tidak ada aksi pada field ChatGPT/Claude/TikTok/Instagram pada versi ini.
3. Masukkan teks di Home; uji copy/paste manual dan sharesheet. Pilih media lokal di Media & Social; verifikasi tujuan dan caption. Login/pindah akun secara resmi dan konfirmasi posting sendiri; jangan tandai PUBLISHED dari handoff.
4. Tambahkan provider/key/model endpoint tepercaya, lalu **Kirim test koneksi** (berpotensi berbiaya). Fake provider tidak memanggil jaringan. Endpoint contoh: OpenAI `https://api.openai.com/v1`, Anthropic `https://api.anthropic.com/v1`, Gemini `https://generativelanguage.googleapis.com/v1beta`, OpenRouter `https://openrouter.ai/api/v1`, Groq `https://api.groq.com/openai/v1`.
5. Tinjau workflow lokal, setujui, run; uji pause/stop dan edit membatalkan approval. Force-close tidak boleh memicu replay otomatis.
6. Jadwalkan pengingat, kunci layar, tunggu sesuai OS. Harus menunggu tindakan pengguna dan tidak membuka aplikasi otomatis. Pastikan notification permission/battery policy sesuai perangkat.
7. Catat hasil manual di Diagnostik. Semua aplikasi target tetap NOT_TESTED sampai memiliki bukti perangkat.

Lihat [arsitektur](docs/ARCHITECTURE.md), [matriks uji](docs/TEST_MATRIX.md), [kompatibilitas](docs/COMPATIBILITY.md), [privasi](docs/PRIVACY.md), dan [prioritas lanjutan](docs/ROADMAP.md).
