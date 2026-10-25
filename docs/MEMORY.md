# Memory & conversation v2

Working memory berupa state/observations/grant aktif di RAM, hilang saat keluar layar/proses. Pengguna menulis goal/koreksi sendiri; hasil state history dan pesan tersebut tersimpan lokal per task ID, tanpa mengimpor isi chat aplikasi lain. Thread baru tidak mewarisi grant lama. Hapus riwayat agent menghapus agent_tasks.

Long-term memory kosong secara default. Hanya tombol 'Saya setuju simpan memori ini' menulis MemoryEntry. Gunakan preferensi umum, bukan key/password/OTP. Search keyword Room case-insensitive, edit memuat draft; versi tersimpan baru diganti setelah consent Save, Forget/Forget-all benar-benar menghapus row, export memakai sharesheet hanya isi memory yang dipilih pengguna untuk disimpan. Tidak ada automatic AI memory atau embedding.

Memori tidak dimasukkan otomatis ke prompt cloud. Planner mengirim tujuan/koreksi pada tap eksplisit. Hapus lokal tidak menarik data yang sudah pernah dikirim ke provider atau aplikasi sharesheet. API key tetap di Keystore-backed AES-GCM, bukan memory/table agent. Database app-private masih plaintext untuk teks yang sengaja disimpan; backup/transfer dikecualikan. Retention otomatis dan encrypted database belum tersedia.
