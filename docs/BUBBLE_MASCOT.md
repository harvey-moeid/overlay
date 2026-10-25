# Robot bubble BYGA

Versi 0.3.0 mengganti lingkaran huruf B dengan maskot robot biru terminal, terinspirasi gambar referensi pengguna. Aset dibuat khusus menggunakan image generation; PNG RGBA berlatar transparan di app/src/main/res/drawable-nodpi/byga_robot.png. Bukan screenshot galeri yang ditempel pada lingkaran/latarnya.

Window default 72dp, dapat disetel 56–96dp pada Settings → Robot bubble BYGA dengan preview. Window tetap hanya sebesar bubble (bukan overlay sentuh fullscreen). Ukuran aktif mengikuti preference DataStore. Drag, edge docking, tap panel prompt, long-press Stop dan notification Stop dipertahankan. Entry/dock memakai animasi terbatas, tanpa idle loop terus-menerus; reduce-motion tetap tersedia. Klik mengaktifkan performClick untuk aksesibilitas.

Panel saat dibuka boleh fokus/keyboard; collapsed tetap FLAG_NOT_FOCUSABLE. Rotation menutup panel dan meng-clamp posisi. Stop membuang window dan membatalkan animator serta agent aktif. Gambar maskot tidak menambah kemampuan kontrol aplikasi atau izin baru.

NEEDS_REAL_DEVICE: skala pada kepadatan layar Samsung, ketajaman 56/72/96dp, transparent edges, drag vs long-press, keyboard, rotasi, revoke permission dan Stop. Screenshot referensi bukan bukti perilaku implementasi. Tidak ada karakter galeri lain yang diklaim telah dibuat.
