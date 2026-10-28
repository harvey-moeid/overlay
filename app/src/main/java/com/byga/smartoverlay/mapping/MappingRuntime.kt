package com.byga.smartoverlay.mapping

import android.os.SystemClock
import com.byga.smartoverlay.accessibility.SafeAccessibilityService
import com.byga.smartoverlay.overlay.OverlayRuntime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MappingConfirmation(val mapping:ButtonMapping,val operation:String="CLICK",val value:String="") { val answer=CompletableDeferred<MappingClickPermit?>() }
object MappingRuntime {
    var onApprovalStatus:((Boolean,String)->Unit)?=null
    @Volatile var paused=false
    private val pending=MutableStateFlow<MappingConfirmation?>(null)
    val confirmation=pending.asStateFlow()
    fun cancel() { pending.value?.answer?.complete(null);pending.value=null }
    private suspend fun awaitUnpaused(){while(paused){currentCoroutineContext().ensureActive();delay(100)}}
    suspend fun waitFor(context:android.content.Context,mapping:ButtonMapping,condition:String,timeoutMs:Long,retries:Int):String = withTimeout(timeoutMs) {
        require(condition in setOf("FOUND","NOT_FOUND") && retries in 0..2)
        awaitMappingScope(mapping.scope,minOf(15000,timeoutMs),{awaitUnpaused()}) {
            checkNotNull(SafeAccessibilityService.current){"Accessibility dicabut"}.mapping.snapshot()
        }
        repeat(retries+1) {
            val result=withTimeoutOrNull((timeoutMs/(retries+1)).coerceAtLeast(1)) {
                while(true) {
                    awaitUnpaused();requireCurrentMapping(context,mapping)
                    val snapshot=checkNotNull(SafeAccessibilityService.current){"Accessibility dicabut"}.mapping.snapshot()
                    val resolution=MappingPolicy.inspect(mapping,snapshot,SystemClock.elapsedRealtime())
                    if(resolution.status in setOf(MappingStatus.BLOCKED,MappingStatus.AMBIGUOUS,MappingStatus.NEEDS_REVIEW))error("${resolution.status}: ${resolution.reason}")
                    if(resolution.status.name==condition)return@withTimeoutOrNull condition
                    delay(1000)
                }
                @Suppress("UNREACHABLE_CODE") ""
            }
            if(result!=null)return@withTimeout result
        }
        error("Timeout kondisi $condition; hanya observasi diulang, tidak ada aksi dikirim")
    }
    suspend fun execute(context:android.content.Context,mapping:ButtonMapping,operation:String="CLICK",value:String=""):String {
        check(mapping.enabled && mapping.validate()){"Mapping belum valid atau dinonaktifkan"}
        val service=SafeAccessibilityService.current ?: error("Aktifkan Accessibility untuk Smart Mapping")
        val ready=awaitMappingScope(mapping.scope,beforeObserve={awaitUnpaused()}){service.mapping.snapshot()}
        check(if(mapping.kind==MappingKind.SEMANTIC)MappingPolicy.resolve(mapping,ready,SystemClock.elapsedRealtime())!=null else MappingPolicy.pointAllowed(mapping,ready,SystemClock.elapsedRealtime())){"Target hilang/kembar atau layout berubah. Petakan ulang."}
        var permit:MappingClickPermit?=null
        if(mapping.askEveryTime || mapping.kind==MappingKind.POINT || mapping.scope.screen!="mapping-test" || operation!="CLICK") {
            check(OverlayRuntime.running.value){"Aktifkan bubble untuk menjawab konfirmasi mapping"}
            check(pending.value==null){"Konfirmasi mapping lain masih aktif"}
            val request=MappingConfirmation(mapping,operation,value);pending.value=request
            onApprovalStatus?.invoke(true,"Setujui aksi $operation melalui bubble setelah memeriksa target/akun")
            try { permit=withTimeout(180000){request.answer.await()};check(permit!=null){"Klik dibatalkan pengguna"} }
            finally { if(pending.value===request)pending.value=null;onApprovalStatus?.invoke(false,"Mengamati ulang target setelah review") }
            delay(400) // Let the review overlay leave the screen before re-observing.
        }
        currentCoroutineContext().ensureActive()
        while(true) { awaitUnpaused();requireCurrentMapping(context,mapping);if(!paused)break }
        return service.mapping.perform(mapping,permit ?: MappingClickPermit(mapping.fingerprint(),SystemClock.elapsedRealtime()),operation,value)
    }
}
suspend fun requireCurrentMapping(context:android.content.Context,mapping:ButtonMapping) {
    val current=com.byga.smartoverlay.data.BygaDatabase.get(context).records().get("button_mapping",mapping.id)
        ?.let{ButtonMapping.decode(it.body)}
    check(current!=null && current.enabled && current.fingerprint()==mapping.fingerprint()){"Mapping diubah, dihapus, atau dinonaktifkan. Klik dibatalkan."}
}

/** Shared, bounded startup observation for launch → wait/action. No dispatch during recovery. */
internal suspend fun awaitMappingScope(expected:MappingScope,timeoutMillis:Long=15000,beforeObserve:suspend()->Unit={},observe:suspend()->MappingSnapshot):MappingSnapshot {
    var reason="Buka layar tujuan yang dipetakan"
    val ready=withTimeoutOrNull(timeoutMillis){
        while(true){
            beforeObserve()
            val snapshot=try{observe()}catch(e:CancellationException){throw e}catch(e:Exception){reason=e.message ?: reason;null}
            if(snapshot?.scope==expected)return@withTimeoutOrNull snapshot
            delay(1000)
        }
        @Suppress("UNREACHABLE_CODE") null
    }
    return checkNotNull(ready){"Layar target belum siap: $reason"}
}
