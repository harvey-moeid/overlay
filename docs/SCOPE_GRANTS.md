# Scope grant

Flag experimental default OFF. Setelah draft plan ditampilkan, pengguna harus mencentang review dan menekan Setujui scope & jalankan. Scope dibentuk lokal oleh controller, bukan JSON/model: random session UUID, random grant UUID, package/tool allowlist immutable, expiry monotonic maksimal 10 menit, 1–30 tool calls dan 0–3 recovery observations.

Grant hidup di memori controller, tidak diekspor, tidak dipersist, tidak dapat diteruskan ke thread lain. Model hanya mengusulkan argumen; UI/native Kotlin menentukan grant. Tidak ada token rahasia yang diberikan ke provider. Expired/stop/disable/keluar layar/akhir run mencabut grant. Resume mengamati ulang state sebelum langkah berikutnya. Stop tidak membatalkan aksi yang sudah selesai; tidak ada klaim rollback.

Cakupan yang tersedia saat ini hanya BYGA + safe field, tanpa account atau publikasi. Teks pengisian ditampilkan saat review. Model bisa menghasilkan proposal di luar cakupan, tetapi approve/policy/adapter akan menolaknya. Biaya uang AI belum dihitung; UI mengungkap bahwa planner dikirim manual, 1 request per tap tanpa retry otomatis. Tidak mengklaim cost cap yang belum tersedia.
