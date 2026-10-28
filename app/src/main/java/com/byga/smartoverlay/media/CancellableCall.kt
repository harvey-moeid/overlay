package com.byga.smartoverlay.media

import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.Response

/** Keep cancellation connected through response-body streaming, not only until HTTP headers arrive. */
internal suspend fun <T> Call.withCancellation(read:suspend(Response)->T):T=coroutineScope {
    val watcher=launch(start=CoroutineStart.UNDISPATCHED) {try{awaitCancellation()}finally{this@withCancellation.cancel()}}
    try{execute().use{read(it)}}catch(e:Exception){currentCoroutineContext().ensureActive();throw e}finally{watcher.cancel()}
}
