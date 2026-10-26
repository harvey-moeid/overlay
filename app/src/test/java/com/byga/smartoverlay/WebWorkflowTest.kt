package com.byga.smartoverlay

import com.byga.smartoverlay.workflow.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

class WebWorkflowTest {
    private fun flow()=Plan("Flow",listOf(Step(Action.OPEN_FLOW),Step(Action.FLOW_CREATE_PROJECT),
        Step(Action.FLOW_UPLOAD_IMAGE,"content://media/external/images/media/1"),
        Step(Action.FLOW_SET_PROMPT,"Video sinematik"),Step(Action.FLOW_GENERATE),Step(Action.FLOW_DOWNLOAD_VIDEO)))
    @Test fun completeFlowIsValid(){assertTrue(flow().validate().isEmpty())}
    @Test fun missingImageIsActionable(){assertTrue(flow().copy(steps=flow().steps.map{if(it.action==Action.FLOW_UPLOAD_IMAGE)it.copy(value="") else it}).validate().any{it.contains("pilih gambar")})}
    @Test fun arbitraryFileCannotBeUploaded(){assertTrue(flow().copy(steps=flow().steps.map{if(it.action==Action.FLOW_UPLOAD_IMAGE)it.copy(value="file:///secret") else it}).validate().isNotEmpty())}
    @Test fun generatedImageCanFeedFlow(){assertTrue(WorkflowCodec.templates.first{it.name.startsWith("ChatGPT gambar")}.validate().isEmpty())}
    @Test fun resultCannotPrecedePrompt(){assertTrue(Plan("x",listOf(Step(Action.OPEN_CHATGPT),Step(Action.CHATGPT_COLLECT_RESULT))).validate().isNotEmpty())}
    @Test fun flowDownloadRequiresGenerate(){assertTrue(flow().copy(steps=flow().steps.filter{it.action!=Action.FLOW_GENERATE}).validate().isNotEmpty())}
    @Test fun newProjectResetsPrompt(){assertTrue(flow().copy(steps=flow().steps+Step(Action.FLOW_CREATE_PROJECT)+Step(Action.FLOW_GENERATE)).validate().isNotEmpty())}
    @Test fun chatCannotRunOnFlow(){assertTrue(Plan("x",listOf(Step(Action.OPEN_FLOW),Step(Action.CHATGPT_SEND_PROMPT,"hello"))).validate().isNotEmpty())}
    @Test fun browserCommandsRoundTrip(){assertEquals(flow(),WorkflowCodec.decode(WorkflowCodec.encode(flow())))}
    @Test fun changingUploadRevokesApproval(){val p=flow();assertFalse(Guard.canRun(p.copy(steps=p.steps.map{if(it.action==Action.FLOW_UPLOAD_IMAGE)it.copy(value="content://other/2") else it}),p.fingerprint()))}
    @Test fun exactOriginsAndPathsOnly(){
        assertTrue(WebWorkflowPolicy.permits(Action.FLOW_GENERATE,"https://labs.google/fx/tools/flow/project/123"))
        assertTrue(WebWorkflowPolicy.permits(Action.CHATGPT_SEND_PROMPT,"https://chatgpt.com/c/123"))
        listOf("https://labs.google.evil/fx/tools/flow","https://labs.google/fx/tools/flow-evil","https://labs.google:8443/fx/tools/flow","https://a@labs.google/fx/tools/flow","http://labs.google/fx/tools/flow","https://accounts.google.com/").forEach{assertFalse(it,WebWorkflowPolicy.permits(Action.FLOW_GENERATE,it))}
        assertFalse(WebWorkflowPolicy.permits(Action.CHATGPT_SEND_PROMPT,"https://chatgpt.com.evil/"))
        assertFalse(WebWorkflowPolicy.permits(Action.LAUNCH_APP,"https://labs.google/fx/tools/flow"))
        assertFalse(WebWorkflowPolicy.permits(Action.FLOW_GENERATE,"https://labs.google/fx/tools/flow/../../other"))
    }
    @Test fun loginPagesAreNavigableButNeverAutomated(){assertTrue(WebWorkflowPolicy.navigable("https://accounts.google.com/"));assertFalse(WebWorkflowPolicy.permits(Action.FLOW_GENERATE,"https://accounts.google.com/"));assertFalse(WebWorkflowPolicy.navigable("intent://foo"));assertFalse(WebWorkflowPolicy.navigable("https://example.com/"))}
    @Test fun failedGenerateIsNotRetried()=runTest {
        val calls=mutableListOf<Action>();val checkpoints=mutableListOf<SavedCheckpoint>()
        val port=object:WorkflowPort {
            override suspend fun ready(step:Step)=true
            override suspend fun act(step:Step,previous:String):String { calls.add(step.action);if(step.action==Action.FLOW_GENERATE)error("verification failed");return previous }
            override suspend fun checkpoint(plan:Plan,next:Int,uncertain:Boolean,output:String){checkpoints.add(SavedCheckpoint(plan.fingerprint(),next,uncertain,output))}
        }
        val p=flow();val runner=WorkflowRunner(port);runner.run(p,p.fingerprint())
        assertEquals(1,calls.count{it==Action.FLOW_GENERATE});assertFalse(calls.contains(Action.FLOW_DOWNLOAD_VIDEO))
        assertTrue(checkpoints.last().uncertain);assertEquals(RunState.BLOCKED,runner.snapshot.value.state)
    }
    @Test fun webTimeoutLeavesUncertainCheckpointWithoutReplay()=runTest {
        var attempts=0;var uncertain=false
        val port=object:WorkflowPort {
            override suspend fun ready(step:Step)=true
            override suspend fun act(step:Step,previous:String):String { attempts++;delay(600001);return "unexpected" }
            override suspend fun checkpoint(plan:Plan,next:Int,pending:Boolean,output:String){uncertain=pending}
        }
        val plan=Plan("x",listOf(Step(Action.OPEN_FLOW)));val runner=WorkflowRunner(port)
        runner.run(plan,plan.fingerprint());assertEquals(1,attempts);assertTrue(uncertain);assertEquals(RunState.BLOCKED,runner.snapshot.value.state);assertTrue(runner.snapshot.value.note.contains("Batas waktu"))
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun pauseResumeKeepsHumanApprovalGate()=runTest {
        var acts=0
        val port=object:WorkflowPort {
            override suspend fun ready(step:Step)=true
            override suspend fun act(step:Step,previous:String):String { acts++;return "done" }
            override suspend fun checkpoint(plan:Plan,next:Int,uncertain:Boolean,output:String)=Unit
        }
        val plan=Plan("x",listOf(Step(Action.REQUEST_USER_APPROVAL,"review"),Step(Action.COPY_TEXT,"x")));val runner=WorkflowRunner(port)
        val job=launch{runner.run(plan,plan.fingerprint())};runCurrent();runner.pause();runner.resume()
        assertEquals(RunState.WAITING_FOR_USER,runner.snapshot.value.state);assertEquals(0,acts)
        runner.approve();job.join();assertEquals(1,acts)
    }
}
