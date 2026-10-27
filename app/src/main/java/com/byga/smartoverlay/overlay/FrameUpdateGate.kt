package com.byga.smartoverlay.overlay

/** Main-thread gate: hundreds of motion events produce at most one window update per frame. */
internal class FrameUpdateGate(
    private val post: (Runnable) -> Unit,
    private val remove: (Runnable) -> Unit,
    private val apply: () -> Unit
) {
    private var pending = false
    private val callback = Runnable { pending = false; apply() }
    fun request() { if (!pending) { pending = true; post(callback) } }
    fun flush() { if (pending) { remove(callback); pending = false; apply() } }
    fun cancel() { if (pending) remove(callback); pending = false }
}
