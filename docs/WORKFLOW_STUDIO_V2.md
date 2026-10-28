# Workflow Studio V2 — 0.7.0

Studio, codec, runner dan Room existing diperluas; tidak ada workflow engine kedua. Library/template/editor/reorder/import/export lama dipertahankan. Tombol penambahan aksi membungkus sesuai lebar HP. Duplikasi membuat draft ID baru dan tidak mengeksekusi alur.

## Membuat dan merekam

- Pilih aplikasi launcher dan/atau mapping tersimpan. Tambahkan input, wait, scroll, Kembali, approval, aksi web/provider atau langkah lokal. Langkah mapping mengikat ID, package dan fingerprint versi database.
- **Start Recording** membuat draft Room. Buka app target, gunakan picker lalu Simpan saja; atau pilih mapping existing di Library → **Pilih untuk rekaman**. Recorder menambahkan launch bila perlu, wait FOUND, kemudian klik/scroll sesuai jenis elemen. Input hanya merekam target wait; teks ditambahkan manual melalui Isi field di editor.
- **Stop Recording** → **Review draft rekaman** → urutkan/edit → Dry run → Simpan. Draft tetap tersedia setelah proses mati tetapi recording tidak aktif kembali otomatis. Stop darurat menghentikan recording; tidak menghapus draft.
- Recorder tidak mengamati/merekam semua sentuhan lintas aplikasi, teks field, atau screenshot. Pilihan eksplisit adalah sumber rekaman.

## Kondisi, retry dan dry run

`WAIT_FOR_STATE` memakai mapping semantik versi yang dipilih, kondisi FOUND/NOT_FOUND, timeout 1000–600000ms dan 0–2 retry observasi. NOT_FOUND hanya berarti identitas tidak ada dalam scope yang benar; scope salah/ambigu/grant dicabut tidak dianggap kondisi sukses. Timeout keseluruhan tetap membatasi percobaan. Hasil wait menjadi output string FOUND/NOT_FOUND; perhatikan ini jika langkah berikutnya memakai `{{output}}`.

IF/ELSE membandingkan output sebelumnya dengan EQUALS atau NOT_EMPTY. IF benar melanjutkan langkah berikutnya, ELSE melewati 1–50 langkah ke depan yang masih dalam plan. Tidak ada jump mundur atau loop. Urutan/prasyarat web tetap diperiksa saat validasi.

Dry run memeriksa struktur, timeout/retry, referensi database, grant package, versi dan jenis mapping tanpa aksi. Ini **bukan** bukti UI/aplikasi/akun sedang sesuai. Aksi costly tidak dapat dikonfigurasi retry, termasuk provider AI.

## Runtime dan pemulihan

Fase yang ditampilkan: OBSERVE → RESOLVE_TARGET (resolver native) → VALIDATE → ACT → VERIFY → CHECKPOINT. Status monitor: IDLE, RUNNING, WAITING_FOR_UI, WAITING_FOR_APPROVAL, PAUSED, SUCCESS, FAILED, UNCERTAIN, CANCELLED. Enum internal legacy tetap didukung; status V2 diturunkan untuk UI.

Runner menyimpan checkpoint uncertain **sebelum** dispatch. Adapter input harus membaca nilai yang sesuai; adapter web menunggu bukti DOM baru dan penyimpanan media. Native klik/scroll/Kembali yang hanya diterima Android membutuhkan pemeriksaan pengguna; tidak menyimpulkan tujuan bisnis dari `performAction=true`.

Stop membatalkan child job aksi, pertanyaan approval, sesi web, recorder, dan pending mapping. Pause menahan antrean/dispatch berikutnya; request yang sudah diterima situs/provider tidak dapat ditarik kembali. Approval saat pause tidak berlaku sampai dilanjutkan.

Checkpoint lokal hanya boleh dilanjutkan jika hash, posisi, output dependensi dan status certainty valid. Checkpoint web tidak di-resume: bukti DOM/session RAM tidak bertahan process death. Menjalankan ulang workflow yang pernah melakukan efek memerlukan dialog review hasil/kredit sebelumnya; setelah itu setiap aksi web sensitif tetap meminta approval baru. Jangan mengulang ketika hasil situs belum diketahui.

Setiap eksekusi memakai ID riwayat baru; menjalankan ulang tidak menimpa riwayat run sebelumnya. Checkpoint terakhir tetap per workflow. Output teks/URI hanya disimpan privat di checkpoint. Ekspor workflow tidak menyertakan API key, grant, cookie, approval atau screenshot; mapping antar HP harus dibuat ulang.

## ChatGPT Image → Google Flow Video → Save to Gallery

1. Pilih template bernama **CHATGPT IMAGE → GOOGLE FLOW VIDEO → SAVE TO GALLERY**. Edit prompt gambar/video dan Dry run, lalu review hash dan Jalankan.
2. Login manual dalam browser BYGA. Periksa akun yang terlihat dan masukkan alias publik pada panel approval sebelum send. Prompt dikirim sekali; hasil harus berasal dari balasan baru dan penanda completion.
3. Gambar valid disimpan ke MediaStore `Pictures/BYGA`. URI hasil dipakai untuk upload melalui `{{output}}`.
4. Di Flow pilih **Buat baru** atau **Pilih proyek terbuka** pada langkah proyek. Untuk proyek existing, buka proyek melalui situs sebelum menyetujui panel akun/proyek. Pemilihan mode/model/crop tetap memakai UI resmi dan dapat memerlukan tindakan manual.
5. Upload dan Generate masing-masing membutuhkan approval di browser BYGA. Preview upload harus memiliki metadata input/frame yang didukung. Avatar/gambar lain tidak menjadi bukti upload. Generate dipanggil sekali dan video baru harus dapat dibedakan dari baseline lama.
6. Video valid disimpan ke `Movies/BYGA`, dengan pemeriksaan MIME/signature/ukuran dan file MediaStore existing. Periksa galeri serta riwayat run.

Native ChatGPT, Chrome dan WebView BYGA memakai adapter/sesi berbeda. Cookie tidak dibagi/diekspor. Jika login WebView ditolak, pilih **Buka browser resmi • handoff manual**. Ini menghentikan otomasi BYGA; browser eksternal tidak mewarisi adapter atau sesi BYGA. Tidak ada bypass login/CAPTCHA.

Guard command mencegah redispatch saat fase begin dipanggil ulang. Activity baru menolak command yang sudah dimiliki Activity sebelumnya, termasuk saat bukti hilang. Origin/proyek berubah menghentikan adapter. Struktur situs asli belum diverifikasi pada HP: **NEEDS_REAL_DEVICE**.
