package com.byga.smartoverlay

import com.byga.smartoverlay.overlay.FrameUpdateGate
import com.byga.smartoverlay.performance.FrameAccumulator
import com.byga.smartoverlay.ui.RobotBitmaps
import org.junit.Assert.*
import org.junit.Test

class FrameUpdateGateTest {
    @Test fun fiveHundredMotionEventsProduceOneUpdateWithLatestPosition() {
        val callbacks=mutableListOf<Runnable>();var position=0;var shown=0;var updates=0
        val gate=FrameUpdateGate({callbacks.add(it)},{callbacks.remove(it)},{shown=position;updates++})
        repeat(500){position=it;gate.request()}
        assertEquals(0,updates);assertEquals(1,callbacks.size)
        callbacks.removeAt(0).run()
        assertEquals(499,shown);assertEquals(1,updates)
        position=501;gate.request();assertEquals(1,callbacks.size)
        gate.flush();assertEquals(501,shown);assertTrue(callbacks.isEmpty())
    }
    @Test fun stopRemovesQueuedWindowWorkAndIdleDoesNotScheduleFrames() {
        val callbacks=mutableListOf<Runnable>();var updates=0
        val gate=FrameUpdateGate({callbacks.add(it)},{callbacks.remove(it)},{updates++})
        assertTrue(callbacks.isEmpty());gate.request();gate.cancel()
        assertTrue(callbacks.isEmpty());assertEquals(0,updates)
    }
    @Test fun iconDecoderDoesNotAllocateTheFullResolutionForSmallRobot() {
        assertEquals(8,RobotBitmaps.sampleFor(80))
        assertEquals(4,RobotBitmaps.sampleFor(248))
    }
    @Test fun frameHistogramIsBoundedAndReportsSlowAndFrozenFrames() {
        val frames=FrameAccumulator()
        repeat(95){frames.add(16_000_000)}
        repeat(4){frames.add(40_000_000)}
        frames.add(800_000_000)
        val s=frames.snapshot()
        assertEquals(100L,s.frames);assertEquals(5L,s.over32ms);assertEquals(1L,s.frozen)
        assertEquals(16,s.p95ms);assertEquals(800L,s.maxMs)
        frames.reset();assertEquals(0L,frames.snapshot().frames)
    }
}
