> Pembaruan 0.7.0: [Workflow Studio V2](WORKFLOW_STUDIO_V2.md) menambahkan approval akun/proyek/aksi di browser BYGA, guard duplicate dispatch, proyek NEW/CURRENT, dan handoff browser resmi. Akun diverifikasi manual oleh pengguna; kompatibilitas situs asli tetap NEEDS_REAL_DEVICE.

# Workflow aplikasi, ChatGPT dan Google Flow — 0.5.0

## Memulai

1. Buka **Alur / Workflows**, pilih template, lalu edit prompt.
2. Pada **Buka aplikasi terinstal**, tekan **Pilih aplikasi terinstal** dan cari nama aplikasi. Daftar berisi aplikasi dengan launcher pada profil Android yang sedang dipakai. Pemilih yang sama ada di **Apps → Pilih & buka aplikasi terinstal**. Tidak memakai izin QUERY_ALL_PACKAGES.
3. Template **ChatGPT → prompt → salin teks / unduh gambar** membuka `https://chatgpt.com/` dalam browser workflow BYGA, mengisi/mengirim prompt satu kali, menunggu jawaban baru selesai, lalu menyalin teks atau menyimpan gambar. Ini terpisah dari aplikasi native ChatGPT dan tidak memerlukan API key provider BYGA atau Accessibility.
4. Template **Flow → unggah gambar → generate → unduh video** memerlukan **Pilih gambar perangkat**. JPEG/PNG/WebP maksimal 20MB diperiksa dan izin baca SAF disimpan. Template sengaja belum valid sebelum gambar dipilih.
5. Template **ChatGPT gambar → Flow → video** mengunduh gambar ChatGPT lalu memakai URI hasil sebagai unggahan Flow melalui `{{output}}` tanpa memilih ulang file.
6. Tinjau prompt, gambar, akun situs, dan kredit yang akan dipakai. Centang review lalu **Jalankan**. Workflow memakai kuota/model akun situs, bukan provider API BYGA.

## Browser workflow

Login dilakukan sendiri di halaman resmi. Halaman login tidak diotomatisasi. Cookie/penyimpanan situs berada pada perangkat; login browser biasa atau aplikasi native tidak otomatis terbawa ke WebView BYGA. **Hapus sesi login browser** menghapus cookie dan web storage lokal ketika tidak ada workflow aktif.

Google/ChatGPT dapat menolak login browser tertanam, meminta CAPTCHA, menerapkan pembatasan negara/akun, atau mengubah UI. Dalam keadaan ini alur menunggu/berhenti dan **belum dapat dianggap kompatibel pada HP tersebut**. Membuka browser eksternal tidak memindahkan sesi atau mengaktifkan adapter ini.

Di Flow, adapter membuat proyek baru, mencoba kontrol Frames to Video/Upload yang dikenali, meneruskan gambar yang dipilih ke file input, mengisi prompt, lalu generate satu kali. Mode/model, dialog proyek, pemilih asset, crop atau konfirmasi lain dapat membutuhkan tindakan manual. Petunjuk langkah muncul di atas situs. Jika tombol tidak cocok, tidak ada tap berdasarkan koordinat atau generate ulang otomatis. Buka preview hasil baru bila situs baru memuat elemen video setelah preview dibuka.

**Alur** kembali ke editor. **Lanjutkan di browser workflow** membuka lagi sesi aktif. **Jeda/Lanjut** menghentikan/melanjutkan langkah BYGA; proses generasi yang sudah diterima situs tetap bisa berjalan. **Stop** menghentikan antrean dan transfer lokal; tidak membatalkan kredit/generasi yang sudah diproses situs.

## Hasil dan pemulihan

- Gambar disimpan ke `Pictures/BYGA`, video ke `Movies/BYGA` melalui MediaStore, Android 10+. Maksimum 50MB; JPEG/PNG/WebP/MP4/WebM. MIME, signature, ukuran dan file tersimpan diperiksa sebelum status sukses. Browser mengunduh dengan sesi situs, termasuk sumber blob; CORS/login dapat menghalangi unduhan sehingga tombol download resmi situs perlu dipakai manual.
- Teks disalin hanya dari jawaban baru yang sudah selesai; hasil sebelumnya tidak dipakai. Hasil langkah juga disimpan lokal pada checkpoint. Batasi prompt/hasil teks sampai 32000 karakter.
- Setiap langkah browser dibatasi 10 menit. Aksi menunggu halaman resmi terlihat, perangkat tidak terkunci, dan sesi aktif. Muat ulang/penutupan browser atau kematian proses dapat menghilangkan bukti langkah; **resume checkpoint web dinonaktifkan**. Periksa proyek/percakapan sebelum memulai workflow baru agar tidak membayar/generate dua kali.
- Checkpoint ditulis sebagai tidak pasti sebelum efek eksternal. Respons situs hanya dipakai sebagai data, bukan perintah atau script workflow. Adapter berupa asset tetap dan tidak mengekspos JavaScript bridge ke situs.

## Bukti uji

Tes JVM memeriksa urutan langkah, perubahan approval, URI, pembatasan origin, timeout, pause/approval, dan generate tanpa retry. `node scripts/test-workflow-adapter.cjs` memakai Playwright + Chromium pada fixture **offline**, mencakup prompt literal/multiline, isolasi jawaban lama, kesiapan upload dan video, serta satu kali dispatch. Script membutuhkan Node, Playwright dan Chromium (`BYGA_CHROMIUM` opsional); dependensi ini hanya untuk pengujian, bukan APK.

Build, fixture, dan lint bukan bukti keberhasilan login/generate pada situs asli. Uji perangkat/akun asli belum dijalankan. Untuk penerimaan, jalankan ketiga template, uji login ditolak, prompt tanpa gambar, file grant dicabut, kredit habis, perubahan UI, CORS, pause/lock/Stop, serta periksa hasil galeri dan jumlah generasi pada akun sebelum memberi status PASS.

UI JVM memeriksa template dan editor langkah aplikasi. Uji dialog native/pemilih launcher dipindahkan ke instrumentation perangkat karena frame/idling dialog Robolectric tidak stabil pada lingkungan ini; instrumentation tersebut baru dikompilasi, belum dijalankan.
