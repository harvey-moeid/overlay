package com.byga.smartoverlay.workflow

object MappingSteps {
    val actions=setOf(Action.CLICK_MAPPING,Action.INPUT_MAPPING,Action.SCROLL_MAPPING,Action.NAVIGATE_BACK,Action.WAIT_FOR_STATE)
    fun operation(step:Step)=when(step.action){Action.NAVIGATE_BACK->"BACK";Action.INPUT_MAPPING->"INPUT";Action.SCROLL_MAPPING->"SCROLL_${step.argument}";else->"CLICK"}
}
object WorkflowSafety {
    val effects=setOf(Action.CLICK_MAPPING,Action.INPUT_MAPPING,Action.SCROLL_MAPPING,Action.NAVIGATE_BACK,Action.GENERATE_AI_TEXT,Action.CHATGPT_SEND_PROMPT,Action.FLOW_CREATE_PROJECT,Action.FLOW_UPLOAD_IMAGE,Action.FLOW_GENERATE)
    fun needsRepeatReview(plan:Plan,checkpoint:SavedCheckpoint?)=checkpoint!=null && plan.steps.any{it.action in effects} && (checkpoint.uncertain || checkpoint.next>0)
}
enum class ExecutionPhase { OBSERVE, RESOLVE_TARGET, VALIDATE, ACT, VERIFY, CHECKPOINT }
enum class AutomationState { IDLE, RUNNING, WAITING_FOR_UI, WAITING_FOR_APPROVAL, PAUSED, SUCCESS, FAILED, UNCERTAIN, CANCELLED }
enum class Evidence { VERIFIED, ACCEPTED, UNCERTAIN }
fun WorkflowSnapshot.automationState()=when(state){
    RunState.DRAFT,RunState.READY->AutomationState.IDLE
    RunState.RUNNING->AutomationState.RUNNING
    RunState.PAUSED->AutomationState.PAUSED
    RunState.WAITING_FOR_USER->if(approvalRequired)AutomationState.WAITING_FOR_APPROVAL else AutomationState.WAITING_FOR_UI
    RunState.SUCCEEDED->AutomationState.SUCCESS
    RunState.STOPPED->AutomationState.CANCELLED
    RunState.UNKNOWN->AutomationState.UNCERTAIN
    RunState.BLOCKED->AutomationState.FAILED
}
object BranchPolicy {
    fun matches(step:Step,output:String)=when(step.selector){"EQUALS"->output==step.value;"NOT_EMPTY"->output.isNotBlank();else->error("Kondisi IF tidak dikenal")}
}
object WorkflowPreflight {
    suspend fun check(plan:Plan,lookup:suspend(String)->String?):List<String> {
        val issues=plan.validate().toMutableList()
        plan.steps.forEachIndexed { i,step ->
            if(step.action in MappingSteps.actions) {
                val mapping=lookup(step.value)?.let{runCatching{com.byga.smartoverlay.mapping.ButtonMapping.decode(it)}.getOrNull()}
                if(mapping==null || !mapping.enabled || mapping.scope.packageName!=step.packageName || mapping.fingerprint()!=step.selector)issues.add("Langkah ${i+1}: mapping hilang, izin dicabut, atau versi berubah")
                else {
                    val kind=mapping.elementKind
                    if(mapping.kind==com.byga.smartoverlay.mapping.MappingKind.POINT && step.action!=Action.CLICK_MAPPING)issues.add("Langkah ${i+1}: posisi manual hanya mendukung klik dengan konfirmasi")
                    if(step.action==Action.INPUT_MAPPING && kind!=com.byga.smartoverlay.mapping.ElementKind.INPUT || step.action==Action.SCROLL_MAPPING && kind!=com.byga.smartoverlay.mapping.ElementKind.SCROLL)issues.add("Langkah ${i+1}: jenis elemen tidak mendukung aksi")
                }
            }
        }
        return issues
    }
}

/** Command owner and acknowledgment guard. A new Activity must not repeat a command. */
class DispatchGuard {
    private var owner:String?=null
    private var claimed=false
    @Synchronized fun attach(instance:String):Boolean {
        if(owner!=null && owner!=instance)return false
        owner=instance;return true
    }
    @Synchronized fun claim():Boolean { if(claimed)return false;claimed=true;return true }
}
