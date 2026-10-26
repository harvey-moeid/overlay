# Smart Mapping dan maskot BG — 0.6.0

## Logo dan robot bubble

Maskot memakai desain referensi pengguna: robot biru/cyan, wajah BG, tulisan byga, serta alpha transparan. Asset produksi `app/src/main/res/drawable-nodpi/byga_robot.png` berasal dari hasil penyuntingan image generation; sumber di `/workspace/generated_images` dipertahankan. PNG 1254×1254 RGBA, alpha 0–255 dan sudut transparan diperiksa. Asset digunakan pada header, dashboard, preview Settings, bubble dan foreground ikon launcher adaptif. Ukuran bubble tersedia 56–128dp, default instalasi baru 80dp; preferensi ukuran lama tetap berlaku.

## Cara memetakan

1. Buka **Lainnya → Smart Mapping** (atau kartu Smart Mapping di dashboard). Aktifkan Accessibility dan izin/bubble BYGA melalui pengaturan Android resmi. Setelah upgrade yang mengubah kapabilitas service, Android dapat memerlukan disable/enable ulang service.
2. Siapkan layar tujuan di **ChatGPT terinstal**, **Chrome pada chatgpt.com / Google Flow**, atau **browser BYGA**. Tombol ChatGPT Web BYGA / Flow Web BYGA membuka sesi browser untuk belajar tanpa menjalankan template.
3. Ketuk robot → **Smart Mapping • deteksi tombol**. Daftar menampilkan tombol beridentitas yang terlihat dan dapat diklik. Pilih satu untuk menyorot target.
4. Alternatifnya pilih **Smart Mapping • tunjuk langsung**. Sentuhan ditangkap overlay pemilihan dan **tidak diteruskan** ke tombol asli. Garis cyan menunjukkan kandidat. Ketuk lokasi target; mode dapat dibatalkan dan berakhir setelah 30 detik.
5. Panel review menunjukkan nama, aplikasi, situs/layar, jenis identitas dan opsi **Tanya sebelum setiap klik workflow**. Pilih **Simpan saja**, **Simpan & uji tombol sekarang**, atau **Batal**. Tidak ada klik uji dari pemilihan atau penyimpanan saja.
6. Bila identitas tidak terbaca atau identik dengan tombol lain, pilihan disimpan sebagai **posisi manual**. Konfirmasi setiap pemakaian wajib, tidak dapat dimatikan. Ukuran, orientasi dan fingerprint layout harus tetap sama. Titik disorot lagi saat konfirmasi. Perubahan visual pada canvas tidak selalu bisa diketahui dari Accessibility; periksa titiknya sebelum menyetujui.

Logo, pemetaan, dan daftar kandidat tidak mengunggah isi layar ke AI. Pemindaian hanya saat pengguna memulai mapping, meminta uji, atau menjalankan langkah workflow yang direview. Service event hanya mencatat metadata kelas layar native, bukan riwayat teks event. Metadata tombol yang dipilih disimpan lokal pada records `button_mapping`; hasil penerimaan perintah uji pada `mapping_test`. Mapping tidak ikut backup konfigurasi saat ini, sehingga workflow dengan referensi mapping dari HP lain perlu dipetakan ulang.

## Library dan workflow

**Smart Mapping** menyediakan daftar, aktif/nonaktif, nama/opsi konfirmasi, hapus, uji dengan pertanyaan terlebih dahulu, dan **Buat workflow dari tombol ini**. Area latihan memungkinkan memetakan tombol BYGA tanpa mengirim prompt/memakai kredit.

Di editor Alur gunakan **+ Klik tombol Smart Mapping** lalu pilih mapping. Step menyimpan ID, package dan fingerprint versi mapping; bukan koordinat bebas dari teks/AI. Workflow tetap perlu review hash sebelum Jalankan. Perubahan nama/opsi/target, penonaktifan atau penghapusan mapping membatalkan kecocokan versi lama. Referensi database dicek lagi sesaat sebelum klik.

