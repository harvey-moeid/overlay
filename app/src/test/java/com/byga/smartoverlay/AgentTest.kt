package com.byga.smartoverlay

import com.byga.smartoverlay.agent.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AgentTest {
    private val pkg="com.byga.smartoverlay"
    private val call=ProposedCall("set_text",pkg,"byga_safe_input","draft","draft")
    private fun grant(expiry: Long=10000, steps: Int=10)=ScopeGrant("id","session",setOf(pkg),setOf("set_text","verify_state"),expiry,steps,1)
    private fun observation()=Observation(pkg,"",1000,true,false)
    private fun decision(g: ScopeGrant?=grant(), c: ProposedCall=call, o: Observation=observation(), enabled: Boolean=true, session: String="session", used: Int=0)=PolicyEngine.evaluate(c,g,session,1000,used,o,enabled).decision
    @Test fun disabledByDefaultDenies() { assertEquals(Decision.DENY,decision(enabled=false)) }
    @Test fun grantRequired() { assertEquals(Decision.NEED_APPROVAL,decision(g=null)) }
    @Test fun grantBoundToSession() { assertEquals(Decision.NEED_APPROVAL,decision(session="other")) }
    @Test fun expiredScopeStops() { assertEquals(Decision.NEED_APPROVAL,decision(g=grant(1000))) }
    @Test fun stepLimitStops() { assertEquals(Decision.NEED_APPROVAL,decision(used=10)) }
    @Test fun scopeCannotChangePackage() { assertEquals(Decision.DENY,decision(c=call.copy(packageName="com.other"))) }
    @Test fun malformedSelectorDenied() { assertEquals(Decision.DENY,decision(c=call.copy(selector="password"))) }
    @Test fun sensitiveWindowDenied() { assertEquals(Decision.DENY,decision(o=observation().copy(sensitive=true))) }
    @Test fun foregroundChangedReobserves() { assertEquals(Decision.NEED_REOBSERVE,decision(o=observation().copy(packageName="else"))) }
    @Test fun futureObservationRejected() { assertEquals(Decision.NEED_REOBSERVE,decision(o=observation().copy(observedAt=2000))) }
    @Test fun lockedDeviceWaits() { assertEquals(Decision.NEED_APPROVAL,decision(o=observation().copy(locked=true))) }
    @Test fun revokedPermissionWaits() { assertEquals(Decision.NEED_APPROVAL,decision(o=observation().copy(healthy=false))) }
    @Test fun purchaseAndPublishNeverExecutable() { for(t in listOf("purchase","publish_post","send_message","delete_data")) assertEquals(Decision.DENY,decision(c=call.copy(tool=t))) }
    @Test fun unknownToolsDenied() { assertEquals(Decision.DENY,decision(c=call.copy(tool="shell"))) }
    @Test fun modelCannotSupplyScopeFields() { assertFalse(call.copy(expected="other").valid()) }
    @Test fun allowedCall() { assertEquals(Decision.ALLOW,decision()) }
    private class Device : AgentDevice {
        var value=""; var actions=0; var locked=false; var accepts=true; var changesValue=true
        override suspend fun observe()=Observation("com.byga.smartoverlay",value,1000,true,locked)
        override suspend fun execute(call: ProposedCall): Boolean { actions++; if(changesValue) value=call.value; return accepts }
    }
    private fun controller(d: AgentDevice, events: MutableList<AgentEvent> = mutableListOf())=AgentController(d,{1000},events::add)
    private fun prepare(c: AgentController, steps: Int=10) { c.review(AgentPlan("goal",listOf(call,call.copy(tool="verify_state")))); c.approve(setOf(pkg),setOf("set_text","verify_state"),5000,steps,1) }
    @Test fun runtimeActsAndVerifies() = runTest { val d=Device(); val c=controller(d); prepare(c); c.run {true}; assertEquals(AgentState.COMPLETED,c.state); assertEquals(2,d.actions) }
    @Test fun runtimeNeverRunsWithoutConsent() = runTest { val d=Device(); val c=controller(d); c.review(AgentPlan("goal",listOf(call))); c.run {true}; assertEquals(0,d.actions); assertEquals(AgentState.WAITING_FOR_USER,c.state) }
    @Test fun actionAcknowledgementIsNotSuccess() = runTest { val d=Device().apply{changesValue=false}; val c=controller(d); prepare(c); c.run {true}; assertEquals(AgentState.BLOCKED,c.state); assertEquals(1,d.actions) }
    @Test fun failedActionNeverBlindRetries() = runTest { val d=Device().apply{accepts=false}; val c=controller(d); prepare(c); c.run {true}; assertEquals(1,d.actions); assertEquals(AgentState.BLOCKED,c.state) }
    @Test fun stopBeforeActionPreventsExecution() = runTest {
        val d=Device(); lateinit var c: AgentController
        c=AgentController(d,{1000}) { if(it.state==AgentState.EXECUTING) c.stop() }; prepare(c); c.run {true}; assertEquals(0,d.actions); assertNotEquals(AgentState.COMPLETED,c.state)
    }
    @Test fun grantIsRevokedAfterRun() = runTest { val d=Device(); val c=controller(d); prepare(c); c.run {true}; val before=d.actions; c.run {true}; assertEquals(before,d.actions); assertEquals(AgentState.WAITING_FOR_USER,c.state) }
    @Test fun runtimeEnforcesStepCap() = runTest { val d=Device(); val c=controller(d); prepare(c,1); c.run {true}; assertEquals(1,d.actions); assertEquals(AgentState.WAITING_FOR_USER,c.state) }
    @Test fun auditDoesNotContainFieldText() = runTest { val events=mutableListOf<AgentEvent>(); val c=controller(Device(),events); prepare(c); c.run {true}; assertFalse(events.joinToString().contains("draft")) }
    @Test fun lockAtStartHasNoActions() = runTest { val d=Device().apply{locked=true}; val c=controller(d); prepare(c); c.run {true}; assertEquals(0,d.actions) }
    @Test fun importedSkillCannotSkipTests() { assertFalse(SkillLifecycle.transition(SkillStatus.DRAFT,SkillStatus.ENABLED,true)); assertFalse(SkillLifecycle.transition(SkillStatus.TESTING,SkillStatus.REVIEW,false)) }
    @Test fun skillProgressionRequiresReview() { assertTrue(SkillLifecycle.transition(SkillStatus.TESTING,SkillStatus.REVIEW,true)); assertTrue(SkillLifecycle.transition(SkillStatus.REVIEW,SkillStatus.APPROVED,true)); assertTrue(SkillLifecycle.transition(SkillStatus.APPROVED,SkillStatus.ENABLED,true)); assertTrue(SkillLifecycle.transition(SkillStatus.ENABLED,SkillStatus.DISABLED,true)) }
    @Test fun disabledSkillMustRetest() { assertFalse(SkillLifecycle.transition(SkillStatus.DISABLED,SkillStatus.ENABLED,true)); assertTrue(SkillLifecycle.transition(SkillStatus.DISABLED,SkillStatus.TESTING,false)) }
    @Test fun stopDuringVerificationPreventsNextTool() = runTest {
        val d=Device(); lateinit var c: AgentController
        c=AgentController(d,{1000}) { if(it.state==AgentState.VERIFYING) c.stop() }; prepare(c); c.run {true}; assertEquals(1,d.actions); assertEquals(AgentState.CANCELLED,c.state)
    }
    @Test fun onlyOneUiRunCanExecute() = runTest {
        val entered=CompletableDeferred<Unit>(); val release=CompletableDeferred<Unit>()
        val waiting=object: AgentDevice {
            override suspend fun observe(): Observation { entered.complete(Unit); release.await(); return observation() }
            override suspend fun execute(call: ProposedCall)=true
        }
        val first=controller(waiting); prepare(first)
        val running=launch { first.run {true} }; entered.await()
        try { val d=Device(); val second=controller(d); prepare(second); second.run {true}; assertEquals(AgentState.BLOCKED,second.state); assertEquals(0,d.actions) }
        finally { first.stop(); release.complete(Unit); running.join() }
    }
}
