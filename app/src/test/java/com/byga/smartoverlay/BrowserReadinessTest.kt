package com.byga.smartoverlay

import com.byga.smartoverlay.workflow.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserReadinessTest {
    @Test fun failedPageStopsImmediatelyInsteadOfWaitingTenMinutes() = runTest {
        try { awaitBrowserReady({BrowserReadiness.FAILED},{fail("Should fail immediately")},clock={currentTime});fail() }
        catch(e:IllegalStateException) { assertTrue(e.message!!.contains("Halaman gagal")) }
        assertEquals(0L,currentTime)
    }
    @Test fun loginWaitIsBoundedAndExplainsRequiredAction() = runTest {
        var note=""
        try { awaitBrowserReady({BrowserReadiness.LOGIN},{note=it},clock={currentTime});fail() }
        catch(e:IllegalStateException) { assertTrue(e.message!!.contains("Batas waktu")) }
        assertEquals(60_000L,currentTime)
        assertTrue(note.contains("login"))
    }
    @Test fun loadingCanBecomeReadyWithoutRetryingAnyAction() = runTest {
        var state=BrowserReadiness.LOADING;var updates=0
        val wait=launch { awaitBrowserReady({state},{updates++},clock={currentTime}) }
        runCurrent();assertEquals(1,updates)
        state=BrowserReadiness.READY;advanceUntilIdle();wait.join()
        assertTrue(wait.isCompleted)
    }
    @Test fun stopCancelsReadinessWaitImmediately() = runTest {
        val wait=launch { awaitBrowserReady({BrowserReadiness.BACKGROUND},{},clock={currentTime}) }
        runCurrent();wait.cancelAndJoin();assertTrue(wait.isCancelled)
        assertEquals(0L,currentTime)
    }
    @Test fun explicitPauseDoesNotConsumeReadinessTime() = runTest {
        var state=BrowserReadiness.PAUSED
        val wait=launch{awaitBrowserReady({state},{},clock={currentTime})}
        advanceTimeBy(120_000);runCurrent();assertTrue(wait.isActive)
        state=BrowserReadiness.READY;advanceUntilIdle();wait.join()
        assertFalse(wait.isCancelled)
    }
    @Test fun parentCancellationIsNotConvertedToPageTimeout() = runTest {
        var timedOut=false
        try { withTimeout(100) { awaitBrowserReady({BrowserReadiness.LOADING},{},clock={currentTime}) } }
        catch(_:TimeoutCancellationException) { timedOut=true }
        assertTrue(timedOut);assertEquals(100L,currentTime)
    }
}