Target aplikasi dibuka kembali bila perlu. Pengguna harus menyiapkan layar/akun/tab yang sesuai; Mapping tidak menavigasi ke proyek atau percakapan tertentu. Untuk alur membuat proyek/unggah/generate/download lengkap, gunakan template web 0.5.0 dan sisipkan langkah mapping bila diperlukan. Memetakan tombol Upload tidak otomatis memilih file atau menyelesaikan dialog file/crop pada aplikasi lain.

Tombol semantik dikenali ulang berdasarkan resource ID, teks/description, role dan parent ID yang tersedia, dengan hasil **tepat satu**. Pergeseran posisi saja tidak merusak mapping semantik. Label berubah, kandidat hilang/kembar, aplikasi/situs/layar berbeda, snapshot lama, window berubah atau HP terkunci menghentikan klik. Sebelum dispatch, identitas node diperbarui dan origin browser diperiksa ulang.

Identitas Chrome dan browser BYGA terpisah, walaupun situs sama. Chrome memerlukan address bar yang terbaca dan tidak sedang diedit; halaman Flow juga memerlukan path Flow yang dapat diperiksa. Browser BYGA memperoleh URL dari Activity native, bukan JavaScript situs. Layar password dan kolom verifikasi yang terdeteksi diblokir; pemeriksaan ini best effort, bukan detektor universal semua data sensitif. Login tetap manual. Pembatasan login WebView dari versi 0.5.0 tetap berlaku.

**CLICK_ACCEPTED** berarti Android menerima perintah klik/gestur satu kali. Ini **bukan** bukti pesan sudah terkirim, kredit terpakai, gambar terunggah, file terunduh, atau publikasi berhasil. Workflow yang memuat mapping menampilkan catatan tersebut pada hasil akhir. Pemeriksaan bisnis lanjutan perlu langkah/verifikasi yang sesuai atau pemeriksaan pengguna.

Jeda menahan langkah mapping; Stop membatalkan antrean/pertanyaan dan menutup overlay. Klik yang sudah dikirim atau proses di server tidak dapat dibatalkan oleh Stop BYGA. Permit konfirmasi satu kali terikat versi mapping dan kedaluwarsa 15 detik setelah pengguna menyetujui. Klik gagal/tidak pasti tidak diulang otomatis. Checkpoint uncertain tetap memerlukan pemeriksaan manual.

## Kapabilitas dan bukti

Android Accessibility sekarang memakai `flagRetrieveInteractiveWindows` dan `canPerformGestures=true` untuk memilih window aplikasi di bawah overlay serta fallback posisi yang dikonfirmasi. Tidak memakai root, Shizuku, ADB, tap koordinat tanpa review, OCR atau planner AI. Agent Lab/registry AI lama tidak mendapat kapabilitas klik baru.

Tes JVM meliputi identitas bergeser, duplikasi, perubahan role/parent/label, scope salah, snapshot kedaluwarsa, layout/orientasi, permit satu kali/expired, codec dan checkpoint tanpa retry. Fixture Android Robolectric menjalankan adapter native terhadap node Accessibility, termasuk klik sekali, tombol bergeser, window/target/lock/password dan pembatasan konten Chrome/area latihan. UI Compose JVM memeriksa route mapping, tombol latihan dan logo. Ini berbeda dari bukti di aplikasi target nyata.

APK instrumentation mencakup smoke route dan latihan. Uji perangkat/One UI, izin service setelah update, overlay pointer, gesture posisi, browser terpasang, dialog IME, URL bar Chrome, dan ChatGPT/Flow dengan akun asli masih **NEEDS_REAL_DEVICE** sampai benar-benar dijalankan. Build dan jumlah tes tidak berarti kompatibilitas 99,9%.


Validasi lokal akhir: **191 tes JVM lulus**, 0 failure/error/skipped; build APK/instrumentation dan lint tanpa error. Signature debug v2 valid. Fixture browser offline lulus. Tidak ada perangkat/emulator tersambung. [Rincian hasil](TEST_MATRIX.md).
