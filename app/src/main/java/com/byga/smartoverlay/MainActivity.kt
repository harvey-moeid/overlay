package com.byga.smartoverlay

import android.Manifest
import com.byga.smartoverlay.ui.MascotSettings
import com.byga.smartoverlay.agent.AgentScreen
import com.byga.smartoverlay.agent.AgentSessions
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.byga.smartoverlay.accessibility.SafeAccessibilityService
import com.byga.smartoverlay.ai.*
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.media.Handoffs
import com.byga.smartoverlay.media.DirectDownload
import com.byga.smartoverlay.media.DownloadResult
import com.byga.smartoverlay.overlay.OverlayService
import com.byga.smartoverlay.scheduler.ReminderWorker
import com.byga.smartoverlay.workflow.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { BygaApp() } }

    @Composable private fun BygaApp() {
        val scope=rememberCoroutineScope()
        val dao=remember { BygaDatabase.get(this).records() }
        val secrets=remember { SecretStore(this) }
        var tab by remember { mutableStateOf("Mulai") }
        var message by remember { mutableStateOf("Semua tindakan dimulai oleh Anda. Tidak ada publikasi otomatis.") }
        var prompt by remember { mutableStateOf("") }
        var overlay by remember { mutableStateOf(Settings.canDrawOverlays(this)) }
        var accessibility by remember { mutableStateOf(SafeAccessibilityService.current != null) }
        val owner=LocalLifecycleOwner.current
        DisposableEffect(owner) {
            val observer=LifecycleEventObserver { _, event -> if(event==Lifecycle.Event.ON_RESUME) { overlay=Settings.canDrawOverlays(this@MainActivity); accessibility=SafeAccessibilityService.current!=null } }
            owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
        }
        val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { message=if(it) "Notifikasi diizinkan" else "Notifikasi ditolak; periksa status jadwal di aplikasi" }
        var media by remember { mutableStateOf<Uri?>(null) }
        val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            media=uri
            if(uri!=null) runCatching { contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        val records by remember(dao) { dao.observe("workflow") }.collectAsState(initial=emptyList())
        val accounts by remember(dao) { dao.observe("account") }.collectAsState(initial=emptyList())
        val providers by remember(dao) { dao.observe("provider") }.collectAsState(initial=emptyList())
        val executions by remember(dao) { dao.observe("execution") }.collectAsState(initial=emptyList())
        val diagnostics by remember(dao) { dao.observe("diagnostic") }.collectAsState(initial=emptyList())
        val schedules by remember(dao) { dao.observe("schedule") }.collectAsState(initial=emptyList())
        var steps by remember { mutableStateOf(listOf(Step(Action.COPY_TEXT,"Halo dari BYGA"),Step(Action.DELAY,"500"),Step(Action.STOP))) }
        var name by remember { mutableStateOf("Salin prompt lokal") }
        var approval by remember { mutableStateOf<String?>(null) }
        var runState by remember { mutableStateOf(RunState.DRAFT) }
        var activeStep by remember { mutableIntStateOf(0) }
        var job by remember { mutableStateOf<Job?>(null) }
        var paused by remember { mutableStateOf(false) }
        val plan=Plan(name,steps)
        fun savePlan() { val payload=JSONObject().put("name",name).put("steps",JSONArray().apply { steps.forEach { put(JSONObject().put("action",it.action.name).put("value",it.value)) } }); scope.launch { dao.save(Record("workflow","local-draft",payload.toString())) } }
        fun stop() { AgentSessions.stopAll(); job?.cancel(); job=null; runState=RunState.STOPPED; paused=false; stopService(Intent(this,OverlayService::class.java)); scope.launch { dao.save(Record("execution","local-draft","STOPPED • checkpoint $activeStep")) } }
        fun startPlan() {
            if(job?.isActive==true) return
            if(!Guard.canRun(plan,approval)) { message="Tinjau dan setujui versi workflow ini dahulu"; return }
            val snapshot=plan
            activeStep=0; paused=false
            job=scope.launch {
                runState=RunState.RUNNING
                try {
                    for((index,step) in snapshot.steps.withIndex()) {
                        activeStep=index
                        while(paused) delay(150)
                        ensureActive()
                        dao.save(Record("execution","local-draft","RUNNING • langkah ${index+1}/${snapshot.steps.size}"))
                        when(step.action) {
                            Action.COPY_TEXT -> Handoffs.copy(this@MainActivity,step.value)
                            Action.DELAY -> delay(step.value.toLong())
                            Action.STOP -> break
                            else -> { runState=RunState.BLOCKED; message="Aksi ${step.action} belum didukung executor PoC"; return@launch }
                        }
                        dao.save(Record("checkpoint","local-draft","${snapshot.fingerprint()}:${index+1}"))
                    }
                    runState=RunState.SUCCEEDED
                } catch (cancelled: CancellationException) { runState=RunState.STOPPED; throw cancelled
                } finally { withContext(NonCancellable) { dao.save(Record("execution","local-draft",runState.name+" • langkah ${activeStep+1}")) } }
            }
        }
        val reduced by remember { settings.data.map { it[booleanPreferencesKey("reduced_motion")] ?: false } }.collectAsState(initial=false)
        MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF6E9FFF),secondary=Color(0xFF61E5EB),background=Color(0xFF080D19),surface=Color(0xFF111C30))) {
            Surface(modifier=Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                Column(Modifier.safeDrawingPadding().imePadding().padding(18.dp)) {
                    Text("BYGA",style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.secondary)
                    Text("SMART OVERLAY   /   LOCAL FIRST",style=MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("Mulai","Home","Agent Experimental","Monitor","Apps","Accounts","AI Providers","Workflows","Scheduler","Media & Social","Diagnostik","Settings").forEach { label -> FilterChip(selected=tab==label,onClick={tab=label},label={Text(label)}) }
                    }
                    Card(Modifier.fillMaxWidth().padding(vertical=10.dp)) { Text(message,Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall) }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        when(tab) {
                            "Mulai" -> {
                                Text("Kendali selalu di tangan Anda",style=MaterialTheme.typography.headlineSmall)
                                Text("Bubble menggunakan izin tampil di atas aplikasi. Accessibility opsional dapat membaca struktur UI dan mengisi kolom uji aman BYGA setelah Anda menekan tombol. Tidak ada rekaman layar, login, atau posting otomatis.")
                                Text("Overlay: ${if(overlay) "DIIZINKAN" else "BELUM DIIZINKAN"} • Accessibility: ${if(accessibility) "AKTIF" else "NONAKTIF"}")
                                Button(onClick={ startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName"))) }) { Text("Atur izin bubble") }
                                var disclosure by remember { mutableStateOf(false) }
                                Row { Checkbox(disclosure,{disclosure=it}); Text("Saya memahami lingkup pembacaan UI opsional") }
                                Button(enabled=disclosure,onClick={startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}) { Text("Buka pengaturan Accessibility") }
                                Button(onClick={if(Build.VERSION.SDK_INT>=33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) else message="Notifikasi tidak perlu runtime permission"}) { Text("Izin notifikasi") }
                                Button(onClick={ if(overlay) { ContextCompat.startForegroundService(this@MainActivity,Intent(this@MainActivity,OverlayService::class.java)); message="Bubble diminta; verifikasi tampil di HP" } else message="Berikan izin overlay terlebih dahulu" }) { Text("Mulai bubble") }
                            }
                            "Agent Experimental" -> { AgentScreen() }
                            "Home" -> {
                                Text("Panel prompt",style=MaterialTheme.typography.headlineSmall)
                                OutlinedTextField(prompt,{prompt=it},label={Text("Teks / prompt")},modifier=Modifier.fillMaxWidth(),minLines=3)
                                Button(onClick={Handoffs.copy(this@MainActivity,prompt); message="Teks disalin; tempel manual di aplikasi tujuan"}) { Text("Salin prompt") }
                                Button(onClick={runCatching { Handoffs.share(this@MainActivity,prompt) }.onFailure { message="Sharesheet tidak tersedia" }}) { Text("Bagikan teks") }
                                Button(onClick={scope.launch { dao.save(Record("prompt","saved",prompt)); message="Prompt disimpan lokal" }}) { Text("Simpan prompt") }
                                Button(onClick={scope.launch { prompt=dao.get("prompt","saved")?.body ?: "" }}) { Text("Muat prompt") }
                                Text("Uji Accessibility: buka kolom aman di bawah, lalu tekan Test Input. Tidak mengirim pesan.")
                                var safeText by remember { mutableStateOf("") }
                                OutlinedTextField(safeText,{safeText=it},label={Text("Kolom uji aman")},modifier=Modifier.fillMaxWidth().semantics { contentDescription="byga_safe_input" })
                                Button(onClick={message="Test Input: ${SafeAccessibilityService.current?.testInput("Uji BYGA berhasil") ?: "PERMISSION_DENIED"}"}) { Text("Test Input") }
                            }
                            "Monitor" -> {
                                Text("${runState.name} • langkah ${activeStep+1}",style=MaterialTheme.typography.headlineSmall)
                                Text("Executor lokal: salin teks, delay, stop. Checkpoint tidak dilanjutkan otomatis setelah restart.")
                                Button(onClick={if(job?.isActive==true) { paused=true; runState=RunState.PAUSED }}) { Text("Pause") }
                                Button(onClick={if(job?.isActive==true) { paused=false; runState=RunState.RUNNING } else message="Tidak ada eksekusi aktif; tinjau ulang sebelum run"}) { Text("Resume") }
                                executions.forEach { Text(it.body) }
                            }
                            "Apps" -> {
                                Text("Aplikasi tujuan",style=MaterialTheme.typography.headlineSmall)
                                Text("Peluncuran manual; kontrol UI pihak ketiga BLOCKED sampai diuji dan profil deterministik tersedia.")
                                listOf("ChatGPT" to "com.openai.chatgpt","Claude" to "com.anthropic.claude","TikTok" to "com.zhiliaoapp.musically","Instagram" to "com.instagram.android").forEach { (label,pkg) ->
                                    OutlinedButton(onClick={ message=if(Handoffs.launch(this@MainActivity,pkg)) "$label dibuka; kompatibilitas belum diuji" else "$label tidak tersedia. Gunakan browser / sharesheet." }) { Text("Buka $label") }
                                }
                                OutlinedButton(onClick={startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://labs.google/flow/")))}) { Text("Google Flow • web") }
                                var custom by remember { mutableStateOf("") }
                                OutlinedTextField(custom,{custom=it},label={Text("Package aplikasi lain")})
                                Button(onClick={if(custom.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*([.][a-zA-Z0-9_]+)+"))) { scope.launch { dao.save(Record("app",custom,custom)) }; message=if(Handoffs.launch(this@MainActivity,custom)) "Aplikasi dibuka" else "Package tidak tersedia/terlihat; gunakan sharesheet" } else message="Package tidak valid"}) { Text("Simpan profil & buka") }
                            }
                            "Accounts" -> {
                                Text("Profil akun lokal",style=MaterialTheme.typography.headlineSmall)
                                Text("Tidak menyimpan password. Ganti akun melalui menu resmi aplikasi. Konfirmasi di sini tidak membuktikan akun aktif; publikasi otomatis BLOCKED.")
                                var app by remember { mutableStateOf("Instagram") }; var alias by remember { mutableStateOf("") }
                                OutlinedTextField(app,{app=it},label={Text("Aplikasi")})
                                OutlinedTextField(alias,{alias=it},label={Text("Label / alias publik")})
                                Button(onClick={if(alias.isNotBlank()&&app.isNotBlank()) scope.launch { dao.save(Record("account",java.util.UUID.randomUUID().toString(),"$app • $alias • UNKNOWN")) }}) { Text("Tambah profil") }
                                accounts.forEach { account -> Card { Column(Modifier.padding(12.dp)) { Text(account.body); TextButton(onClick={scope.launch{dao.delete("account",account.id)}}) { Text("Hapus profil") } } } }
                            }
                            "AI Providers" -> {
                                Text("Provider AI • Experimental",style=MaterialTheme.typography.headlineSmall)
                                Text("Permintaan teks langsung dari HP. Kunci dienkripsi Android Keystore. Pilih model dari daftar saran/API atau isi ID sesuai akun Anda. Vision, role routing, budget cap belum tersedia.")
                                ProviderEditor { message=it }
                                Text("Test koneksi mengirim teks 'Balas singkat: OK' ke endpoint yang Anda simpan dan dapat dikenakan biaya.")
                                providers.forEach { record ->
                                    val json=JSONObject(record.body)
                                    Card { Column(Modifier.padding(12.dp)) {
                                        Text("${ProviderCatalog.get(ProviderType.valueOf(json.getString("type"))).label} • ${json.getString("model")} • key ••••••")
                                        Button(onClick={scope.launch { message="Menguji provider…"; val result=try { HttpProvider(ProviderConfig(record.id,ProviderType.valueOf(json.getString("type")),json.getString("url"),json.getString("model")),secrets.read(record.id) ?: "").text("Balas singkat: OK") } catch(_: Exception) { AiResult.Failure("KEY_UNAVAILABLE") }; message=when(result) { is AiResult.Text -> "Respons provider diterima"; is AiResult.Failure -> result.code } }}) { Text("Kirim test koneksi") }
                                        TextButton(onClick={scope.launch { dao.delete("provider",record.id); secrets.delete(record.id) }}) { Text("Hapus") }
                                    } }
                                }
                                Button(onClick={scope.launch { val r=FakeProvider().text(prompt); prompt=r.value; message="Konsep fake lokal disalin ke panel Home; bukan hasil API" }}) { Text("Coba fake provider tanpa biaya") }
                            }
                            "Workflows" -> {
                                Text("Workflow Studio • PoC lokal",style=MaterialTheme.typography.headlineSmall)
                                Text("Langkah salin teks, delay dan stop dapat dijalankan. DSL lain BLOCKED. Perubahan membatalkan persetujuan versi lama.")
                                OutlinedTextField(name,{name=it; approval=null},label={Text("Nama workflow")})
                                steps.forEachIndexed { index,step ->
                                    Card { Column(Modifier.padding(12.dp)) {
                                        Text("${index+1}. ${step.action}")
                                        OutlinedTextField(step.value,{value -> steps=steps.toMutableList().apply { set(index,step.copy(value=value)) }; approval=null},label={Text(if(step.action==Action.DELAY) "Milidetik" else "Teks")},modifier=Modifier.fillMaxWidth())
                                        Row { TextButton(onClick={steps=steps.toMutableList().apply { removeAt(index) }; approval=null}) { Text("Hapus") }; if(index>0) TextButton(onClick={steps=steps.toMutableList().apply { val old=removeAt(index); add(index-1,old) }; approval=null}) { Text("Naik") } }
                                    } }
                                }
                                Row { TextButton(onClick={steps=steps+Step(Action.COPY_TEXT,prompt); approval=null}){Text("+ Salin")}; TextButton(onClick={steps=steps+Step(Action.DELAY,"1000"); approval=null}){Text("+ Delay")}; TextButton(onClick={steps=steps+Step(Action.STOP); approval=null}){Text("+ Stop")} }
                                Button(onClick={message=plan.validate().joinToString().ifEmpty { "Dry run valid; tidak ada aksi dijalankan" }}) { Text("Validasi / dry run") }
                                Button(onClick={savePlan(); message="Workflow disimpan lokal"}) { Text("Simpan") }
                                records.firstOrNull()?.let { saved -> Button(onClick={val j=JSONObject(saved.body); name=j.getString("name"); val array=j.getJSONArray("steps"); steps=(0 until array.length()).map { val s=array.getJSONObject(it); Step(Action.valueOf(s.getString("action")),s.getString("value")) }; approval=null}) { Text("Muat workflow tersimpan") } }
                                Row { Checkbox(approval==plan.fingerprint(),{approval=if(it)plan.fingerprint() else null}); Text("Saya telah meninjau semua langkah versi ini") }
                                Button(enabled=job?.isActive!=true,onClick={startPlan()}) { Text("Jalankan lokal") }
                            }
                            "Scheduler" -> {
                                Text("Pengingat workflow",style=MaterialTheme.typography.headlineSmall)
                                Text("WorkManager tidak menjamin menit tepat. UI selalu WAITING_FOR_USER; tidak auto-unlock atau membuka aplikasi diam-diam. Zona acuan Asia/Jakarta. Dependency/priority queue belum tersedia.")
                                var minutes by remember { mutableStateOf("15") }; var recurring by remember { mutableStateOf(false) }
                                OutlinedTextField(minutes,{minutes=it},label={Text("Jeda / interval menit (berulang minimal 15)")})
                                Row { Checkbox(recurring,{recurring=it}); Text("Berulang") }
                                Button(onClick={val m=minutes.toLongOrNull(); if(m==null||m !in 1..525600||(recurring&&m<15)) message="Interval tidak valid" else { ReminderWorker.schedule(this@MainActivity,m,recurring); scope.launch { dao.save(Record("schedule","local-draft","ENQUEUED • $m menit • ${if(recurring) "berulang" else "sekali"}")) }; message="Pengingat terdaftar; eksekusi perlu tindakan pengguna" }}) { Text("Jadwalkan pengingat") }
                                Button(onClick={ReminderWorker.cancel(this@MainActivity); scope.launch { dao.save(Record("schedule","local-draft","CANCELLED")) }}) { Text("Batalkan pengingat") }
                                schedules.forEach { Text(it.body) }
                            }
                            "Media & Social" -> {
                                Text("Siapkan draft sosial",style=MaterialTheme.typography.headlineSmall)
                                Text("Pilih media lokal via pemilih sistem, tinjau caption di Home, lalu sharesheet. Status handoff bukan PUBLISHED; cek akun dan konfirmasi posting di aplikasi tujuan.")
                                Button(onClick={pick.launch(arrayOf("image/*","video/*"))}) { Text("Pilih media") }
                                Text(if(media==null) "Belum ada media" else "Media lokal dipilih")
                                Button(enabled=media!=null,onClick={runCatching { Handoffs.share(this@MainActivity,prompt,media); message="HANDED_OFF • cek akun dan konfirmasi di aplikasi tujuan" }.onFailure {message="FAILED • media tidak dapat dibagikan"}}) { Text("Bagikan draft dengan caption") }
                                var downloadUrl by remember { mutableStateOf("") }
                                var downloading by remember { mutableStateOf(false) }
                                OutlinedTextField(downloadUrl,{downloadUrl=it},label={Text("URL media HTTPS langsung")},modifier=Modifier.fillMaxWidth())
                                Text("Unduh hanya media yang Anda berhak akses. Maksimal 50MB, JPEG/PNG/WebP/MP4/WebM. Redirect/login ditolak. Android 10+.")
                                Button(enabled=!downloading,onClick={downloading=true; scope.launch { try { when(val r=DirectDownload.save(this@MainActivity,downloadUrl)) { is DownloadResult.Saved -> { media=r.uri; message="Media tersimpan dan diverifikasi di galeri"; dao.save(Record("media",r.uri.toString(),"VERIFIED_LOCAL")) }; is DownloadResult.Blocked -> message=r.code } } finally { downloading=false } }}) { Text(if(downloading) "Mengunduh…" else "Unduh dengan izin saya") }
                                Text("Native download: gunakan tombol resmi aplikasi secara manual. Screenshot / Vision: NOT_IMPLEMENTED, tidak ada capture yang berjalan.")
                            }
                            "Diagnostik" -> {
                                Text("Diagnostics & PoC Matrix",style=MaterialTheme.typography.headlineSmall)
                                Text("Status awal NOT_TESTED. Tombol di bawah adalah catatan manual Anda, bukan bukti tes otomatis.")
                                listOf("P01 Instal APK","P02 Overlay drag/dock/stop","P03 Izin Accessibility","P04 Input aman","P05 Screenshot consent","P06 Native download","P07 URL download","P08 Share sosial","P09 Switch akun","P10 Scheduler terkunci","P11 AI fallback","P12 Crash/resume guard","P13 CI APK").forEach { id ->
                                    val result=diagnostics.find{it.id==id}?.body ?: "NOT_TESTED"
                                    Card { Column(Modifier.padding(12.dp)) { Text("$id • $result"); Row { listOf("PASS","FAIL","BLOCKED","NOT_TESTED").forEach { state -> TextButton(onClick={scope.launch{dao.save(Record("diagnostic",id,"$state • manual"))}}){Text(state,style=MaterialTheme.typography.labelSmall)} } } } }
                                }
                            }
                            "Settings" -> {
                                Text("Privasi & kontrol",style=MaterialTheme.typography.headlineSmall)
                                MascotSettings()
                                Text("Tidak ada analytics/backend. API key dienkripsi, tidak dibackup. Perangkat yang telah dikompromikan tetap berisiko. Tidak ada capture layar aktif. Accessibility pihak ketiga dinonaktifkan pada PoC.")
                                Row { Checkbox(reduced,{value -> scope.launch { settings.edit { it[booleanPreferencesKey("reduced_motion")]=value } }}); Text("Kurangi gerak bubble") }
                                Text("Pengaturan ini menonaktifkan animasi dock bubble. Durasi animator sistem juga dihormati.")
                                Button(onClick={scope.launch { dao.clearHistory(); message="Riwayat, prompt dan diagnostik dihapus" }}) { Text("Hapus riwayat lokal") }
                                Button(onClick={startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))}) { Text("Izin & penyimpanan Android") }
                            }
                        }
                    }
                    Button(onClick={stop(); message="STOP • workflow dibatalkan dan bubble ditutup"},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF842C41))) { Text("STOP DARURAT") }
                }
            }
        }
    }
}
