package com.byga.smartoverlay.media

object MediaPolicy {
    const val MAX_BYTES=50L*1024*1024
    val extensions=mapOf("image/jpeg" to "jpg","image/png" to "png","image/webp" to "webp","video/mp4" to "mp4","video/webm" to "webm")
    fun acceptsSize(length: Long) = length in 1..MAX_BYTES
    fun matches(mime: String, header: ByteArray): Boolean {
        fun ascii(start: Int, value: String) = header.size >= start+value.length && String(header,start,value.length,Charsets.US_ASCII)==value
        return when(mime) {
            "image/jpeg" -> header.size>=3 && header[0]==0xff.toByte() && header[1]==0xd8.toByte() && header[2]==0xff.toByte()
            "image/png" -> header.take(8)==listOf(137,80,78,71,13,10,26,10).map{it.toByte()}
            "image/webp" -> ascii(0,"RIFF") && ascii(8,"WEBP")
            "video/mp4" -> ascii(4,"ftyp")
            "video/webm" -> header.take(4)==listOf(0x1a,0x45,0xdf,0xa3).map{it.toByte()}
            else -> false
        }
    }
}
