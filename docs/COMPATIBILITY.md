# Kompatibilitas

Target/compile API 36 (Android 16), min API 28. Toolchain: AGP 8.11.1, Gradle 8.13, Kotlin/Compose compiler 2.1.20, JDK 17 di CI. Compose BOM 2025.04.01. FGS overlay menggunakan tipe specialUse untuk kontrol yang dinyalakan pengguna, bukan pekerjaan AI tanpa batas.

Belum diuji pada perangkat Android 14/15/16, Samsung One UI atau emulator. Android dapat mencabut izin, membatasi FGS/baterai/overlay, menunda WorkManager, atau menyembunyikan overlay pada layar sensitif. Semua harus diuji manual. Izin notifikasi Android 13+ opsional namun disarankan agar pengingat terlihat.

| Tujuan | Jalur saat ini | Status interaksi nyata |
|---|---|---|
| ChatGPT | launcher / share | NOT_TESTED |
| Claude | launcher / share | NOT_TESTED |
| Google Flow | browser resmi | NOT_TESTED |
| TikTok | launcher / sharesheet media | NOT_TESTED |
| Instagram | launcher / sharesheet media | NOT_TESTED |
| Package tambahan | peluncuran manual, tunduk package visibility | NOT_TESTED |

Accessibility pihak ketiga masih BLOCKED. Tidak ada selector target yang diklaim kompatibel. App package regional dapat berbeda. Package visibility dapat membatasi deteksi package tambahan; sharesheet adalah fallback. Situs Flow membutuhkan browser dan login manual. Tidak ada jaminan field private, native download, akun aktif atau posting dapat diakses.
