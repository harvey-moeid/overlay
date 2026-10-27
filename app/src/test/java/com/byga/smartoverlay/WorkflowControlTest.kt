package com.byga.smartoverlay

import com.byga.smartoverlay.workflow.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkflowControlTest {
    private class Port : WorkflowPort {
        var readiness: suspend () -> Boolean = { true }
        var save: suspend () -> Unit = {}
        var action: suspend () -> String = { "done" }
        var acts = 0
        val checkpoints = mutableListOf<SavedCheckpoint>()
        override suspend fun ready(step: Step) = readiness()
        override suspend fun act(step: Step, previous: String): String { acts++; return action() }
        override suspend fun checkpoint(plan: Plan, next: Int, uncertain: Boolean, output: String) {
            save()
            checkpoints.add(SavedCheckpoint(plan.fingerprint(), next, uncertain, output))
        }
    }
    private val plan = Plan("Control", listOf(Step(Action.COPY_TEXT, "hello")))

    @Test fun pauseDuringReadinessPreventsActionUntilResume() = runTest {
        val ready = CompletableDeferred<Boolean>()
        val port = Port().apply { readiness = { ready.await() } }
        val runner = WorkflowRunner(port)
        val job = launch { runner.run(plan, plan.fingerprint()) }
        runCurrent(); runner.pause(); ready.complete(true); runCurrent()
        assertEquals(0, port.acts)
        assertEquals(RunState.PAUSED, runner.snapshot.value.state)
        runner.resume(); job.join()
        assertEquals(1, port.acts)
        assertEquals(RunState.SUCCEEDED, runner.snapshot.value.state)
    }

    @Test fun stopDuringFailedReadinessKeepsStoppedState() = runTest {
        val ready = CompletableDeferred<Boolean>()
        val port = Port().apply { readiness = { ready.await() } }
        val runner = WorkflowRunner(port)
        val job = launch { runner.run(plan, plan.fingerprint()) }
        runCurrent(); runner.stop(); ready.complete(false); job.join()
        assertEquals(0, port.acts)
        assertEquals(RunState.STOPPED, runner.snapshot.value.state)
    }

    @Test fun stopDuringReadinessDoesNotOpenNewApprovalGate() = runTest {
        val ready = CompletableDeferred<Boolean>()
        val port = Port().apply { readiness = { ready.await() } }
        val runner = WorkflowRunner(port)
        val approvalPlan = Plan("Review", listOf(Step(Action.REQUEST_USER_APPROVAL, "review")))
        val job = launch { runner.run(approvalPlan, approvalPlan.fingerprint()) }
        runCurrent(); runner.stop(); ready.complete(true); runCurrent()
        assertFalse(runner.awaitingApproval())
        assertTrue(job.isCompleted)
        assertEquals(RunState.STOPPED, runner.snapshot.value.state)
    }

    @Test fun pauseWhileSavingUncertaintyPreventsExternalAction() = runTest {
        val saved = CompletableDeferred<Unit>()
        val port = Port().apply { save = { saved.await() } }
        val runner = WorkflowRunner(port)
        val job = launch { runner.run(plan, plan.fingerprint()) }
        runCurrent(); runner.pause(); saved.complete(Unit); runCurrent()
        assertEquals(0, port.acts)
        assertEquals(RunState.PAUSED, runner.snapshot.value.state)
        runner.resume(); job.join()
        assertEquals(1, port.acts)
    }

    @Test fun parentTimeoutPropagatesAndKeepsUncertainCheckpoint() = runTest {
        val port = Port().apply { action = { delay(10_000); "late" } }
        val runner = WorkflowRunner(port)
        var propagated = false
        try { withTimeout(100) { runner.run(plan, plan.fingerprint()) } }
        catch (_: TimeoutCancellationException) { propagated = true }
        assertTrue(propagated)
        assertEquals(RunState.STOPPED, runner.snapshot.value.state)
        assertTrue(port.checkpoints.last().uncertain)
        assertFalse(WorkflowRunner.hasActiveRun())
    }

    @Test fun stopDuringFinalCheckpointCannotReportSuccess() = runTest {
        val finalSave = CompletableDeferred<Unit>()
        var saves = 0
        val port = Port().apply { save = { if (++saves == 3) finalSave.await() } }
        val runner = WorkflowRunner(port)
        val job = launch { runner.run(plan, plan.fingerprint()) }
        runCurrent(); runner.stop(); finalSave.complete(Unit); job.join()
        assertEquals(1, port.acts)
        assertEquals(RunState.STOPPED, runner.snapshot.value.state)
    }
}
