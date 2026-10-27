package com.byga.smartoverlay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.agent.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageTest {
    @Test fun historyHeadersPageWithoutReadingConversationPayloads() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,BygaDatabase::class.java).build()
        try {
            val dao=db.records()
            repeat(25){index->
                dao.save(Record("chat",index.toString(),"large conversation ".repeat(10_000),index.toLong()))
                if(index!=0)dao.save(Record("chat_title",index.toString(),"Title $index"))
            }
            val first=dao.chatHeaders(8,0).first()
            assertEquals(8,first.size);assertEquals("24",first.first().id)
            assertTrue(first.all{it.body.length<100})
            val last=dao.chatHeaders(8,24).first()
            assertEquals(1,last.size);assertEquals("0",last.single().id)
            assertEquals("Percakapan tersimpan",last.single().body)
        }finally{db.close()}
    }
    @Test fun roomSurvivesCloseAndReopen() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="test-${System.nanoTime()}.db"
        val first=Room.databaseBuilder(context,BygaDatabase::class.java,name).build()
        first.records().save(Record("checkpoint","run","step:2")); first.close()
        val reopened=Room.databaseBuilder(context,BygaDatabase::class.java,name).build()
        assertEquals("step:2",reopened.records().get("checkpoint","run")?.body)
        reopened.close(); context.deleteDatabase(name);Unit
    }
    @Test fun encryptedSecretRoundTripAndDelete() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=SecretStore(context); val id="instrumented-test"
        try { store.save(id,"fake-value-no-real-credential"); assertEquals("fake-value-no-real-credential",store.read(id)); store.delete(id); assertNull(store.read(id)) } finally { store.delete(id) }
    }
    @Test fun migrationPreservesV1AndMemoryDeletionWorks() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="migration-${System.nanoTime()}.db"
        context.openOrCreateDatabase(name,0,null).use { old ->
            old.execSQL("CREATE TABLE records (kind TEXT NOT NULL, id TEXT NOT NULL, body TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(kind,id))")
            old.execSQL("INSERT INTO records VALUES ('workflow','old','preserved',1)")
            old.version=1
        }
        val db=Room.databaseBuilder(context,BygaDatabase::class.java,name).addMigrations(AGENT_MIGRATION_1_2).build()
        try {
            assertEquals("preserved",db.records().get("workflow","old")?.body)
            db.agent().saveMemory(MemoryEntry("a","caption singkat",1))
            assertEquals(1,db.agent().searchMemory("caption").size)
            db.agent().forgetAll()
            assertTrue(db.agent().searchMemory("caption").isEmpty())
            val p=PlanCodec.local("hello","com.byga.smartoverlay")
            assertEquals(p,PlanCodec.decode(PlanCodec.encode(p)))
            try { PlanCodec.decode("{\"goal\":\"x\",\"calls\":[],\"scope\":\"admin\"}"); fail("Unknown fields must be rejected") } catch(_: Exception) { }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
