package com.byga.smartoverlay.capture

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.profiles.targetApps
import com.byga.smartoverlay.media.Handoffs
import com.byga.smartoverlay.overlay.OverlayService
import com.byga.smartoverlay.accessibility.SafeAccessibilityService
import com.byga.smartoverlay.ui.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File

@Composable fun CaptureScreen(){
    val context=LocalContext.current;val scope=rememberCoroutineScope();val dao=remember{BygaDatabase.get(context).records()}
    val captures by remember{dao.observe("capture")}.collectAsStateWithLifecycle(initialValue=emptyList());val errors by remember{dao.observe("capture_error")}.collectAsStateWithLifecycle(initialValue=emptyList())
    var requested by remember{mutableStateOf(context.packageName)};var consent by remember{mutableStateOf(false)};var note by remember{mutableStateOf("")};var share by remember{mutableStateOf<Pair<String,Uri>?>(null)}
    val launch=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if(result.resultCode==Activity.RESULT_OK && result.data!=null){
            context.stopService(Intent(context,OverlayService::class.java))
            runCatching{
                ContextCompat.startForegroundService(context,Intent(context,CaptureService::class.java).putExtra("package",requested).putExtra("consent",result.data))
                if(requested!=context.packageName)check(Handoffs.launch(context,requested)){"Aplikasi target tidak tersedia"}
                note="Sesi capture dimulai. Kembali ke halaman ini untuk meninjau gambar; tidak ada upload otomatis."
            }.onFailure{context.stopService(Intent(context,CaptureService::class.java));note="Sesi tidak dapat dimulai"}
        }else note="Consent sistem ditolak/dibatalkan; tidak ada capture"
    }
    PremiumCard{Eyebrow("One frame. Your permission.");Text("Layar Anda,\nkeputusan Anda.",style=MaterialTheme.typography.headlineSmall);Text("Capture sekali via dialog MediaProjection Android. Disimpan di cache privat; file lebih dari 24 jam dibersihkan pada sesi berikutnya. Bubble ditutup, tidak dikirim otomatis. Jangan menangkap password, OTP, login atau informasi finansial.",color=Muted)}
    Text("Aplikasi yang Anda izinkan untuk sesi ini")
    Row(Modifier.horizontalScroll(rememberScrollState())){(listOf(com.byga.smartoverlay.profiles.TargetApp("BYGA",context.packageName))+targetApps).forEach{app->FilterChip(requested==app.packageName,{requested=app.packageName;consent=false},label={Text(app.name)})}}
    Text("Accessibility harus aktif untuk memeriksa package/lock/field sensitif sebelum frame diambil. Browser umum tidak diizinkan karena domain layar belum diverifikasi. Layar FLAG_SECURE tetap dilindungi Android.",color=Muted,style=MaterialTheme.typography.bodySmall)
    Row{Checkbox(consent,{consent=it});Text("Saya mengizinkan satu capture aplikasi ini dan memahami gambar dapat berisi data pribadi.")}
    Button(enabled=consent,onClick={if(SafeAccessibilityService.current==null)note="Aktifkan Accessibility melalui pengaturan resmi dahulu" else launch.launch(if(android.os.Build.VERSION.SDK_INT>=34) context.getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent(android.media.projection.MediaProjectionConfig.createConfigForDefaultDisplay()) else context.getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())},modifier=Modifier.fillMaxWidth().height(52.dp)){Text("Minta izin capture satu sesi")}
    if(note.isNotBlank())Text(note,color=Cyan)
    errors.firstOrNull()?.let{Text(it.body,color=MaterialTheme.colorScheme.error)}
    SectionTitle("Tinjau sebelum berbagi", "Tidak ada gambar yang dikirim ke provider AI secara otomatis. Model Vision hanya menerima gambar setelah kemampuan model dikonfirmasi dan consent pengiriman terpisah.")
    if(captures.isEmpty())EmptyState("Belum ada capture", "Gambar baru muncul setelah sistem memberi izin dan file berhasil dibuat.")
    var page by remember { mutableIntStateOf(0) }
    LaunchedEffect(captures.size) { page=page.coerceAtMost(((captures.size-1)/5).coerceAtLeast(0)) }
    if(captures.size>5) Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
        TextButton(enabled=page>0,onClick={page--}) { Text("Sebelumnya") }
        Text("${page+1} / ${(captures.size+4)/5}")
        TextButton(enabled=(page+1)*5<captures.size,onClick={page++}) { Text("Berikutnya") }
    }
    captures.drop(page*5).take(5).forEach{row->val j=runCatching{JSONObject(row.body)}.getOrNull();if(j!=null){val uri=Uri.parse(j.getString("uri"));val bitmap by produceState<android.graphics.Bitmap?>(null,row.id){value=null;value=withContext(Dispatchers.IO){runCatching{context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=2})}}.getOrNull()}};PremiumCard{StatusPill(when(j.optString("status")){"CLOUD_REQUESTED"->"KIRIM AI DIMINTA";"ANALYZED_BY_PROVIDER"->"HASIL ANALISIS DITERIMA";"SHARE_REQUESTED"->"SHARESHEET DIMINTA";else->"LOKAL • PERLU REVIEW"});Text(j.optString("package"),color=Muted);if(bitmap!=null)Image(bitmap!!.asImageBitmap(),"Preview capture privat",Modifier.fillMaxWidth().heightIn(max=350.dp))else Text("File tidak tersedia / cache sudah dibersihkan");Row{TextButton(enabled=bitmap!=null,onClick={share=row.id to uri}){Text("Tinjau & bagikan manual")};TextButton(onClick={scope.launch{if(row.id.matches(Regex("capture-[A-Za-z0-9-]+[.]png")))File(context.cacheDir,"shared/${row.id}").delete();dao.delete("capture",row.id)}}){Text("Hapus permanen")}};if(bitmap!=null)VisionReview(uri,row.id)}}}
    share?.let{(captureId,uri)->AlertDialog(onDismissRequest={share=null},title={Text("Izinkan berbagi gambar layar?")},text={Text("Pastikan tidak ada password, OTP, akun/login, informasi finansial atau konten pribadi yang tidak Anda izinkan. Aplikasi tujuan yang Anda pilih dapat menerima seluruh gambar. Ini consent terpisah dari izin capture sistem.")},confirmButton={TextButton(onClick={runCatching{Handoffs.share(context,"Gambar layar yang saya pilih",uri);scope.launch{val row=dao.get("capture",captureId);if(row!=null)dao.save(row.copy(body=JSONObject(row.body).put("status","SHARE_REQUESTED").toString()))}}.onFailure{note="Sharesheet tidak tersedia"};share=null}){Text("Saya setuju; pilih tujuan")}},dismissButton={TextButton(onClick={share=null}){Text("Jangan bagikan")}})}
}
