package com.byga.smartoverlay

import android.view.WindowManager
import com.byga.smartoverlay.overlay.*
import com.byga.smartoverlay.ui.PagedItems
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class OverlayRegressionTest {
    @get:Rule val compose=createComposeRule()

    @Test fun compactBubbleKeepsAnAccessibleTouchTarget() {
        assertEquals(28,OverlayAppearance.DEFAULT_DP)
        assertEquals(48,OverlayAppearance.touchDp(28))
        assertEquals(24,OverlayAppearance.visualDp(0))
        assertEquals(56,OverlayAppearance.visualDp(128))
    }
    @Test fun panelsAllowOutsideTouchesWithoutDisablingTextInput() {
        val flags=OverlayAppearance.PANEL_FLAGS
        assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL != 0)
        assertTrue(flags and WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH != 0)
        assertEquals(0,flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }
    @Test fun largeLibraryOnlyComposesOnePageAndCanNavigateToAllItems() {
        compose.setContent { PagedItems((0 until 1000).toList()) { Text("Item $it") } }
        compose.onNodeWithText("Item 0").assertExists()
        compose.onNodeWithText("Item 8").assertDoesNotExist()
        compose.onNodeWithText("Berikutnya").performClick()
        compose.onNodeWithText("Item 0").assertDoesNotExist()
        compose.onNodeWithText("Item 8").assertExists()
        compose.onNodeWithText("Sebelumnya").performClick()
        compose.onNodeWithText("Item 0").assertExists()
    }
}
