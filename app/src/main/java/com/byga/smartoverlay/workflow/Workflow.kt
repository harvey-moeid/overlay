package com.byga.smartoverlay.workflow

import java.net.URI
import java.security.MessageDigest

enum class Action { LAUNCH_APP, FIND_NODE, SET_TEXT, COPY_TEXT, CLICK_NODE, SWIPE, WAIT_FOR_STATE, GENERATE_AI_TEXT, ANALYZE_SCREEN_ON_DEMAND, DOWNLOAD_URL, TAP_NATIVE_DOWNLOAD, SHARE_MEDIA, VERIFY_ACCOUNT, REQUEST_USER_APPROVAL, BRANCH, DELAY, STOP }
enum class RunState { DRAFT, READY, RUNNING, PAUSED, WAITING_FOR_USER, SUCCEEDED, STOPPED, BLOCKED, UNKNOWN }
data class Step(val action: Action, val value: String = "", val packageName: String = "", val selector: String = "")
data class Plan(val name: String, val steps: List<Step>) {
    fun fingerprint(): String = MessageDigest.getInstance("SHA-256").digest(toString().toByteArray()).joinToString("") { "%02x".format(it) }
    fun validate(): List<String> = buildList {
        if (name.isBlank()) add("Nama wajib diisi")
        if (steps.isEmpty() || steps.size > 50) add("Workflow perlu 1–50 langkah")
        steps.forEachIndexed { i, s ->
            if (s.action in setOf(Action.FIND_NODE, Action.SET_TEXT, Action.CLICK_NODE, Action.TAP_NATIVE_DOWNLOAD) && (s.selector.isBlank() || s.packageName.isBlank())) add("Langkah ${i+1}: package dan selector wajib")
            if (s.action == Action.DELAY && (s.value.toLongOrNull() ?: -1) !in 0..30000) add("Delay harus 0–30000 ms")
            if (s.action == Action.DOWNLOAD_URL && !secureUrl(s.value)) add("URL wajib HTTPS")
        }
    }
}
fun secureUrl(value: String): Boolean = runCatching { val u=URI(value); u.scheme == "https" && !u.host.isNullOrBlank() && u.userInfo == null }.getOrDefault(false)
object Guard {
    fun canRun(plan: Plan, approvedHash: String?) = plan.validate().isEmpty() && approvedHash == plan.fingerprint()
    fun canPublish(target: String?, visibleAccount: String?, freshApproval: Boolean) = !target.isNullOrBlank() && target == visibleAccount && freshApproval
    fun schedulerReady(locked: Boolean, requiresUi: Boolean, userPresent: Boolean) = !requiresUi || (!locked && userPresent)
    fun mayRetry(action: Action, attempted: Int, uncertainSideEffect: Boolean) = !uncertainSideEffect && attempted < 2 && action in setOf(Action.FIND_NODE, Action.WAIT_FOR_STATE, Action.GENERATE_AI_TEXT)
}
class ExecutionLedger {
    private val completed = mutableSetOf<String>()
    fun claim(token: String): Boolean = completed.add(token)
}
