package com.byga.smartoverlay.accessibility

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
    private var lastAction = 0L
    override fun onServiceConnected() { current = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
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
        if (output.size >= 200 || node.isPassword) return
        output.add(node)
        for (i in 0 until node.childCount) node.getChild(i)?.let { nodes(it, output) }
    }
    fun readAccessibleNodes(pkg: String): List<NodeSummary> {
        val root=safeRoot(pkg) ?: return emptyList()
        val list=mutableListOf<AccessibilityNodeInfo>(); nodes(root,list)
        return list.map { NodeSummary(it.viewIdResourceName,it.isEditable,it.isClickable) }
    }
    fun testInput(value: String): UiResult = act(packageName,"byga_safe_input",AccessibilityNodeInfo.ACTION_SET_TEXT,value)
    fun act(pkg: String, selector: String, action: Int, value: String = ""): UiResult {
        if (selector != "byga_safe_input" || action != AccessibilityNodeInfo.ACTION_SET_TEXT) return UiResult.BLOCKED
        if (SystemClock.elapsedRealtime() - lastAction < 750) return UiResult.BLOCKED
        val root=safeRoot(pkg) ?: return UiResult.PERMISSION_DENIED
        val list=mutableListOf<AccessibilityNodeInfo>(); nodes(root,list)
        val node=list.firstOrNull { it.isEditable && it.isEnabled && it.isVisibleToUser && it.contentDescription?.toString() == selector && !it.isPassword } ?: return UiResult.NOT_FOUND
        lastAction=SystemClock.elapsedRealtime()
        val arguments=Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value.take(4000)) }
        return if (node.performAction(action, arguments)) UiResult.SUCCESS else UiResult.UNKNOWN
    }
    fun observeAgent(): Observation {
        val locked=getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val root=rootInActiveWindow
        val pkg=root?.packageName?.toString() ?: ""
        val allowed=pkg==packageName && !locked
        val list=mutableListOf<AccessibilityNodeInfo>()
        if(allowed && root!=null) nodes(root,list)
        val field=list.firstOrNull { it.isEditable && it.isVisibleToUser && it.contentDescription?.toString()=="byga_safe_input" && !it.isPassword }
        return Observation(pkg,field?.text?.toString(),SystemClock.elapsedRealtime(),current===this && Settings.canDrawOverlays(this) && field!=null,locked)
    }
    fun performClick(): UiResult = UiResult.BLOCKED
    fun scroll(): UiResult = UiResult.BLOCKED
}
