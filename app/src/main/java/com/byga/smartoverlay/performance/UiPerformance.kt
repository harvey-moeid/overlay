package com.byga.smartoverlay.performance

import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import androidx.annotation.Keep

@Keep data class FrameSnapshot(val frames:Long,val over32ms:Long,val frozen:Long,val p95ms:Int,val maxMs:Long)

/** Bounded, content-free histogram. No per-frame database writes, timers or recompositions. */
@Keep class FrameAccumulator {
    private val histogram=LongArray(1001)
    private var frames=0L;private var slow=0L;private var frozen=0L;private var max=0L
    @Synchronized fun add(nanos:Long) {
        if(nanos<=0)return
        val ms=(nanos/1_000_000L).coerceAtLeast(1)
        frames++;histogram[minOf(1000L,ms).toInt()]++
        if(nanos>32_000_000L)slow++
        if(nanos>700_000_000L)frozen++
        max=maxOf(max,ms)
    }
    @Synchronized fun reset() { histogram.fill(0);frames=0;slow=0;frozen=0;max=0 }
    @Synchronized fun snapshot():FrameSnapshot {
        val target=(frames*95+99)/100;var sum=0L;var p95=0
        if(frames>0)for(i in histogram.indices){sum+=histogram[i];if(sum>=target){p95=i;break}}
        return FrameSnapshot(frames,slow,frozen,p95,max)
    }
}
@Keep object UiPerformance { val frames=FrameAccumulator() }

class WindowFrameMonitor(private val window:Window) {
    private var thread:HandlerThread?=null
    private var attached=false
    private val listener=Window.OnFrameMetricsAvailableListener { _,metrics,_ ->
        UiPerformance.frames.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION))
    }
    fun start() {
        if(attached)return
        val worker=thread ?: HandlerThread("byga-frame-metrics",android.os.Process.THREAD_PRIORITY_BACKGROUND).apply{start();thread=this}
        attached=runCatching{window.addOnFrameMetricsAvailableListener(listener,Handler(worker.looper));true}.getOrDefault(false)
    }
    fun stop() { if(attached){window.removeOnFrameMetricsAvailableListener(listener);attached=false} }
    fun close() { stop();thread?.quitSafely();thread=null }
}
