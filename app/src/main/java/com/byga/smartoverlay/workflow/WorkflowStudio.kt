package com.byga.smartoverlay.workflow

import android.app.Application
import android.app.KeyguardManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.byga.smartoverlay.ai.AiRepository
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.agent.AgentSessions
import com.byga.smartoverlay.media.*
import com.byga.smartoverlay.ui.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.UUID

class WorkflowViewModel(app: Application): AndroidViewModel(app) {
    private val dao=BygaDatabase.get(app).records()
    val editorId=mutableStateOf("local-draft")
    val editorTitle=mutableStateOf("Workflow baru")
    val editorSteps=mutableStateOf(WorkflowCodec.templates.first().steps)
    var workflowId="local-draft"
    var providerId=""
    var foreground: ()->Boolean = {false}
    private var job: Job?=null
    val runner=WorkflowRunner(object: WorkflowPort {
        override suspend fun ready(step: Step): Boolean {
            val locked=app.getSystemService(KeyguardManager::class.java).isKeyguardLocked
            return step.action in setOf(Action.GENERATE_AI_TEXT,Action.DELAY,Action.STOP) || (!locked && foreground())
        }
        override suspend fun act(step: Step, previous: String): String {
            val value=step.value.replace("{{output}}",previous)
            return when(step.action) {
                Action.COPY_TEXT -> { Handoffs.copy(app,value); value }
                Action.LAUNCH_APP -> { check(Handoffs.launch(app,step.packageName)){"Aplikasi tidak tersedia; tidak ditandai berhasil"}; previous }
                Action.GENERATE_AI_TEXT -> AiRepository(app).generate(value,listOf(providerId)).text
                Action.DOWNLOAD_URL -> when(val r=DirectDownload.save(app,value)) { is DownloadResult.Saved -> r.uri.toString(); is DownloadResult.Blocked -> error(r.code) }
                else -> error("Adapter belum tersedia")
            }
        }
        override suspend fun checkpoint(plan: Plan, next: Int, uncertain: Boolean, output: String) {
            dao.save(Record("checkpoint",workflowId,JSONObject().put("hash",plan.fingerprint()).put("next",next).put("uncertain",uncertain).put("output",output.take(32000)).toString()))
            dao.save(Record("execution",workflowId,if(uncertain) "EXECUTING • langkah ${next+1} • verifikasi manual bila proses berhenti" else "CHECKPOINT • langkah $next/${plan.steps.size}"))
        }
    })
    init { StudioSessions.register("workflow",::stop) }
    fun start(plan: Plan, approved: String?, resume: Boolean=false) {
        if(job?.isActive==true)return
        AgentSessions.stopAll()
        job=viewModelScope.launch {
            val saved=if(resume)dao.get("checkpoint",workflowId)?.let { runCatching { val j=JSONObject(it.body); SavedCheckpoint(j.getString("hash"),j.getInt("next"),j.optBoolean("uncertain",true),j.optString("output")) }.getOrNull() } else null
            if(resume && saved==null) { runner.refuse("Belum ada checkpoint valid untuk workflow ini");return@launch }
            try { runner.run(plan,approved,saved) }
            finally { withContext(NonCancellable) { dao.save(Record("execution",workflowId,runner.snapshot.value.state.name+" • "+runner.snapshot.value.note)) } }
        }
    }
    fun stop() { runner.stop(); job?.cancel() }
    fun active()=job?.isActive==true
    override fun onCleared() { StudioSessions.unregister("workflow"); super.onCleared() }
}
@Composable fun WorkflowStudio() {
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    val dao=remember { BygaDatabase.get(context).records() }
    val vm: WorkflowViewModel=viewModel()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    vm.foreground={lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)}
    val status by vm.runner.snapshot.collectAsState()
    val rows by remember { dao.observe("workflow") }.collectAsState(initial=emptyList())
    val providers by remember { dao.observe("provider") }.collectAsState(initial=emptyList())
    var id by vm.editorId; var title by vm.editorTitle
    var steps by vm.editorSteps
    var approval by remember { mutableStateOf<String?>(null) }; var note by remember { mutableStateOf("") }
    var provider by rememberSaveable { mutableStateOf("") }; var aiConsent by remember { mutableStateOf(false) }
    var imported by remember { mutableStateOf("") }
    val plan=Plan(title,steps)
    fun load(p: Plan, selected: String=UUID.randomUUID().toString()) { if(vm.active()) { note="Stop workflow aktif dahulu"; return }; title=p.name; steps=p.steps; id=selected; approval=null; note="Draft dimuat; tinjau ulang sebelum run" }
    LaunchedEffect(providers) { if(providers.none{it.id==provider})provider=providers.firstOrNull()?.id ?: "" }
    PremiumCard { Eyebrow("Design your flow"); Text("Dari satu langkah,\nke alur yang rapi.",style=MaterialTheme.typography.headlineSmall); Text("Versi immutable, approval jelas, checkpoint lokal. Tidak ada publikasi otomatis.",color=Muted) }
    SectionTitle("Template pilihan")
    WorkflowCodec.templates.forEach { t -> OutlinedButton(onClick={load(t)},modifier=Modifier.fillMaxWidth()) { Text(t.name) } }
    if(rows.isNotEmpty()) { SectionTitle("Library (${rows.size})"); rows.forEach { row -> val p=runCatching{WorkflowCodec.decode(row.body)}.getOrNull(); PremiumCard { Text(p?.name ?: "Workflow legacy / tidak valid"); Row { if(p!=null) TextButton(onClick={load(p,row.id)}){Text("Edit")}; TextButton(onClick={scope.launch { dao.delete("workflow",row.id) }},enabled=!vm.active()){Text("Hapus")}; if(p!=null) TextButton(onClick={Handoffs.share(context,WorkflowCodec.encode(p))}){Text("Ekspor")} } } } }
    SectionTitle("Editor")
    OutlinedTextField(title,{title=it;approval=null},label={Text("Nama workflow")},modifier=Modifier.fillMaxWidth(),enabled=!vm.active())
    steps.forEachIndexed { index,step -> PremiumCard {
        Text("${index+1}  /  ${step.action}",style=MaterialTheme.typography.titleMedium)
        OutlinedTextField(if(step.action==Action.LAUNCH_APP) step.packageName else step.value,{v->steps=steps.toMutableList().apply {set(index,if(step.action==Action.LAUNCH_APP)step.copy(packageName=v) else step.copy(value=v))};approval=null},label={Text(if(step.action==Action.LAUNCH_APP) "Package aplikasi" else if(step.action==Action.DELAY) "Jeda 0–30000 ms" else "Isi / prompt / persetujuan")},modifier=Modifier.fillMaxWidth(),enabled=!vm.active(),maxLines=5)
        Row { TextButton(enabled=index>0 && !vm.active(),onClick={steps=steps.toMutableList().apply{add(index-1,removeAt(index))};approval=null}){Text("↑ Naik")}; TextButton(enabled=index<steps.lastIndex && !vm.active(),onClick={steps=steps.toMutableList().apply{add(index+1,removeAt(index))};approval=null}){Text("↓ Turun")}; TextButton(enabled=!vm.active(),onClick={steps=steps.toMutableList().apply{removeAt(index)};approval=null}){Text("Hapus")} }
    } }
    Row(Modifier.horizontalScroll(rememberScrollState())) { listOf(Action.COPY_TEXT,Action.GENERATE_AI_TEXT,Action.REQUEST_USER_APPROVAL,Action.DELAY,Action.LAUNCH_APP,Action.DOWNLOAD_URL,Action.STOP).forEach { action -> TextButton(enabled=!vm.active() && steps.size<50,onClick={steps=steps+Step(action,when(action){Action.DELAY->"500";Action.COPY_TEXT->"{{output}}";Action.GENERATE_AI_TEXT->"Buat caption Indonesia singkat";else->""});approval=null}){Text("+ ${action.name}")} } }
    Text("Gunakan {{output}} untuk memakai hasil langkah sebelumnya. Output disimpan privat bersama checkpoint; hasil tidak pasti harus diperiksa manual dan tidak diulang otomatis.",color=Muted,style=MaterialTheme.typography.bodySmall)
    if(steps.any{it.action==Action.GENERATE_AI_TEXT}) PremiumCard {
        Text("Provider untuk langkah AI")
        Row(Modifier.horizontalScroll(rememberScrollState())) { providers.forEach { r-> val j=JSONObject(r.body); FilterChip(provider==r.id,{provider=r.id;aiConsent=false},label={Text(j.optString("model"))}) } }
        Row { Checkbox(aiConsent,{aiConsent=it;approval=null}); Text("Saya setuju prompt langkah AI dikirim ke model ini. Batas 12 request/hari bersama AI Chat, tanpa fallback otomatis.") }
    }
    Row { OutlinedButton(onClick={note=plan.validate().joinToString("\n").ifBlank{"Dry run valid; tidak ada aksi dijalankan"}}){Text("Dry run")}; Spacer(Modifier.width(8.dp)); Button(enabled=!vm.active(),onClick={if(plan.validate().isEmpty())scope.launch{dao.save(Record("workflow",id,WorkflowCodec.encode(plan)));note="Workflow disimpan"} else note="Validasi belum lulus"}){Text("Simpan")}; TextButton(enabled=!vm.active(),onClick={load(WorkflowCodec.templates.first())}){Text("Baru")} }
    Row { Checkbox(approval==plan.fingerprint(),{approval=if(it)plan.fingerprint() else null},enabled=!vm.active()); Text("Saya meninjau aplikasi, teks, URL, dan semua langkah versi ini.") }
    fun start(resume: Boolean) { if(steps.any{it.action==Action.GENERATE_AI_TEXT} && (!aiConsent||provider.isBlank())) {note="Pilih provider dan berikan consent AI dahulu";return}; vm.workflowId=id;vm.providerId=provider;vm.start(plan,approval,resume) }
    Row { Button(enabled=!vm.active(),onClick={start(false)}){Text("Jalankan")}; TextButton(enabled=!vm.active(),onClick={start(true)}){Text("Resume checkpoint")} }
    if(note.isNotBlank())Text(note,color=Cyan)
    PremiumCard { StatusPill(status.state.name,status.state !in setOf(RunState.BLOCKED,RunState.UNKNOWN)); Text("Langkah ${minOf(status.index+1,status.total)} / ${status.total}"); Text(status.note); if(status.total>0)LinearProgressIndicator(progress={status.index.toFloat()/status.total},modifier=Modifier.fillMaxWidth()); if(status.output.isNotBlank()){Text(status.output.take(6000));TextButton(onClick={Handoffs.copy(context,status.output)}){Text("Salin hasil")}}; Row { TextButton(onClick={vm.runner.pause()}){Text("Pause")}; TextButton(onClick={vm.runner.resume()}){Text("Resume")}; TextButton(onClick={vm.stop()}){Text("Stop")} }; if(status.state==RunState.WAITING_FOR_USER && vm.active()) Button(onClick={vm.runner.approve()}){Text("Saya menyetujui, lanjutkan")} }
    SectionTitle("Impor JSON", "Impor hanya menjadi draft; scope dan approval tidak ikut diimpor.")
    OutlinedTextField(imported,{imported=it.take(128000)},label={Text("Workflow JSON")},modifier=Modifier.fillMaxWidth(),maxLines=5)
    Button(enabled=!vm.active(),onClick={runCatching{WorkflowCodec.decode(imported)}.onSuccess{load(it)}.onFailure{note="JSON/skema workflow tidak valid"}}){Text("Validasi & impor draft")}
}
