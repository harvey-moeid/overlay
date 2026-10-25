package com.byga.smartoverlay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.byga.smartoverlay.data.*
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
    @Test fun encryptedSecretRoundTripAndDelete() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=SecretStore(context); val id="instrumented-test"
        try { store.save(id,"fake-value-no-real-credential"); assertEquals("fake-value-no-real-credential",store.read(id)); store.delete(id); assertNull(store.read(id)) } finally { store.delete(id) }
    }
}
