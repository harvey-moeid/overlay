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
import kotlin.math.roundToInt

/** Never decode the 1254px source on Main for a 28/48/124dp icon. Cache bitmaps fitted to their requested pixel size. */
object RobotBitmaps {
    private val cache = object : LruCache<Int, Bitmap>(2 * 1024 * 1024) {
        override fun sizeOf(key:Int,value:Bitmap)=value.allocationByteCount
    }
    suspend fun load(context:Context,pixels:Int):Bitmap = withContext(Dispatchers.IO) {
        val target=pixels.coerceAtLeast(1)
        cache.get(target) ?: run {
            val decoded=checkNotNull(BitmapFactory.decodeResource(context.resources,R.drawable.byga_robot,
                BitmapFactory.Options().apply { inSampleSize=sampleFor(target);inScaled=false }))
            // Sampling alone can leave a 627px texture for a 326px view. Fit once on IO.
            val scale=minOf(1f,target.toFloat()/maxOf(decoded.width,decoded.height))
            val width=(decoded.width*scale).roundToInt().coerceAtLeast(1)
            val height=(decoded.height*scale).roundToInt().coerceAtLeast(1)
            val fitted=if(width==decoded.width && height==decoded.height)decoded
                else Bitmap.createScaledBitmap(decoded,width,height,true)
            if(fitted!==decoded)decoded.recycle()
            cache.put(target,fitted)
            fitted
        }
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
