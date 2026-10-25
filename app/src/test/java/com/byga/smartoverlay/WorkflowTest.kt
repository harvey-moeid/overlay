package com.byga.smartoverlay

import com.byga.smartoverlay.workflow.*
import org.junit.Assert.*
import org.junit.Test

class WorkflowTest {
    private fun plan() = Plan("lokal",listOf(Step(Action.COPY_TEXT,"halo"),Step(Action.STOP)))
    @Test fun draftCannotExecute() { assertFalse(Guard.canRun(plan(),null)) }
    @Test fun approvedVersionCanExecute() { val p=plan(); assertTrue(Guard.canRun(p,p.fingerprint())) }
    @Test fun editsRevokeApproval() { val p=plan(); assertFalse(Guard.canRun(p.copy(steps=listOf(Step(Action.COPY_TEXT,"lain"))),p.fingerprint())) }
    @Test fun emptyPlanBlocked() { assertTrue(Plan("", emptyList()).validate().isNotEmpty()) }
    @Test fun invalidSelectorBlocked() { assertTrue(Plan("tes",listOf(Step(Action.SET_TEXT,"x"))).validate().isNotEmpty()) }
    @Test fun excessiveStepsBlocked() { assertTrue(Plan("tes",List(51){Step(Action.STOP)}).validate().isNotEmpty()) }
    @Test fun invalidDelayBlocked() { assertTrue(Plan("tes",listOf(Step(Action.DELAY,"-1"))).validate().isNotEmpty()) }
    @Test fun boundedDelayAccepted() { assertTrue(Plan("tes",listOf(Step(Action.DELAY,"500"))).validate().isEmpty()) }
    @Test fun unknownAccountCannotPublish() { assertFalse(Guard.canPublish(null,null,true)) }
    @Test fun wrongAccountCannotPublish() { assertFalse(Guard.canPublish("A","B",true)) }
    @Test fun approvalMustBeFresh() { assertFalse(Guard.canPublish("A","A",false)) }
    @Test fun accountAndApprovalRequired() { assertTrue(Guard.canPublish("A","A",true)) }
    @Test fun lockedUiWaits() { assertFalse(Guard.schedulerReady(true,true,true)) }
    @Test fun backgroundDoesNotNeedUnlock() { assertTrue(Guard.schedulerReady(true,false,false)) }
    @Test fun unattendedUiWaits() { assertFalse(Guard.schedulerReady(false,true,false)) }
    @Test fun noRetrySideEffect() { assertFalse(Guard.mayRetry(Action.CLICK_NODE,0,false)); assertFalse(Guard.mayRetry(Action.FIND_NODE,0,true)) }
    @Test fun retriesAreBounded() { assertTrue(Guard.mayRetry(Action.FIND_NODE,0,false)); assertFalse(Guard.mayRetry(Action.FIND_NODE,2,false)) }
    @Test fun duplicateTokenRejected() { val ledger=ExecutionLedger(); assertTrue(ledger.claim("run:step")); assertFalse(ledger.claim("run:step")) }
    @Test fun httpsOnlyNoCredentials() { assertTrue(secureUrl("https://example.com/file")); listOf("http://example.com","file:///secret","https://a:b@example.com","https:///x").forEach { assertFalse(secureUrl(it)) } }
}
