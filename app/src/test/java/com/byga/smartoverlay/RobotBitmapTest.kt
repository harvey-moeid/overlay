package com.byga.smartoverlay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.byga.smartoverlay.ui.RobotBitmaps
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RobotBitmapTest {
    @Test fun cachedTexturesMatchDisplayBudgetWithoutReusingALowerResolution() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val small=RobotBitmaps.load(context,96)
        assertEquals(96,maxOf(small.width,small.height))
        assertSame(small,RobotBitmaps.load(context,96))
        // Both requests have the same decode sample, but need different final textures.
        val larger=RobotBitmaps.load(context,128)
        assertEquals(128,maxOf(larger.width,larger.height))
        assertNotSame(small,larger)
        assertFalse(small.isRecycled)
        assertFalse(larger.isRecycled)
    }
}
