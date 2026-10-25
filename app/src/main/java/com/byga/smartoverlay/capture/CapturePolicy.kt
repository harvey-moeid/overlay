package com.byga.smartoverlay.capture

object CapturePolicy {
    val packages=setOf("com.byga.smartoverlay","com.openai.chatgpt","com.anthropic.claude","com.zhiliaoapp.musically","com.instagram.android")
    fun mayCapture(requested: String, active: String, locked: Boolean, sensitive: Boolean) = requested in packages && active==requested && !locked && !sensitive
}
