package com.byga.smartoverlay.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.byga.smartoverlay.workflow.secureUrl
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

sealed class DownloadResult {
    data class Saved(val uri: Uri): DownloadResult()
    data class Blocked(val code: String): DownloadResult()
}
object DirectDownload {
    /** Copies a browser-session download after the same size/signature/file checks as URL media. */
    suspend fun saveLocal(context:Context,file:java.io.File,mime:String):DownloadResult=withContext(Dispatchers.IO) {
        if(Build.VERSION.SDK_INT<29)return@withContext DownloadResult.Blocked("NEEDS_ANDROID_10")
        var target:Uri?=null
        try {
            val extension=MediaPolicy.extensions[mime] ?: error("MIME_UNSUPPORTED")
            val size=file.length()
            check(MediaPolicy.acceptsSize(size)){"SIZE_LIMIT"}
            val header=file.inputStream().use{input->val bytes=ByteArray(32);bytes.copyOf(maxOf(0,input.read(bytes)))}
            check(MediaPolicy.matches(mime,header)){"SIGNATURE_MISMATCH"}
            val images=mime.startsWith("image/")
            target=checkNotNull(context.contentResolver.insert(if(images)MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI,ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME,"BYGA-${System.currentTimeMillis()}.$extension")
                put(MediaStore.MediaColumns.MIME_TYPE,mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH,if(images)"Pictures/BYGA" else "Movies/BYGA")
                put(MediaStore.MediaColumns.IS_PENDING,1)
            }))
            context.contentResolver.openOutputStream(target,"w").use { output ->
                checkNotNull(output)
                file.inputStream().use { input ->
                    val bytes=ByteArray(8192)
                    while(true) { currentCoroutineContext().ensureActive();val count=input.read(bytes);if(count<0)break;output.write(bytes,0,count) }
                }
            }
            context.contentResolver.openAssetFileDescriptor(target,"r")?.use{check(it.length==size){"INCOMPLETE_FILE"}} ?: error("FILE_MISSING")
            currentCoroutineContext().ensureActive()
            check(context.contentResolver.update(target,ContentValues().apply{put(MediaStore.MediaColumns.IS_PENDING,0)},null,null)==1)
            DownloadResult.Saved(target)
        } catch(e:CancellationException) { target?.let{context.contentResolver.delete(it,null,null)};throw e }
        catch(_:Exception) { target?.let{runCatching{context.contentResolver.delete(it,null,null)}};DownloadResult.Blocked("Media gagal diverifikasi. JPEG/PNG/WebP/MP4/WebM, maksimum 50MB, Android 10+.") }
    }
    private val client=OkHttpClient.Builder().callTimeout(90,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()
    suspend fun save(context: Context, url: String): DownloadResult = withContext(Dispatchers.IO) {
        if(Build.VERSION.SDK_INT<29) return@withContext DownloadResult.Blocked("NEEDS_ANDROID_10")
        if(!secureUrl(url)) return@withContext DownloadResult.Blocked("HTTPS_REQUIRED")
        var uri: Uri?=null
        try {
            client.newCall(Request.Builder().url(url).build()).withCancellation { response ->
                check(response.isSuccessful) { "HTTP_BLOCKED" }
                val body=checkNotNull(response.body)
                val mime=body.contentType()?.let { "${it.type}/${it.subtype}" } ?: ""
                val extension=MediaPolicy.extensions[mime] ?: error("MIME_UNSUPPORTED")
                check(body.contentLength()==-1L || MediaPolicy.acceptsSize(body.contentLength())) { "SIZE_LIMIT" }
                val source=body.source()
                source.request(32)
                val header=source.peek().readByteArray(minOf(32,source.buffer.size))
                check(MediaPolicy.matches(mime,header)) { "SIGNATURE_MISMATCH" }
                val collection=if(mime.startsWith("image/")) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val values=ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME,"BYGA-${System.currentTimeMillis()}.$extension")
                    put(MediaStore.MediaColumns.MIME_TYPE,mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH,if(mime.startsWith("image/")) "Pictures/BYGA" else "Movies/BYGA")
                    put(MediaStore.MediaColumns.IS_PENDING,1)
                }
                val target=checkNotNull(context.contentResolver.insert(collection,values)); uri=target
                var total=0L
                context.contentResolver.openOutputStream(target,"w").use { output ->
                    checkNotNull(output)
                    val bytes=ByteArray(8192)
                    while(true) {
                        currentCoroutineContext().ensureActive()
                        val read=source.read(bytes)
                        if(read<0) break
                        total+=read
                        check(total<=MediaPolicy.MAX_BYTES) { "SIZE_LIMIT" }
                        output.write(bytes,0,read)
                    }
                }
                check(MediaPolicy.acceptsSize(total)) { "EMPTY_FILE" }
                val declared=body.contentLength()
                check(declared<0 || total==declared) { "INCOMPLETE_FILE" }
                context.contentResolver.openAssetFileDescriptor(target,"r")?.use { check(it.length==total) { "FILE_VERIFICATION_FAILED" } } ?: error("FILE_MISSING")
                check(context.contentResolver.update(target,ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING,0) },null,null)==1)
                DownloadResult.Saved(target)
            }
        } catch(e: CancellationException) {
            uri?.let { runCatching { context.contentResolver.delete(it,null,null) } }; throw e
        } catch(_: Exception) {
            uri?.let { runCatching { context.contentResolver.delete(it,null,null) } }
            DownloadResult.Blocked("DOWNLOAD_FAILED_OR_UNSUPPORTED • HTTPS langsung, MIME/signature, ukuran <=50MB diperlukan; redirect/login tidak didukung")
        }
    }
}
