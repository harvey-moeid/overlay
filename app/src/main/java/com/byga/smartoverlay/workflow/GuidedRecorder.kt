package com.byga.smartoverlay.workflow

import android.content.Context
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.mapping.*
import com.byga.smartoverlay.ui.StudioSessions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Records explicit selections, never raw touches, field contents, or accessibility event text. */
object GuidedRecorder {
    private val lock=Mutex()
    private val active=MutableStateFlow<String?>(null)
    val recording=active.asStateFlow()
    init {StudioSessions.register("guided-recorder",::stop)}
    suspend fun start(context:Context):String=lock.withLock {
        check(!WorkflowRunner.hasActiveRun()){"Stop workflow dahulu"}
        val id=UUID.randomUUID().toString()
        val plan=Plan("Rekaman terpandu",listOf(Step(Action.REQUEST_USER_APPROVAL,"Periksa aplikasi, akun, dan urutan rekaman sebelum melanjutkan.")))
        BygaDatabase.get(context).records().save(Record("workflow_recording",id,WorkflowCodec.encode(plan)))
        active.value=id;id
    }
    fun stop(){active.value=null}
    suspend fun append(context:Context,mapping:ButtonMapping):Boolean=lock.withLock {
        val id=active.value ?: return@withLock false
        require(mapping.validate() && mapping.enabled)
        val dao=BygaDatabase.get(context).records()
        val draft=dao.get("workflow_recording",id) ?: return@withLock false
        val plan=WorkflowCodec.decode(draft.body)
        val wait=Step(Action.WAIT_FOR_STATE,mapping.id,mapping.scope.packageName,mapping.fingerprint(),"FOUND",30000)
        val action=if(mapping.kind==MappingKind.POINT)Step(Action.CLICK_MAPPING,mapping.id,mapping.scope.packageName,mapping.fingerprint()) else when(mapping.elementKind){
            ElementKind.INPUT->null // Input text is supplied explicitly later in the step editor.
            ElementKind.SCROLL->Step(Action.SCROLL_MAPPING,mapping.id,mapping.scope.packageName,mapping.fingerprint(),"FORWARD")
            else->Step(Action.CLICK_MAPPING,mapping.id,mapping.scope.packageName,mapping.fingerprint())
        }
        val additions=(if(plan.steps.lastOrNull()?.packageName!=mapping.scope.packageName && mapping.scope.packageName!=MappingPolicy.BYGA)listOf(Step(Action.LAUNCH_APP,packageName=mapping.scope.packageName)) else emptyList())+(if(mapping.kind==MappingKind.SEMANTIC)listOf(wait) else emptyList())+listOfNotNull(action)
        check(plan.steps.size+additions.size<=50){"Rekaman maksimal 50 langkah; selesaikan draft ini dahulu"}
        // Stop can arrive while Room is suspending. It must prevent any subsequent append.
        if(active.value!=id)return@withLock false
        dao.save(draft.copy(body=WorkflowCodec.encode(plan.copy(steps=plan.steps+additions)),updatedAt=System.currentTimeMillis()))
        true
    }
}
