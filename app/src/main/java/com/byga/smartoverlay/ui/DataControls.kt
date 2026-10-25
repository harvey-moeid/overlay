package com.byga.smartoverlay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.room.withTransaction
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.media.Handoffs
import com.byga.smartoverlay.agent.AgentSessions
import com.byga.smartoverlay.overlay.OverlayService
import com.byga.smartoverlay.capture.CaptureService
import android.content.Intent
import kotlinx.coroutines.*
import org.json.JSONObject

@Composable fun DataControls(){
    val context=LocalContext.current;val scope=rememberCoroutineScope();val db=remember{BygaDatabase.get(context)}
    val rows by remember{db.records().exportable()}.collectAsState(initial=emptyList())
    var raw by remember{mutableStateOf("")};var preview by remember{mutableStateOf<List<Record>?>(null)};var note by remember{mutableStateOf("")};var exportDialog by remember{mutableStateOf(false)}
    SectionTitle("Data milik Anda", "Backup konfigurasi secara sadar, tanpa API key atau scope aktif.")
    PremiumCard{
        Text("Ekspor meliputi workflow, prompt, profil aplikasi dan alias akun. Data dapat bersifat pribadi. Tidak menyertakan API key, provider secret, gambar, memory, chat, approval atau jadwal aktif.",color=Muted)
        Button(onClick={exportDialog=true}){Text("Tinjau & ekspor konfigurasi")}
        OutlinedTextField(raw,{raw=it.take(256000);preview=null},label={Text("JSON konfigurasi untuk impor")},modifier=Modifier.fillMaxWidth(),maxLines=5)
        OutlinedButton(onClick={runCatching{ConfigTransfer.parse(raw)}.onSuccess{preview=it;note="${it.size} entri siap ditinjau; belum disimpan"}.onFailure{note="Format atau isi konfigurasi ditolak"}}){Text("Validasi tanpa menjalankan")}
        preview?.let{parsed->Text(parsed.groupingBy{it.kind}.eachCount().entries.joinToString{"${it.key}: ${it.value}"});Text("Semua ID baru, akun UNKNOWN, workflow draft. Tidak menimpa konfigurasi lama atau menjalankan aksi.",color=Muted);Button(onClick={scope.launch{db.withTransaction{parsed.forEach{db.records().save(it)}};preview=null;raw="";note="Impor selesai sebagai draft lokal"}}){Text("Saya setuju impor draft")}}
    }
    if(note.isNotBlank())Text(note,color=Cyan)
    if(exportDialog)AlertDialog(onDismissRequest={exportDialog=false},title={Text("Bagikan konfigurasi lokal?")},text={Text("File teks dapat memuat prompt, URL, workflow dan alias akun. Jangan membagikannya jika berisi informasi pribadi. Key/provider authentication tidak diekspor.")},confirmButton={TextButton(onClick={runCatching{Handoffs.share(context,ConfigTransfer.export(rows))}.onFailure{note="Ekspor dibatasi 100 entri; gunakan ekspor per workflow bila melebihi batas"};exportDialog=false}){Text("Saya setuju; pilih tujuan")}},dismissButton={TextButton(onClick={exportDialog=false}){Text("Batal")}})
}
