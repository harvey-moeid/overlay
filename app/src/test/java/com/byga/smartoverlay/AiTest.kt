package com.byga.smartoverlay

import com.byga.smartoverlay.ai.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class AiTest {
    @Test fun fallbackSkipsFailure() = runTest {
        val broken=object: AiProviderAdapter { override suspend fun text(prompt: String)=AiResult.Failure("TIMEOUT") }
        assertEquals(AiResult.Text("Konsep lokal: halo"),Fallback(listOf(broken,FakeProvider())).text("halo"))
    }
    @Test fun emptyFallbackFailsHonestly() = runTest { assertTrue(Fallback(emptyList()).text("x") is AiResult.Failure) }
    @Test fun fallbackIsBounded() = runTest {
        var calls=0
        val broken=object: AiProviderAdapter { override suspend fun text(prompt: String): AiResult { calls++; return AiResult.Failure("RATE_LIMIT") } }
        Fallback(List(10){broken}).text("x"); assertEquals(3,calls)
    }
    @Test fun cancellationNeverFallsThrough() = runTest {
        val cancelled=object: AiProviderAdapter { override suspend fun text(prompt: String): AiResult = throw CancellationException() }
        try { Fallback(listOf(cancelled,FakeProvider())).text("x"); fail("must cancel") } catch (_: CancellationException) { }
    }
}
