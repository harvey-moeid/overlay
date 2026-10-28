# Penerimaan perangkat asli — 0.7.0

Target prioritas Samsung Galaxy A06 Android 16/One UI. **Seluruh skenario di bawah NEEDS_REAL_DEVICE** sampai dijalankan dan dicatat. JVM/Robolectric, fixture Chromium, APK build dan emulator CI tidak membuktikan kompatibilitas aplikasi target asli.

Cloud 28 Oktober 2026: tidak ada device/emulator pada `adb devices -l`; `/dev/kvm` tidak tersedia. Instrumentation APK dapat dikompilasi, tetapi tidak boleh dilaporkan sebagai device PASS.

## Persiapan dan bukti

Gunakan perangkat/akun QA dengan kuota yang disetujui. Catat model, RAM, OS/One UI, build/versionCode/signature BYGA, versi ChatGPT/Chrome/Android System WebView, akun publik/model/proyek yang digunakan (tanpa password, OTP, API key, cookie). Gunakan **Compatibility Lab** untuk hasil manual dan Settings → Performa untuk metrik lokal.

Izin Android: overlay, Accessibility opsional untuk native mapping/input, notifikasi untuk bubble, SAF untuk gambar pilihan, MediaProjection terpisah hanya bila capture diminta. Android sideload dapat meminta Izinkan setelan terbatas melalui Settings resmi. Tidak memakai root/ADB/Shizuku untuk penggunaan aplikasi; perintah instrumentation hanya untuk perangkat QA developer.

## Checklist

| Skenario | Bukti yang wajib diperiksa |
|---|---|
| Install/update APK dengan data lama | Signature sama, versionCode naik, workflow/mapping/keys lama tetap terbaca; jangan uninstall data pengguna |
| Izin ditolak/dicabut saat run | Tidak ada dispatch berikutnya, observer/job berhenti, pesan kegagalan jelas |
| Bubble drag/dock/keyboard/rotasi/reduce motion | Visual default 28dp, touch >=48dp, sentuhan di luar bubble tidak terblokir; tidak Stop palsu saat drag |
| Picker overlay | Sorotan sesuai bounds; sentuhan pemilihan tidak diteruskan; cancel/30s timeout tidak mengklik |
| Mapping jenis kontrol | Button/icon, input tanpa isi, toggle, tab, menu/dialog dan scroll container; item disabled ditolak |
| Tombol pindah / resource ID berubah / kembar | Pindah semantik FOUND; atribut berubah NEEDS_REVIEW; duplikasi AMBIGUOUS, tidak ada klik tebakan |
| App/window/origin/proyek salah | BLOCKED, termasuk Chrome address bar tersembunyi/diubah dan WebView tanpa adapter |
| Posisi manual + rotasi | Approval tiap eksekusi; perubahan ukuran/layout/orientasi ditolak |
| Recorder | Hanya pilihan eksplisit, urutan draft benar, input manual, stop tidak menambah langkah lagi; draft bertahan reopen |
| Runtime pause/resume/Stop | Antrean berhenti, dialog tidak menyetujui sendiri, stop saat UI/AI/download aktif membatalkan job |
| Process death | Checkpoint uncertain tidak replay; sesi web tidak resume; history tidak menyatakan sukses |
| IF/ELSE/wait/retry | Cabang sesuai output; scope salah bukan NOT_FOUND; timeout terbatas, tidak mengulang tindakan costly |
| ChatGPT web | Akun direview; prompt dikirim tepat sekali; jawaban lama/percakapan berubah ditolak; gambar valid baru |
| Flow proyek/mode/upload | Approval akun/proyek; CURRENT tidak membuat proyek baru; file grant dicabut/upload gagal tidak dianggap selesai |
| Flow Generate | Approval baru sebelum kredit; satu generate; timeout/jaringan hilang tidak menyebabkan generate ulang |
| Download/galeri | MIME/signature/ukuran/file valid; MediaStore terlihat di Pictures/Movies BYGA; CORS/blob gagal diungkap |
| Login WebView ditolak | Browser resmi/manual handoff; tidak bypass, tidak mengklaim otomasi eksternal berlanjut |
| AI provider gagal/cancel | Tidak ada aksi native dari respons AI; tidak ada screenshot outbound tanpa consent kedua |
| Workflow dijalankan ulang | Review hasil/kredit sebelumnya; dispatch baru hanya dengan approval baru |
| Layar dikunci | Native dan browser tidak mengirim aksi; tidak auto-unlock |

## Pengukuran

Mulai pengukuran baru di Settings. Scroll dashboard, Mapping Library, Studio, history dan panel bubble selama sedikitnya 60 detik pada build optimized. Catat frame count, p95/max dan frame >32ms/>700ms, heap terpakai, scan count/rata-rata/max/node, serta historical ANR exit dari OS bila tersedia. Catat suhu/kondisi background. Tidak ada target FPS/latensi atau klaim ANR-free tanpa hasil HP.

Hasil sukses generasi memerlukan identitas output baru, file galeri yang dapat dibuka, dan jumlah tindakan/kredit yang sesuai. Klik diterima, intent handoff atau JavaScript berhasil dievaluasi saja tidak cukup. Posting sosial tetap manual; HANDED_OFF bukan PUBLISHED.
