package com.byga.smartoverlay

import com.byga.smartoverlay.workflow.*
import com.byga.smartoverlay.profiles.*
import com.byga.smartoverlay.ai.RequestBudget
import com.byga.smartoverlay.capture.CapturePolicy
import com.byga.smartoverlay.scheduler.*
import com.byga.smartoverlay.data.Record
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

class StudioTest {
    private fun plan()=Plan("Draft",listOf(Step(Action.GENERATE_AI_TEXT,"caption"),Step(Action.COPY_TEXT,"{{output}}")))
    private class Port:WorkflowPort{
        val calls=mutableListOf<Action>();val checkpoints=mutableListOf<SavedCheckpoint>();var ready=true;var fail=false
        override suspend fun ready(step:Step)=ready
        override suspend fun act(step:Step,previous:String):String{calls.add(step.action);if(fail)error("failure");return if(step.action==Action.GENERATE_AI_TEXT)"hasil AI" else step.value.replace("{{output}}",previous)}
        override suspend fun checkpoint(plan:Plan,next:Int,uncertain:Boolean,output:String){checkpoints.add(SavedCheckpoint(plan.fingerprint(),next,uncertain,output))}
    }
    @Test fun approvedWorkflowPropagatesActualOutput()=runTest{val p=Port();val r=WorkflowRunner(p);val plan=plan();r.run(plan,plan.fingerprint());assertEquals(RunState.SUCCEEDED,r.snapshot.value.state);assertEquals("hasil AI",r.snapshot.value.output);assertEquals(listOf(Action.GENERATE_AI_TEXT,Action.COPY_TEXT),p.calls)}
    @Test fun draftHasNoEffects()=runTest{val p=Port();val r=WorkflowRunner(p);r.run(plan(),null);assertTrue(p.calls.isEmpty());assertEquals(RunState.BLOCKED,r.snapshot.value.state)}
    @Test fun changedPlanRevokesApproval()=runTest{val p=Port();val r=WorkflowRunner(p);r.run(plan().copy(name="Changed"),plan().fingerprint());assertTrue(p.calls.isEmpty())}
    @Test fun unknownAdapterNeverExecutes()=runTest{val p=Port();val r=WorkflowRunner(p);val plan=Plan("x",listOf(Step(Action.SHARE_MEDIA)));r.run(plan,plan.fingerprint());assertTrue(p.calls.isEmpty());assertEquals(RunState.BLOCKED,r.snapshot.value.state)}
    @Test fun lockedOrBackgroundUiWaits()=runTest{val p=Port().apply{ready=false};val r=WorkflowRunner(p);val plan=plan();r.run(plan,plan.fingerprint());assertTrue(p.calls.isEmpty());assertEquals(RunState.WAITING_FOR_USER,r.snapshot.value.state)}
    @Test fun failurePersistsUncertaintyWithoutRetry()=runTest{val p=Port().apply{fail=true};val r=WorkflowRunner(p);val plan=plan();r.run(plan,plan.fingerprint());assertEquals(1,p.calls.size);assertTrue(p.checkpoints.last().uncertain);assertEquals(RunState.BLOCKED,r.snapshot.value.state)}
    @Test fun uncertainCheckpointCannotReplay()=runTest{val p=Port();val r=WorkflowRunner(p);val plan=plan();r.run(plan,plan.fingerprint(),SavedCheckpoint(plan.fingerprint(),0,true));assertTrue(p.calls.isEmpty())}
    @Test fun checkpointMustMatchVersion() {assertFalse(CheckpointPolicy.mayResume(plan(),SavedCheckpoint("other",1,false,"output")))}
    @Test fun resumeUsesPersistedOutputWithoutRepeatingAi()=runTest{val p=Port();val r=WorkflowRunner(p);val plan=plan();r.run(plan,plan.fingerprint(),SavedCheckpoint(plan.fingerprint(),1,false,"persisted"));assertEquals(listOf(Action.COPY_TEXT),p.calls);assertEquals("persisted",r.snapshot.value.output)}
    @Test fun missingCheckpointOutputRefusesDependentStep(){assertFalse(CheckpointPolicy.mayResume(plan(),SavedCheckpoint(plan().fingerprint(),1,false)))}
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun approvalGateCannotBeSkipped()=runTest{val p=Port();val r=WorkflowRunner(p);val plan=Plan("x",listOf(Step(Action.REQUEST_USER_APPROVAL,"review"),Step(Action.COPY_TEXT,"yes")));val job=launch{r.run(plan,plan.fingerprint())};runCurrent();assertTrue(p.calls.isEmpty());assertEquals(RunState.WAITING_FOR_USER,r.snapshot.value.state);r.approve();job.join();assertEquals(1,p.calls.size)}
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun stopDuringApprovalPreventsNextStep()=runTest{val p=Port();val r=WorkflowRunner(p);val plan=Plan("x",listOf(Step(Action.REQUEST_USER_APPROVAL),Step(Action.COPY_TEXT,"yes")));val job=launch{r.run(plan,plan.fingerprint())};runCurrent();r.stop();job.join();assertTrue(p.calls.isEmpty());assertNotEquals(RunState.SUCCEEDED,r.snapshot.value.state)}
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun pauseStopsQueueUntilResume()=runTest{val p=Port();val r=WorkflowRunner(p);val plan=Plan("x",listOf(Step(Action.DELAY,"1000"),Step(Action.COPY_TEXT,"yes")));val job=launch{r.run(plan,plan.fingerprint())};runCurrent();r.pause();advanceTimeBy(2000);assertTrue(p.calls.isEmpty());r.resume();job.join();assertEquals(1,p.calls.size)}
    @Test fun workflowCodecRoundTripKeepsPackageSelectors(){val p=Plan("Test",listOf(Step(Action.LAUNCH_APP,packageName="com.openai.chatgpt"),Step(Action.COPY_TEXT,"safe")));assertEquals(p,WorkflowCodec.decode(WorkflowCodec.encode(p)))}
    @Test fun importCannotInjectApproval(){try{WorkflowCodec.decode("""{"name":"x","approved":true,"steps":[{"action":"STOP"}]}""");fail()}catch(_:IllegalArgumentException){}}
    @Test fun unsupportedSchemaVersionRejected(){try{WorkflowCodec.decode("""{"name":"x","version":7,"steps":[{"action":"STOP"}]}""");fail()}catch(_:IllegalArgumentException){}}
    @Test fun legacyWorkflowCanBeLoaded(){assertEquals(Action.COPY_TEXT,WorkflowCodec.decode("""{"name":"old","steps":[{"action":"COPY_TEXT","value":"x"}]}""").steps.first().action)}
    @Test fun launchPackageIsRequired(){assertTrue(Plan("x",listOf(Step(Action.LAUNCH_APP))).validate().isNotEmpty())}
    @Test fun requestBudgetBoundaries(){assertTrue(RequestBudget.mayReserve(0,12));assertTrue(RequestBudget.mayReserve(11,12));assertFalse(RequestBudget.mayReserve(12,12));assertFalse(RequestBudget.mayReserve(-1,12));assertFalse(RequestBudget.mayReserve(0,0));assertFalse(RequestBudget.mayReserve(0,101))}
    private fun account()=AccountProfile("a","com.instagram.android","personal","@me","USER_CONFIRMED",1000)
    @Test fun accountConfirmationIsBoundToPackage(){assertFalse(AccountGuard.canHandoff(account(),"com.zhiliaoapp.musically",true,2000))}
    @Test fun missingAccountBlocksHandoff(){assertFalse(AccountGuard.canHandoff(null,"com.instagram.android",true,2000))}
    @Test fun unknownAccountBlocksHandoff(){assertFalse(AccountGuard.canHandoff(account().copy(status="UNKNOWN"),"com.instagram.android",true,2000))}
    @Test fun freshHumanReviewIsMandatory(){assertFalse(AccountGuard.canHandoff(account(),"com.instagram.android",false,2000))}
    @Test fun expiredAndFutureConfirmationBlockHandoff(){assertFalse(AccountGuard.canHandoff(account(),"com.instagram.android",true,301001));assertFalse(AccountGuard.canHandoff(account(),"com.instagram.android",true,999))}
    @Test fun validConfirmationAllowsOnlyHandoff(){assertTrue(AccountGuard.canHandoff(account(),"com.instagram.android",true,2000))}
    @Test fun legacyAccountNeverBecomesVerified(){val p=AccountProfile.decode(Record("account","legacy","Instagram • alias • UNKNOWN"));assertEquals("UNKNOWN",p.status);assertEquals(0,p.confirmedAt);assertFalse(AccountGuard.canHandoff(p,"com.instagram.android",true,1000))}
    @Test fun accountSerializationPreservesAliasAndBinding(){val a=account();assertEquals(a,AccountProfile.decode(Record("account",a.id,a.encode())))}
    @Test fun captureIsStrictlyAllowlisted(){assertFalse(CapturePolicy.mayCapture("com.bank.app","com.bank.app",false,false));assertFalse(CapturePolicy.mayCapture("com.android.chrome","com.android.chrome",false,false))}
    @Test fun captureRejectsChangedForeground(){assertFalse(CapturePolicy.mayCapture("com.openai.chatgpt","com.instagram.android",false,false))}
    @Test fun captureNeverUnlocksOrUsesSensitiveWindow(){assertFalse(CapturePolicy.mayCapture("com.byga.smartoverlay","com.byga.smartoverlay",true,false));assertFalse(CapturePolicy.mayCapture("com.byga.smartoverlay","com.byga.smartoverlay",false,true))}
    @Test fun matchingCaptureWithConsentIsEligible(){assertTrue(CapturePolicy.mayCapture("com.byga.smartoverlay","com.byga.smartoverlay",false,false))}
    @Test fun scheduleRejectsPastTimeAndInvalidInterval(){assertFalse(SchedulePolicy.valid(ScheduleConfig("a","w","name",900,0,1),1000));assertFalse(SchedulePolicy.valid(ScheduleConfig("a","w","name",2000,14,1),1000))}
    @Test fun scheduleAcceptsDeferredAndRecurring(){assertTrue(SchedulePolicy.valid(ScheduleConfig("a","w","name",2000,0,1),1000));assertTrue(SchedulePolicy.valid(ScheduleConfig("a","w","name",900,15,1),1000))}
    @Test fun selfDependencyAndInvalidPriorityRejected(){assertFalse(SchedulePolicy.valid(ScheduleConfig("a","w","name",2000,0,1,"a"),1000));assertFalse(SchedulePolicy.valid(ScheduleConfig("a","w","name",2000,0,8),1000))}
    @Test fun dependenciesRequireManualCompletionNotHandoff(){assertFalse(SchedulePolicy.dependencyReady("a","WAITING_FOR_USER"));assertFalse(SchedulePolicy.dependencyReady("a","HANDED_OFF"));assertTrue(SchedulePolicy.dependencyReady("a","USER_COMPLETED"));assertTrue(SchedulePolicy.dependencyReady("",null))}
}
