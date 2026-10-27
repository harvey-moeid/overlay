package com.byga.smartoverlay

import com.byga.smartoverlay.overlay.BubbleGesture
import com.byga.smartoverlay.overlay.BubbleGesture.Release
import org.junit.Assert.*
import org.junit.Test

class BubbleGestureTest {
    @Test fun delayedFirstMoveAfterHoldDeadlineIsStillADrag() {
        val g=BubbleGesture(8,500)
        g.down(10f,100f,0)
        // Delivery may stall for >500ms; there is no timer that can stop the bubble.
        g.move(200f,100f)
        assertEquals(Release.DRAG,g.up(400f,100f,1500))
    }
    @Test fun longDragReturningToStartCannotStopOrOpenPanel() {
        val g=BubbleGesture(8,500)
        g.down(10f,100f,0);g.move(300f,400f);g.move(10f,100f)
        assertEquals(Release.DRAG,g.up(10f,100f,2000))
    }
    @Test fun stationaryHoldStopsOnReleaseAndShortTapOpensPanel() {
        val g=BubbleGesture(8,500)
        g.down(10f,100f,100)
        assertEquals(Release.HOLD,g.up(12f,101f,700))
        g.down(10f,100f,1000)
        assertEquals(Release.TAP,g.up(10f,100f,1100))
    }
    @Test fun cancellationCannotTriggerHoldAndNewGestureResetsDrag() {
        val g=BubbleGesture(8,500)
        g.down(10f,100f,0);g.move(300f,400f);g.cancel()
        assertEquals(Release.NONE,g.up(10f,100f,2000))
        g.down(10f,100f,3000)
        assertEquals(Release.TAP,g.up(10f,100f,3100))
    }
    @Test fun releaseDisplacementCountsWhenMoveWasNotDelivered() {
        val g=BubbleGesture(8,500)
        g.down(10f,100f,0)
        assertEquals(Release.DRAG,g.up(300f,100f,1500))
    }
}
