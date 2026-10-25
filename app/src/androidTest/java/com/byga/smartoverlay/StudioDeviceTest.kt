package com.byga.smartoverlay

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import androidx.room.Room
import com.byga.smartoverlay.data.BygaDatabase
import com.byga.smartoverlay.ai.BudgetLedger
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StudioDeviceTest {
    @Test fun budgetReservationsAreAtomicAndPersisted()=runBlocking{
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,BygaDatabase::class.java).build()
        try{val results=(1..20).map{async(Dispatchers.IO){runCatching{BudgetLedger.reserve(db,"test-day",12)}.isSuccess}}.awaitAll();assertEquals(12,results.count{it});assertEquals("12",db.records().get("ai_request_count","test-day")?.body)}finally{db.close()}
    }
    @Test fun clearingLogsPreservesPromptsAndProviderMetadata()=runBlocking{
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,BygaDatabase::class.java).build()
        try{
            val dao=db.records()
            dao.save(com.byga.smartoverlay.data.Record("prompt","p","template"))
            dao.save(com.byga.smartoverlay.data.Record("provider","k","metadata-only"))
            dao.save(com.byga.smartoverlay.data.Record("diagnostic","d","result"))
            dao.save(com.byga.smartoverlay.data.Record("execution","e","result"))
            dao.clearHistory()
            assertNotNull(dao.get("prompt","p"));assertNotNull(dao.get("provider","k"));assertNull(dao.get("diagnostic","d"));assertNull(dao.get("execution","e"))
        }finally{db.close()}
    }
    @Test fun dashboardChatProvidersAndWorkflowRoutesOpenWithoutKeys(){
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext;val device=UiDevice.getInstance(instrumentation)
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue("Dashboard rendered",device.wait(Until.hasObject(By.textContains("Ide besar.")),60000))
        device.takeScreenshot(File(context.getExternalFilesDir(null),"byga-home.png"))
        device.findObject(By.text("Chat")).click()
        assertTrue("Real chat screen",device.wait(Until.hasObject(By.textContains("Dari ide ke draft.")),30000))
        device.takeScreenshot(File(context.getExternalFilesDir(null),"byga-chat.png"))
        val providers=device.findObject(By.text("Buka AI Providers"));assertNotNull(providers);providers.click()
        assertTrue("Provider editor",device.wait(Until.hasObject(By.text("Pilih provider & model")),30000))
        device.takeScreenshot(File(context.getExternalFilesDir(null),"byga-providers.png"))
        device.findObject(By.text("Alur")).click()
        assertTrue("Workflow editor route",device.wait(Until.hasObject(By.text("Dari satu langkah,\nke alur yang rapi.")),30000))
        device.takeScreenshot(File(context.getExternalFilesDir(null),"byga-workflows.png"))
    }
}
