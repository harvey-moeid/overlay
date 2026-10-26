package com.byga.smartoverlay.mapping

import android.content.Intent
import android.provider.Settings
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import com.byga.smartoverlay.accessibility.SafeAccessibilityService
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.media.Handoffs
import com.byga.smartoverlay.overlay.OverlayService
import com.byga.smartoverlay.ui.*
import com.byga.smartoverlay.workflow.*
import kotlinx.coroutines.*
import java.util.UUID

@Composable fun MappingStudio() {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val dao=remember{BygaDatabase.get(context).records()}
    val rows by remember{dao.observe("button_mapping")}.collectAsState(initial=emptyList())
    val results by remember{dao.observe("mapping_test")}.collectAsState(initial=emptyList())
    var note by remember{mutableStateOf("")};var test by remember{mutableStateOf<ButtonMapping?>(null)}
    var edit by remember{mutableStateOf<ButtonMapping?>(null)};var editName by remember{mutableStateOf("")};var ask by remember{mutableStateOf(false)}
    var busy by remember{mutableStateOf(false)};var demoCount by remember{mutableIntStateOf(0)}
    PremiumCard {
        Eyebrow("Teach once, use in your flow")
        Text("Kenali tombol.\nRangkai tindakan.",style=MaterialTheme.typography.headlineSmall)
        Text("Deteksi tombol atau tunjuk langsung lewat robot. Anda memilih Simpan saja, Uji sekarang, atau Batal sebelum ada klik pada aplikasi tujuan.",color=Muted)
    }
    PremiumCard {
        Text("Mulai memetakan",style=MaterialTheme.typography.titleMedium)
        Text("1. Aktifkan Accessibility dan bubble.\n2. Buka ChatGPT, Chrome, atau browser BYGA pada layar tujuan.\n3. Ketuk robot → Smart Mapping → deteksi atau tunjuk.\n4. Beri nama tombol, tinjau, lalu simpan.",color=Muted)
        TextButton(onClick={context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}){Text("Atur Accessibility")}
        Button(onClick={if(Settings.canDrawOverlays(context)) {ContextCompat.startForegroundService(context,Intent(context,OverlayService::class.java));note="Bubble dimulai. Buka aplikasi target, lalu gunakan Smart Mapping dari robot."} else context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:${context.packageName}")))}){Text("Aktifkan robot bubble")}
        OutlinedButton(onClick={note=if(Handoffs.launch(context,MappingPolicy.CHATGPT))"Buka layar tujuan lalu ketuk robot" else "ChatGPT tidak terinstal"}){Text("Buka ChatGPT terinstal")}
        OutlinedButton(onClick={note=if(Handoffs.launch(context,MappingPolicy.CHROME))"Buka chatgpt.com atau Google Flow. Tampilkan address bar saat mapping." else "Chrome tidak terinstal"}){Text("Buka Chrome")}
        Row {
            TextButton(onClick={context.startActivity(Intent(context,WorkflowBrowserActivity::class.java).putExtra("mapping_site","chatgpt").addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))}){Text("ChatGPT Web BYGA")}
            TextButton(onClick={context.startActivity(Intent(context,WorkflowBrowserActivity::class.java).putExtra("mapping_site","flow").addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))}){Text("Flow Web BYGA")}
        }
    }
    if(note.isNotBlank())Text(note,color=Cyan)
    SectionTitle("Mapping tersimpan (${rows.size})", "Perubahan mapping membatalkan kecocokan versi yang dipakai workflow lama. Pilih ulang mapping di editor setelah mengubahnya.")
    if(rows.isEmpty())EmptyState("Belum ada tombol tersimpan","Buka aplikasi tujuan dan mulai dari bubble. Mapping disimpan lokal, tanpa mengunggah isi layar ke AI.")
    PagedItems(rows) { row ->
        val mapping=runCatching{ButtonMapping.decode(row.body)}.getOrNull()
        if(mapping==null)PremiumCard{Text("Mapping tidak valid");TextButton(onClick={scope.launch{dao.delete("button_mapping",row.id)}}){Text("Hapus")}}
        else PremiumCard {
            Row { Text(mapping.name,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f));Switch(mapping.enabled,{enabled->scope.launch{dao.save(Record("button_mapping",mapping.id,mapping.copy(enabled=enabled).encode()))}},enabled=!busy) }
            StatusPill(if(mapping.kind==MappingKind.SEMANTIC)"Identitas tombol" else "Posisi manual • selalu tanya",mapping.kind==MappingKind.SEMANTIC)
            Text("${mapping.scope.packageName}\n${mapping.scope.site.ifBlank{mapping.scope.screen}}\nLayar: ${mapping.scope.screen}",color=Muted)
            Text(if(mapping.askEveryTime)"Meminta konfirmasi setiap kali workflow menekan tombol." else "Setelah workflow direview, tombol dapat diklik otomatis jika cocok tepat.",style=MaterialTheme.typography.bodySmall,color=Muted)
            Text(results.firstOrNull{it.id==mapping.id}?.body ?: "Belum diuji. Simpan mapping tidak membuktikan hasil tombol.",style=MaterialTheme.typography.bodySmall,color=Muted)
            Row {
                TextButton(enabled=!busy && mapping.enabled,onClick={test=mapping}){Text("Uji tombol")}
                TextButton(enabled=!busy,onClick={edit=mapping;editName=mapping.name;ask=mapping.askEveryTime}){Text("Atur")}
                TextButton(enabled=!busy,onClick={scope.launch{dao.delete("button_mapping",mapping.id);dao.delete("mapping_test",mapping.id)}}){Text("Hapus")}
            }
            OutlinedButton(enabled=mapping.enabled && !busy,onClick={scope.launch {
                val plan=Plan("Klik ${mapping.name}".take(120),listOf(Step(Action.CLICK_MAPPING,mapping.id,mapping.scope.packageName,mapping.fingerprint())))
                dao.save(Record("workflow",UUID.randomUUID().toString(),WorkflowCodec.encode(plan)))
                note="Workflow dibuat sebagai draft. Buka Alur → Library, tinjau mapping dan aktifkan aplikasi/layar tujuan sebelum menjalankannya."
            }}){Text("Buat workflow dari tombol ini")}
        }
    }
    edit?.let { mapping -> AlertDialog(onDismissRequest={edit=null},title={Text("Atur mapping")},text={Column {
        OutlinedTextField(editName,{editName=it.take(80)},label={Text("Nama aksi")})
        Row{Checkbox(ask,{ask=it},enabled=mapping.kind!=MappingKind.POINT);Text("Tanya sebelum setiap klik workflow")}
        Text("Untuk mengganti target tombol, lakukan mapping baru melalui bubble.")
    }},confirmButton={TextButton(enabled=editName.isNotBlank(),onClick={scope.launch{dao.save(Record("button_mapping",mapping.id,mapping.copy(name=editName,askEveryTime=ask || mapping.kind==MappingKind.POINT).encode()))};edit=null}){Text("Simpan")}},dismissButton={TextButton(onClick={edit=null}){Text("Batal")}}) }
    test?.let { mapping -> AlertDialog(onDismissRequest={test=null},title={Text("Uji tombol sekarang?")},text={Text("${mapping.name}\n${mapping.scope.packageName}\n${mapping.scope.site.ifBlank{mapping.scope.screen}}\n\nAplikasi tujuan akan dibuka. Satu klik dapat mengirim prompt atau memakai kredit Generate. Pastikan layar tujuan sudah disiapkan. Hasil aplikasi tetap perlu Anda periksa.")},confirmButton={TextButton(onClick={
        test=null;busy=true
        val permit=MappingClickPermit(mapping.fingerprint(),SystemClock.elapsedRealtime())
        scope.launch { try {
            openMappingTarget(context,mapping)
            delay(700)
            requireCurrentMapping(context,mapping)
            val outcome=checkNotNull(SafeAccessibilityService.current){"Accessibility belum aktif"}.mapping.click(mapping,permit)
            dao.save(Record("mapping_test",mapping.id,outcome));note=outcome
        } catch(e:CancellationException){throw e} catch(e:Exception){note=e.message ?: "Target tidak cocok. Tidak diulang otomatis."} finally { busy=false } }
    }){Text("Ya, uji satu kali")}},dismissButton={TextButton(onClick={test=null}){Text("Batal")}}) }
    SectionTitle("Area latihan", "Gunakan bubble untuk memetakan tombol latihan tanpa mengirim prompt atau memakai kredit.")
    PremiumCard(Modifier.semantics{contentDescription="byga_mapping_test_screen"}) {
        Button(onClick={demoCount++},modifier=Modifier.semantics{contentDescription="byga_mapping_demo_button"}){Text("Tombol latihan")}
        Text("Ditekan $demoCount kali")
    }
    Text("Chrome perlu address bar terlihat untuk memeriksa situs. Jika tombol hilang, kembar, layar berubah, atau HP terkunci, klik dihentikan. Posisi manual selalu meminta konfirmasi serta ukuran/orientasi/layout yang sama.",style=MaterialTheme.typography.bodySmall,color=Muted)
}

fun openMappingTarget(context:android.content.Context,mapping:ButtonMapping) {
    if(mapping.scope.packageName==MappingPolicy.BYGA) {
        if(mapping.scope.site.isNotBlank())context.startActivity(Intent(context,WorkflowBrowserActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        else context.startActivity(Intent(context,com.byga.smartoverlay.MainActivity::class.java).putExtra("open_mapping",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
    } else check(Handoffs.launch(context,mapping.scope.packageName)){"Aplikasi target tidak tersedia"}
}
