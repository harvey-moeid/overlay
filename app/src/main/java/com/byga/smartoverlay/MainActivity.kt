package com.byga.smartoverlay

import android.Manifest
import com.byga.smartoverlay.scheduler.ScheduleStudio
import com.byga.smartoverlay.ui.*
import com.byga.smartoverlay.chat.ChatStudio
import com.byga.smartoverlay.profiles.AccountsScreen
import com.byga.smartoverlay.publishing.PublishingStudio
import com.byga.smartoverlay.capture.CaptureScreen
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
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
    private var requestedDestination by mutableStateOf<String?>(null)
    private fun destination(intent:Intent?)=when { intent?.getBooleanExtra("open_mapping",false)==true -> "Smart Mapping";intent?.getBooleanExtra("open_workflow",false)==true -> "Workflows";intent?.getBooleanExtra("open_capture",false)==true -> "Capture";intent?.getBooleanExtra("open_schedule",false)==true -> "Scheduler";else -> null }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); requestedDestination=destination(intent);setContent { BygaApp() } }
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);requestedDestination=destination(intent)}

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun BygaApp() {
        val scope=rememberCoroutineScope()
        DisposableEffect(Unit) { StudioSessions.register("workspace-actions") { scope.coroutineContext.cancelChildren() };onDispose { StudioSessions.unregister("workspace-actions") } }
        val dao=remember { BygaDatabase.get(this).records() }
        val secrets=remember { SecretStore(this) }
        var tab by rememberSaveable { mutableStateOf("Home") }
        LaunchedEffect(requestedDestination) { requestedDestination?.let{tab=it;requestedDestination=null} }
        val screenState=rememberSaveableStateHolder()
        var chatSeed by rememberSaveable { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        var prompt by rememberSaveable { mutableStateOf("") }
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
        val workflowCount by remember(dao) { dao.count("workflow") }.collectAsState(initial=0)
        val appProfiles by remember(dao) { dao.observe("app") }.collectAsState(initial=emptyList())
        val accountCount by remember(dao) { dao.count("account") }.collectAsState(initial=0)
        val providers by remember(dao) { dao.observe("provider") }.collectAsState(initial=emptyList())
        val executions by remember(dao) { dao.recent("execution",20) }.collectAsState(initial=emptyList())
        val diagnostics by remember(dao) { dao.recent("diagnostic",30) }.collectAsState(initial=emptyList())
        val schedules by remember(dao) { dao.observe("schedule") }.collectAsState(initial=emptyList())
        fun stop() { StudioSessions.stopAll(); AgentSessions.stopAll(); stopService(Intent(this,OverlayService::class.java)); scope.launch { dao.save(Record("audit","last-stop","Pengguna menekan Stop darurat")) } }
        val bubbleRunning by com.byga.smartoverlay.overlay.OverlayRuntime.running.collectAsState()
        fun toggleBubble() { if(bubbleRunning) {stopService(Intent(this,OverlayService::class.java));AgentSessions.stopAll()} else if(overlay) ContextCompat.startForegroundService(this,Intent(this,OverlayService::class.java)) else tab="Mulai" }
        val reduced by remember { settings.data.map { it[booleanPreferencesKey("reduced_motion")] ?: false } }.collectAsState(initial=false)
        var modules by remember { mutableStateOf(false) }
        var showMessage by remember { mutableStateOf(false) }
        val snack=remember { SnackbarHostState() }
        LaunchedEffect(message) { if(message.isNotBlank()) snack.showSnackbar(message,duration=SnackbarDuration.Short) }
        BygaTheme {
            Scaffold(containerColor=Ink, modifier=Modifier.imePadding(), contentWindowInsets=WindowInsets(0,0,0,0),
                topBar={ Row(Modifier.fillMaxWidth().background(Ink).statusBarsPadding().padding(horizontal=20.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.byga.smartoverlay.R.drawable.byga_robot),"Logo robot BYGA BG",Modifier.size(48.dp))
                        Column(Modifier.weight(1f)) { Text("BYGA",style=MaterialTheme.typography.titleLarge,maxLines=1); Text("SMART OVERLAY STUDIO",style=MaterialTheme.typography.labelSmall,color=Muted,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    }
                    Row { IconButton(onClick={showMessage=true}){Icon(Icons.Default.Info,"Lihat status",tint=Muted)}; TextButton(onClick={stop();message="Semua request/workflow dihentikan dan bubble ditutup"}) { Icon(Icons.Default.Close,"Stop darurat semua sesi",tint=Color(0xFFFF849A),modifier=Modifier.size(16.dp));Text("STOP",color=Color(0xFFFF849A),style=MaterialTheme.typography.labelSmall) } }
                } },
                bottomBar={ NavigationBar(containerColor=Color(0xFF0E1728),tonalElevation=0.dp) {
                    listOf("Home","Chat","Workflows","Media & Social").forEach { id -> val d=destinations.first{it.id==id}; NavigationBarItem(selected=tab==id,onClick={tab=id},icon={Icon(d.icon,d.title)},label={Text(when(id){"Workflows"->"Alur";"Media & Social"->"Media";"Chat"->"Chat";else->"Home"})},colors=NavigationBarItemDefaults.colors(indicatorColor=Color(0xFF233E6A),selectedIconColor=Cyan)) }
                    NavigationBarItem(selected=tab !in listOf("Home","Chat","Workflows","Media & Social"),onClick={modules=true},icon={Icon(Icons.Default.Menu,"Semua modul")},label={Text("Lainnya")})
                } },snackbarHost={SnackbarHost(snack)}
            ) { insets ->
                screenState.SaveableStateProvider(tab) {
                Column(Modifier.fillMaxSize().padding(insets).background(Brush.verticalGradient(listOf(Ink,Color(0xFF0B1424)))).padding(horizontal=18.dp).verticalScroll(rememberScrollState()).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    if(tab!="Home") { val d=destinations.firstOrNull{it.id==tab}; Column(Modifier.padding(top=12.dp,bottom=4.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Eyebrow("BYGA / ${d?.title ?: tab}"); Text(d?.title ?: tab,style=MaterialTheme.typography.headlineLarge); Text(d?.caption ?: "",color=Muted)} } else Spacer(Modifier.height(4.dp))
                        when(tab) {
                            "Mulai" -> {
                                Text("Kendali selalu di tangan Anda",style=MaterialTheme.typography.headlineSmall)
                                Text("Bubble menggunakan izin tampil di atas aplikasi. Accessibility opsional membaca metadata tombol saat Smart Mapping atau workflow yang Anda review. Dapat mengklik tombol yang dipetakan dan mengisi kolom fokus sesuai review; posisi manual selalu meminta konfirmasi. Capture memiliki consent sistem terpisah. Capture satu frame hanya setelah consent sistem terpisah; tidak ada login atau posting otomatis.")
                                Text("Overlay: ${if(overlay) "DIIZINKAN" else "BELUM DIIZINKAN"} • Accessibility: ${if(accessibility) "AKTIF" else "NONAKTIF"}")
                                Button(onClick={ startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName"))) }) { Text("Atur izin bubble") }
                                var disclosure by remember { mutableStateOf(false) }
                                Row { Checkbox(disclosure,{disclosure=it}); Text("Saya memahami lingkup pembacaan UI opsional") }
                                Button(enabled=disclosure,onClick={startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}) { Text("Buka pengaturan Accessibility") }
                                Button(onClick={if(Build.VERSION.SDK_INT>=33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) else message="Notifikasi tidak perlu runtime permission"}) { Text("Izin notifikasi") }
                                Button(onClick={ if(overlay) { ContextCompat.startForegroundService(this@MainActivity,Intent(this@MainActivity,OverlayService::class.java)); message="Bubble diminta; verifikasi tampil di HP" } else message="Berikan izin overlay terlebih dahulu" }) { Text("Mulai bubble") }
                            }
                            "Agent Experimental" -> { AgentScreen() }
                            "Home" -> { Dashboard(overlay,accessibility,providers.size,workflowCount,accountCount,bubbleRunning,{tab=it},::toggleBubble) }
                            "Chat" -> { ChatStudio(initialPrompt=chatSeed,onDraft={prompt=it;tab="Media & Social"},onNavigateProviders={tab="AI Providers"}) }
                            "Capture" -> { CaptureScreen() }
                            "Studio" -> { PromptLibrary(onChat={chatSeed=it;tab="Chat"},onDraft={prompt=it;tab="Media & Social"}) }
                            "Monitor" -> { RuntimeMonitor() }
                            "Apps" -> {
                                Text("Aplikasi tujuan",style=MaterialTheme.typography.headlineSmall)
                                var chooseInstalled by remember { mutableStateOf(false) }
                                Button(onClick={chooseInstalled=true}) { Text("Pilih & buka aplikasi terinstal") }
                                if(chooseInstalled)InstalledAppPicker("",{pkg->
                                    chooseInstalled=false
                                    scope.launch{dao.save(Record("app",pkg,pkg))}
                                    message=if(Handoffs.launch(this@MainActivity,pkg))"Aplikasi dibuka" else "Aplikasi tidak dapat dibuka pada profil ini"
                                },{chooseInstalled=false})
                                Text("Input manual eksperimental: fokuskan kolom di app ini, buka panel bubble, review teks/target/akun lalu pilih Isi kolom fokus. Tidak menekan Kirim. Aksi otomatis agent lintas aplikasi tetap BLOCKED sampai diuji.")
                                listOf("ChatGPT" to "com.openai.chatgpt","Claude" to "com.anthropic.claude","TikTok" to "com.zhiliaoapp.musically","Instagram" to "com.instagram.android").forEach { (label,pkg) ->
                                    OutlinedButton(onClick={ message=if(Handoffs.launch(this@MainActivity,pkg)) "$label dibuka; kompatibilitas belum diuji" else "$label tidak tersedia. Gunakan browser / sharesheet." }) { Text("Buka $label") }
                                }
                                OutlinedButton(onClick={runCatching{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://labs.google/flow/")))}.onFailure{message="Browser tidak tersedia; buka Flow melalui browser yang terpasang"}}) { Text("Google Flow • web") }
                                if(appProfiles.isNotEmpty()) SectionTitle("Profil tambahan tersimpan")
                                PagedItems(appProfiles) { profile -> PremiumCard { Text(profile.body,style=MaterialTheme.typography.titleMedium);Row { TextButton(onClick={runCatching{Handoffs.launch(this@MainActivity,profile.body)}.onSuccess{message=if(it)"Aplikasi dibuka" else "Package tidak terlihat/tersedia; gunakan sharesheet"}.onFailure{message="Aplikasi tidak dapat dibuka"}}){Text("Buka")};TextButton(onClick={scope.launch{dao.delete("app",profile.id)}}){Text("Hapus profil")} } } }
                                var custom by remember { mutableStateOf("") }
                                OutlinedTextField(custom,{custom=it},label={Text("Package aplikasi lain")})
                                Button(onClick={if(custom.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*([.][a-zA-Z0-9_]+)+"))) { scope.launch { dao.save(Record("app",custom,custom)) }; message=if(Handoffs.launch(this@MainActivity,custom)) "Aplikasi dibuka" else "Package tidak tersedia/terlihat; gunakan sharesheet" } else message="Package tidak valid"}) { Text("Simpan profil & buka") }
                            }
                            "Accounts" -> { AccountsScreen() }
                            "AI Providers" -> {
                                Text("Provider AI • Experimental",style=MaterialTheme.typography.headlineSmall)
                                Text("Permintaan teks langsung dari HP. Kunci dienkripsi Android Keystore. Pilih model dari daftar saran/API atau isi ID sesuai akun Anda. Vision, role routing, budget cap belum tersedia.")
                                ProviderEditor { message=it }
                                Text("Test koneksi mengirim teks 'Balas singkat: OK' ke endpoint yang Anda simpan dan dapat dikenakan biaya.")
                                providers.forEach { record ->
                                    val json=JSONObject(record.body)
                                    Card { Column(Modifier.padding(12.dp)) {
                                        Text("${ProviderCatalog.get(ProviderType.valueOf(json.getString("type"))).label} • ${json.getString("model")} • key ••••••")
                                        Button(onClick={scope.launch { message="Menguji provider…"; val result=try { HttpProvider(ProviderConfig(record.id,ProviderType.valueOf(json.getString("type")),json.getString("url"),json.getString("model")),secrets.read(record.id) ?: "").text("Balas singkat: OK") } catch(e:CancellationException){throw e} catch(_: Exception) { AiResult.Failure("KEY_UNAVAILABLE") }; message=when(result) { is AiResult.Text -> "Respons provider diterima"; is AiResult.Failure -> result.code } }}) { Text("Kirim test koneksi") }
                                        if(ProviderType.valueOf(json.getString("type"))!=ProviderType.COHERE) Row { Checkbox(json.optBoolean("vision",false),{enabled->scope.launch { dao.save(record.copy(body=JSONObject(record.body).put("vision",enabled).toString(),updatedAt=System.currentTimeMillis())) } });Text("Model mendukung gambar (konfirmasi kemampuan Vision)") }
                                        TextButton(onClick={scope.launch { dao.delete("provider",record.id); secrets.delete(record.id) }}) { Text("Hapus") }
                                    } }
                                }
                                Button(onClick={scope.launch { val r=FakeProvider().text(prompt); prompt=r.value; message="Konsep fake lokal disalin ke panel Home; bukan hasil API" }}) { Text("Coba fake provider tanpa biaya") }
                            }
                            "Workflows" -> { WorkflowStudio() }
                            "Smart Mapping" -> { com.byga.smartoverlay.mapping.MappingStudio() }
                            "Scheduler" -> { ScheduleStudio { tab="Workflows" } }
                            "Media & Social" -> { PublishingStudio(prompt,{prompt=it},{tab="Accounts"}) }
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
                                DataControls()
                                Text("Tidak ada analytics/backend. API key dienkripsi, tidak dibackup. Perangkat yang telah dikompromikan tetap berisiko. Capture hanya berjalan per sesi dengan consent sistem dan review lokal. Aksi Accessibility agent masih dibatasi ke kolom uji BYGA.")
                                Row { Checkbox(reduced,{value -> scope.launch { settings.edit { it[booleanPreferencesKey("reduced_motion")]=value } }}); Text("Kurangi gerak bubble") }
                                Text("Pengaturan ini menonaktifkan animasi dock bubble. Durasi animator sistem juga dihormati.")
                                Button(onClick={scope.launch { dao.clearHistory(); message="Log eksekusi dan diagnostik dihapus; template, chat dan profil tetap tersedia" }}) { Text("Hapus log & diagnostik") }
                                Button(onClick={startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))}) { Text("Izin & penyimpanan Android") }
                            }
                        } }
                    }
            }
            if(modules) ModalBottomSheet(onDismissRequest={modules=false},containerColor=Color(0xFF101B2D)) {
                Column(Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Eyebrow("Everything, in one place"); Text("Ruang kerja Anda",style=MaterialTheme.typography.headlineSmall)
                    destinations.forEach { d -> Surface(onClick={tab=d.id;modules=false},color=if(tab==d.id)Color(0xFF203A61) else Color(0xFF152239),shape=RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) { Icon(d.icon,null,tint=Cyan); Column(Modifier.weight(1f)){Text(d.title,style=MaterialTheme.typography.titleMedium);Text(d.caption,color=Muted,style=MaterialTheme.typography.bodySmall)};Icon(Icons.Default.KeyboardArrowRight,null,tint=Muted) } } }
                }
            }
            if(showMessage) AlertDialog(onDismissRequest={showMessage=false},title={Text("Status terakhir")},text={Text(message.ifBlank{"Belum ada aktivitas"})},confirmButton={TextButton(onClick={showMessage=false}){Text("Tutup")}})
        }
    }
}
