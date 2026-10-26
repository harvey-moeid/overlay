package com.byga.smartoverlay.capture

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.byga.smartoverlay.ai.*
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.ui.*
import com.byga.smartoverlay.media.Handoffs
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream

@Composable fun VisionReview(uri:Uri,captureId:String){
    val context=LocalContext.current;val scope=rememberCoroutineScope();val dao=remember{BygaDatabase.get(context).records()}
    val all by remember{dao.observe("provider")}.collectAsStateWithLifecycle(initialValue=emptyList())
    val providers=all.filter{runCatching{val j=JSONObject(it.body);j.optBoolean("vision",false)&&j.optString("type")!="COHERE"}.getOrDefault(false)}
    var selected by remember{mutableStateOf("")};var question by remember(uri){mutableStateOf("Jelaskan layar ini secara ringkas dalam Bahasa Indonesia. Jangan mengusulkan atau mengklaim eksekusi aksi apa pun.")}
    var consent by remember(uri,selected){mutableStateOf(false)};var confirm by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var answer by remember(uri){mutableStateOf("")};var job by remember{mutableStateOf<Job?>(null)}
    val sessionId=remember(uri){"vision-${uri.hashCode()}"}
    DisposableEffect(sessionId){StudioSessions.register(sessionId){job?.cancel()};onDispose{job?.cancel();StudioSessions.unregister(sessionId)}}
    LaunchedEffect(providers){if(providers.none{it.id==selected})selected=providers.firstOrNull()?.id?:""}
    if(providers.isEmpty())Text("Untuk Vision: tambahkan konfigurasi provider baru dengan model yang mendukung gambar dan centang kemampuan Vision pada AI Providers.",color=Muted,style=MaterialTheme.typography.bodySmall)
    else PremiumCard{
        Eyebrow("Vision • opt-in per gambar")
        Row(Modifier.horizontalScroll(rememberScrollState())){providers.forEach{row->val j=JSONObject(row.body);FilterChip(selected==row.id,{selected=row.id},label={Text(j.optString("model"))})}}
        OutlinedTextField(question,{question=it.take(4000);consent=false},label={Text("Apa yang ingin dianalisis?")},modifier=Modifier.fillMaxWidth(),maxLines=5)
        Text("Seluruh gambar yang Anda lihat di atas akan dikirim ke model pilihan melalui HTTPS. Tinjau dulu; jangan kirim layar login, OTP, password, finansial atau data yang tidak diizinkan. Hasil hanya teks, bukan tool call. Maksimal 1 request, cap harian bersama AI Chat/Workflow.",color=Muted,style=MaterialTheme.typography.bodySmall)
        Row{Checkbox(consent,{consent=it});Text("Saya meninjau gambar dan menyetujui pengiriman gambar ini ke model yang dipilih.")}
        Button(enabled=consent&&!busy&&selected.isNotBlank()&&question.isNotBlank(),onClick={confirm=true}){Text("Tinjau & kirim untuk analisis")}
        if(busy){LinearProgressIndicator(Modifier.fillMaxWidth());TextButton(onClick={job?.cancel()}){Text("Hentikan analisis")}}
        if(answer.isNotBlank()){Text(answer);TextButton(onClick={Handoffs.copy(context,answer)}){Text("Salin analisis")}}
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Kirim gambar ke provider cloud?")},text={Text("Model: ${providers.find{it.id==selected}?.let{JSONObject(it.body).optString("model")}}\n\nGambar lengkap, bukan hanya package metadata, akan keluar dari HP. Tidak ada redaksi otomatis yang dijamin. Lanjutkan hanya jika Anda mengizinkan seluruh isinya.")},confirmButton={TextButton(onClick={confirm=false;busy=true;consent=false;val providerId=selected;val prompt=question;job=scope.launch{
        try{
            val row=dao.get("provider",providerId)?:error("Provider tidak tersedia");val j=JSONObject(row.body);check(j.optBoolean("vision",false)){"Kemampuan vision belum dikonfirmasi"}
            val key=SecretStore(context).read(providerId)?:error("Key tidak tersedia")
            val base64=withContext(Dispatchers.IO){
                val decoded=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)}?:error("Gambar tidak tersedia")
                val scale=minOf(1f,1024f/decoded.width,2048f/decoded.height)
                val image=Bitmap.createScaledBitmap(decoded,(decoded.width*scale).toInt().coerceAtLeast(1),(decoded.height*scale).toInt().coerceAtLeast(1),true)
                val bytes=ByteArrayOutputStream().use{out->check(image.compress(Bitmap.CompressFormat.JPEG,80,out));out.toByteArray()}
                if(image!==decoded)image.recycle();decoded.recycle();check(bytes.size<=2_000_000){"Gambar terlalu besar"};Base64.encodeToString(bytes,Base64.NO_WRAP)
            }
            AiRepository(context).reserve(12)
            val capture=dao.get("capture",captureId)
            if(capture!=null)dao.save(capture.copy(body=JSONObject(capture.body).put("status","CLOUD_REQUESTED").put("provider",j.getString("model")).toString()))
            when(val r=HttpProvider(ProviderConfig(row.id,ProviderType.valueOf(j.getString("type")),j.getString("url"),j.getString("model")),key).analyze(prompt,base64)){is AiResult.Text->{answer=r.value;val current=dao.get("capture",captureId);if(current!=null)dao.save(current.copy(body=JSONObject(current.body).put("status","ANALYZED_BY_PROVIDER").toString()))};is AiResult.Failure->answer="Analisis gagal: ${r.code}"}
        }catch(e:CancellationException){answer="Analisis dibatalkan; provider mungkin telah menerima request.";throw e}catch(e:Exception){answer=e.message?:"Analisis gagal"}finally{busy=false}
    }}){Text("Saya setuju kirim gambar")}},dismissButton={TextButton(onClick={confirm=false}){Text("Batal")}})
}
