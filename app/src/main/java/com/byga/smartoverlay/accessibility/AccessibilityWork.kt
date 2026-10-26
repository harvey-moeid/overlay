package com.byga.smartoverlay.accessibility

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Serialize remote accessibility calls off the UI thread. Never queue concurrent scans. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
object AccessibilityWork {
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)
    suspend fun <T> run(block: suspend () -> T): T = withContext(dispatcher) { block() }
}

internal class ScanBudget(
    private val context: CoroutineContext,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val deadline: Long = clock() + 2000L
) {
    fun check() {
        context.ensureActive()
        check(clock() < deadline) { "Pemindaian terlalu lama. Sederhanakan layar lalu coba lagi." }
    }
}
