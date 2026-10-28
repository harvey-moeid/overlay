# Smart Mapping V2 — 0.7.0

Pengembangan engine `mapping/` existing. Data tetap di Room `records`, database versi 2; mapping v1 masih dibaca dan fingerprint lama tidak ditambah field default baru.

## Penggunaan pada HP

1. Aktifkan Accessibility dan bubble melalui pengaturan Android resmi. Visual bubble default 28dp, area sentuh minimal 48dp; pengaturan ukuran/reduce motion tetap tersedia.
2. ChatGPT native, Chrome dengan address bar yang dapat diverifikasi, browser BYGA, dan area latihan memakai adapter bawaan. Untuk aplikasi native lain, buka Smart Mapping → **Pilih & izinkan aplikasi**, baca disclosure dan beri consent. Cabut grant dari halaman yang sama.
3. Buka layar target → bubble → deteksi/tunjuk. Picker menampilkan button/icon, input, checkbox/switch, tab, dan container scroll yang diekspos Accessibility. Pilih dan beri nama; Simpan tidak mengeksekusi aksi.
4. Library menyediakan **Periksa target tanpa aksi**, Uji tombol dengan review, toggle aktif, edit, hapus, buat draft workflow dan tambahkan pilihan ke recorder aktif.
5. Di Studio pilih aksi Klik, Isi field, Scroll, Kembali, atau Tunggu kondisi, lalu pilih mapping. Input hanya mengisi field, tidak menekan Kirim. Scroll memerlukan container semantik. Kembali terikat layar/target mapping; bukan akses tombol sistem atau izin.

## Bukti dan resolusi

Identitas memakai resource ID, label/description kontrol yang disaring, kelas, parent ID dan fingerprint struktur kelas parent/child. Bounds dipakai untuk picker; replay semantik mencari ulang tanpa memakai koordinat lama. Metadata juga membawa jenis kontrol, enabled/clickable, checked/selected dan window ID snapshot. Window ID bukan identitas persisten antar peluncuran aplikasi.

| Status | Arti / respons |
|---|---|
| FOUND | Tepat satu identitas, jenis, scope dan window cocok; target enabled |
| AMBIGUOUS | Kandidat ganda; tidak memilih berdasarkan jarak/skor AI |
| NOT_FOUND | Tidak ada kecocokan; wait boleh mengamati ulang secara terbatas |
| STALE | Snapshot >3 detik, clock tidak valid, atau layout posisi berubah |
| BLOCKED | Package/origin/layar/grant/window berbeda atau elemen nonaktif |
| NEEDS_REVIEW | Label serupa dengan atribut berubah, atau mapping posisi manual |

Resource ID berubah dapat menghasilkan kandidat untuk ditinjau; **tidak** menyebabkan klik fuzzy otomatis. Pilih ulang lewat picker. Confidence lokal FOUND=1, kandidat perlu review=0.5; angka ini menjelaskan kecocokan metadata, bukan probabilitas hasil bisnis.

Posisi manual selalu memerlukan review setiap pemakaian, ukuran/orientasi/fingerprint layout yang sama, dan permit satu kali 15 detik. Tidak dipakai untuk input/scroll/Kembali. Semua aksi native di luar area latihan meminta approval; permit input terikat hash teks dan jenis aksi. Klik/scroll/Kembali diterima Android hanya berstatus ACCEPTED; runner menunggu pemeriksaan hasil oleh pengguna sebelum checkpoint dianggap selesai.

## Privasi, performa dan batas

Isi field editable tidak menjadi label identitas; input menggunakan hint/resource ID. Password serta indikasi OTP/verifikasi dari hint/description/resource ID memblokir snapshot. Label yang menyerupai email, kode numerik panjang atau token disaring. Ini pemeriksaan best effort, bukan jaminan dapat mengenali semua data sensitif; jangan memetakan percakapan pribadi atau layar login. Tidak ada screenshot/OCR/upload AI dalam discovery.

Grant native tidak mengizinkan browser generik, WebView aplikasi tambahan, Settings, permission controller, system UI, atau aplikasi sensitif yang dikenali. Browser memerlukan adapter origin khusus. Grant tidak memperluas izin capture atau agent safe-field lama.

Service hanya menerima event pergantian window (debounce sistem 250ms), tanpa menyimpan teks event. Snapshot diminta saat teaching, action, atau wait aktif. Traversal dibatasi 1200 node, 150 child per node, dan deadline 2 detik di dispatcher serial background. Node tidak disimpan dalam cache lintas operasi. Wait memindai paling cepat sekali per detik dan dibatalkan bersama workflow.

Dialog/panel sistem aktif atau fokus di atas target memblokir snapshot dan dispatch, termasuk Kembali; selesaikan permission/proteksi sistem secara manual. Permission revoke/interrupt memutus sesi. Perubahan window/site sebelum dispatch memblokir aksi. UI custom/canvas, label kosong, URL bar Chrome tersembunyi, dan layar terlalu kompleks dapat memerlukan handoff/pemetaan ulang. Dukungan aplikasi native tambahan bersifat eksperimental: **NEEDS_REAL_DEVICE** per app/versi.
