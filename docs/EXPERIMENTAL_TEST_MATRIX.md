# Experimental Agent — bukti pengujian

P14–P29 adalah penerimaan fisik/fault injection; status awal semuanya NOT_TESTED. Unit test native policy membantu membuktikan kontrak tetapi tidak mengganti perilaku Android nyata.

| ID | Uji fisik | Status |
|---|---|---|
| P14 | Instal baru, flag OFF, tidak ada agent action | NOT_TESTED |
| P15 | Goal multi-step pada aplikasi pihak ketiga | BLOCKED — M0 belum dibuktikan; harness BYGA NEEDS_REAL_DEVICE |
| P16 | Expiry menolak tool berikutnya | NOT_TESTED pada HP; pure policy unit test tersedia |
| P17 | Proposal package lain | NOT_TESTED pada HP; policy unit test tersedia |
| P18 | Selector hilang, bounded recovery | NOT_TESTED pada HP |
| P19 | UI lambat tanpa spam retry | NOT_TESTED pada HP |
| P20 | Switching akun | BLOCKED — adapter account-aware belum tersedia |
| P21 | Screenshot ditolak/FLAG_SECURE | NOT_IMPLEMENTED, tidak ada capture agent |
| P22 | Consent sebelum upload screen | NOT_IMPLEMENTED, tidak ada screen upload |
| P23 | UI prompt injection | NOT_TESTED pada HP; schema/package/tool gates tetap native |
| P24 | Stop saat workflow | NOT_TESTED pada HP; unit test no-next-action tersedia |
| P25 | Accessibility dicabut / rotasi / lock | NOT_TESTED pada HP; grant dicabut saat disposal, native readiness checks |
| P26 | Skill test→review→approve→enable | NOT_TESTED pada HP; lifecycle unit tests tersedia |
| P27 | Memory search/edit/forget-all | NOT_TESTED pada HP; instrumentation test disediakan |
| P28 | Publikasi/send final confirmation | BLOCKED — tool tidak tersedia; tidak ada bypass confirmation |
| P29 | Provider timeout/request/cost cap | NOT_TESTED API nyata; 1 request manual tanpa retry; cost cap uang NOT_IMPLEMENTED |

## Prosedur harness nyata

1. Instal APK, grant overlay dan Accessibility setelah disclosure. Buka Agent Experimental. Flag awal harus OFF.
2. Aktifkan, isi tujuan dan koreksi. Buat draft lokal. Cek dua tool dan isi field, package BYGA, selector safe, expiry/step/recovery.
3. Setujui scope dan jalankan. EditText harus berubah nyata dan kedua hasil diperiksa sebelum COMPLETED. Tidak ada pesan dikirim ke aplikasi lain.
4. Uji maxSteps=1 (langkah kedua WAITING_FOR_USER), lock/revoke/rotasi/keluar tab/Stop. Tidak ada action berikutnya atau scope yang diwariskan.
5. Simpan skill DRAFT, muat & test dengan grant baru, cek hanya run berhasil menghasilkan REVIEW, approve lalu enable; disable dan tes ulang; impor harus DRAFT.
6. Simpan preferensi nonrahasia di Memory, cari, edit/hapus semuanya lalu cari lagi: tidak ada entri lama.
7. Catat versi OS/package/app/operasi di Compatibility Lab. Hasil manual tidak diberi label CI.
8. Provider opsional: tujuan yang diketik saja dikirim setelah tap tombol provider; tidak ada tree/memory/screenshot terkirim. Gunakan akun/key milik pengguna, jangan unggah key sebagai bukti.

Lihat changelog untuk perintah dan hasil build final; tidak ada emulator/HP yang terhubung di cloud ini.
