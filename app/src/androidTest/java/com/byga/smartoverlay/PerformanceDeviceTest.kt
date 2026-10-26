package com.byga.smartoverlay

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.byga.smartoverlay.overlay.OverlayService
import com.byga.smartoverlay.performance.UiPerformance
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Runs against the optimized, non-debuggable APK on an actual Android emulator/device. */
@RunWith(AndroidJUnit4::class)
class PerformanceDeviceTest {
    @Test fun scrollingBubbleAndOutsideTouchesRemainResponsive() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val device=UiDevice.getInstance(instrumentation)
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.textContains("Ide besar.")),30_000))
        val x=device.displayWidth/2;val top=device.displayHeight/3;val bottom=device.displayHeight*3/4
        repeat(2){device.swipe(x,bottom,x,top,30);device.swipe(x,top,x,bottom,30)}
        device.waitForIdle()
        UiPerformance.frames.reset()
        repeat(8){device.swipe(x,bottom,x,top,30);device.swipe(x,top,x,bottom,30)}
        device.waitForIdle()
        val frames=UiPerformance.frames.snapshot()
        assertTrue("Real frame samples were received: $frames",frames.frames>=10)
        assertEquals("No frozen frame during warmed scrolling: $frames",0L,frames.frozen)
        assertTrue("Coarse emulator regression threshold, not a 60fps guarantee: $frames",frames.p95ms<100)

        device.executeShellCommand("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        var panelMs=0L
        try {
            context.startForegroundService(Intent(context,OverlayService::class.java))
            val selector=By.descStartsWith("Robot BYGA:")
            assertTrue("Bubble attached",device.wait(Until.hasObject(selector),10_000))
            repeat(5) {
                val bounds=device.findObject(selector).visibleBounds
                device.swipe(bounds.centerX(),bounds.centerY(),device.displayWidth*3/4,device.displayHeight/3,90)
                device.waitForIdle()
                assertNotNull("Bubble remains after drag",device.findObject(selector))
            }
            val start=SystemClock.elapsedRealtime()
            device.findObject(selector).click()
            assertTrue("Quick panel opens without accessibility scan",device.wait(Until.hasObject(By.text("BYGA  /  QUICK STUDIO")),3000))
            panelMs=SystemClock.elapsedRealtime()-start
            device.findObject(By.text("Chat")).click()
            assertTrue("Outside touch reaches the app",device.wait(Until.hasObject(By.textContains("Dari ide ke draft.")),5000))
            assertTrue("Outside tap closes panel",device.wait(Until.gone(By.text("BYGA  /  QUICK STUDIO")),3000))
        } finally { context.stopService(Intent(context,OverlayService::class.java)) }
        val report=JSONObject().put("version",BuildConfig.VERSION_NAME).put("buildType",BuildConfig.BUILD_TYPE)
            .put("frames",frames.frames).put("over32ms",frames.over32ms).put("frozen",frames.frozen)
            .put("p95ms",frames.p95ms).put("maxMs",frames.maxMs).put("quickPanelMs",panelMs)
        File(context.getExternalFilesDir(null),"performance-smoke.json").writeText(report.toString(2))
    }
}
