package com.byga.smartoverlay

import android.content.Intent
import android.os.SystemClock
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
    // Real, slow gestures handle nested Compose scrollers and avoid skipping a button in a fling.
    private fun scrollTo(device:UiDevice,selector:BySelector):UiObject2 {
        for(direction in listOf(-1,1,-1))repeat(16) {
            device.wait(Until.findObject(selector),300)?.let { node ->
                val bounds=node.visibleBounds
                if(bounds.width()>0 && bounds.height()>0) {
                    // A clipped text node can sit beneath the fixed toolbar/navigation chrome.
                    // Scroll it into the content viewport, then requery after layout settles.
                    if(bounds.top<device.displayHeight*0.16) {
                        device.swipe(device.displayWidth/2,device.displayHeight*2/5,device.displayWidth/2,device.displayHeight*3/5,100)
                        device.waitForIdle();return@repeat
                    }
                    if(bounds.bottom>device.displayHeight*0.84) {
                        device.swipe(device.displayWidth/2,device.displayHeight*3/5,device.displayWidth/2,device.displayHeight*2/5,100)
                        device.waitForIdle();return@repeat
                    }
                    device.waitForIdle();SystemClock.sleep(150)
                    device.findObject(selector)?.let { fresh -> if(fresh.visibleBounds==bounds)return fresh }
                    return@repeat
                }
            }
            val low=device.displayHeight*3/4;val high=device.displayHeight*2/5
            device.swipe(device.displayWidth/2,if(direction<0)low else high,device.displayWidth/2,if(direction<0)high else low,100)
            device.waitForIdle()
        }
        val dir=InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        device.dumpWindowHierarchy(File(dir,"scroll-failure.xml"))
        device.takeScreenshot(File(dir,"scroll-failure.png"))
        throw AssertionError("Visible scroll target not found: $selector")
    }
    private fun expect(device:UiDevice,selector:BySelector,message:String,artifact:String) {
        if(device.wait(Until.hasObject(selector),10000))return
        val dir=InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        device.dumpWindowHierarchy(File(dir,"$artifact.xml"));device.takeScreenshot(File(dir,"$artifact.png"))
        fail(message)
    }
    @Test fun smartMappingRouteAndPracticeButton(){
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext;val device=UiDevice.getInstance(instrumentation)
        context.startActivity(Intent(context,MainActivity::class.java).putExtra("open_mapping",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.textContains("Kenali tombol.")),30000))
        device.takeScreenshot(File(context.getExternalFilesDir(null),"byga-smart-mapping.png"))
        scrollTo(device,By.desc("byga_mapping_demo_button")).click()
        expect(device,By.text("Ditekan 1 kali"),"One practice click must increment the counter","mapping-click-failure")
    }
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
        scrollTo(device,By.text("Buka aplikasi terinstal")).click()
        scrollTo(device,By.text("Pilih aplikasi terinstal")).click()
        expect(device,By.text("Cari nama aplikasi"),"Installed app search dialog","app-picker-failure")
        device.findObject(By.text("Tutup")).click()
    }
}
