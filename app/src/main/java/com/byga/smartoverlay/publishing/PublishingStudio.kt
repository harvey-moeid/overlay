package com.byga.smartoverlay.publishing

import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.media.*
import com.byga.smartoverlay.profiles.*
import com.byga.smartoverlay.ui.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.UUID

@Composable fun PublishingStudio(initialCaption: String,onCaption:(String)->Unit,onAccounts:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val dao=remember{BygaDatabase.get(context).records()}
    DisposableEffect(Unit){StudioSessions.register("publisher"){scope.coroutineContext.cancelChildren()};onDispose{StudioSessions.unregister("publisher")}}
    val drafts by remember{dao.observe("social_draft")}.collectAsState(initial=emptyList());val accountRows by remember{dao.observe("account")}.collectAsState(initial=emptyList())
    val accounts=accountRows.map{AccountProfile.decode(it)}
    var id by rememberSaveable{mutableStateOf(UUID.randomUUID().toString())};var caption by rememberSaveable{mutableStateOf(initialCaption)};var hashtags by rememberSaveable{mutableStateOf("")}
    var uriText by rememberSaveable{mutableStateOf("")};var pkg by rememberSaveable{mutableStateOf("com.instagram.android")};var accountId by rememberSaveable{mutableStateOf("")}
    var reviewed by remember{mutableStateOf(false)};var note by remember{mutableStateOf("")};var confirmation by remember{mutableStateOf(false)};var downloading by remember{mutableStateOf(false)};var url by remember{mutableStateOf("")}
    val uri=uriText.takeIf{it.startsWith("content://")}?.let{Uri.parse(it)}
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){selected->if(selected!=null){runCatching{context.contentResolver.takePersistableUriPermission(selected,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)};uriText=selected.toString();reviewed=false}}
    LaunchedEffect(initialCaption){if(initialCaption.isNotBlank())caption=initialCaption}
    val account=accounts.find{it.id==accountId && it.packageName==pkg}
    fun body(state:String)=JSONObject().put("caption",caption).put("hashtags",hashtags).put("uri",uriText).put("package",pkg).put("account",accountId).put("status",state).toString()
    PremiumCard{Eyebrow("Create. Review. Share.");Text("Draft yang siap\nAnda lanjutkan.",style=MaterialTheme.typography.headlineSmall);Text("Siapkan media dan caption, tinjau akun tujuan, lalu lanjutkan lewat fitur resmi aplikasi. Handoff bukan publikasi.",color=Muted)}
    Row(Modifier.horizontalScroll(rememberScrollState())){targetApps.filter{it.name in setOf("Instagram","TikTok")}.forEach{app->FilterChip(pkg==app.packageName,{pkg=app.packageName;accountId="";reviewed=false},label={Text(app.name)})}}
    OutlinedButton(onClick={pick.launch(arrayOf("image/*","video/*"))},modifier=Modifier.fillMaxWidth()){Text(if(uri==null)"Pilih foto / video" else "Ganti media")}
    if(uri!=null) {
        val preview by produceState<android.graphics.Bitmap?>(null,uriText){value=withContext(Dispatchers.IO){runCatching{if(context.contentResolver.getType(uri)?.startsWith("image/")==true){val opts=BitmapFactory.Options().apply{inJustDecodeBounds=true};context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,opts)};var sample=1;while(opts.outWidth/sample>1024||opts.outHeight/sample>1024)sample*=2;context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample})}}else null}.getOrNull()}}
        PremiumCard{if(preview!=null)Image(preview!!.asImageBitmap(),"Preview media draft",Modifier.fillMaxWidth().heightIn(max=260.dp));Text("Media dipilih • ${runCatching{context.contentResolver.getType(uri)}.getOrNull()?:"tipe tidak diketahui"}",color=Cyan);Text("Periksa rasio, durasi dan hak penggunaan pada aplikasi tujuan.",color=Muted,style=MaterialTheme.typography.bodySmall)}
    }
    OutlinedTextField(caption,{caption=it;reviewed=false;onCaption(it)},label={Text("Caption")},modifier=Modifier.fillMaxWidth(),minLines=3,maxLines=10)
    OutlinedTextField(hashtags,{hashtags=it;reviewed=false},label={Text("Hashtag")},modifier=Modifier.fillMaxWidth())
    SectionTitle("Akun tujuan")
    if(accounts.none{it.packageName==pkg})EmptyState("Belum ada profil tujuan", "Tambahkan label dan handle akun untuk aplikasi ini.","Kelola akun",onAccounts)
    accounts.filter{it.packageName==pkg}.forEach{p->FilterChip(accountId==p.id,{accountId=p.id;reviewed=false},label={Text("${p.label} · ${p.alias}")})}
    Row{Checkbox(reviewed,{reviewed=it});Text("Saya baru memeriksa akun ${account?.alias ?: "tujuan"}, media dan caption. Saya akan memeriksa lagi sebelum posting di aplikasi tujuan.")}
    Text("Profil perlu konfirmasi pengguna yang masih berlaku di Accounts (5 menit). BYGA tidak menganggap akun aktif otomatis benar.",color=Muted,style=MaterialTheme.typography.bodySmall)
    Row { Button(onClick={scope.launch{dao.save(Record("social_draft",id,body("DRAFT")));note="Draft disimpan lokal"}}){Text("Simpan draft")};TextButton(onClick={Handoffs.copy(context,listOf(caption,hashtags).filter{it.isNotBlank()}.joinToString("\n\n"));note="Caption disalin"}){Text("Salin caption")} }
    Button(enabled=uri!=null && account!=null && reviewed,onClick={if(AccountGuard.canHandoff(account,pkg,reviewed,System.currentTimeMillis()))confirmation=true else note="Konfirmasi akun belum ada/kedaluwarsa. Periksa akun dan konfirmasi ulang di Accounts."},modifier=Modifier.fillMaxWidth().height(52.dp)){Text("Tinjau & teruskan draft")}
    if(note.isNotBlank())Text(note,color=Cyan)
    confirmation.let{visible->if(visible)AlertDialog(onDismissRequest={confirmation=false},title={Text("Lanjutkan ke aplikasi tujuan?")},text={Text("${account?.label}\n${account?.alias}\n$pkg\n\n${caption.take(350)}\n\nBelum mempublikasikan. Periksa akun/media/caption dan konfirmasikan posting sendiri pada aplikasi tujuan.")},confirmButton={TextButton(onClick={confirmation=false;if(!AccountGuard.canHandoff(account,pkg,reviewed,System.currentTimeMillis())){note="Konfirmasi akun kedaluwarsa"}else runCatching{Handoffs.share(context,listOf(caption,hashtags).filter{it.isNotBlank()}.joinToString("\n\n"),uri,pkg);scope.launch{dao.save(Record("social_draft",id,body("HANDED_OFF")))};note="HANDED_OFF • bukan PUBLISHED; verifikasi di aplikasi tujuan"}.onFailure{note="Handoff gagal. Aplikasi/format mungkin tidak didukung."};reviewed=false}){Text("Buka draft di aplikasi")}},dismissButton={TextButton(onClick={confirmation=false}){Text("Batal")}})}
    SectionTitle("Library draft (${drafts.size})")
    drafts.forEach{row->val j=runCatching{JSONObject(row.body)}.getOrNull();if(j!=null)PremiumCard{StatusPill(j.optString("status","DRAFT"));Text(j.optString("caption").take(140));Row{TextButton(onClick={id=row.id;caption=j.optString("caption");hashtags=j.optString("hashtags");uriText=j.optString("uri");pkg=j.optString("package");accountId=j.optString("account");reviewed=false;onCaption(caption)}){Text("Buka")};TextButton(onClick={scope.launch{dao.delete("social_draft",row.id)}}){Text("Hapus")};TextButton(onClick={id=UUID.randomUUID().toString();caption=j.optString("caption");hashtags=j.optString("hashtags");uriText=j.optString("uri");pkg=j.optString("package");accountId=j.optString("account");reviewed=false;note="Salinan draft baru siap disimpan"}){Text("Duplikat")}}}}
    SectionTitle("Unduh media langsung")
    OutlinedTextField(url,{url=it},label={Text("URL HTTPS foto / video")},modifier=Modifier.fillMaxWidth())
    Text("Maks. 50MB, MIME & signature diverifikasi. Redirect, login dan format di luar JPEG/PNG/WebP/MP4/WebM ditolak.",color=Muted,style=MaterialTheme.typography.bodySmall)
    Button(enabled=!downloading,onClick={downloading=true;scope.launch{try{when(val r=DirectDownload.save(context,url)){is DownloadResult.Saved->{uriText=r.uri.toString();reviewed=false;note="Media tersimpan di galeri dan dipilih untuk draft"};is DownloadResult.Blocked->note=r.code}}finally{downloading=false}}}){Text(if(downloading)"Mengunduh…" else "Unduh dengan izin saya")}
}
