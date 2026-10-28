# AI Agent Assistance — 0.7.0

Buka **Lainnya → Agent Lab / Agent Experimental → AI Agent Assistance V2**. Fitur OFF secara default. Aktifkan, pilih mapping dan provider existing, baca preview metadata, tulis pertanyaan, lalu berikan consent outbound per request.

Implementasi menggunakan `AiRepository`, batas request harian bersama, HTTPS dan API key Android Keystore existing. Satu provider/request, tanpa fallback atau retry otomatis. Stop darurat, Stop bantuan, keluar halaman, atau mematikan fitur membatalkan job. Provider mungkin sudah menerima request sebelum cancellation.

AI membantu menjelaskan kelas/struktur kontrol, atribut yang berubah, kemungkinan kegagalan dan urutan pemulihan manual. Inputnya hanya brief mapping yang dipilih serta pertanyaan pengguna. Isi field tidak dipindai; screenshot tidak dibuat atau dikirim. Preview menunjukkan metadata yang benar-benar dikirim. Resource ID/kelas/package merupakan metadata aplikasi, label kontrol disaring kembali. Tinjau agar tidak menyertakan data sensitif dalam pertanyaan.

Confidence ditentukan lokal: FOUND 1.0; NEEDS_REVIEW 0.5; status lain 0.0. Brief mapping tersimpan selalu NEEDS_REVIEW karena belum merupakan observasi UI baru. Angka ini bukan jaminan keberhasilan, tidak dibuat/dinaikkan oleh model, dan tidak dapat mengizinkan aksi.

Respons provider ditampilkan sebagai teks saran tanpa parser tool call, koordinat, permit, atau akses runner. Prompt instruksi dari layar diperlakukan sebagai data tidak terpercaya. AI tidak dapat memperluas grant aplikasi, menyetujui approval sendiri, mengganti target secara diam-diam, atau mengulang send/upload/generate. Provider gagal menghasilkan pesan kegagalan dengan fallback pemeriksaan/pemetaan ulang manual.

Screen Capture/Vision existing tetap terpisah: consent MediaProjection sistem per sesi, allowlist capture existing, preview lokal dan consent kedua sebelum mengirim gambar ke provider. Mengaktifkan assistance tidak mengaktifkan capture ataupun tindakan. Vision tidak dihubungkan ke klik otomatis.

Agent Lab safe-field existing, Skills/Memory dan scope opt-in tetap dipertahankan. Bantuan V2 ini bukan autonomous cross-app agent; replanning AI yang langsung menjalankan tindakan pihak ketiga tetap belum tersedia. Provider/account nyata dan capture pada Samsung: **NEEDS_REAL_DEVICE**.
