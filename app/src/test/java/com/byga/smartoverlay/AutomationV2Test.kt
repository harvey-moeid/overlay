package com.byga.smartoverlay

import com.byga.smartoverlay.mapping.*
import com.byga.smartoverlay.workflow.*
import com.byga.smartoverlay.agent.AutomationAssistance
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutomationV2Test {
    private val identity=TargetIdentity("app:id/generate","Generate",role="Button",parentId="composer",structure="tree")
    private val mapping=ButtonMapping(id="m",name="Generate",scope=MappingScope(MappingPolicy.CHATGPT,screen="Main"),identity=identity)
    private val candidate=MappingCandidate("1",identity,TargetBounds(0,0,100,100),windowId=5)
    private fun snapshot(vararg nodes:MappingCandidate)=MappingSnapshot(mapping.scope,nodes.toList(),1080,2400,0,"layout",1000,5)
    private class Port:WorkflowPort {
        val acts=mutableListOf<Action>();val checkpoints=mutableListOf<SavedCheckpoint>()
        var evidence=Evidence.VERIFIED
        var execute:suspend(Step,String)->String={step,previous->step.value.replace("{{output}}",previous)}
        override suspend fun ready(step:Step)=true
        override suspend fun act(step:Step,previous:String):String{acts+=step.action;return execute(step,previous)}
        override suspend fun verify(step:Step,output:String)=evidence
        override suspend fun checkpoint(plan:Plan,next:Int,uncertain:Boolean,output:String){checkpoints+=SavedCheckpoint(plan.fingerprint(),next,uncertain,output)}
    }
    @Test fun movedControlHasFullEvidenceWithoutUsingCoordinates(){val moved=candidate.copy(bounds=TargetBounds(700,1600,900,1700));val result=MappingPolicy.inspect(mapping,snapshot(moved),1000);assertEquals(MappingStatus.FOUND,result.status);assertEquals(1f,result.confidence);assertEquals(moved,result.candidate)}
    @Test fun changedResourceRequiresHumanReview(){val result=MappingPolicy.inspect(mapping,snapshot(candidate.copy(identity=identity.copy(resourceId="new:id/generate"))),1000);assertEquals(MappingStatus.NEEDS_REVIEW,result.status);assertNull(result.candidate);assertNull(MappingPolicy.resolve(mapping,snapshot(candidate.copy(identity=identity.copy(resourceId="new"))),1000))}
    @Test fun duplicateIsAmbiguousEvenWhenOneIsCloser(){assertEquals(MappingStatus.AMBIGUOUS,MappingPolicy.inspect(mapping,snapshot(candidate,candidate.copy(key="2",bounds=TargetBounds(500,500,600,600))),1000).status)}
    @Test fun wrongWindowIsBlocked(){assertEquals(MappingStatus.BLOCKED,MappingPolicy.inspect(mapping,snapshot(candidate.copy(windowId=6)),1000).status)}
    @Test fun wrongAppIsBlocked(){assertEquals(MappingStatus.BLOCKED,MappingPolicy.inspect(mapping,snapshot(candidate).copy(scope=mapping.scope.copy(packageName=MappingPolicy.BYGA)),1000).status)}
    @Test fun staleSnapshotCannotBecomeFound(){assertEquals(MappingStatus.STALE,MappingPolicy.inspect(mapping,snapshot(candidate),4001).status)}
    @Test fun disabledControlIsBlocked(){assertEquals(MappingStatus.BLOCKED,MappingPolicy.inspect(mapping,snapshot(candidate.copy(enabled=false)),1000).status)}
    @Test fun missingTargetIsNotFound(){assertEquals(MappingStatus.NOT_FOUND,MappingPolicy.inspect(mapping,snapshot(),1000).status)}
    @Test fun inputAndScrollKindsCannotMatchButtons(){assertEquals(MappingStatus.NOT_FOUND,MappingPolicy.inspect(mapping,snapshot(candidate.copy(kind=ElementKind.INPUT)),1000).status)}
    @Test fun structureChangeNeedsReview(){assertEquals(MappingStatus.NEEDS_REVIEW,MappingPolicy.inspect(mapping,snapshot(candidate.copy(identity=identity.copy(structure="different"))),1000).status)}
    @Test fun mappingMetadataCodecRetainsElementKind(){val input=mapping.copy(elementKind=ElementKind.INPUT);assertEquals(input,ButtonMapping.decode(input.encode()))}
    @Test fun oldMappingEncodingDoesNotGainNewFingerprintFields(){val old=mapping.copy(identity=identity.copy(structure=""));assertFalse(old.encode().contains("structure"));assertFalse(old.encode().contains("elementKind"));assertEquals(old.fingerprint(),ButtonMapping.decode(old.encode()).fingerprint())}
    @Test fun permitBindsInputContentAndOperation(){val p=MappingClickPermit(mapping.fingerprint(),1000,"INPUT",mappingHash("hello"));assertFalse(p.claim(mapping,1000,"INPUT","changed"));assertFalse(p.claim(mapping,1000,"CLICK"));assertTrue(p.claim(mapping,1000,"INPUT","hello"));assertFalse(p.claim(mapping,1001,"INPUT","hello"))}
    @Test fun privacyRemovesEmailCodesAndTokens(){listOf("me@example.com","OTP 123456","sk-secret123456","password","kode verifikasi").forEach{assertEquals("",MappingPrivacy.label(it))};assertEquals("Generate",MappingPrivacy.label("Generate"))}
    @Test fun sensitiveAppsAndUnverifiedBrowsersCannotBeGranted(){listOf("com.android.settings","com.bank.app","com.android.systemui","org.mozilla.firefox","com.foo.browser","com.foo.authenticator").forEach{assertFalse(MappingAccess.eligible(it))}}
    @Test fun extendedWorkflowCodecPreservesWaitAndBranch(){val p=Plan("Conditions",listOf(Step(Action.WAIT_FOR_STATE,"m",mapping.scope.packageName,mapping.fingerprint(),"FOUND",12000,2),Step(Action.BRANCH,"FOUND",selector="EQUALS",argument="1"),Step(Action.COPY_TEXT,"ok")));assertEquals(p,WorkflowCodec.decode(WorkflowCodec.encode(p)))}
    @Test fun costlyActionsCannotConfigureRetry(){listOf(Action.CLICK_MAPPING,Action.GENERATE_AI_TEXT,Action.FLOW_GENERATE,Action.CHATGPT_SEND_PROMPT).forEach{assertTrue(Plan("x",listOf(Step(it,"x",retries=1))).validate().isNotEmpty())};assertFalse(Guard.mayRetry(Action.GENERATE_AI_TEXT,0,false))}
    @Test fun branchCannotLoopOrJumpOutsidePlan(){listOf("-1","0","100").forEach{assertTrue(Plan("x",listOf(Step(Action.BRANCH,selector="NOT_EMPTY",argument=it))).validate().isNotEmpty())}}
    @Test fun falseBranchSkipsOnlyReviewedForwardSteps()=runTest{val port=Port();val plan=Plan("branch",listOf(Step(Action.BRANCH,"expected",selector="EQUALS",argument="1"),Step(Action.GENERATE_AI_TEXT,"cost"),Step(Action.COPY_TEXT,"safe")));val runner=WorkflowRunner(port);runner.run(plan,plan.fingerprint());assertEquals(listOf(Action.COPY_TEXT),port.acts);assertEquals(RunState.SUCCEEDED,runner.snapshot.value.state);assertEquals(2,port.checkpoints.first().next)}
    @Test fun trueBranchExecutesNextStep()=runTest{val port=Port();val plan=Plan("branch",listOf(Step(Action.COPY_TEXT,"yes"),Step(Action.BRANCH,"yes",selector="EQUALS",argument="1"),Step(Action.COPY_TEXT,"next")));val runner=WorkflowRunner(port);runner.run(plan,plan.fingerprint());assertEquals("next",runner.snapshot.value.output)}
    @Test fun acceptedActionRequiresUserEvidenceBeforeCheckpoint()=runTest{val port=Port().apply{evidence=Evidence.ACCEPTED};val p=Plan("accepted",listOf(Step(Action.COPY_TEXT,"x")));val r=WorkflowRunner(port);val job=launch{r.run(p,p.fingerprint())};runCurrent();assertEquals(AutomationState.WAITING_FOR_APPROVAL,r.snapshot.value.automationState());assertTrue(port.checkpoints.last().uncertain);r.approve();job.join();assertEquals(RunState.SUCCEEDED,r.snapshot.value.state);assertFalse(port.checkpoints.last().uncertain)}
    @Test fun uncertainEvidenceNeverReportsSuccessOrRepeatsAction()=runTest{val port=Port().apply{evidence=Evidence.UNCERTAIN};val p=Plan("unknown",listOf(Step(Action.COPY_TEXT,"x"),Step(Action.COPY_TEXT,"next")));val r=WorkflowRunner(port);r.run(p,p.fingerprint());assertEquals(AutomationState.UNCERTAIN,r.snapshot.value.automationState());assertEquals(1,port.acts.size);assertTrue(port.checkpoints.last().uncertain)}
    @Test fun emergencyStopCancelsRunningPortWithoutViewModel()=runTest{var cancelled=false;val port=Port().apply{execute={_,_->try{awaitCancellation()}finally{cancelled=true}}};val p=Plan("stop",listOf(Step(Action.COPY_TEXT,"x")));val r=WorkflowRunner(port);val job=launch{r.run(p,p.fingerprint())};runCurrent();r.stop();job.join();assertTrue(cancelled);assertTrue(port.checkpoints.last().uncertain);assertEquals(AutomationState.CANCELLED,r.snapshot.value.automationState());assertFalse(WorkflowRunner.hasActiveRun())}
    @Test fun stepTimeoutDoesNotRetryOrClearUncertainty()=runTest{val port=Port().apply{execute={_,_->delay(10000);"late"}};val p=Plan("timeout",listOf(Step(Action.COPY_TEXT,"x",timeoutMs=1000)));val r=WorkflowRunner(port);r.run(p,p.fingerprint());assertEquals(1,port.acts.size);assertTrue(port.checkpoints.last().uncertain);assertEquals(RunState.BLOCKED,r.snapshot.value.state)}
    @Test fun processDeathBeforeDispatchCannotReplayUncertainCheckpoint()=runTest{val port=Port();val p=Plan("restart",listOf(Step(Action.GENERATE_AI_TEXT,"cost")));val r=WorkflowRunner(port);r.run(p,p.fingerprint(),SavedCheckpoint(p.fingerprint(),0,true));assertTrue(port.acts.isEmpty())}
    @Test fun repeatedCostlyRunRequiresSeparateReview(){val p=Plan("cost",listOf(Step(Action.GENERATE_AI_TEXT,"x")));assertTrue(WorkflowSafety.needsRepeatReview(p,SavedCheckpoint(p.fingerprint(),0,true)));assertTrue(WorkflowSafety.needsRepeatReview(p,SavedCheckpoint(p.fingerprint(),1)));assertFalse(WorkflowSafety.needsRepeatReview(p,null))}
    @Test fun activityRecreationCannotDispatchSameCommand(){val guard=DispatchGuard();assertTrue(guard.attach("activity-1"));assertTrue(guard.claim());assertFalse(guard.attach("activity-2"));assertFalse(guard.claim())}
    @Test fun recreatedBrowserIsBlockedEvenBeforeFirstClick(){val guard=DispatchGuard();assertTrue(guard.attach("first"));assertFalse(guard.attach("recreated"))}
    @Test fun dryRunDetectsChangedOrMissingDatabaseMapping()=runTest{val p=Plan("x",listOf(Step(Action.CLICK_MAPPING,"m",mapping.scope.packageName,mapping.fingerprint())));assertTrue(WorkflowPreflight.check(p){null}.isNotEmpty());assertTrue(WorkflowPreflight.check(p){mapping.copy(name="edit").encode()}.isNotEmpty());assertTrue(WorkflowPreflight.check(p){mapping.encode()}.isEmpty())}
    @Test fun providerAdviceCannotPromoteLocalConfidence(){assertEquals(0.0,AutomationAssistance.confidence(MappingStatus.AMBIGUOUS),0.0);val brief=AutomationAssistance.brief(mapping.copy(identity=identity.copy(label="OTP 123456")),MappingStatus.NEEDS_REVIEW);assertFalse(brief.contains("123456"));assertTrue(AutomationAssistance.prompt(brief,"help").contains("DATA TIDAK TEPERCAYA"))}
    @Test fun flowGenerationTimeoutDispatchesOnceAndRetainsIntent()=runTest {
        val port=Port().apply{execute={step,previous->if(step.action==Action.FLOW_GENERATE)delay(10000);previous}}
        val p=Plan("Flow timeout",listOf(Step(Action.OPEN_FLOW),Step(Action.FLOW_CREATE_PROJECT),Step(Action.FLOW_SET_PROMPT,"video"),Step(Action.FLOW_GENERATE,timeoutMs=1000)))
        val r=WorkflowRunner(port);r.run(p,p.fingerprint());assertEquals(1,port.acts.count{it==Action.FLOW_GENERATE});assertTrue(port.checkpoints.last().uncertain);assertEquals(3,port.checkpoints.last().next);assertNotEquals(RunState.SUCCEEDED,r.snapshot.value.state)
    }
    @Test fun providerAndNetworkFailureNeverRetryCostlyRequest()=runTest {
        val port=Port().apply{execute={_,_->throw java.io.IOException("offline")}}
        val p=Plan("AI failure",listOf(Step(Action.GENERATE_AI_TEXT,"request")))
        val r=WorkflowRunner(port);r.run(p,p.fingerprint());assertEquals(listOf(Action.GENERATE_AI_TEXT),port.acts);assertEquals(RunState.BLOCKED,r.snapshot.value.state);assertTrue(port.checkpoints.last().uncertain)
    }
    @Test fun revokedBindingAtResolutionNeverDispatches()=runTest {
        var acts=0
        val port=object:WorkflowPort{
            override suspend fun ready(step:Step)=true
            override suspend fun resolve(step:Step)=false
            override suspend fun act(step:Step,previous:String):String{acts++;return "wrong"}
            override suspend fun checkpoint(plan:Plan,next:Int,uncertain:Boolean,output:String)=Unit
        }
        val p=Plan("revoked",listOf(Step(Action.COPY_TEXT,"x")));val r=WorkflowRunner(port);r.run(p,p.fingerprint());assertEquals(0,acts);assertEquals(RunState.BLOCKED,r.snapshot.value.state)
    }

    @Test fun legacyClickableCheckboxMappingRemainsUsableAfterTypedDiscovery(){
        val old=mapping.copy(identity=identity.copy(role="android.widget.CheckBox",structure=""))
        val checkbox=candidate.copy(identity=old.identity,kind=ElementKind.TOGGLE)
        assertEquals(MappingStatus.FOUND,MappingPolicy.inspect(old,snapshot(checkbox),1000).status)
    }

    @Test fun launchToWaitRecoversFromLoadingOrPreviousForegroundWithoutActing()=runTest {
        var observations=0
        val ready=awaitMappingScope(mapping.scope,3000){
            observations++
            if(observations==1)error("app loading")
            if(observations==2)snapshot(candidate).copy(scope=MappingScope(MappingPolicy.BYGA,screen="mapping-test")) else snapshot(candidate)
        }
        assertEquals(mapping.scope,ready.scope);assertEquals(3,observations)
    }
    @Test fun stoppingScopeWaitCancelsFurtherObservations()=runTest {
        var observations=0
        val job=launch{awaitMappingScope(mapping.scope){observations++;snapshot(candidate).copy(scope=MappingScope(MappingPolicy.BYGA,screen="mapping-test"))}}
        runCurrent();job.cancel();job.join();advanceTimeBy(5000);assertEquals(1,observations)
    }

}
