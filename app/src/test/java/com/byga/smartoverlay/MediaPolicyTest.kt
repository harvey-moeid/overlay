package com.byga.smartoverlay
import com.byga.smartoverlay.media.MediaPolicy
import org.junit.Assert.*
import org.junit.Test
class MediaPolicyTest {
    @Test fun sizeBoundaries() { assertFalse(MediaPolicy.acceptsSize(0)); assertTrue(MediaPolicy.acceptsSize(1)); assertTrue(MediaPolicy.acceptsSize(MediaPolicy.MAX_BYTES)); assertFalse(MediaPolicy.acceptsSize(MediaPolicy.MAX_BYTES+1)) }
    @Test fun htmlNeverAnImage() { assertFalse(MediaPolicy.matches("image/png","<html>login</html>".toByteArray())) }
    @Test fun pngSignature() { assertTrue(MediaPolicy.matches("image/png",listOf(137,80,78,71,13,10,26,10).map{it.toByte()}.toByteArray())) }
    @Test fun unsupportedMimeRejected() { assertFalse(MediaPolicy.matches("application/zip",byteArrayOf(1,2,3))) }
    @Test fun truncatedHeaderRejected() { assertFalse(MediaPolicy.matches("video/mp4",byteArrayOf(1,2,3))) }
}
