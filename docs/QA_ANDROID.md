# QA Android: batas bukti dan menjalankan tes

## Perintah

- Unit/UI JVM/build/lint: `./scripts/cloud-build.sh testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`.
- Hardware/emulator yang berfungsi: `ANDROID_SERIAL=<serial> ./scripts/cloud-build.sh connectedDebugAndroidTest`.
- Toolchain biasa: gunakan `./gradlew` dengan JDK 17 dan ANDROID_HOME SDK36. Robolectric UI fixture secara eksplisit memakai SDK35, native graphics dan Canvas; Android 16 perlu tes perangkat terpisah.

Enam device tests: Room reopen, Keystore roundtrip/delete, additive migration + memory delete/codec, transaction request-budget concurrency, preserve prompt/provider metadata saat clear logs, startup/navigation dashboard→Chat→Providers→Workflows. Semuanya hanya memakai data/key palsu dan tidak memanggil provider berbayar. UI test menyimpan PNG pada external files milik aplikasi uji.

## Upaya cloud task ini

Satu AVD AOSP Android 16/API36 x86_64 disiapkan di /workspace/toolchains dengan SDK Manager resmi, TLS/system CA dan proxy JVM. Tanpa /dev/kvm, emulator memakai TCG/software rendering, 720×1600, 2GB RAM. Boot lambat dan service readiness tidak stabil untuk runner. Gradle menemukan API-level/discovery failures, lalu installer langsung memberi NPE StorageManager dari service PackageInstaller sebelum aplikasi terinstal. Tidak ada metode device test yang dinyatakan lulus.

Tidak mengubah API level emulator, menonaktifkan assertions, memalsukan test count atau mem-bypass permission perangkat agar tes hijau. Test APK tetap tersedia untuk PC dengan emulator berakselerasi atau HP QA. Emulator task ini telah dihentikan; proses runtime tidak boleh diasumsikan bertahan pada snapshot cloud baru.

## UI JVM

`PremiumUiTest` memakai Activity MainActivity yang sama, Material3/Compose, app resources dan route handlers. PixelCopy tidak mempunyai frame-pump perangkat di Robolectric; preview digambar oleh native Canvas decor view. Assertions tetap memeriksa layout/navigasi/Stop; preview ini tidak memvalidasi overlay, keyboard OEM, MediaProjection, Accessibility atau provider nyata. Tidak menjadi pengganti release gate perangkat pada PRD.
