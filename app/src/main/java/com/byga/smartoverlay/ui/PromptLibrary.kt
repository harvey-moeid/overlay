package com.byga.smartoverlay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.media.Handoffs
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.UUID

@Composable fun PromptLibrary(onChat:(String)->Unit,onDraft:(String)->Unit){
    val context=LocalContext.current;val scope=rememberCoroutineScope();val dao=remember{BygaDatabase.get(context).records()};val rows by remember{dao.observe("prompt")}.collectAsState(initial=emptyList())
    var title by remember{mutableStateOf("")};var text by remember{mutableStateOf("")};var tags by remember{mutableStateOf("")};var query by remember{mutableStateOf("")};var edit by remember{mutableStateOf<String?>(null)}
    PremiumCard{Eyebrow("Keep your best ideas");Text("Prompt yang bagus,\nsiap dipakai lagi.",style=MaterialTheme.typography.headlineSmall);Text("Template lokal, pencarian dan favorit. Tidak dikirim ke AI sampai Anda memilihnya lalu menekan Kirim di AI Chat.",color=Muted)}
    OutlinedTextField(title,{title=it},label={Text("Judul template")},modifier=Modifier.fillMaxWidth())
    OutlinedTextField(text,{text=it},label={Text("Isi prompt")},minLines=3,maxLines=10,modifier=Modifier.fillMaxWidth())
    OutlinedTextField(tags,{tags=it},label={Text("Tag, mis. caption, Flow, ide")},modifier=Modifier.fillMaxWidth())
    Button(onClick={if(text.isNotBlank())scope.launch{dao.save(Record("prompt",edit?:UUID.randomUUID().toString(),JSONObject().put("title",title.ifBlank{"Prompt"}).put("text",text).put("tags",tags).put("favorite",false).toString()));edit=null;title="";text="";tags=""}}){Text(if(edit==null)"Simpan template"else"Simpan perubahan")}
    OutlinedTextField(query,{query=it},label={Text("Cari template / tag")},modifier=Modifier.fillMaxWidth())
    val parsed by produceState<List<Pair<Record,JSONObject>>>(emptyList(),rows,query) { value=withContext(Dispatchers.Default) { rows.map{row->val j=runCatching{JSONObject(row.body)}.getOrElse{JSONObject().put("title","Prompt tersimpan v0.3").put("text",row.body)};row to j}.filter{(_,j)->listOf(j.optString("title"),j.optString("text"),j.optString("tags")).any{it.contains(query,true)}}.sortedByDescending{it.second.optBoolean("favorite")} } }
    if(parsed.isEmpty())EmptyState("Belum ada template yang cocok", "Simpan prompt baru atau ubah pencarian Anda.")
    PagedItems(parsed,query){(row,j)->PremiumCard{if(j.optBoolean("favorite"))StatusPill("FAVORIT");Text(j.optString("title"),style=MaterialTheme.typography.titleMedium);Text(j.optString("text"));Text(j.optString("tags"),color=Muted,style=MaterialTheme.typography.labelSmall);Row{TextButton(onClick={onChat(j.optString("text"))}){Text("Pakai di Chat")};TextButton(onClick={onDraft(j.optString("text"))}){Text("Jadikan draft")};TextButton(onClick={Handoffs.copy(context,j.optString("text"))}){Text("Salin")}};Row{TextButton(onClick={edit=row.id;title=j.optString("title");text=j.optString("text");tags=j.optString("tags")}){Text("Edit")};TextButton(onClick={scope.launch{dao.save(row.copy(body=j.put("favorite",!j.optBoolean("favorite")).toString(),updatedAt=System.currentTimeMillis()))}}){Text("Favorit")};TextButton(onClick={scope.launch{dao.delete("prompt",row.id)}}){Text("Hapus")}}}}
}
