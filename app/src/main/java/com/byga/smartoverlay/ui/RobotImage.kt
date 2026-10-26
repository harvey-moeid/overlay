package com.byga.smartoverlay.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.byga.smartoverlay.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Never decode the 1254px source on Main for a 28/48/124dp icon. Cache sampled bitmaps. */
object RobotBitmaps {
    private val cache = object : LruCache<Int, Bitmap>(2 * 1024 * 1024) {
        override fun sizeOf(key:Int,value:Bitmap)=value.allocationByteCount
    }
    suspend fun load(context:Context,pixels:Int):Bitmap = withContext(Dispatchers.IO) {
        val sample = sampleFor(pixels)
        cache.get(sample) ?: BitmapFactory.decodeResource(context.resources,R.drawable.byga_robot,
            BitmapFactory.Options().apply { inSampleSize=sample;inScaled=false }).also { cache.put(sample,it) }
    }
    internal fun sampleFor(pixels:Int):Int {
        var sample=1
        while(1254/(sample*2)>=pixels.coerceAtLeast(1))sample*=2
        return sample
    }
}

@Composable fun RobotImage(size:Dp,description:String,modifier:Modifier=Modifier) {
    val context=LocalContext.current.applicationContext
    val pixels=with(LocalDensity.current){size.roundToPx()}
    val bitmap by produceState<Bitmap?>(null,pixels){value=RobotBitmaps.load(context,pixels)}
    Box(modifier.semantics { contentDescription=description }) {
        bitmap?.let { Image(it.asImageBitmap(),null,Modifier.matchParentSize()) }
    }
}
