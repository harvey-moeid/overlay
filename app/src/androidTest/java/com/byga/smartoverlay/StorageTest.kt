package com.byga.smartoverlay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.agent.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageTest {
    @Test fun roomSurvivesCloseAndReopen() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="test-${System.nanoTime()}.db"
        val first=Room.databaseBuilder(context,BygaDatabase::class.java,name).build()
        first.records().save(Record("checkpoint","run","step:2")); first.close()
        val reopened=Room.databaseBuilder(context,BygaDatabase::class.java,name).build()
        assertEquals("step:2",reopened.records().get("checkpoint","run")?.body)
        reopened.close(); context.deleteDatabase(name)
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
