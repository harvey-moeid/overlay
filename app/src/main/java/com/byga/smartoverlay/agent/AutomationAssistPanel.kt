package com.byga.smartoverlay.agent

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byga.smartoverlay.ai.AiRepository
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.mapping.*
import com.byga.smartoverlay.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import org.json.JSONObject

@Composable fun AutomationAssistPanel() {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val dao=remember{BygaDatabase.get(context).records()}
    val key=remember{booleanPreferencesKey("automationAssistanceEnabled")}
    val enabled by remember{context.settings.data.map{it[key] ?: false}}.collectAsStateWithLifecycle(initialValue=false)
    val mappings by remember{dao.recent("button_mapping",20)}.collectAsStateWithLifecycle(initialValue=emptyList())
    val providers by remember{dao.observe("provider")}.collectAsStateWithLifecycle(initialValue=emptyList())
    var selected by remember{mutableStateOf("")};var provider by remember{mutableStateOf("")}
    var question by remember{mutableStateOf("Jelaskan kontrol ini dan cara aman memulihkan mapping ketika UI berubah.")}
    var consent by remember(selected,provider,question){mutableStateOf(false)}
    var output by remember{mutableStateOf("")};var job by remember{mutableStateOf<Job?>(null)}
    val mapping=mappings.firstOrNull{it.id==selected}?.let{runCatching{ButtonMapping.decode(it.body)}.getOrNull()}
    // Saved mappings are not fresh observations. An AI response cannot promote this status.
    val brief=remember(mapping){mapping?.let{AutomationAssistance.brief(it,MappingStatus.NEEDS_REVIEW)}.orEmpty()}
    DisposableEffect(Unit){StudioSessions.register("automation-assistance"){job?.cancel()};onDispose{job?.cancel();StudioSessions.unregister("automation-assistance")}}
    LaunchedEffect(enabled){if(!enabled){job?.cancel();consent=false}}
    PremiumCard {
        Text("AI Agent Assistance V2",style=MaterialTheme.typography.titleMedium)
        Row{Switch(enabled,{value->scope.launch{context.settings.edit{it[key]=value}}});Text("Opt-in bantuan diagnosis dan saran")}
        Text("Metadata mapping yang dipilih dapat dikirim ke provider. Saran AI tidak menjalankan tindakan. Capture/Vision memakai izin dan consent terpisah di Screen Capture.",color=Muted)
        if(enabled) {
            Row(Modifier.horizontalScroll(rememberScrollState())){mappings.forEach{row->val m=runCatching{ButtonMapping.decode(row.body)}.getOrNull();if(m!=null)FilterChip(selected==row.id,{selected=row.id;consent=false},label={Text(m.name)})}}
            Row(Modifier.horizontalScroll(rememberScrollState())){providers.forEach{row->FilterChip(provider==row.id,{provider=row.id;consent=false},label={Text(runCatching{JSONObject(row.body).optString("model")}.getOrDefault("Provider"))})}}
            if(brief.isNotBlank())Text("Preview metadata yang dikirim:\n$brief",style=MaterialTheme.typography.bodySmall)
            else Text("Pilih mapping tersimpan terlebih dahulu.",color=Muted)
            OutlinedTextField(question,{question=it.take(2000)},label={Text("Pertanyaan / penyebab kegagalan")},modifier=Modifier.fillMaxWidth(),maxLines=4)
            Row{Checkbox(consent,{consent=it});Text("Saya meninjau metadata dan pertanyaan ini; setuju mengirimnya ke model pilihan (1 request, dapat berbiaya).")}
            Button(enabled=consent && mapping!=null && provider.isNotBlank() && question.isNotBlank() && job?.isActive!=true,onClick={
                val request=AutomationAssistance.prompt(brief,question);val providerId=provider
                consent=false
                job=scope.launch{try{output=withTimeout(90000){AiRepository(context).generate(request,listOf(providerId)).text}}catch(e:CancellationException){output="Bantuan dibatalkan. Provider mungkin telah menerima request; tidak diulang otomatis.";throw e}catch(_:Exception){output="AI provider gagal atau batas request tercapai. Tidak ada aksi UI dijalankan. Periksa target atau petakan ulang secara manual."}}
            }){Text("Minta saran tanpa menjalankan aksi")}
            if(job?.isActive==true)TextButton(onClick={job?.cancel()}){Text("Stop bantuan AI")}
            if(output.isNotBlank())Text(output)
        }
    }
}
