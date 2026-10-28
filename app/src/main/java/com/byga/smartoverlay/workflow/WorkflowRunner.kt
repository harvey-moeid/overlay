package com.byga.smartoverlay.workflow

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

data class WorkflowSnapshot(val state:RunState=RunState.DRAFT,val index:Int=0,val total:Int=0,val note:String="Belum ada workflow aktif",val output:String="",val phase:ExecutionPhase=ExecutionPhase.OBSERVE,val approvalRequired:Boolean=false)
data class SavedCheckpoint(val hash:String,val next:Int,val uncertain:Boolean=false,val output:String="")
object CheckpointPolicy {
    fun mayResume(plan:Plan,checkpoint:SavedCheckpoint)=checkpoint.hash==plan.fingerprint() && !checkpoint.uncertain && checkpoint.next in 0..plan.steps.size && (checkpoint.next==0 || checkpoint.output.isNotBlank() || plan.steps.drop(checkpoint.next).none{it.value.contains("{{output}}") || it.argument.contains("{{output}}")})
}
interface WorkflowPort {
    suspend fun ready(step:Step):Boolean
    suspend fun resolve(step:Step):Boolean=true
    suspend fun act(step:Step,previous:String):String
    /** Existing adapters return only after their documented result checks. Native click acceptance is weaker. */
    suspend fun verify(step:Step,output:String):Evidence=Evidence.VERIFIED
    suspend fun checkpoint(plan:Plan,next:Int,uncertain:Boolean,output:String)
}
class WorkflowRunner(private val port:WorkflowPort) {
    private val _snapshot=MutableStateFlow(WorkflowSnapshot())
    val snapshot=_snapshot.asStateFlow()
    private var running=false
    private var paused=false
    private var stopped=false
    private var beforePause=WorkflowSnapshot()
    private var approval:CompletableDeferred<Boolean>?=null
    private var actionJob:Job?=null
    fun pause(){if(!running || paused)return;beforePause=_snapshot.value;paused=true;_snapshot.value=_snapshot.value.copy(state=RunState.PAUSED,note="Dijeda oleh pengguna")}
    fun resume(){if(running && paused){paused=false;_snapshot.value=beforePause}}
    fun refuse(reason:String){if(!running)_snapshot.value=WorkflowSnapshot(RunState.BLOCKED,note=reason)}
    fun awaitingApproval()=approval?.isActive==true
    fun reportBrowserStatus(state:RunState,note:String){if(running && !stopped && !paused && approval==null)_snapshot.value=_snapshot.value.copy(state=state,note=note)}
    fun reportExternalApproval(required:Boolean,note:String){if(running && !stopped && !paused && approval==null)_snapshot.value=_snapshot.value.copy(state=if(required)RunState.WAITING_FOR_USER else RunState.RUNNING,approvalRequired=required,note=note)}
    fun reportPhase(phase:ExecutionPhase){if(running && !stopped && !paused)_snapshot.value=_snapshot.value.copy(phase=phase)}
    fun approve(){if(!paused && !stopped)approval?.complete(true)}
    fun stop(){stopped=true;approval?.complete(false);actionJob?.cancel();_snapshot.value=_snapshot.value.copy(state=RunState.STOPPED,approvalRequired=false,note="Dihentikan; tidak ada langkah berikutnya")}
    private suspend fun awaitRunning():Boolean {
        currentCoroutineContext().ensureActive()
        while(paused && !stopped)delay(100)
        currentCoroutineContext().ensureActive()
        return !stopped
    }
    private suspend fun ask(note:String):Boolean {
        if(!awaitRunning())return false
        approval=CompletableDeferred()
        _snapshot.value=_snapshot.value.copy(state=RunState.WAITING_FOR_USER,note=note,approvalRequired=true)
        return try {
            val accepted=withTimeoutOrNull(300000){approval!!.await()}==true
            if(!accepted){if(!stopped)stop();false} else awaitRunning()
        } finally {approval=null;_snapshot.value=_snapshot.value.copy(approvalRequired=false)}
    }
    companion object {
        private val lock=Mutex()
        fun hasActiveRun()=lock.isLocked
        val supported=setOf(Action.COPY_TEXT,Action.DELAY,Action.LAUNCH_APP,Action.GENERATE_AI_TEXT,Action.REQUEST_USER_APPROVAL,Action.DOWNLOAD_URL,Action.BRANCH,Action.STOP)+MappingSteps.actions+WebWorkflowPolicy.actions
    }
    suspend fun run(plan:Plan,approvedHash:String?,checkpoint:SavedCheckpoint?=null) {
        if(!Guard.canRun(plan,approvedHash) || (checkpoint!=null && !CheckpointPolicy.mayResume(plan,checkpoint))){refuse("Review versi atau checkpoint tidak valid/hasil sebelumnya tidak pasti");return}
        if(!lock.tryLock()){refuse("Workflow lain masih aktif");return}
        running=true;stopped=false;paused=false
        var output=checkpoint?.output ?: ""
        var i=checkpoint?.next ?: 0
        try {
            while(i<plan.steps.size) {
                if(!awaitRunning())return
                val step=plan.steps[i]
                _snapshot.value=WorkflowSnapshot(RunState.RUNNING,i,plan.steps.size,step.action.displayName(),output)
                if(step.action !in supported){_snapshot.value=_snapshot.value.copy(state=RunState.BLOCKED,note="Aksi ${step.action} belum memiliki adapter teruji");return}
                val ready=withTimeout(minOf(step.timeoutMs,60000L)){port.ready(step)}
                if(!awaitRunning())return
                if(!ready){_snapshot.value=_snapshot.value.copy(state=RunState.WAITING_FOR_USER,note="Perangkat/foreground belum siap. Tinjau checkpoint sebelum melanjutkan.");return}
                reportPhase(ExecutionPhase.RESOLVE_TARGET)
                val resolved=withTimeout(minOf(step.timeoutMs,30000L)){port.resolve(step)}
                if(!awaitRunning())return
                if(!resolved){_snapshot.value=_snapshot.value.copy(state=RunState.BLOCKED,note="Target/versi mapping tidak dapat di-resolve");return}
                reportPhase(ExecutionPhase.VALIDATE)
                var next=i+1
                when(step.action) {
                    Action.REQUEST_USER_APPROVAL->if(!ask(step.value.ifBlank{"Tinjau hasil dan setujui langkah berikutnya"}))return
                    Action.BRANCH->if(!BranchPolicy.matches(step,output))next+=step.argument.toInt()
                    Action.DELAY->{var remaining=step.value.toLong();while(remaining>0){if(!awaitRunning())return;val chunk=minOf(100L,remaining);delay(chunk);remaining-=chunk}}
                    Action.STOP->break
                    else->{
                        // Durable intent before dispatch. No external action is retried, including AI requests.
                        reportPhase(ExecutionPhase.CHECKPOINT)
                        port.checkpoint(plan,i,true,output)
                        if(!awaitRunning())return
                        reportPhase(ExecutionPhase.ACT)
                        output=coroutineScope {
                            val work=async { withTimeout(minOf(step.timeoutMs,if(step.action in WebWorkflowPolicy.actions || step.action in MappingSteps.actions)600000L else 120000L)){port.act(step,output)} }
                            actionJob=work
                            try{work.await()}finally{actionJob=null}
                        }
                        if(!awaitRunning())return
                        reportPhase(ExecutionPhase.VERIFY)
                        when(withTimeout(minOf(step.timeoutMs,30000L)){port.verify(step,output)}) {
                            Evidence.UNCERTAIN->{_snapshot.value=_snapshot.value.copy(state=RunState.UNKNOWN,note="Bukti hasil belum cukup. Periksa target; aksi tidak diulang otomatis.");return}
                            Evidence.ACCEPTED->if(!ask("Perintah ${step.action.displayName()} diterima, hasil belum terverifikasi. Periksa layar aplikasi, akun, dan hasil. Setujui hanya jika hasil benar; Stop jika ragu."))return
                            Evidence.VERIFIED->Unit
                        }
                    }
                }
                if(!awaitRunning())return
                reportPhase(ExecutionPhase.CHECKPOINT)
                port.checkpoint(plan,next,false,output)
                i=next
            }
            if(!awaitRunning())return
            port.checkpoint(plan,plan.steps.size,false,output)
            if(!awaitRunning())return
            _snapshot.value=_snapshot.value.copy(state=RunState.SUCCEEDED,index=plan.steps.size,note="Semua langkah selesai dengan verifikasi adapter atau pemeriksaan pengguna",output=output)
        }catch(e:TimeoutCancellationException){
            if(!currentCoroutineContext().isActive){stop();throw e}
            if(!stopped)_snapshot.value=_snapshot.value.copy(state=RunState.BLOCKED,note="Batas waktu langkah tercapai. Periksa hasil sebelum memulai ulang; aksi tidak diulang otomatis.")
        }catch(e:CancellationException){if(!stopped){stop();throw e}}
        catch(e:Exception){if(!stopped)_snapshot.value=_snapshot.value.copy(state=RunState.BLOCKED,note=e.message ?: "Langkah gagal; cek hasil manual sebelum retry")}
        finally{running=false;approval=null;actionJob=null;lock.unlock()}
    }
}
