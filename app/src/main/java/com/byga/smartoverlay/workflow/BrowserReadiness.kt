package com.byga.smartoverlay.workflow

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

internal enum class BrowserReadiness { READY, PAUSED, BACKGROUND, LOCKED, LOADING, LOGIN, FAILED }

/** Loading/login waits are bounded separately from a long-running video generation. */
internal suspend fun awaitBrowserReady(
    state: () -> BrowserReadiness,
    onWaiting: (String) -> Unit,
    timeoutMillis: Long = 60_000L
) {
    val ready = withTimeoutOrNull(timeoutMillis) {
        while (true) {
            val current = state()
            check(current != BrowserReadiness.FAILED) { "Halaman gagal dimuat. Periksa koneksi atau akses akun, lalu mulai ulang alur setelah memeriksa hasil." }
            if (current == BrowserReadiness.READY) return@withTimeoutOrNull true
            onWaiting(when (current) {
                BrowserReadiness.PAUSED -> "Dijeda. Tekan Lanjut untuk meneruskan."
                BrowserReadiness.LOCKED -> "Buka kunci HP untuk melanjutkan."
                BrowserReadiness.LOADING -> "Menunggu halaman resmi dimuat (maksimal 60 detik)…"
                BrowserReadiness.BACKGROUND -> "Buka kembali browser workflow BYGA untuk melanjutkan."
                else -> "Selesaikan login atau buka halaman tujuan secara manual (maksimal 60 detik)."
            })
            delay(400)
        }
        @Suppress("UNREACHABLE_CODE") false
    }
    check(ready == true) { "Batas waktu menunggu halaman/login tercapai (60 detik). Periksa halaman dan hasil langkah sebelum memulai ulang." }
}
