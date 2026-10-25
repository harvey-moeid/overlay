# Robot bubble BYGA

Versi 0.3.0 mengganti lingkaran huruf B dengan maskot robot biru terminal, terinspirasi gambar referensi pengguna. Aset dibuat khusus menggunakan image generation; PNG RGBA berlatar transparan di app/src/main/res/drawable-nodpi/byga_robot.png. Bukan screenshot galeri yang ditempel pada lingkaran/latarnya.

Window default 72dp, dapat disetel 56–96dp pada Settings → Robot bubble BYGA dengan preview. Window tetap hanya sebesar bubble (bukan overlay sentuh fullscreen). Ukuran aktif mengikuti preference DataStore. Drag, edge docking, tap panel prompt, long-press Stop dan notification Stop dipertahankan. Entry/dock memakai animasi terbatas, tanpa idle loop terus-menerus; reduce-motion tetap tersedia. Klik mengaktifkan performClick untuk aksesibilitas.

Panel saat dibuka boleh fokus/keyboard; collapsed tetap FLAG_NOT_FOCUSABLE. Rotation menutup panel dan meng-clamp posisi. Stop membuang window dan membatalkan animator serta agent aktif. Gambar maskot tidak menambah kemampuan kontrol aplikasi atau izin baru.

NEEDS_REAL_DEVICE: skala pada kepadatan layar Samsung, ketajaman 56/72/96dp, transparent edges, drag vs long-press, keyboard, rotasi, revoke permission dan Stop. Screenshot referensi bukan bukti perilaku implementasi. Tidak ada karakter galeri lain yang diklaim telah dibuat.

## Studio 0.4
Panel kini scrollable dengan header Quick Studio dan kontrol Stop. `Isi kolom fokus` memerlukan review teks/target/akun; permit 10 detik single-use, active package whitelist, unlocked context, non-password editable focus, rate limit, ACTION_SET_TEXT availability dan post-value verification. Tidak menekan Send/Publish atau melakukan gesture koordinat. UNKNOWN/NOT_FOUND/BLOCKED adalah hasil non-success, bukan izin blind retry. Stop membatalkan pending input callback. Kompatibilitas pihak ketiga belum diuji di HP.
