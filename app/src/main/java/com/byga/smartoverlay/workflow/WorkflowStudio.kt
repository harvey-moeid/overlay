package com.byga.smartoverlay.workflow

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.Application
import android.app.KeyguardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.byga.smartoverlay.mapping.*
import com.byga.smartoverlay.accessibility.SafeAccessibilityService
import com.byga.smartoverlay.ui.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.UUID

class WorkflowViewModel(app: Application): AndroidViewModel(app) {
    private val dao=BygaDatabase.get(app).records()
    val editorId=mutableStateOf("local-draft")
    val editorTitle=mutableStateOf(WorkflowCodec.templates.first().name)
    val editorSteps=mutableStateOf(WorkflowCodec.templates.first().steps)
    var workflowId="local-draft"
    var providerId=""
    var foreground: ()->Boolean = {false}
    private var job: Job?=null
    val runner=WorkflowRunner(object: WorkflowPort {
        override suspend fun ready(step: Step): Boolean {
            val locked=app.getSystemService(KeyguardManager::class.java).isKeyguardLocked
            if(step.action==Action.CLICK_MAPPING)return !locked && SafeAccessibilityService.current!=null
            return step.action in setOf(Action.GENERATE_AI_TEXT,Action.DELAY,Action.STOP) || (!locked && (foreground() || WorkflowBrowserSession.visible))
        }
        override suspend fun act(step: Step, previous: String): String {
            val value=step.value.replace("{{output}}",previous)
            if(step.action==Action.CLICK_MAPPING) {
                val saved=dao.get("button_mapping",step.value) ?: error("Mapping dihapus. Pilih ulang tombol di editor.")
                val mapping=ButtonMapping.decode(saved.body)
                check(mapping.enabled && mapping.fingerprint()==step.selector && mapping.scope.packageName==step.packageName){"Versi mapping berubah/nonaktif. Pilih ulang dan review workflow."}
                val active=SafeAccessibilityService.current?.foregroundPackage()
                if(active!=mapping.scope.packageName || mapping.scope.packageName==MappingPolicy.BYGA)openMappingTarget(app,mapping)
                val result=MappingRuntime.execute(app,mapping)
                dao.save(Record("mapping_test",mapping.id,result))
                return previous
            }
            if(step.action in WebWorkflowPolicy.actions) {
                val result=WorkflowBrowserSession.execute(app,step.action,value)
                return if(step.action in setOf(Action.CHATGPT_COPY_RESULT,Action.CHATGPT_COLLECT_RESULT,Action.CHATGPT_DOWNLOAD_IMAGE,Action.FLOW_DOWNLOAD_VIDEO))result else previous
            }
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
            val web=plan.steps.any{it.action in WebWorkflowPolicy.actions}
            if(web && resume) { runner.refuse("Sesi browser tidak disimpan. Periksa hasil situs lalu mulai workflow baru; jangan ulangi generate yang sudah terjadi.");return@launch }
            if(web && Guard.canRun(plan,approved))WorkflowBrowserSession.begin(false)
            WorkflowBrowserSession.onStatus=runner::reportBrowserStatus
            WorkflowBrowserSession.onPauseChanged={if(it)pause() else resume()}
            WorkflowBrowserSession.paused=false
            MappingRuntime.paused=false
            try { runner.run(plan,approved,saved) }
            finally { WorkflowBrowserSession.onPauseChanged=null;if(web)WorkflowBrowserSession.end();withContext(NonCancellable) { dao.save(Record("execution",workflowId,runner.snapshot.value.state.name+" • "+runner.snapshot.value.note)) } }
        }
    }
    fun stop() { MappingRuntime.cancel();runner.stop(); job?.cancel();WorkflowBrowserSession.end() }
    fun pause() { MappingRuntime.paused=true;runner.pause();WorkflowBrowserSession.paused=true }
    fun resume() { MappingRuntime.paused=false;runner.resume();WorkflowBrowserSession.paused=false }
    fun active()=job?.isActive==true
    override fun onCleared() { StudioSessions.unregister("workflow"); super.onCleared() }
}
@Composable fun WorkflowStudio() {
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    val dao=remember { BygaDatabase.get(context).records() }
    val vm: WorkflowViewModel=viewModel()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    vm.foreground={lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)}
    val status by vm.runner.snapshot.collectAsStateWithLifecycle()
    val mappingRows by remember { dao.observe("button_mapping") }.collectAsStateWithLifecycle(initialValue=emptyList())
    val rows by remember { dao.observe("workflow") }.collectAsStateWithLifecycle(initialValue=emptyList())
    val providers by remember { dao.observe("provider") }.collectAsStateWithLifecycle(initialValue=emptyList())
    var id by vm.editorId; var title by vm.editorTitle
    var steps by vm.editorSteps
    var approval by remember { mutableStateOf<String?>(null) }; var note by remember { mutableStateOf("") }
    var provider by rememberSaveable { mutableStateOf("") }; var aiConsent by remember { mutableStateOf(false) }
    var imported by remember { mutableStateOf("") }
    var appStep by remember { mutableStateOf<Int?>(null) }
    var imageStep by rememberSaveable { mutableStateOf<Int?>(null) }
    var imagePlan by rememberSaveable { mutableStateOf("") }
    val plan=Plan(title,steps)
    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val index=imageStep
        if(uri!=null && index!=null && index in steps.indices && plan.fingerprint()==imagePlan && !vm.active()) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                .onSuccess { steps=steps.toMutableList().apply{set(index,steps[index].copy(value=uri.toString()))};approval=null;note="Gambar dipilih; akan diunggah hanya saat workflow dijalankan." }
                .onFailure { note="Izin gambar tidak dapat disimpan. Pilih gambar dari penyimpanan perangkat." }
        }
        imageStep=null
    }
    appStep?.let { index ->
        InstalledAppPicker(steps.getOrNull(index)?.packageName ?: "",{ pkg ->
            if(!vm.active() && steps.getOrNull(index)?.action==Action.LAUNCH_APP) { steps=steps.toMutableList().apply{set(index,steps[index].copy(packageName=pkg))};approval=null }
            appStep=null
        },{appStep=null})
    }
    fun load(p: Plan, selected: String=UUID.randomUUID().toString()) { if(vm.active()) { note="Stop workflow aktif dahulu"; return }; title=p.name; steps=p.steps; id=selected; approval=null; note="Draft dimuat; tinjau ulang sebelum run" }
    LaunchedEffect(providers) { if(providers.none{it.id==provider})provider=providers.firstOrNull()?.id ?: "" }
    PremiumCard { Eyebrow("Design your flow"); Text("Dari satu langkah,\nke alur yang rapi.",style=MaterialTheme.typography.headlineSmall); Text("Pilih aplikasi terinstal atau jalankan alur ChatGPT dan Google Flow dari awal sampai hasil tersimpan.",color=Muted) }
    SectionTitle("Template pilihan")
    WorkflowCodec.templates.forEach { t -> OutlinedButton(onClick={load(t)},modifier=Modifier.fillMaxWidth()) { Text(t.name) } }
    if(rows.isNotEmpty()) { SectionTitle("Library (${rows.size})"); PagedItems(rows) { row -> val p=runCatching{WorkflowCodec.decode(row.body)}.getOrNull(); PremiumCard { Text(p?.name ?: "Workflow legacy / tidak valid"); Row { if(p!=null) TextButton(onClick={load(p,row.id)}){Text("Edit")}; TextButton(onClick={scope.launch { dao.delete("workflow",row.id) }},enabled=!vm.active()){Text("Hapus")}; if(p!=null) TextButton(onClick={Handoffs.share(context,WorkflowCodec.encode(p))}){Text("Ekspor")} } } } }
    SectionTitle("Editor")
    OutlinedTextField(title,{title=it;approval=null},label={Text("Nama workflow")},modifier=Modifier.fillMaxWidth(),enabled=!vm.active())
    PagedItems(steps.withIndex().toList()) { (index,step) -> PremiumCard {
        Text("${index+1}  /  ${step.action.displayName()}",style=MaterialTheme.typography.titleMedium)
        when(step.action) {
            Action.CLICK_MAPPING -> {
                val selected=mappingRows.firstOrNull{it.id==step.value}?.let{runCatching{ButtonMapping.decode(it.body)}.getOrNull()}
                Text(selected?.name ?: "Pilih tombol dari Smart Mapping")
                if(selected!=null)Text("${selected.scope.packageName} • ${selected.scope.site.ifBlank{selected.scope.screen}}",color=Muted,style=MaterialTheme.typography.bodySmall)
                if(selected!=null && selected.fingerprint()!=step.selector)Text("Mapping berubah. Pilih ulang untuk memperbarui versi.",color=MaterialTheme.colorScheme.error)
                Row(Modifier.horizontalScroll(rememberScrollState())) { PagedItems(mappingRows) { row ->
                    val m=runCatching{ButtonMapping.decode(row.body)}.getOrNull()
                    if(m!=null && m.enabled)FilterChip(selected=step.value==m.id && step.selector==m.fingerprint(),enabled=!vm.active(),onClick={steps=steps.toMutableList().apply{set(index,Step(Action.CLICK_MAPPING,m.id,m.scope.packageName,m.fingerprint()))};approval=null},label={Text(m.name)})
                } }
                if(mappingRows.isEmpty())Text("Buka Lainnya → Smart Mapping untuk membuat mapping melalui bubble.",color=Muted)
            }
            Action.LAUNCH_APP -> {
                val label=remember(step.packageName) { runCatching{context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(step.packageName,0)).toString()}.getOrNull() }
                Text(label ?: if(step.packageName.isBlank())"Belum memilih aplikasi" else "Aplikasi tidak tersedia pada profil ini")
                if(step.packageName.isNotBlank())Text(step.packageName,style=MaterialTheme.typography.bodySmall,color=Muted)
                OutlinedButton(enabled=!vm.active(),onClick={appStep=index}){Text("Pilih aplikasi terinstal")}
            }
            Action.FLOW_UPLOAD_IMAGE -> {
                val name=remember(step.value) { runCatching {
                    context.contentResolver.query(Uri.parse(step.value),arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->if(c.moveToFirst())c.getString(0) else null}
                }.getOrNull() }
                Text(if(step.value=="{{output}}")"Pakai gambar dari hasil langkah sebelumnya" else name ?: "Belum memilih gambar")
                Text("JPEG, PNG, WebP • maks. 20MB",style=MaterialTheme.typography.bodySmall,color=Muted)
                OutlinedButton(enabled=!vm.active(),onClick={imageStep=index;imagePlan=plan.fingerprint();imagePicker.launch(arrayOf("image/jpeg","image/png","image/webp"))}){Text("Pilih gambar perangkat")}
                TextButton(enabled=!vm.active(),onClick={steps=steps.toMutableList().apply{set(index,step.copy(value="{{output}}"))};approval=null}){Text("Pakai hasil langkah sebelumnya")}
            }
            Action.OPEN_CHATGPT,Action.OPEN_FLOW -> Text("Halaman resmi dibuka dalam browser workflow BYGA. Login manual satu kali; sesi tersimpan pada perangkat.",color=Muted)
            Action.CHATGPT_COLLECT_RESULT -> Text("Menunggu jawaban baru selesai. Teks disalin ke clipboard; gambar diunduh ke galeri BYGA.",color=Muted)
            Action.CHATGPT_COPY_RESULT,Action.CHATGPT_DOWNLOAD_IMAGE,Action.FLOW_CREATE_PROJECT,Action.FLOW_GENERATE,Action.FLOW_DOWNLOAD_VIDEO,Action.STOP -> Text(when(step.action){Action.FLOW_GENERATE->"Generate satu kali memakai akun, model, dan kredit Flow yang dipilih pada situs.";Action.FLOW_DOWNLOAD_VIDEO->"Menunggu video baru selesai, lalu simpan ke Movies/BYGA (maks. 50MB).";Action.CHATGPT_DOWNLOAD_IMAGE->"Simpan gambar pada jawaban baru ke Pictures/BYGA (maks. 50MB).";else->"Langkah berjalan setelah prasyarat pada halaman terverifikasi."},color=Muted)
            else -> OutlinedTextField(step.value,{v->steps=steps.toMutableList().apply {set(index,step.copy(value=v))};approval=null},label={Text(if(step.action==Action.DELAY)"Jeda 0–30000 ms" else "Isi / prompt / persetujuan")},modifier=Modifier.fillMaxWidth(),enabled=!vm.active(),maxLines=5)
        }
        Row { TextButton(enabled=index>0 && !vm.active(),onClick={steps=steps.toMutableList().apply{add(index-1,removeAt(index))};approval=null}){Text("↑ Naik")}; TextButton(enabled=index<steps.lastIndex && !vm.active(),onClick={steps=steps.toMutableList().apply{add(index+1,removeAt(index))};approval=null}){Text("↓ Turun")}; TextButton(enabled=!vm.active(),onClick={steps=steps.toMutableList().apply{removeAt(index)};approval=null}){Text("Hapus")} }
    } }
    Row(Modifier.horizontalScroll(rememberScrollState())) { (listOf(Action.LAUNCH_APP,Action.CLICK_MAPPING)+WebWorkflowPolicy.actions+listOf(Action.COPY_TEXT,Action.GENERATE_AI_TEXT,Action.REQUEST_USER_APPROVAL,Action.DELAY,Action.DOWNLOAD_URL,Action.STOP)).forEach { action -> TextButton(enabled=!vm.active() && steps.size<50,onClick={steps=steps+Step(action,when(action){Action.DELAY->"500";Action.COPY_TEXT->"{{output}}";Action.GENERATE_AI_TEXT->"Buat caption Indonesia singkat";else->""});approval=null}){Text("+ ${action.displayName()}")} } }
    Text("Gunakan {{output}} untuk memakai hasil langkah sebelumnya. Output disimpan privat bersama checkpoint; hasil tidak pasti harus diperiksa manual dan tidak diulang otomatis.",color=Muted,style=MaterialTheme.typography.bodySmall)
    if(steps.any{it.action==Action.CLICK_MAPPING})Text("Mapping mengirim perintah klik satu kali, bukan bukti hasil Generate/Download. Siapkan layar tujuan. Posisi manual atau opsi selalu tanya memerlukan bubble aktif. Mapping berubah, target kembar, atau layar berbeda akan menghentikan langkah.",color=Muted,style=MaterialTheme.typography.bodySmall)
    if(steps.any{it.action in WebWorkflowPolicy.actions}) PremiumCard {
        Text("Sesi ChatGPT / Google Flow",style=MaterialTheme.typography.titleMedium)
        if(vm.active())TextButton(onClick={context.startActivity(Intent(context,WorkflowBrowserActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))}){Text("Lanjutkan di browser workflow")}
        TextButton(enabled=!vm.active(),onClick={android.webkit.CookieManager.getInstance().removeAllCookies{note="Sesi login browser pada perangkat dihapus"};android.webkit.WebStorage.getInstance().deleteAllData()}){Text("Hapus sesi login browser")}
        Text("Alur berjalan saat browser BYGA terlihat dan HP terbuka. Login, pemilihan mode/model Flow, serta crop gambar dapat memerlukan tindakan Anda. Prompt/gambar dikirim ke situs dengan akun Anda; Generate menggunakan kuota/kredit akun. Jika situs menolak login WebView, alur ini belum dapat diteruskan pada perangkat tersebut.",color=Muted)
        Text("Saat halaman/login belum siap, status menunggu dan petunjuk tampil di browser serta Monitor, maksimal 60 detik. Halaman gagal langsung menghentikan langkah. Generasi media dapat menunggu hingga 10 menit. Setelah browser ditutup atau proses mati, periksa hasil sebelum memulai alur baru.",style=MaterialTheme.typography.bodySmall,color=Muted)
    }
    if(steps.any{it.action==Action.GENERATE_AI_TEXT}) PremiumCard {
        Text("Provider untuk langkah AI")
        Row(Modifier.horizontalScroll(rememberScrollState())) { providers.forEach { r-> val j=JSONObject(r.body); FilterChip(provider==r.id,{provider=r.id;aiConsent=false},label={Text(j.optString("model"))}) } }
        Row { Checkbox(aiConsent,{aiConsent=it;approval=null}); Text("Saya setuju prompt langkah AI dikirim ke model ini. Batas 12 request/hari bersama AI Chat, tanpa fallback otomatis.") }
    }
    Row { OutlinedButton(onClick={note=plan.validate().joinToString("\n").ifBlank{"Dry run valid; tidak ada aksi dijalankan"}}){Text("Dry run")}; Spacer(Modifier.width(8.dp)); Button(enabled=!vm.active(),onClick={if(plan.validate().isEmpty())scope.launch{dao.save(Record("workflow",id,WorkflowCodec.encode(plan)));note="Workflow disimpan"} else note="Validasi belum lulus"}){Text("Simpan")}; TextButton(enabled=!vm.active(),onClick={load(WorkflowCodec.templates.first())}){Text("Baru")} }
    Row { Checkbox(approval==plan.fingerprint(),{approval=if(it)plan.fingerprint() else null},enabled=!vm.active()); Text("Saya meninjau aplikasi, prompt, gambar, akun situs, serta penggunaan kuota/kredit pada semua langkah ini.") }
    plan.validate().take(5).forEach{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
    fun start(resume: Boolean) { if(steps.any{it.action==Action.GENERATE_AI_TEXT} && (!aiConsent||provider.isBlank())) {note="Pilih provider dan berikan consent AI dahulu";return}; vm.workflowId=id;vm.providerId=provider;vm.start(plan,approval,resume) }
    Row { Button(enabled=!vm.active() && Guard.canRun(plan,approval),onClick={start(false)}){Text("Jalankan")}; TextButton(enabled=!vm.active() && steps.none{it.action in WebWorkflowPolicy.actions},onClick={start(true)}){Text("Resume checkpoint")} }
    if(note.isNotBlank())Text(note,color=Cyan)
    PremiumCard { StatusPill(status.state.name,status.state !in setOf(RunState.BLOCKED,RunState.UNKNOWN)); Text("Langkah ${minOf(status.index+1,status.total)} / ${status.total}"); Text(status.note); if(status.total>0)LinearProgressIndicator(progress={status.index.toFloat()/status.total},modifier=Modifier.fillMaxWidth()); if(status.output.isNotBlank()){Text(status.output.take(6000));TextButton(onClick={Handoffs.copy(context,status.output)}){Text("Salin hasil")}}; Row { TextButton(onClick={vm.pause()}){Text("Pause")}; TextButton(onClick={vm.resume()}){Text("Resume")}; TextButton(onClick={vm.stop()}){Text("Stop")} }; if(vm.runner.awaitingApproval() && vm.active()) Button(onClick={vm.runner.approve()}){Text("Saya menyetujui, lanjutkan")} }
    SectionTitle("Impor JSON", "Impor hanya menjadi draft; scope dan approval tidak ikut diimpor.")
    OutlinedTextField(imported,{imported=it.take(128000)},label={Text("Workflow JSON")},modifier=Modifier.fillMaxWidth(),maxLines=5)
    Button(enabled=!vm.active(),onClick={runCatching{WorkflowCodec.decode(imported)}.onSuccess{load(it)}.onFailure{note="JSON/skema workflow tidak valid"}}){Text("Validasi & impor draft")}
}
