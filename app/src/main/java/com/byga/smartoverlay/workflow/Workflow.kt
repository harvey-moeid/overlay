package com.byga.smartoverlay.workflow

import java.net.URI
import java.security.MessageDigest

enum class Action { LAUNCH_APP, CLICK_MAPPING, OPEN_CHATGPT, CHATGPT_SEND_PROMPT, CHATGPT_COPY_RESULT, CHATGPT_COLLECT_RESULT, CHATGPT_DOWNLOAD_IMAGE, OPEN_FLOW, FLOW_CREATE_PROJECT, FLOW_UPLOAD_IMAGE, FLOW_SET_PROMPT, FLOW_GENERATE, FLOW_DOWNLOAD_VIDEO, FIND_NODE, SET_TEXT, COPY_TEXT, CLICK_NODE, SWIPE, WAIT_FOR_STATE, GENERATE_AI_TEXT, ANALYZE_SCREEN_ON_DEMAND, DOWNLOAD_URL, TAP_NATIVE_DOWNLOAD, SHARE_MEDIA, VERIFY_ACCOUNT, REQUEST_USER_APPROVAL, BRANCH, DELAY, STOP }
enum class RunState { DRAFT, READY, RUNNING, PAUSED, WAITING_FOR_USER, SUCCEEDED, STOPPED, BLOCKED, UNKNOWN }
data class Step(val action: Action, val value: String = "", val packageName: String = "", val selector: String = "")
data class Plan(val name: String, val steps: List<Step>) {
    fun fingerprint(): String = MessageDigest.getInstance("SHA-256").digest(toString().toByteArray()).joinToString("") { "%02x".format(it) }
    fun validate(): List<String> = buildList {
        if (name.isBlank() || name.length>120) add("Nama wajib 1–120 karakter")
        if(steps.sumOf{it.value.length+it.packageName.length+it.selector.length}>96000) add("Total isi workflow terlalu besar")
        if (steps.isEmpty() || steps.size > 50) add("Workflow perlu 1–50 langkah")
        steps.forEachIndexed { i, s ->
            if(s.value.length>32000) add("Langkah ${i+1}: isi melebihi 32000 karakter")
            if(s.action in setOf(Action.COPY_TEXT,Action.GENERATE_AI_TEXT) && s.value.isBlank()) add("Langkah ${i+1}: teks wajib")
            if(s.action in setOf(Action.CHATGPT_SEND_PROMPT,Action.FLOW_SET_PROMPT) && s.value.isBlank()) add("Langkah ${i+1}: prompt wajib diisi")
            if(s.action==Action.FLOW_UPLOAD_IMAGE && !(s.value.startsWith("content://") || s.value=="{{output}}")) add("Langkah ${i+1}: pilih gambar dari perangkat")
            if(s.action==Action.CLICK_MAPPING && (s.value.isBlank() || s.packageName !in com.byga.smartoverlay.mapping.MappingPolicy.packages || !s.selector.matches(Regex("[a-f0-9]{64}")))) add("Langkah ${i+1}: pilih mapping tersimpan beserta versinya")
            if(s.action==Action.LAUNCH_APP && !s.packageName.matches(Regex("[A-Za-z][A-Za-z0-9_]*([.][A-Za-z0-9_]+)+"))) add("Langkah ${i+1}: package tidak valid")
            if (s.action in setOf(Action.FIND_NODE, Action.SET_TEXT, Action.CLICK_NODE, Action.TAP_NATIVE_DOWNLOAD) && (s.selector.isBlank() || s.packageName.isBlank())) add("Langkah ${i+1}: package dan selector wajib")
            if (s.action == Action.DELAY && (s.value.toLongOrNull() ?: -1) !in 0..30000) add("Delay harus 0–30000 ms")
            if (s.action == Action.DOWNLOAD_URL && !secureUrl(s.value)) add("URL wajib HTTPS")
        }
        var web: Action?=null
        var sent=false; var project=false; var prompted=false; var generated=false
        steps.forEachIndexed { i,s ->
            when(s.action) {
                Action.OPEN_CHATGPT -> { web=s.action; sent=false }
                Action.OPEN_FLOW -> { web=s.action; project=false; prompted=false; generated=false }
                Action.CHATGPT_SEND_PROMPT -> { if(web!=Action.OPEN_CHATGPT)add("Langkah ${i+1}: buka ChatGPT dahulu"); sent=true }
                Action.CHATGPT_COPY_RESULT,Action.CHATGPT_COLLECT_RESULT,Action.CHATGPT_DOWNLOAD_IMAGE -> if(web!=Action.OPEN_CHATGPT || !sent)add("Langkah ${i+1}: kirim prompt ChatGPT dahulu")
                Action.FLOW_CREATE_PROJECT -> { if(web!=Action.OPEN_FLOW)add("Langkah ${i+1}: buka Flow dahulu"); project=true; prompted=false; generated=false }
                Action.FLOW_UPLOAD_IMAGE -> if(web!=Action.OPEN_FLOW || !project)add("Langkah ${i+1}: buat proyek Flow dahulu")
                Action.FLOW_SET_PROMPT -> { if(web!=Action.OPEN_FLOW || !project)add("Langkah ${i+1}: buat proyek Flow dahulu"); prompted=true; generated=false }
                Action.FLOW_GENERATE -> { if(web!=Action.OPEN_FLOW || !prompted)add("Langkah ${i+1}: isi prompt Flow dahulu"); generated=true }
                Action.FLOW_DOWNLOAD_VIDEO -> if(web!=Action.OPEN_FLOW || !generated)add("Langkah ${i+1}: generate video Flow dahulu")
                else -> Unit
            }
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
