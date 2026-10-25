package com.byga.smartoverlay.accessibility

import java.util.concurrent.atomic.AtomicBoolean
// Constructed only by the visible bubble's human review action, never from AI/JSON.
class ManualInputPermit(val target:String,val value:String,private val reviewed:Boolean,private val approvedAt:Long,private val expectedField:String="focused") {
    private val used=AtomicBoolean(false)
    companion object { val packages=setOf("com.byga.smartoverlay","com.openai.chatgpt","com.anthropic.claude","com.zhiliaoapp.musically","com.instagram.android") }
    fun claim(active:String,locked:Boolean,sensitive:Boolean,now:Long,field:String="focused"):Boolean {
        if(field!=expectedField || !reviewed || target !in packages || active!=target || locked || sensitive || value.isBlank() || value.length>4000 || now-approvedAt !in 0..10000)return false
        return used.compareAndSet(false,true)
    }
}
