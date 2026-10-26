package com.byga.smartoverlay.accessibility

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import android.accessibilityservice.AccessibilityService
import android.provider.Settings
import com.byga.smartoverlay.agent.Observation
import android.app.KeyguardManager
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

// No background event text collection. A one-use permit is created by a visible user action.
enum class UiResult { SUCCESS, NOT_FOUND, PERMISSION_DENIED, BLOCKED, UNKNOWN }
data class NodeSummary(val id: String?, val editable: Boolean, val clickable: Boolean)
class SafeAccessibilityService : AccessibilityService() {
    companion object { @Volatile var current: SafeAccessibilityService? = null; private set }
    val mapping by lazy { com.byga.smartoverlay.mapping.MappingAccessibility(this) }
    private var lastAction = 0L
    private lateinit var budget: ScanBudget
    private suspend fun <T> scan(block: () -> T): T = AccessibilityWork.run {
        budget=ScanBudget(currentCoroutineContext())
        budget.check()
        block()
    }
    override fun onServiceConnected() { current = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { if(event?.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)mapping.screenChanged(event.packageName?.toString().orEmpty(),event.className?.toString().orEmpty()) }
    override fun onInterrupt() { current = null }
    override fun onDestroy() { current = null; super.onDestroy() }
    private fun safeRoot(pkg: String): AccessibilityNodeInfo? {
        // Phase 0 deliberately only permits the app's safe test screen.
        if (pkg != packageName || getSystemService(KeyguardManager::class.java).isKeyguardLocked) return null
        val root=rootInActiveWindow ?: return null
        if (root.packageName?.toString() != pkg) return null
        return root
    }
    private fun nodes(node: AccessibilityNodeInfo, output: MutableList<AccessibilityNodeInfo>) {
        budget.check()
        if (output.size >= 200 || node.isPassword) return
        output.add(node)
        for (i in 0 until minOf(node.childCount,200)) { if(output.size>=200)break;budget.check();node.getChild(i)?.let { nodes(it, output) } }
    }
    suspend fun readAccessibleNodes(pkg: String): List<NodeSummary> = scan { readAccessibleNodesBlocking(pkg) }
    private fun readAccessibleNodesBlocking(pkg: String): List<NodeSummary> {
        val root=safeRoot(pkg) ?: return emptyList()
        val list=mutableListOf<AccessibilityNodeInfo>(); nodes(root,list)
        return list.map { NodeSummary(it.viewIdResourceName,it.isEditable,it.isClickable) }
    }
    suspend fun testInput(value: String): UiResult = act(packageName,"byga_safe_input",AccessibilityNodeInfo.ACTION_SET_TEXT,value)
    suspend fun act(pkg: String, selector: String, action: Int, value: String = ""): UiResult = scan { actBlocking(pkg,selector,action,value) }
    private fun actBlocking(pkg: String, selector: String, action: Int, value: String = ""): UiResult {
        if (selector != "byga_safe_input" || action != AccessibilityNodeInfo.ACTION_SET_TEXT) return UiResult.BLOCKED
        if (SystemClock.elapsedRealtime() - lastAction < 750) return UiResult.BLOCKED
        val root=safeRoot(pkg) ?: return UiResult.PERMISSION_DENIED
        val list=mutableListOf<AccessibilityNodeInfo>(); nodes(root,list)
        val node=list.firstOrNull { it.isEditable && it.isEnabled && it.isVisibleToUser && it.contentDescription?.toString() == selector && !it.isPassword } ?: return UiResult.NOT_FOUND
        lastAction=SystemClock.elapsedRealtime()
        val arguments=Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value.take(4000)) }
        budget.check()
        return if (node.performAction(action, arguments)) UiResult.SUCCESS else UiResult.UNKNOWN
    }
    suspend fun observeAgent(): Observation = scan { observeAgentBlocking() }
    private fun observeAgentBlocking(): Observation {
        val locked=getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val root=rootInActiveWindow
        val pkg=root?.packageName?.toString() ?: ""
        val allowed=pkg==packageName && !locked
        val list=mutableListOf<AccessibilityNodeInfo>()
        if(allowed && root!=null) nodes(root,list)
        val field=list.firstOrNull { it.isEditable && it.isVisibleToUser && it.contentDescription?.toString()=="byga_safe_input" && !it.isPassword }
        return Observation(pkg,field?.text?.toString(),SystemClock.elapsedRealtime(),current===this && Settings.canDrawOverlays(this) && field!=null,locked)
    }
    suspend fun foregroundPackage(): String = scan { foregroundPackageBlocking() }
    private fun foregroundPackageBlocking():String = if(getSystemService(KeyguardManager::class.java).isKeyguardLocked) "" else rootInActiveWindow?.packageName?.toString() ?: ""
    private fun fingerprint(node:AccessibilityNodeInfo)="${node.windowId}:${node.hashCode()}:${node.viewIdResourceName}:${node.className}:${node.inputType}"
    suspend fun focusedFieldFingerprint(target:String): String = scan { focusedFieldFingerprintBlocking(target) }
    private fun focusedFieldFingerprintBlocking(target:String):String {
        if(target !in ManualInputPermit.packages)return ""
        val root=rootInActiveWindow ?: return ""
        if(root.packageName?.toString()!=target)return ""
        val field=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return ""
        if(!field.isEditable || field.isPassword || !field.isVisibleToUser)return ""
        return fingerprint(field)
    }
    suspend fun applyReviewedInput(permit:ManualInputPermit): UiResult = scan { applyReviewedInputBlocking(permit) }
    private fun applyReviewedInputBlocking(permit:ManualInputPermit):UiResult {
        val root=rootInActiveWindow ?: return UiResult.NOT_FOUND
        val active=root.packageName?.toString() ?: return UiResult.BLOCKED
        val locked=getSystemService(KeyguardManager::class.java).isKeyguardLocked
        if(active !in ManualInputPermit.packages || locked)return UiResult.BLOCKED
        val queue=java.util.ArrayDeque<AccessibilityNodeInfo>();queue.add(root);var count=0;var sensitive=false
        while(queue.isNotEmpty() && count++<400){
            budget.check()
            val node=queue.removeFirst()
            if(node.isPassword){sensitive=true;break}
            val description=(node.contentDescription?.toString()?:"")+" "+(node.text?.toString()?:"")
            if(active!=packageName && Regex("(?i)\\b(otp|2fa|verification code|kode verifikasi|kode keamanan|sign in|log in)\\b").containsMatchIn(description)){sensitive=true;break}
            for(i in 0 until minOf(50,node.childCount))node.getChild(i)?.let{queue.add(it)}
        }
        if(queue.isNotEmpty() && !sensitive)return UiResult.BLOCKED
        val field=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return UiResult.NOT_FOUND
        if(!permit.claim(active,locked,sensitive,SystemClock.elapsedRealtime(),fingerprint(field)))return UiResult.BLOCKED
        if(SystemClock.elapsedRealtime()-lastAction<750)return UiResult.BLOCKED
        if(field.packageName?.toString()!=permit.target || !field.isEditable || !field.isEnabled || !field.isVisibleToUser || field.isPassword)return UiResult.BLOCKED
        if(field.actionList.none{it.id==AccessibilityNodeInfo.ACTION_SET_TEXT})return UiResult.BLOCKED
        lastAction=SystemClock.elapsedRealtime()
        val args=Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,permit.value)}
        budget.check()
        if(!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args))return UiResult.UNKNOWN
        field.refresh()
        return if(field.text?.toString()==permit.value)UiResult.SUCCESS else UiResult.UNKNOWN
    }
    suspend fun captureAllowed(requested: String): Boolean = try { scan { captureAllowedBlocking(requested) } } catch(e:CancellationException){throw e} catch(_:Exception){false}
    private fun captureAllowedBlocking(requested: String): Boolean {
        val root=rootInActiveWindow ?: return false
        val active=root.packageName?.toString() ?: return false
        val locked=getSystemService(KeyguardManager::class.java).isKeyguardLocked
        if(!com.byga.smartoverlay.capture.CapturePolicy.mayCapture(requested,active,locked,false)) return false
        val queue=java.util.ArrayDeque<AccessibilityNodeInfo>();queue.add(root);var count=0
        while(queue.isNotEmpty() && count++<500) {
            budget.check()
            val node=queue.removeFirst()
            if(node.isPassword)return false
            val text=(node.text?.toString() ?: "")+" "+(node.contentDescription?.toString() ?: "")
            if(node.isEditable && Regex("(?i)\\b(otp|2fa|one.time.password|verification code|kode verifikasi|kode keamanan|sign in|log in)\\b").containsMatchIn(text))return false
            for(i in 0 until minOf(node.childCount,50))node.getChild(i)?.let{queue.add(it)}
        }
        return queue.isEmpty()
    }
    fun performClick(): UiResult = UiResult.BLOCKED
    fun scroll(): UiResult = UiResult.BLOCKED
}
