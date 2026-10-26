package com.byga.smartoverlay.workflow

import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal enum class BrowserReadiness { READY, PAUSED, BACKGROUND, LOCKED, LOADING, LOGIN, FAILED }

/** Loading/login waits are bounded separately from a long-running video generation. */
internal suspend fun awaitBrowserReady(
    state: () -> BrowserReadiness,
    onWaiting: (String) -> Unit,
    timeoutMillis: Long = 60_000L,
    clock: () -> Long = { System.nanoTime()/1_000_000L }
) {
    var remaining=timeoutMillis
    var last=clock();var paused=false
    while (true) {
            currentCoroutineContext().ensureActive()
            val now=clock()
            if(!paused)remaining-=(now-last).coerceAtLeast(0)
            last=now
            val current = state()
            paused=current==BrowserReadiness.PAUSED
            check(current != BrowserReadiness.FAILED) { "Halaman gagal dimuat. Periksa koneksi atau akses akun, lalu mulai ulang alur setelah memeriksa hasil." }
            if (current == BrowserReadiness.READY) return
            check(current==BrowserReadiness.PAUSED || remaining>0) { "Batas waktu menunggu halaman/login tercapai (60 detik). Periksa halaman dan hasil langkah sebelum memulai ulang." }
            onWaiting(when (current) {
                BrowserReadiness.PAUSED -> "Dijeda. Tekan Lanjut untuk meneruskan."
                BrowserReadiness.LOCKED -> "Buka kunci HP untuk melanjutkan."
                BrowserReadiness.LOADING -> "Menunggu halaman resmi dimuat (maksimal 60 detik)…"
                BrowserReadiness.BACKGROUND -> "Buka kembali browser workflow BYGA untuk melanjutkan."
                else -> "Selesaikan login atau buka halaman tujuan secara manual (maksimal 60 detik)."
            })
            val interval=if(current==BrowserReadiness.PAUSED)400L else minOf(400L,remaining)
            delay(interval)
    }
}
