# Experimental Agent v2 — implementasi bertahap

PRD v1 dipertahankan di PRD.md, PRD v2 asli di PRD_v2.md. Ini bukan klaim M0–M4 selesai.

## IMPLEMENTED

- Tab Agent Experimental, default flag DataStore `experimentalAgentEnabled=false`, thread tujuan/koreksi, draft lokal dan planner provider teks opsional lewat HTTPS. Setiap tap planner adalah consent mengirim goal/koreksi yang diketik, maksimal satu request (timeout 30s, tanpa retry). Tidak mengirim UI tree/memory/screenshot. Hasil model hanya JSON proposal.
- `PlanCodec`: strict keys/size/count/typed tool schema; tidak mengeksekusi output AI saat parsing. Import selalu DRAFT. Paket tidak cocok tidak mendapat grant.
- `AgentCore`: immutable session-bound scope, monotonic clock, tool/package allowlist, expiry, step/recovery cap, single UI run mutex, reobserve, verify actual field value, pause/resume/stop/revoke. ACK aksi bukan keberhasilan tugas.
- `AgentDevice` Android: Accessibility hanya kolom native EditText `byga_safe_input` milik BYGA, foreground package, keyguard, overlay dan service readiness. Dua tool nyata: set_text dan verify_state. Semua tool registry lainnya ditolak.
- Timeline kode state/tool/reason dengan timestamp WIB; kode audit tidak memuat teks layar, key atau screenshot. History tujuan/pesan buatan pengguna disimpan lokal dan dapat dihapus. Screen value hanya working memory sementara.
- Room v2 menambah agent_tasks, agent_memory, agent_skills, agent_compatibility tanpa menghapus records v1; migration 1→2, schema export.
- Memory manual opt-in, keyword search, edit/delete/forget-all/export. Skill JSON versioned, test pada kolom nyata sebelum REVIEW, human approval sebelum ENABLED, disable/delete/export/import DRAFT. Compatibility Lab menyimpan OS/package/version/operation/outcome yang dicatat manual.
- Stop darurat Activity dan Stop overlay/notification mencabut sesi agent. Keluar tab/Activity membatalkan coroutine dan grant; tidak ada restore otomatis.

## EXPERIMENTAL / NOT TESTED

Observe → policy → action → verify Android harness belum dieksekusi pada HP. Planner API nyata belum dipanggil di cloud (tidak ada credential berbayar dipakai). Compiling APK bukan bukti UI agent berhasil. Skill test memerlukan run COMPLETED terverifikasi, bukan tombol 'anggap lulus'.

## BLOCKED / NOT_IMPLEMENTED

M0 kontrol satu aplikasi pihak ketiga belum memiliki bukti fisik; tidak ada enable universal app automation. Screenshot/MediaProjection/Vision, coordinate gestures, publish/send/delete/purchase tetap tidak tersedia. Dynamic AI replan, cost accounting/cap uang, automatic skill learning, event hub, provider role routing dan account-aware multi-app agent belum tersedia. Recovery saat ini reobserve terbatas tanpa mengulangi aksi; bukan replanner AI.

Task scope tidak otomatis diwariskan ke jadwal/skill/thread. Scheduler lama tetap pengingat WAITING_FOR_USER. Tidak ada Android service background agent baru. Menonaktifkan flag tidak mematikan izin Accessibility sistem; pengguna mencabutnya melalui Settings resmi.
