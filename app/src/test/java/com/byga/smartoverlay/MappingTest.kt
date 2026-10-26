package com.byga.smartoverlay

import com.byga.smartoverlay.mapping.*
import com.byga.smartoverlay.workflow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MappingTest {
    private val native=MappingScope(MappingPolicy.CHATGPT,screen="ChatActivity")
    private val identity=TargetIdentity("app:id/send","Kirim","Kirim prompt","android.widget.Button","app:id/composer")
    private fun candidate(key:String="1",bounds:TargetBounds=TargetBounds(10,20,90,80))=MappingCandidate(key,identity,bounds)
    private fun snapshot(scope:MappingScope=native,candidates:List<MappingCandidate> = listOf(candidate()),time:Long=1000)=MappingSnapshot(scope,candidates,1080,2400,0,"layout-a",time)
    private fun mapping()=ButtonMapping(id="saved",name="Kirim",scope=native,identity=identity)
    private fun point()=mapping().copy(kind=MappingKind.POINT,x=0.5f,y=0.8f,width=1080,height=2400,rotation=0,layoutHash="layout-a",askEveryTime=true)
    @Test fun semanticMappingSurvivesMovedButton(){val moved=candidate(bounds=TargetBounds(800,1800,1000,1900));assertEquals(moved,MappingPolicy.resolve(mapping(),snapshot(candidates=listOf(moved)),1000))}
    @Test fun duplicateButtonsAreNotGuessed(){assertNull(MappingPolicy.resolve(mapping(),snapshot(candidates=listOf(candidate(),candidate("2",TargetBounds(200,200,250,250)))),1000))}
    @Test fun sameLabelWrongResourceIsRejected(){assertNull(MappingPolicy.resolve(mapping(),snapshot(candidates=listOf(candidate().copy(identity=identity.copy(resourceId="app:id/delete")))),1000))}
    @Test fun changedTextIsRejected(){assertNull(MappingPolicy.resolve(mapping(),snapshot(candidates=listOf(candidate().copy(identity=identity.copy(label="Bayar")))),1000))}
    @Test fun changedParentIsRejected(){assertNull(MappingPolicy.resolve(mapping(),snapshot(candidates=listOf(candidate().copy(identity=identity.copy(parentId="app:id/other")))),1000))}
    @Test fun changedRoleIsRejected(){assertNull(MappingPolicy.resolve(mapping(),snapshot(candidates=listOf(candidate().copy(identity=identity.copy(role="android.widget.EditText")))),1000))}
    @Test fun nativeAndChromeMappingsAreSeparate(){assertNull(MappingPolicy.resolve(mapping(),snapshot(scope=MappingScope(MappingPolicy.CHROME,"https://chatgpt.com","chat")),1000))}
    @Test fun wrongNativeScreenIsRejected(){assertNull(MappingPolicy.resolve(mapping(),snapshot(scope=native.copy(screen="SettingsActivity")),1000))}
    @Test fun missingTargetIsRejected(){assertNull(MappingPolicy.resolve(mapping(),snapshot(candidates=emptyList()),1000))}
    @Test fun disabledMappingCannotRun(){assertNull(MappingPolicy.resolve(mapping().copy(enabled=false),snapshot(),1000))}
    @Test fun staleAndFutureSnapshotsAreRejected(){assertNull(MappingPolicy.resolve(mapping(),snapshot(),4001));assertNull(MappingPolicy.resolve(mapping(),snapshot(),999))}
    @Test fun manualPointCannotOptOutOfConfirmation(){assertFalse(point().copy(askEveryTime=false).validate());assertTrue(point().validate())}
    @Test fun manualPointRejectsMovedLayout(){assertFalse(MappingPolicy.pointAllowed(point(),snapshot().copy(layoutHash="other"),1000))}
    @Test fun manualPointRejectsRotationAndResizing(){assertFalse(MappingPolicy.pointAllowed(point(),snapshot().copy(rotation=1),1000));assertFalse(MappingPolicy.pointAllowed(point(),snapshot().copy(width=2400,height=1080),1000))}
    @Test fun manualPointIsBoundToSite(){assertFalse(MappingPolicy.pointAllowed(point(),snapshot(scope=MappingScope(MappingPolicy.CHROME,"https://chatgpt.com","chat")),1000))}
    @Test fun directPointChoosesSmallestContainingButton(){val small=candidate("small",TargetBounds(20,30,40,50));val s=snapshot(candidates=listOf(candidate(),small));assertEquals(small,MappingPolicy.candidateAt(s,25f,35f));assertNull(MappingPolicy.candidateAt(s,500f,500f))}
    @Test fun permitIsSingleUse(){val p=MappingClickPermit(mapping().fingerprint(),1000);assertTrue(p.claim(mapping(),1000));assertFalse(p.claim(mapping(),1001))}
    @Test fun permitCannotApplyToChangedMapping(){val p=MappingClickPermit(mapping().fingerprint(),1000);assertFalse(p.claim(mapping().copy(name="Lain"),1000))}
    @Test fun permitsExpireAndRejectClockReversal(){assertFalse(MappingClickPermit(mapping().fingerprint(),1000).claim(mapping(),16001));assertFalse(MappingClickPermit(mapping().fingerprint(),1000).claim(mapping(),999))}
    @Test fun nativeAndPointMappingsRoundTrip(){assertEquals(mapping(),ButtonMapping.decode(mapping().encode()));assertEquals(point(),ButtonMapping.decode(point().encode()))}
    @Test fun mappingEditsChangeBoundWorkflowVersion(){val m=mapping();val p=Plan("x",listOf(Step(Action.CLICK_MAPPING,m.id,m.scope.packageName,m.fingerprint())));assertTrue(p.validate().isEmpty());assertNotEquals(m.fingerprint(),m.copy(askEveryTime=true).fingerprint());assertNotEquals(m.fingerprint(),m.copy(enabled=false).fingerprint());assertEquals(p,WorkflowCodec.decode(WorkflowCodec.encode(p)))}
    @Test fun workflowNeedsMappingVersion(){assertTrue(Plan("x",listOf(Step(Action.CLICK_MAPPING,"saved",native.packageName))).validate().isNotEmpty())}
    @Test fun exactBrowserSitesOnly(){assertEquals("chat",MappingPolicy.site("chatgpt.com/c/123")?.second);assertEquals("flow-project",MappingPolicy.site("https://labs.google/fx/tools/flow/project/a")?.second);listOf("https://chatgpt.com.evil/","https://chatgpt.com@evil.com/","http://chatgpt.com/","https://chatgpt.com:8443/","https://labs.google/fx/tools/flow-evil","https://labs.google/fx/tools/flow/../../other","https://accounts.google.com/","https://chatgpt.com/auth/login","javascript:alert(1)").forEach{assertNull(it,MappingPolicy.site(it))}}
    @Test fun unsupportedAppsAndUnscopedBrowserAreRejected(){assertFalse(mapping().copy(scope=MappingScope("com.bank.app",screen="Main")).validate());assertFalse(mapping().copy(scope=MappingScope(MappingPolicy.CHROME,screen="Main")).validate())}
    @Test fun noIdentifiableTargetCannotBecomeSemanticMapping(){assertFalse(mapping().copy(identity=TargetIdentity()).validate())}
    @Test fun failedMappedClickPersistsUncertaintyAndNeverRetries()=runTest {
        val m=mapping();val plan=Plan("x",listOf(Step(Action.CLICK_MAPPING,m.id,m.scope.packageName,m.fingerprint()),Step(Action.COPY_TEXT,"next")))
        var attempts=0;var uncertain=false
        val runner=WorkflowRunner(object:WorkflowPort {
            override suspend fun ready(step:Step)=true
            override suspend fun act(step:Step,previous:String):String{attempts++;error("ambiguous")}
            override suspend fun checkpoint(plan:Plan,next:Int,pending:Boolean,output:String){uncertain=pending}
        })
        runner.run(plan,plan.fingerprint());assertEquals(1,attempts);assertTrue(uncertain);assertEquals(RunState.BLOCKED,runner.snapshot.value.state)
    }
}
