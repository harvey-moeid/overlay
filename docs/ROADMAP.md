# Gerbang berikutnya setelah Studio 0.4.0

Studio sudah menghubungkan Chat, workflow, capture/Vision berizin, draft sosial, konfirmasi akun dan multi-pengingat. Status rinci terhadap PRD tetap ada di IMPLEMENTATION_STATUS.md; daftar ini bukan klaim fitur yang belum ada sudah selesai.

1. Acceptance Android16/Samsung: install/upgrade, permission revoke, drag/panel/keyboard/font besar/rotation/Stop; uji input per field/app, capture/FLAG_SECURE, URI handoff, downloader/storage dan pengingat saat terkunci. Jalankan enam instrumentation tests pada HP atau emulator berakselerasi. Catat versi dan bukti; upaya emulator cloud belum memberi hasil lulus.
2. Inference QA memakai akun/model yang benar: text, cancellation, fallback/limit concurrency dan Vision masing-masing protocol. Uji quota/gated/rate limit tanpa menulis secrets pada laporan.
3. Release hardening setelah hasil perangkat: perbaikan yang terbukti diperlukan, localization lengkap, aksesibilitas UI, retention policy, release signing dan distribusi/upgrade yang stabil.
4. PRD lanjutan yang belum tersedia: branching/macro/selector-backed native downloads, event subscriptions, role routing/monetary cost cap dan dynamic multi-app agent replanning. Perlu desain, batas otorisasi dan bukti per aplikasi sebelum diaktifkan.
5. Full agent/skill learning lintas aplikasi dan account identity automation memerlukan pekerjaan tersendiri. Input manual Studio tidak memberi wewenang agent untuk click/send/publish/delete. Final posting tetap dilakukan pengguna.

PRD asli tetap baseline dan tidak diubah untuk menutupi gap. Tidak menggunakan angka persentase kesiapan sebagai pengganti acceptance test.
