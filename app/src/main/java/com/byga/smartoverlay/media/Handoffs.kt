package com.byga.smartoverlay.media

import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Environment
import com.byga.smartoverlay.workflow.secureUrl

object Handoffs {
    fun copy(context: Context, text: String) { context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("BYGA",text)) }
    fun share(context: Context, text: String, uri: Uri? = null, targetPackage: String? = null) {
        val intent=Intent(Intent.ACTION_SEND).apply {
            type=uri?.let { context.contentResolver.getType(it) } ?: "text/plain"
            putExtra(Intent.EXTRA_TEXT,text)
            if(targetPackage!=null) setPackage(targetPackage)
            if (uri != null) { putExtra(Intent.EXTRA_STREAM,uri); clipData=ClipData.newUri(context.contentResolver,"Media",uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        if(targetPackage!=null && context.packageManager.queryIntentActivities(intent,android.content.pm.PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) throw android.content.ActivityNotFoundException("Aplikasi tujuan tidak dapat menerima format media ini")
        context.startActivity(Intent.createChooser(intent,"Bagikan draft • belum dipublikasikan").apply { if(context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
    fun launch(context: Context, pkg: String): Boolean {
        return runCatching {
            val intent=context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));true
        }.getOrDefault(false)
    }
}
