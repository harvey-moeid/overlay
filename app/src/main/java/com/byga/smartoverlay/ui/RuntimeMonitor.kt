package com.byga.smartoverlay.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byga.smartoverlay.workflow.WorkflowViewModel
import com.byga.smartoverlay.agent.AgentSessions
import com.byga.smartoverlay.data.BygaDatabase
import kotlinx.coroutines.delay

@Composable fun RuntimeMonitor() {
    val context=LocalContext.current;val vm: WorkflowViewModel=viewModel()
    val status by vm.runner.snapshot.collectAsStateWithLifecycle()
    val dao=remember {BygaDatabase.get(context).records()}
    val rows by remember{dao.recent("execution",20)}.collectAsStateWithLifecycle(initialValue=emptyList())
    val agent by produceState("Tidak ada sesi agent aktif"){while(true){value=AgentSessions.active?.state?.name?:"Tidak ada sesi agent aktif";delay(1000)}}
    PremiumCard{Eyebrow("Live activity");Text(status.state.name,style=MaterialTheme.typography.headlineSmall);Text(status.note);Text("Langkah ${status.index} / ${status.total}",color=Cyan);if(status.total>0)LinearProgressIndicator(progress={status.index.toFloat()/status.total},modifier=Modifier.fillMaxWidth());Row{TextButton(onClick={vm.pause()}){Text("Pause")};TextButton(onClick={vm.resume()}){Text("Resume")};TextButton(onClick={vm.stop()}){Text("Stop")}}}
    PremiumCard{Text("Agent Lab",style=MaterialTheme.typography.titleMedium);Text(agent,color=Muted);TextButton(onClick={AgentSessions.stopAll()}){Text("Cabut scope agent")}}
    SectionTitle("Riwayat workflow")
    if(rows.isEmpty())EmptyState("Belum ada eksekusi", "Workflow yang Anda jalankan akan menampilkan checkpoint dan hasil di sini.")
    rows.take(20).forEach{row->PremiumCard{Text(row.body);Text(row.id,style=MaterialTheme.typography.labelSmall,color=Muted)}}
}
