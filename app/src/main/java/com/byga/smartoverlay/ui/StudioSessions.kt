package com.byga.smartoverlay.ui

import java.util.concurrent.ConcurrentHashMap
object StudioSessions {
    private val cancellations=ConcurrentHashMap<String,()->Unit>()
    fun register(id: String, stop: () -> Unit) { cancellations[id]=stop }
    fun unregister(id: String) { cancellations.remove(id) }
    fun stopAll() { cancellations.values.toList().forEach { it() } }
}
