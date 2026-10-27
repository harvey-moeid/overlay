package com.byga.smartoverlay.workflow

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

data class WorkflowSnapshot(val state: RunState=RunState.DRAFT,val index: Int=0,val total: Int=0,val note: String="Belum ada workflow aktif",val output: String="")
data class SavedCheckpoint(val hash: String,val next: Int,val uncertain: Boolean=false,val output: String="")
object CheckpointPolicy {
    fun mayResume(plan: Plan, checkpoint: SavedCheckpoint) = checkpoint.hash==plan.fingerprint() && !checkpoint.uncertain && checkpoint.next in 0..plan.steps.size && (checkpoint.next==0 || checkpoint.output.isNotBlank() || plan.steps.drop(checkpoint.next).none{it.value.contains("{{output}}")})
}
interface WorkflowPort {
    suspend fun ready(step: Step): Boolean
    suspend fun act(step: Step, previous: String): String
    suspend fun checkpoint(plan: Plan, next: Int, uncertain: Boolean, output: String)
}
class WorkflowRunner(private val port: WorkflowPort) {
    private val _snapshot=MutableStateFlow(WorkflowSnapshot())
    val snapshot=_snapshot.asStateFlow()
    private var running=false
    private var paused=false
    private var stopped=false
    private var beforePause=WorkflowSnapshot()
    private var approval: CompletableDeferred<Boolean>?=null
    fun pause() { if(!running || paused)return; beforePause=_snapshot.value;paused=true; _snapshot.value=_snapshot.value.copy(state=RunState.PAUSED,note="Dijeda oleh pengguna") }
    fun resume() { if(running && paused) { paused=false;_snapshot.value=beforePause } }
    fun refuse(reason:String) { if(!running)_snapshot.value=WorkflowSnapshot(RunState.BLOCKED,note=reason) }
    fun awaitingApproval() = approval?.isActive == true
    fun reportBrowserStatus(state:RunState,note:String) {
        if(running && !stopped && !paused && approval==null) _snapshot.value=_snapshot.value.copy(state=state,note=note)
    }
    fun approve() { approval?.complete(true) }
    fun stop() { stopped=true; approval?.complete(false); _snapshot.value=_snapshot.value.copy(state=RunState.STOPPED,note="Dihentikan; tidak ada langkah berikutnya") }
    private suspend fun awaitRunning(): Boolean {
        currentCoroutineContext().ensureActive()
        while(paused && !stopped) delay(100)
        currentCoroutineContext().ensureActive()
        return !stopped
    }
    companion object { private val lock=Mutex(); fun hasActiveRun()=lock.isLocked; val supported=setOf(Action.CLICK_MAPPING,Action.COPY_TEXT,Action.DELAY,Action.LAUNCH_APP,Action.GENERATE_AI_TEXT,Action.REQUEST_USER_APPROVAL,Action.DOWNLOAD_URL,Action.STOP)+WebWorkflowPolicy.actions }
    suspend fun run(plan: Plan, approvedHash: String?, checkpoint: SavedCheckpoint?=null) {
        if(!Guard.canRun(plan,approvedHash) || (checkpoint!=null && !CheckpointPolicy.mayResume(plan,checkpoint))) { _snapshot.value=WorkflowSnapshot(RunState.BLOCKED,note="Review versi atau checkpoint tidak valid/hasil sebelumnya tidak pasti"); return }
        if(!lock.tryLock()) { _snapshot.value=WorkflowSnapshot(RunState.BLOCKED,note="Workflow lain masih aktif"); return }
        running=true; stopped=false; paused=false
        var output=checkpoint?.output ?: ""
        val start=checkpoint?.next ?: 0
        try {
            for(i in start until plan.steps.size) {
                if(!awaitRunning())return
                val step=plan.steps[i]
                _snapshot.value=WorkflowSnapshot(RunState.RUNNING,i,plan.steps.size,"${step.action}",output)
                if(step.action !in supported) { _snapshot.value=_snapshot.value.copy(state=RunState.BLOCKED,note="Aksi ${step.action} belum memiliki adapter teruji"); return }
                val ready=port.ready(step)
                if(!awaitRunning())return
                if(!ready) { _snapshot.value=_snapshot.value.copy(state=RunState.WAITING_FOR_USER,note="Perangkat/foreground belum siap. Tinjau checkpoint sebelum melanjutkan."); return }
                if(step.action==Action.REQUEST_USER_APPROVAL) {
                    approval=CompletableDeferred()
                    _snapshot.value=_snapshot.value.copy(state=RunState.WAITING_FOR_USER,note=step.value.ifBlank{"Tinjau hasil dan setujui langkah berikutnya"})
                    if(withTimeoutOrNull(300000){approval!!.await()}!=true || stopped) { _snapshot.value=_snapshot.value.copy(state=RunState.STOPPED,note="Persetujuan ditolak atau kedaluwarsa"); return }
                    approval=null
                } else if(step.action==Action.DELAY) {
                    var remaining=step.value.toLong()
                    while(remaining>0) { if(!awaitRunning())return; val chunk=minOf(100L,remaining); delay(chunk); remaining-=chunk }
                } else if(step.action==Action.STOP) { break }
                else {
                    // Persist uncertainty before external side effects. Process death cannot blindly replay this step.
                    port.checkpoint(plan,i,true,output)
                    if(!awaitRunning())return
                    output=withTimeout(if(step.action in WebWorkflowPolicy.actions || step.action==Action.CLICK_MAPPING)600000L else 120000L){port.act(step,output)}
                }
                if(stopped)return
                port.checkpoint(plan,i+1,false,output)
            }
            if(!awaitRunning())return
            port.checkpoint(plan,plan.steps.size,false,output)
            if(!awaitRunning())return
            _snapshot.value=_snapshot.value.copy(state=RunState.SUCCEEDED,index=plan.steps.size,note=if(plan.steps.any{it.action==Action.CLICK_MAPPING})"Semua perintah klik dikirim satu kali; periksa hasil di aplikasi tujuan" else "Semua langkah yang didukung selesai",output=output)
        } catch(e: TimeoutCancellationException) {
            // Only a step's own deadline is a recoverable failure; cancellation of the run must propagate.
            if(!currentCoroutineContext().isActive) { stop(); throw e }
            if(!stopped)_snapshot.value=_snapshot.value.copy(state=RunState.BLOCKED,note="Batas waktu langkah tercapai. Periksa hasil di aplikasi/situs sebelum memulai ulang; aksi tidak diulang otomatis.")
        }
        catch(e: CancellationException) { stop(); throw e }
        catch(e: Exception) { if(!stopped)_snapshot.value=_snapshot.value.copy(state=RunState.BLOCKED,note=e.message ?: "Langkah gagal; cek hasil manual sebelum retry") }
        finally { running=false; approval=null; lock.unlock() }
    }
}
