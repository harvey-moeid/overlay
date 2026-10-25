# Privasi dan izin

Bubble hanya dimulai dari aplikasi dengan izin overlay. Notification Stop, tahan bubble, atau Stop Darurat menutupnya. Accessibility bersifat opsional: pengungkapan sebelum Settings, tidak ada event text logging dan PoC hanya dapat mengisi kolom uji BYGA. Nonaktifkan di pengaturan Android untuk mencabut akses sistem.

API key dienkripsi AES-GCM di Android Keystore; setiap provider punya authenticated additional data. Aplikasi tidak dibackup. Tidak ada biometrik wajib. Perangkat root/terkompromi masih bisa mengekstrak informasi saat runtime. Jangan masukkan password atau OTP ke prompt.

Health test provider mengirim prompt pendek ke endpoint HTTPS yang pengguna simpan, menggunakan key miliknya dan mungkin berbiaya. Jangan gunakan endpoint yang tidak dipercaya. Tidak ada key untuk build/CI. Fake provider bekerja lokal. Tidak ada screenshot yang diambil atau dikirim: MediaProjection/Vision belum diimplementasi. Jangan mengklaim izin atau capture berhasil.

Sharesheet mengirim media/prompt yang dipilih ke aplikasi pilihan pengguna. Pastikan akun tujuan sendiri sebelum posting; handoff tidak berarti publikasi. Riwayat/prompt/diagnostik dapat dihapus melalui Settings; provider dapat dihapus beserta key. Hapus data aplikasi untuk menghapus semua profil/workflow/preferensi lokal.

Tidak ada bypass FLAG_SECURE, lock screen, password, OTP, autentikasi atau DRM. Tidak ada clone account, auto-login, silent post, tracking atau backend pribadi. Audit kebijakan Accessibility wajib sebelum rencana distribusi Play Store.

V2: Agent OFF saat instalasi. Scope sementara hanya untuk safe field BYGA, dicabut saat stop/disposal. Tujuan/koreksi pengguna disimpan sebagai task history; memori jangka panjang hanya melalui consent manual. Keduanya dapat dihapus pada tab agent masing-masing. Planner provider hanya menerima goal/koreksi pada tap eksplisit; tidak menerima screenshot/UI tree/memori otomatis. Lihat MEMORY.md dan SCOPE_GRANTS.md.
