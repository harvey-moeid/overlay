package com.byga.smartoverlay.agent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

enum class AgentState { CREATED, PLANNING, WAITING_FOR_SCOPE, OBSERVING, EXECUTING, VERIFYING, RECOVERING, WAITING_FOR_USER, PAUSED, COMPLETED, BLOCKED, FAILED, CANCELLED }
enum class Decision { ALLOW, DENY, NEED_APPROVAL, NEED_REOBSERVE }
data class PolicyDecision(val decision: Decision, val reason: String)
data class ToolSpec(val id: String, val available: Boolean, val external: Boolean = false)
object ToolRegistry {
    val tools = listOf("open_app","read_ui","observe_screen","find_element","tap_node","swipe","scroll","set_text","copy_text","share_text","share_media","download_media","switch_account","verify_state","verify_account","save_skill","save_memory","prepare_post","publish_post","send_message","delete_data","purchase").associateWith { id ->
        ToolSpec(id, id in setOf("set_text","verify_state"), id in setOf("share_text","share_media","download_media","switch_account","publish_post","send_message","delete_data","purchase"))
    }
}
data class ProposedCall(val tool: String, val packageName: String, val selector: String, val value: String, val expected: String) {
    fun valid() = tool in ToolRegistry.tools && packageName.isNotBlank() && selector == "byga_safe_input" && value.length in 1..4000 && expected == value
}
data class AgentPlan(val goal: String, val calls: List<ProposedCall>) {
    fun valid() = goal.isNotBlank() && goal.length <= 4000 && calls.size in 1..30 && calls.all { it.valid() }
}
// Raw grant objects are immutable. Only the controller receives an active, session-bound grant.
internal data class ScopeGrant(val id: String, val session: String, val packages: Set<String>, val tools: Set<String>, val expiresAt: Long, val maxSteps: Int, val maxReplans: Int)
data class Observation(val packageName: String, val value: String?, val observedAt: Long, val healthy: Boolean, val locked: Boolean, val sensitive: Boolean = false)
internal object PolicyEngine {
    fun evaluate(call: ProposedCall, grant: ScopeGrant?, session: String, now: Long, steps: Int, observation: Observation, enabled: Boolean): PolicyDecision {
        fun result(d: Decision, why: String) = PolicyDecision(d,why)
        if (!enabled) return result(Decision.DENY,"EXPERIMENTAL_DISABLED")
        if (grant == null || grant.session != session) return result(Decision.NEED_APPROVAL,"SESSION_SCOPE_REQUIRED")
        if (now >= grant.expiresAt || steps >= grant.maxSteps) return result(Decision.NEED_APPROVAL,"SCOPE_EXPIRED_OR_STEP_LIMIT")
        if (!call.valid()) return result(Decision.DENY,"INVALID_SCHEMA")
        val tool=ToolRegistry.tools[call.tool] ?: return result(Decision.DENY,"UNKNOWN_TOOL")
        if (!tool.available) return result(Decision.DENY,"TOOL_NOT_IMPLEMENTED_OR_COMPATIBILITY_BLOCKED")
        if (call.packageName !in grant.packages || call.tool !in grant.tools) return result(Decision.DENY,"OUT_OF_SCOPE")
        if (observation.sensitive) return result(Decision.DENY,"SENSITIVE_WINDOW")
        if (observation.locked || !observation.healthy) return result(Decision.NEED_APPROVAL,"DEVICE_OR_PERMISSION_NOT_READY")
        if (observation.packageName != call.packageName || now-observation.observedAt !in 0..5000) return result(Decision.NEED_REOBSERVE,"STALE_OR_CHANGED_CONTEXT")
        return result(Decision.ALLOW,"SESSION_AND_STATE_VALID")
    }
}
interface AgentDevice {
    suspend fun observe(): Observation
    suspend fun execute(call: ProposedCall): Boolean
}
data class AgentEvent(val at: Long, val state: AgentState, val tool: String?, val reason: String)
class AgentController(private val device: AgentDevice, private val now: () -> Long, private val event: (AgentEvent) -> Unit) {
    companion object { private val deviceLock=Mutex() }
    private val session=UUID.randomUUID().toString()
    @Volatile private var grant: ScopeGrant?=null
    @Volatile private var stopped=false
    @Volatile private var paused=false
    @Volatile var state=AgentState.CREATED; private set
    private var used=0
    private var replans=0
    private var approvedPlan: AgentPlan?=null
    private fun emit(s: AgentState, tool: String?=null, why: String) { state=s; event(AgentEvent(now(),s,tool,why)) }
    fun review(plan: AgentPlan) { check(state !in setOf(AgentState.EXECUTING,AgentState.OBSERVING,AgentState.VERIFYING)); grant=null; approvedPlan=plan.copy(calls=plan.calls.toList()); emit(AgentState.WAITING_FOR_SCOPE,why="REVIEW_REQUIRED") }
    fun approve(packages: Set<String>, tools: Set<String>, durationMs: Long, maxSteps: Int, maxReplans: Int) {
        val plan=checkNotNull(approvedPlan); require(plan.valid()); require(durationMs in 1000..600000 && maxSteps in 1..30 && maxReplans in 0..3)
        require(plan.calls.all { it.packageName in packages && it.tool in tools && ToolRegistry.tools[it.tool]?.available==true })
        grant=ScopeGrant(UUID.randomUUID().toString(),session,packages.toSet(),tools.toSet(),now()+durationMs,maxSteps,maxReplans)
        stopped=false; paused=false; used=0; replans=0
    }
    fun pause() { paused=true; emit(AgentState.PAUSED,why="USER_PAUSED") }
    fun resume() { paused=false } // Next action always observes and checks scope again.
    fun stop() { stopped=true; grant=null; emit(AgentState.CANCELLED,why="USER_STOPPED_SCOPE_REVOKED") }
    fun revoke() = stop()
    suspend fun run(enabled: () -> Boolean) {
        if (!deviceLock.tryLock()) { emit(AgentState.BLOCKED,why="ANOTHER_UI_RUN_ACTIVE"); return }
        try {
            val plan=approvedPlan
            if(plan==null) { emit(AgentState.BLOCKED,why="NO_REVIEWED_PLAN"); return }
            if(!plan.valid()) { emit(AgentState.BLOCKED,why="INVALID_PLAN"); return }
            for (call in plan.calls) {
                while(paused && !stopped) { currentCoroutineContext().ensureActive(); delay(100) }
                currentCoroutineContext().ensureActive()
                if(stopped) return
                emit(AgentState.OBSERVING,call.tool,"OBSERVE_BEFORE_ACTION")
                var observation=device.observe()
                var decision=PolicyEngine.evaluate(call,grant,session,now(),used,observation,enabled())
                if(decision.decision==Decision.NEED_REOBSERVE) {
                    emit(AgentState.RECOVERING,call.tool,"REOBSERVE_ONCE")
                    observation=device.observe()
                    decision=PolicyEngine.evaluate(call,grant,session,now(),used,observation,enabled())
                }
                if(stopped) return
                if(decision.decision!=Decision.ALLOW) { emit(if(decision.decision==Decision.NEED_APPROVAL) AgentState.WAITING_FOR_USER else AgentState.BLOCKED,call.tool,decision.reason); return }
                currentCoroutineContext().ensureActive()
                emit(AgentState.EXECUTING,call.tool,decision.reason)
                if(stopped) return
                if(paused) { emit(AgentState.WAITING_FOR_USER,why="CONTROL_CHANGED_BEFORE_ACTION"); return }
                used++
                if(!device.execute(call)) { emit(AgentState.BLOCKED,call.tool,"ACTION_FAILED_NO_BLIND_RETRY"); return }
                if(stopped) return
                emit(AgentState.VERIFYING,call.tool,"VERIFY_OBSERVED_VALUE")
                if(stopped) return
                var actual=device.observe()
                if(stopped) return
                if(actual.sensitive) { emit(AgentState.BLOCKED,call.tool,"SENSITIVE_WINDOW"); return }
                if(actual.locked || !actual.healthy || actual.packageName!=call.packageName) { emit(AgentState.WAITING_FOR_USER,call.tool,"CONTEXT_CHANGED_AFTER_ACTION"); return }
                if(!verified(actual,call)) {
                    if(replans >= (grant?.maxReplans ?: 0)) { emit(AgentState.BLOCKED,call.tool,"VERIFICATION_FAILED"); return }
                    replans++; emit(AgentState.RECOVERING,call.tool,"REOBSERVE_WITHOUT_REPEATING_ACTION")
                    actual=device.observe()
                    if(!verified(actual,call)) { emit(AgentState.BLOCKED,call.tool,"VERIFICATION_FAILED"); return }
                }
                if(stopped) return
            }
            if(!stopped) emit(AgentState.COMPLETED,why="ALL_EXPECTED_VALUES_OBSERVED")
        } catch(e: CancellationException) { stop(); throw e }
        catch(_: Exception) { emit(AgentState.FAILED,why="DEVICE_ADAPTER_ERROR_REDACTED") }
        finally { grant=null; deviceLock.unlock() }
    }
    private fun verified(o: Observation, c: ProposedCall) = o.healthy && !o.locked && !o.sensitive && o.packageName==c.packageName && o.value==c.expected && now()-o.observedAt in 0..5000
}
