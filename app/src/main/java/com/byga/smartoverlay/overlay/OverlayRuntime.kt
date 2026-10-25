package com.byga.smartoverlay.overlay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
object OverlayRuntime {
    private val value=MutableStateFlow(false)
    val running=value.asStateFlow()
    internal fun setRunning(running: Boolean){value.value=running}
}
