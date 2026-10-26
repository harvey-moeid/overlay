package com.byga.smartoverlay.mapping

import android.os.SystemClock
import com.byga.smartoverlay.accessibility.SafeAccessibilityService
import com.byga.smartoverlay.overlay.OverlayRuntime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MappingConfirmation(val mapping:ButtonMapping) { val answer=CompletableDeferred<MappingClickPermit?>() }
object MappingRuntime {
    @Volatile var paused=false
    private val pending=MutableStateFlow<MappingConfirmation?>(null)
    val confirmation=pending.asStateFlow()
    fun cancel() { pending.value?.answer?.complete(null);pending.value=null }
    private suspend fun awaitUnpaused(){while(paused){currentCoroutineContext().ensureActive();delay(100)}}
    suspend fun execute(context:android.content.Context,mapping:ButtonMapping):String {
        check(mapping.enabled && mapping.validate()){"Mapping belum valid atau dinonaktifkan"}
        val service=SafeAccessibilityService.current ?: error("Aktifkan Accessibility untuk Smart Mapping")
        // Allow a preceding LAUNCH_APP step to finish opening its target; never click a different app.
        var reason="Buka layar tujuan yang dipetakan"
        val ready=withTimeoutOrNull(15000) {
            while(true) {
                awaitUnpaused()
                val snapshot=runCatching{service.mapping.snapshot()}.onFailure{reason=it.message ?: reason}.getOrNull()
                if(snapshot?.scope==mapping.scope)return@withTimeoutOrNull snapshot
                delay(350)
            }
            @Suppress("UNREACHABLE_CODE") null
        }
        check(ready!=null){"Layar target belum siap: $reason"}
        check(if(mapping.kind==MappingKind.SEMANTIC)MappingPolicy.resolve(mapping,ready,SystemClock.elapsedRealtime())!=null else MappingPolicy.pointAllowed(mapping,ready,SystemClock.elapsedRealtime())){"Target hilang/kembar atau layout berubah. Petakan ulang."}
        var permit:MappingClickPermit?=null
        if(mapping.askEveryTime || mapping.kind==MappingKind.POINT) {
            check(OverlayRuntime.running.value){"Aktifkan bubble untuk menjawab konfirmasi mapping"}
            check(pending.value==null){"Konfirmasi mapping lain masih aktif"}
            val request=MappingConfirmation(mapping);pending.value=request
            try { permit=withTimeout(180000){request.answer.await()};check(permit!=null){"Klik dibatalkan pengguna"} }
            finally { if(pending.value===request)pending.value=null }
            delay(400) // Let the review overlay leave the screen before re-observing.
        }
        currentCoroutineContext().ensureActive()
        while(true) { awaitUnpaused();requireCurrentMapping(context,mapping);if(!paused)break }
        return service.mapping.click(mapping,permit ?: MappingClickPermit(mapping.fingerprint(),SystemClock.elapsedRealtime()))
    }
}
suspend fun requireCurrentMapping(context:android.content.Context,mapping:ButtonMapping) {
    val current=com.byga.smartoverlay.data.BygaDatabase.get(context).records().get("button_mapping",mapping.id)
        ?.let{ButtonMapping.decode(it.body)}
    check(current!=null && current.enabled && current.fingerprint()==mapping.fingerprint()){"Mapping diubah, dihapus, atau dinonaktifkan. Klik dibatalkan."}
}
