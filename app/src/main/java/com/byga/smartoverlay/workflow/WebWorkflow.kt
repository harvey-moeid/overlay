package com.byga.smartoverlay.workflow

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.util.UUID

/** Only these fixed commands can reach the browser. Imported plans cannot supply JavaScript. */
object WebWorkflowPolicy {
    val actions=setOf(Action.OPEN_CHATGPT,Action.CHATGPT_SEND_PROMPT,Action.CHATGPT_COPY_RESULT,Action.CHATGPT_COLLECT_RESULT,
        Action.CHATGPT_DOWNLOAD_IMAGE,Action.OPEN_FLOW,Action.FLOW_CREATE_PROJECT,
        Action.FLOW_UPLOAD_IMAGE,Action.FLOW_SET_PROMPT,Action.FLOW_GENERATE,Action.FLOW_DOWNLOAD_VIDEO)
    fun isChat(action:Action)=action.name.contains("CHATGPT")
    fun home(action:Action)=if(isChat(action)) "https://chatgpt.com/" else "https://labs.google/fx/tools/flow"
    fun permits(action:Action,url:String):Boolean=runCatching {
        val u=URI(url).normalize()
        action in actions && u.scheme=="https" && u.userInfo==null && (u.port==-1 || u.port==443) &&
            if(isChat(action)) u.host=="chatgpt.com" && com.byga.smartoverlay.mapping.MappingPolicy.site(url)!=null
            else u.host=="labs.google" && (u.path=="/fx/tools/flow" || u.path.startsWith("/fx/tools/flow/"))
    }.getOrDefault(false)
    fun navigable(url:String):Boolean=runCatching {
        val u=URI(url)
        u.scheme=="https" && u.userInfo==null && (u.port==-1 || u.port==443) &&
            u.host in setOf("chatgpt.com","auth.openai.com","auth0.openai.com","accounts.google.com","labs.google")
    }.getOrDefault(false)
}

class WebCommand(val action:Action,val value:String) {
    val id=UUID.randomUUID().toString()
    val result=CompletableDeferred<String>()
    val dispatch=DispatchGuard()
}

/** RAM-only session, cancelled with the runner. No grant survives process death. */
object WorkflowBrowserSession {
    private val _command=MutableStateFlow<WebCommand?>(null)
    val command=_command.asStateFlow()
    @Volatile var visible=false
        internal set
    @Volatile var paused=false
    var onStatus: ((RunState,String)->Unit)? = null
    var onApprovalStatus:((Boolean,String)->Unit)?=null
    fun report(state:RunState,note:String) { onStatus?.invoke(state,note) }
    var onPauseChanged:((Boolean)->Unit)?=null
    fun changePause(value:Boolean) { paused=value;onPauseChanged?.invoke(value) }
    private var activeRun=false
    var chatResponse=""
        internal set
    fun begin(resuming:Boolean) {
        check(!resuming) { "Sesi web tidak dapat di-resume dari checkpoint. Periksa hasil di situs lalu mulai workflow baru." }
        activeRun=true; paused=false; chatResponse=""
    }
    fun end() { activeRun=false;onStatus=null;onApprovalStatus=null;onPauseChanged=null; _command.value?.result?.cancel(); _command.value=null }
    fun isActive()=activeRun
    suspend fun execute(context:Context,action:Action,value:String):String {
        check(activeRun && action in WebWorkflowPolicy.actions)
        check(_command.value==null) { "Langkah browser lain masih aktif" }
        val command=WebCommand(action,value)
        _command.value=command
        try {
            context.startActivity(Intent(context,WorkflowBrowserActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            return command.result.await()
        } finally { if(_command.value===command)_command.value=null }
    }
}

fun Action.displayName():String=when(this) {
    Action.CLICK_MAPPING -> "Klik tombol Smart Mapping"
    Action.NAVIGATE_BACK -> "Kembali dari layar Smart Mapping"
    Action.INPUT_MAPPING -> "Isi field Smart Mapping"
    Action.SCROLL_MAPPING -> "Scroll container Smart Mapping"
    Action.WAIT_FOR_STATE -> "Tunggu kondisi target"
    Action.BRANCH -> "IF / ELSE berdasarkan output"
    Action.LAUNCH_APP -> "Buka aplikasi terinstal"
    Action.OPEN_CHATGPT -> "Buka ChatGPT Web"
    Action.CHATGPT_SEND_PROMPT -> "Tempel & kirim prompt ChatGPT"
    Action.CHATGPT_COLLECT_RESULT -> "Ambil hasil ChatGPT (teks/gambar otomatis)"
    Action.CHATGPT_COPY_RESULT -> "Tunggu & salin jawaban ChatGPT"
    Action.CHATGPT_DOWNLOAD_IMAGE -> "Unduh gambar hasil ChatGPT"
    Action.OPEN_FLOW -> "Buka Google Flow"
    Action.FLOW_CREATE_PROJECT -> "Buat proyek Flow"
    Action.FLOW_UPLOAD_IMAGE -> "Unggah gambar ke proyek"
    Action.FLOW_SET_PROMPT -> "Tempel prompt video"
    Action.FLOW_GENERATE -> "Generate video (memakai kredit Flow)"
    Action.FLOW_DOWNLOAD_VIDEO -> "Tunggu & unduh video"
    Action.COPY_TEXT -> "Salin teks"
    Action.GENERATE_AI_TEXT -> "Buat teks dengan provider AI"
    Action.DOWNLOAD_URL -> "Unduh URL media"
    Action.REQUEST_USER_APPROVAL -> "Tinjau & lanjutkan"
    Action.DELAY -> "Jeda"
    Action.STOP -> "Selesai"
    else -> name
}
