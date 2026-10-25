package com.byga.smartoverlay.ai

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.byga.smartoverlay.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import com.byga.smartoverlay.ui.StudioSessions
import kotlinx.coroutines.cancelChildren

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ProviderEditor(onStatus: (String) -> Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    DisposableEffect(Unit){StudioSessions.register("model-discovery"){scope.coroutineContext.cancelChildren()};onDispose{StudioSessions.unregister("model-discovery")}}
    var type by remember { mutableStateOf(ProviderType.OPENAI) }
    var endpoint by remember { mutableStateOf(ProviderCatalog.get(type).baseUrl) }
    var model by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var liveModels by remember { mutableStateOf<List<String>?>(null) }
    var fetching by remember { mutableStateOf(false) }
    var vision by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    Text("Pilih provider & model",style=MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        ProviderType.entries.forEach { t -> FilterChip(type==t,{
            if(type!=t) { type=t; endpoint=ProviderCatalog.get(t).baseUrl; model=""; key=""; liveModels=null; filter=""; expanded=false;vision=false }
        },label={Text(ProviderCatalog.get(t).label)}) }
    }
    Text("${ProviderCatalog.get(type).label} • ${if(type==ProviderType.COHERE) "Chat v2" else if(type in setOf(ProviderType.ANTHROPIC,ProviderType.GEMINI)) "API native" else "Chat Completions"}")
    OutlinedTextField(endpoint,{endpoint=it; liveModels=null;vision=false},label={Text(if(type==ProviderType.COHERE) "Base URL HTTPS (tanpa /v2)" else "Base URL HTTPS")},modifier=Modifier.fillMaxWidth())
    OutlinedTextField(key,{key=it; liveModels=null},label={Text(if(type==ProviderType.HUGGING_FACE) "Hugging Face token (Inference Providers)" else "API key")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
    Text("Key dikosongkan saat pindah provider agar tidak terkirim ke provider lain. Key disimpan terenkripsi hanya setelah menekan Simpan.")
    val choices=liveModels ?: ProviderCatalog.get(type).models
    Text(if(liveModels==null) "Saran model awal — akses, harga dan ketersediaan tergantung akun. Pilih atau isi ID manual." else "Daftar respons API untuk endpoint ini; bukan jaminan kuota atau dukungan chat setiap model.")
    OutlinedTextField(filter,{filter=it},label={Text("Cari model dalam daftar")},modifier=Modifier.fillMaxWidth())
    Box {
        OutlinedButton(onClick={expanded=true},enabled=choices.isNotEmpty()) { Text(if(model.isBlank()) "Pilih model (${choices.size})" else model) }
        DropdownMenu(expanded,{expanded=false},modifier=Modifier.heightIn(max=320.dp)) {
            val matches=choices.filter { it.contains(filter,ignoreCase=true) }
            if(matches.isEmpty()) DropdownMenuItem(text={Text("Tidak ada model yang cocok")},onClick={expanded=false})
            matches.take(100).forEach { id -> DropdownMenuItem(text={Text(id)},onClick={model=id; expanded=false;vision=false}) }
            if(matches.size>100) DropdownMenuItem(text={Text("100 pertama ditampilkan; persempit pencarian")},onClick={expanded=false})
        }
    }
    OutlinedTextField(model,{model=it;vision=false},label={Text("ID model terpilih / manual")},modifier=Modifier.fillMaxWidth())
    if(type==ProviderType.HUGGING_FACE) Text("Opsional tambahkan :provider pada ID model sesuai routing Hugging Face, misalnya model-id:provider. Akses gated model harus disetujui di akun HF.")
    Button(enabled=!fetching && ProviderCatalog.validBaseUrl(endpoint) && key.isNotBlank(),onClick={
        val requestType=type; val requestEndpoint=endpoint; val requestKey=key
        fetching=true
        scope.launch {
            try {
                val result=HttpProvider(ProviderConfig("preview",requestType,requestEndpoint,""),requestKey).listModels()
                if(type==requestType && endpoint==requestEndpoint && key==requestKey) when(result) {
                    is ModelListResult.Models -> { liveModels=result.ids; onStatus("${result.ids.size} model dari API${if(result.partial) " • daftar parsial, ID manual tetap tersedia" else ""}") }
                    is ModelListResult.Failure -> onStatus("Daftar model gagal: ${result.code}; saran/manual tetap dapat digunakan")
                }
            } finally { fetching=false }
        }
    }) { Text(if(fetching) "Mengambil daftar…" else "Ambil model dari API dengan key ini") }
    if(type!=ProviderType.COHERE) Row { Checkbox(vision,{vision=it}); Text("Model ini mendukung input gambar (saya sudah memeriksa kemampuan model). Aktifkan untuk analisis capture dengan consent terpisah.") }
    Button(onClick={
        if(!ProviderCatalog.validBaseUrl(endpoint)||model.isBlank()||key.isBlank()) onStatus("Endpoint HTTPS tanpa query/fragment, ID model dan key wajib diisi")
        else {
            val selectedType=type; val selectedEndpoint=endpoint; val selectedModel=model.trim(); val selectedKey=key; val selectedVision=vision && type!=ProviderType.COHERE
            scope.launch {
                try {
                    val id=UUID.randomUUID().toString()
                    SecretStore(context).save(id,selectedKey)
                    val body=JSONObject().put("type",selectedType.name).put("url",selectedEndpoint).put("model",selectedModel).put("vision",selectedVision)
                    BygaDatabase.get(context).records().save(Record("provider",id,body.toString()))
                    if(type==selectedType && key==selectedKey) key=""
                    onStatus("${ProviderCatalog.get(selectedType).label} • $selectedModel disimpan; key terenkripsi")
                } catch(e: CancellationException) { throw e } catch(_: Exception) { onStatus("Gagal menyimpan provider; periksa Keystore/penyimpanan") }
            }
        }
    }) { Text("Simpan provider terenkripsi") }
}
