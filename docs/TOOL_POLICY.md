# Tool policy v2

AI/website/UI text adalah data tidak tepercaya. AI tidak memiliki jalur raw Accessibility, shell, filesystem, cookies atau scope mutation. Proposal JSON tidak membawa grant/approval/permission field: field ekstra ditolak.

Registry mengenali tool ID PRD untuk menampilkan kontrak. Hanya `set_text` dan `verify_state` tersedia pada harness BYGA. Ketersediaan registry tidak boleh disamakan dengan implementasi; tool lain menghasilkan DENY. Purchase, publish, send dan delete tidak diimplementasikan. Tidak ada efek eksternal final yang dapat dijalankan agent versi ini.

Keputusan: ALLOW hanya bila flag aktif, grant sesi benar dan belum expired, langkah di bawah cap, schema/selector valid, package+tool di scope, window tidak sensitif, perangkat unlocked, service/overlay sehat dan observation berumur <=5 detik. Paket berubah/observation stale memberi NEED_REOBSERVE satu kali; masalah izin/grant memberi NEED_APPROVAL/WAITING_FOR_USER; lainnya DENY/BLOCKED. Hanya ALLOW mencapai adapter. Adapter menerapkan ulang allowlist package/selector.

Verifier membaca nilai field sebenarnya: execute()==true tidak cukup. Kegagalan aksi tidak diulang. Ketidakcocokan hasil hanya reobserve sesuai batas 0–3 tanpa action replay. Tidak ada tap tebakan atau blind retry side effects. Audit hanya tool/state/alasan, tanpa nilai field.
