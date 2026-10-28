package com.byga.smartoverlay

import com.byga.smartoverlay.media.withCancellation
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class DownloadCancellationTest {
    @Test fun stopClosesNetworkCallWhileWaitingForHeaders()=runBlocking {
        val server=MockWebServer()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));server.start()
        val call=OkHttpClient().newCall(Request.Builder().url(server.url("/fixture")).build())
        try {
            val job=launch(Dispatchers.IO){call.withCancellation{it.body?.string()}}
            assertNotNull(server.takeRequest(5,TimeUnit.SECONDS))
            job.cancel();withTimeout(3000){job.join()}
            assertTrue(call.isCanceled())
        }finally{call.cancel();server.shutdown()}
    }
    @Test fun stopAlsoClosesNetworkCallDuringBodyRead()=runBlocking {
        val server=MockWebServer()
        server.enqueue(MockResponse().setBody("partial body").throttleBody(1,1,TimeUnit.SECONDS));server.start()
        val call=OkHttpClient().newCall(Request.Builder().url(server.url("/fixture")).build())
        try {
            val entered=CompletableDeferred<Unit>()
            val job=launch(Dispatchers.IO){call.withCancellation{entered.complete(Unit);it.body?.string()}}
            withTimeout(5000){entered.await()}
            job.cancel();withTimeout(3000){job.join()};assertTrue(call.isCanceled())
        }finally{call.cancel();server.shutdown()}
    }
}
