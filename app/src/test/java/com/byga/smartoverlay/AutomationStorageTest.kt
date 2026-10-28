package com.byga.smartoverlay

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.byga.smartoverlay.agent.AGENT_MIGRATION_1_2
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.mapping.*
import com.byga.smartoverlay.workflow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class AutomationStorageTest {
    @Test fun realRoomMigrationPreservesMappingWorkflowAndUncertainCheckpoint()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="v2-migration-${System.nanoTime()}.db"
        val mapping=ButtonMapping(id="legacy",name="Latihan",scope=MappingScope(MappingPolicy.BYGA,screen="mapping-test"),identity=TargetIdentity("button"))
        val workflow=Plan("Preserved",listOf(Step(Action.CLICK_MAPPING,mapping.id,mapping.scope.packageName,mapping.fingerprint())))
        context.openOrCreateDatabase(name,0,null).use{old->
            old.execSQL("CREATE TABLE records (kind TEXT NOT NULL, id TEXT NOT NULL, body TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(kind,id))")
            old.execSQL("INSERT INTO records VALUES (?,?,?,1)",arrayOf("button_mapping",mapping.id,mapping.encode()))
            old.execSQL("INSERT INTO records VALUES (?,?,?,1)",arrayOf("workflow","w",WorkflowCodec.encode(workflow)))
            old.execSQL("INSERT INTO records VALUES ('checkpoint','w','uncertain',1)")
            old.version=1
        }
        val migrated=Room.databaseBuilder(context,BygaDatabase::class.java,name).addMigrations(AGENT_MIGRATION_1_2).build()
        try{
            assertEquals(mapping,ButtonMapping.decode(migrated.records().get("button_mapping",mapping.id)!!.body))
            assertEquals(workflow,WorkflowCodec.decode(migrated.records().get("workflow","w")!!.body))
            assertEquals("uncertain",migrated.records().get("checkpoint","w")?.body)
            migrated.records().save(Record("workflow_recording","r",WorkflowCodec.encode(workflow)))
        }finally{migrated.close()}
        val reopened=Room.databaseBuilder(context,BygaDatabase::class.java,name).build()
        try{assertEquals(workflow,WorkflowCodec.decode(reopened.records().get("workflow_recording","r")!!.body))}finally{reopened.close();context.deleteDatabase(name)}
    }
    @Test fun guidedSelectionsPersistDraftAndStopPreventsFurtherRecording()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val id=GuidedRecorder.start(context)
        val mapping=ButtonMapping(name="Latihan",scope=MappingScope(MappingPolicy.BYGA,screen="mapping-test"),identity=TargetIdentity("button"))
        val dao=BygaDatabase.get(context).records()
        try{
            assertTrue(GuidedRecorder.append(context,mapping))
            val draft=WorkflowCodec.decode(dao.get("workflow_recording",id)!!.body)
            assertEquals(listOf(Action.REQUEST_USER_APPROVAL,Action.WAIT_FOR_STATE,Action.CLICK_MAPPING),draft.steps.map{it.action})
            assertEquals(mapping.fingerprint(),draft.steps.last().selector)
            GuidedRecorder.stop()
            assertFalse(GuidedRecorder.append(context,mapping))
            assertEquals(draft,WorkflowCodec.decode(dao.get("workflow_recording",id)!!.body))
        }finally{GuidedRecorder.stop();dao.delete("workflow_recording",id)}
    }
    @Test fun nativeGrantSurvivesReloadAndRevocationBlocksDecode()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val pkg="com.example.notes"
        try{
            MappingAccess.grant(context,pkg)
            MappingPolicy.userPackages=emptySet();MappingAccess.load(context)
            assertTrue(pkg in MappingPolicy.packages)
            val mapping=ButtonMapping(name="Notes",scope=MappingScope(pkg,screen="Main"),identity=TargetIdentity("add"))
            assertTrue(mapping.validate())
            MappingAccess.revoke(context,pkg)
            assertFalse(mapping.validate())
        }finally{MappingAccess.revoke(context,pkg)}
    }
}
