package com.byga.smartoverlay.mapping

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.graphics.Path
import android.graphics.Rect
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.currentCoroutineContext
import com.byga.smartoverlay.accessibility.AccessibilityWork
import com.byga.smartoverlay.accessibility.ScanBudget
import kotlin.coroutines.resume

class MappingAccessibility(private val service:AccessibilityService) {
    private data class Observed(val snapshot:MappingSnapshot,val nodes:Map<String,AccessibilityNodeInfo>,val windowId:Int,val addressBar:AccessibilityNodeInfo?)
    @Volatile private var nativeScreen=""
    private fun childLabel(root:AccessibilityNodeInfo,budget:ScanBudget):String {
        val queue=java.util.ArrayDeque<Pair<AccessibilityNodeInfo,Int>>();queue.add(root to 0)
        val labels=mutableListOf<String>();var count=0
        while(queue.isNotEmpty() && count++<20 && labels.size<3) {
            budget.check()
            val (node,depth)=queue.removeFirst()
            if(node.isEditable || node.isPassword)continue
            if(depth>0)node.text?.toString()?.trim()?.takeIf{it.length in 1..80}?.let{labels.add(it)}
            if(depth<3)for(i in 0 until minOf(12,node.childCount))node.getChild(i)?.let{queue.add(it to depth+1)}
        }
        return labels.joinToString(" ")
    }
    fun screenChanged(pkg:String,screen:String) {
        if(pkg==MappingPolicy.CHATGPT && screen.startsWith(pkg) && !screen.contains("View"))nativeScreen=screen.take(200)
    }
    private fun descendants(root:AccessibilityNodeInfo,budget:ScanBudget):List<AccessibilityNodeInfo> {
        val queue=java.util.ArrayDeque<AccessibilityNodeInfo>();queue.add(root)
        val nodes=mutableListOf<AccessibilityNodeInfo>()
        while(queue.isNotEmpty()) {
            budget.check()
            check(nodes.size<1200){"Layar terlalu kompleks. Persempit layar dan coba lagi."}
            val node=queue.removeFirst();nodes.add(node)
            check(node.childCount<=150){"Layar terlalu kompleks"}
            for(i in 0 until node.childCount) { budget.check();node.getChild(i)?.let{queue.add(it)} }
        }
        return nodes
    }
    private fun observe(budget:ScanBudget):Observed {
        val started=SystemClock.elapsedRealtime()
        check(!service.getSystemService(KeyguardManager::class.java).isKeyguardLocked){"Buka kunci HP dahulu"}
        val root=service.windows.filter{it.type==AccessibilityWindowInfo.TYPE_APPLICATION}.maxByOrNull{it.layer}?.root
            ?: service.rootInActiveWindow ?: error("Layar aplikasi tidak dapat dibaca")
        val pkg=root.packageName?.toString() ?: error("Aplikasi tidak diketahui")
        check(pkg in MappingPolicy.packages){"Target tahap ini: ChatGPT, Chrome, atau browser BYGA"}
        val all=descendants(root,budget)
        check(all.none { it.isVisibleToUser && (it.isPassword || (it.isEditable && Regex("(?i)otp|verification|verifikasi|password|kata sandi|2fa").containsMatchIn("${it.hintText} ${it.contentDescription}"))) }){"Selesaikan login/verifikasi dahulu; layar sensitif tidak dipetakan"}
        var scope:MappingScope
        var content=all
        var addressBar:AccessibilityNodeInfo?=null
        if(pkg==MappingPolicy.CHROME) {
            val bar=all.singleOrNull{it.viewIdResourceName=="com.android.chrome:id/url_bar" && it.isVisibleToUser} ?: error("Tampilkan address bar Chrome untuk memverifikasi situs")
            check(!bar.isFocused){"Tutup penyuntingan alamat Chrome dahulu"}
            addressBar=bar
            val site=MappingPolicy.site(bar.text?.toString() ?: "") ?: error("Buka ChatGPT atau Google Flow pada tab Chrome aktif")
            scope=MappingScope(pkg,site.first,site.second)
            val web=all.firstOrNull{it.className?.toString()=="android.webkit.WebView"} ?: error("Konten situs belum terbaca")
            content=descendants(web,budget)
        } else if(pkg==MappingPolicy.BYGA) {
            if(MappingBrowserContext.visible) {
                val site=MappingPolicy.site(MappingBrowserContext.url) ?: error("Buka halaman ChatGPT atau Flow; login tidak dipetakan")
                scope=MappingScope(pkg,site.first,site.second)
                content=descendants(all.firstOrNull{it.className?.toString()=="android.webkit.WebView"} ?: error("Browser belum siap"),budget)
            } else {
                val demo=all.firstOrNull{it.contentDescription?.toString()=="byga_mapping_test_screen"} ?: error("Buka aplikasi target. Layar BYGA hanya mendukung area uji Mapping.")
                content=descendants(demo,budget)
                scope=MappingScope(pkg,screen="mapping-test")
            }
        } else scope=MappingScope(pkg,screen=nativeScreen.ifBlank{root.className?.toString() ?: "ChatGPT"})
        check(MappingPolicy.allowedScope(scope))
        val refs=linkedMapOf<String,AccessibilityNodeInfo>()
        val candidates=content.filter { node -> budget.check();node.isVisibleToUser && node.isEnabled && node.isClickable && !node.isEditable && !node.isPassword && node.actionList.any{it.id==AccessibilityNodeInfo.ACTION_CLICK} }.mapNotNull { node ->
            val rect=Rect();node.getBoundsInScreen(rect)
            if(rect.isEmpty)return@mapNotNull null
            val direct=node.text?.toString()?.trim().orEmpty()
            val identity=TargetIdentity(node.viewIdResourceName.orEmpty().take(200),(direct.ifBlank{childLabel(node,budget)}).take(100),node.contentDescription?.toString().orEmpty().take(150),node.className?.toString().orEmpty().take(150),node.parent?.viewIdResourceName.orEmpty().take(200))
            if(!identity.stable())return@mapNotNull null
            val key=refs.size.toString();refs[key]=node
            MappingCandidate(key,identity,TargetBounds(rect.left,rect.top,rect.right,rect.bottom))
        }
        @Suppress("DEPRECATION") val display=service.getSystemService(WindowManager::class.java).defaultDisplay
        val metrics=DisplayMetrics();@Suppress("DEPRECATION") display.getRealMetrics(metrics)
        val layout=mappingHash(candidates.map{"${it.identity}:${it.bounds}"}.sorted().joinToString("|"))
        return Observed(MappingSnapshot(scope,candidates,metrics.widthPixels,metrics.heightPixels,display.rotation,layout,started),refs,root.windowId,addressBar)
    }
    suspend fun snapshot():MappingSnapshot=AccessibilityWork.run { observe(ScanBudget(currentCoroutineContext())).snapshot }
    suspend fun click(mapping:ButtonMapping,permit:MappingClickPermit):String = AccessibilityWork.run {
        val budget=ScanBudget(currentCoroutineContext())
        val observed=observe(budget);val snapshot=observed.snapshot;val now=SystemClock.elapsedRealtime()
        check(permit.claim(mapping,now)){"Persetujuan klik kedaluwarsa, berubah, atau sudah digunakan"}
        val front=service.windows.filter{it.type==AccessibilityWindowInfo.TYPE_APPLICATION}.maxByOrNull{it.layer}?.root ?: service.rootInActiveWindow
        check(front?.packageName?.toString()==mapping.scope.packageName && front.windowId==observed.windowId){"Jendela aplikasi berubah sebelum klik"}
        observed.addressBar?.let{bar->check(bar.refresh() && !bar.isFocused && MappingPolicy.site(bar.text?.toString().orEmpty())==(mapping.scope.site to mapping.scope.screen)){"Tab Chrome berubah sebelum klik"}}
        if(mapping.scope.packageName==MappingPolicy.BYGA && mapping.scope.site.isNotBlank())check(MappingBrowserContext.visible && MappingPolicy.site(MappingBrowserContext.url)==(mapping.scope.site to mapping.scope.screen)){"Halaman browser BYGA berubah sebelum klik"}
        if(mapping.kind==MappingKind.SEMANTIC) {
            val candidate=MappingPolicy.resolve(mapping,snapshot,now) ?: error("Tombol hilang/ambigu atau layar berubah. Petakan ulang; tidak ada klik tebakan.")
            val node=checkNotNull(observed.nodes[candidate.key]);check(node.refresh() && node.isVisibleToUser && node.isEnabled && !node.isPassword && !node.isEditable)
            val refreshed=TargetIdentity(node.viewIdResourceName.orEmpty().take(200),node.text?.toString()?.trim().orEmpty().ifBlank{childLabel(node,budget)}.take(100),node.contentDescription?.toString().orEmpty().take(150),node.className?.toString().orEmpty().take(150),node.parent?.viewIdResourceName.orEmpty().take(200))
            check(refreshed==candidate.identity){"Identitas tombol berubah sebelum klik"}
            budget.check()
            check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){"Aplikasi menolak klik. Periksa hasil sebelum mengulang."}
        } else {
            check(MappingPolicy.pointAllowed(mapping,snapshot,now)){"Ukuran, orientasi, atau layout berubah. Petakan ulang posisi manual."}
            val path=Path().apply{moveTo(mapping.x*snapshot.width,mapping.y*snapshot.height)}
            budget.check()
            val gesture=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path,0,60)).build()
            val delivered=suspendCancellableCoroutine<Boolean> { continuation ->
                val started=service.dispatchGesture(gesture,object:AccessibilityService.GestureResultCallback(){
                    override fun onCompleted(gestureDescription:GestureDescription?) { if(continuation.isActive)continuation.resume(true) }
                    override fun onCancelled(gestureDescription:GestureDescription?) { if(continuation.isActive)continuation.resume(false) }
                },null)
                if(!started && continuation.isActive)continuation.resume(false)
            }
            check(delivered){"Gestur tidak terkonfirmasi. Periksa hasil; jangan ulangi otomatis."}
        }
        "CLICK_ACCEPTED • perintah dikirim satu kali; hasil aplikasi perlu diperiksa"
    }
}
