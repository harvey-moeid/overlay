package com.byga.smartoverlay.performance

data class ScanSnapshot(val count:Long,val totalMs:Long,val maxMs:Long,val maxNodes:Int)
class ScanAccumulator {
    private var count=0L;private var total=0L;private var max=0L;private var nodes=0
    @Synchronized fun add(ms:Long,nodeCount:Int){count++;total+=ms.coerceAtLeast(0);max=maxOf(max,ms);nodes=maxOf(nodes,nodeCount)}
    @Synchronized fun snapshot()=ScanSnapshot(count,total,max,nodes)
}
object AutomationPerformance { val scans=ScanAccumulator() }
