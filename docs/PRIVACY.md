# Privasi & kontrol — Studio 0.4.0

Tidak ada backend pribadi atau analytics. Orkestrasi di HP, HTTPS langsung hanya ke provider yang dikonfigurasi/dipilih pengguna. API key disimpan AES-GCM dengan Android Keystore/AAD provider ID; backup/transfer aplikasi dikecualikan. Metadata/model terpisah dari key. Perangkat terkompromi tetap dapat mengekspos data runtime; database teks app-private bukan encrypted database.

## Data keluar

AI Chat mengirim prompt dan maksimal 8 pesan konteks terakhir/24k karakter setelah consent. Fallback ke maksimal dua provider tambahan hanya bila diaktifkan pengguna. Workflow AI mengirim prompt langkah yang ditinjau. Agent Lab planner mengirim goal/koreksi yang diketik, bukan UI tree/memory/screenshot otomatis. Health test dan model discovery adalah tindakan terpisah. Batas request harian Studio mencakup Chat/Workflow/Vision, bukan dollar cap atau jaminan tagihan provider.

Vision memerlukan model yang ditandai mendukung input gambar dan consent kedua per gambar/model. Preview capture ditampilkan terlebih dahulu. Satu JPEG terkompresi dan pertanyaan dikirim setelah dialog konfirmasi; tidak ada guaranteed automatic redaction. Jangan kirim OTP, password, login, finansial atau konten yang tidak Anda izinkan. Hasil Vision hanya teks, tidak dieksekusi sebagai tool. Cohere Vision belum tersedia. Tidak ada screenshot di audit log.

## Capture & Accessibility

Capture hanya satu sesi MediaProjection dengan dialog resmi Android, foreground service, target package allowlist, keyguard dan field checks best effort. Browser diblokir karena domain belum diverifikasi. Bubble ditutup untuk capture; callback/timeout/Stop membersihkan sesi. FLAG_SECURE tetap diproteksi OS, dapat menghasilkan area hitam. Heuristik tidak dapat menjamin semua isi sensitif terdeteksi: pengguna harus meninjau gambar. Gambar berada di cache privat/FileProvider, dapat dihapus; cache >24 jam dibersihkan saat sesi capture berikutnya. Tidak menjadi long-term memory default.

Accessibility hanya aktif jika pengguna mengaktifkannya di Settings setelah disclosure. Tidak mengumpulkan event text pasif. Action agent tetap hanya safe field BYGA. Input manual terpisah di panel bubble boleh mengisi focused editable field pada whitelist setelah review teks/akun/target per aksi, permit single-use 10 detik dan context checks; tidak klik send/publish. Capture boleh memeriksa semantics pada target yang pengguna pilih untuk sesi itu. Tidak ada OTP/password reader, auto-unlock, permission bypass, root, ADB runtime atau Shizuku. Untuk mencabut izin sistem, gunakan pengaturan Android resmi.

## Penyimpanan dan hapus

Chat/history, prompt, workflows/checkpoint output, account aliases, schedule dan draft media URI disimpan lokal sesuai tindakan pengguna. Akun tidak menyimpan password/sesi; USER_CONFIRMED adalah konfirmasi manusia sementara, bukan bukti login otomatis. Long-term memory hanya melalui tombol simpan consent; dapat dicari/edit/forget/export. Penghapusan lokal tidak menarik data yang sudah dikirim ke provider/aplikasi tujuan.

Config export hanya whitelist workflow/prompt/account/app, tanpa key/provider secrets/chat/capture/scope/jadwal aktif. Isi prompt/alias dapat privat: ada review dialog sebelum sharesheet. Import strict schema/size, UUID baru, reset account confirmation dan tidak menjalankan workflow. Export memory terpisah tetap eksplisit. Hapus template/chat/draft/account/capture melalui modul masing-masing; hapus data aplikasi untuk membersihkan seluruh data termasuk key.

## Stop & side effects

Toolbar Stop, bubble/notification Stop mencabut agent dan membatalkan pekerjaan BYGA terdaftar secara best effort. Tidak membatalkan aksi yang sudah selesai atau menutup aplikasi tujuan. Worker jadwal hanya notifikasi WAITING_FOR_USER; tidak auto-launch saat terkunci. Sharesheet hanya HANDED_OFF, bukan PUBLISHED. Pengguna memeriksa akun dan menekan publikasi sendiri pada app tujuan.

Sideload bukan pengecualian kebijakan. Aplikasi tidak mengaku accessibility tool disabilitas; audit Google Play diperlukan sebelum rencana distribusi publik.
