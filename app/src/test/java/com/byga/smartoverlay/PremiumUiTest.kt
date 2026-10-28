package com.byga.smartoverlay

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w412dp-h915dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class PremiumUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun screenshot(name:String){
        val dir=File("build/reports/ui").apply{mkdirs()}
        // Robolectric has no device PixelCopy frame pump; draw the actual Android decor view on its native Canvas.
        lateinit var image:Bitmap
        compose.runOnIdle {
            val decor=compose.activity.window.decorView
            assertTrue("Measured window",decor.width>100 && decor.height>100)
            image=Bitmap.createBitmap(decor.width,decor.height,Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(image))
        }
        assertTrue("PNG produced",File(dir,name).outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)})
    }
    @Test fun premiumDashboardAndRealChatRoute(){
        compose.onNodeWithText("Ide besar.\nKendali penuh.").assertIsDisplayed()
        screenshot("byga-home.png")
        compose.onNodeWithText("Chat").performClick()
        compose.onNodeWithText("Dari ide ke draft.").assertIsDisplayed()
        compose.onNodeWithText("STOP").assertIsDisplayed()
        compose.onNodeWithText("Hubungkan provider pertama").assertExists()
        screenshot("byga-chat.png")
    }
    @Test fun workflowTemplatesAndInstalledAppStep(){
        compose.onNodeWithText("Alur").performClick()
        compose.onNode(hasText("ChatGPT → prompt → salin teks / unduh gambar") and hasSetTextAction().not()).assertExists()
        screenshot("byga-workflows.png")
        compose.onNodeWithText("Buka aplikasi terinstal").performScrollTo().performClick()
        compose.onNodeWithText("Pilih aplikasi terinstal").performScrollTo()
        compose.onNodeWithText("Belum memilih aplikasi").assertExists()
        screenshot("byga-app-step.png")
    }
    @Test fun smartMappingRouteAndPracticeButton(){
        compose.onNodeWithTag("dashboard-list").performScrollToNode(hasText("Smart Mapping"))
        compose.onNodeWithText("Smart Mapping").performClick()
        compose.onNodeWithText("Kenali tombol.\nRangkai tindakan.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Logo robot BYGA BG").assertExists()
        screenshot("byga-mapping.png")
        compose.onNodeWithText("Tombol latihan").performScrollTo().performClick()
        compose.onNodeWithText("Ditekan 1 kali").assertExists()
    }
    @Test fun workflowV2RecorderAndConditionEditorsAreReachable(){
        compose.onNodeWithText("Alur").performClick()
        compose.onNodeWithText("Start Recording").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("+ IF / ELSE berdasarkan output").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("ELSE: lewati 1–50 langkah berikutnya").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Dry run").performScrollTo().performClick()
        compose.onNodeWithText("Duplikasi workflow").assertExists()
    }

}
