package com.byga.smartoverlay.workflow

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val label:String,val packageName:String)

@Composable fun InstalledAppPicker(selected:String,onSelect:(String)->Unit,onDismiss:()->Unit) {
    val context=LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var search by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        apps=withContext(Dispatchers.IO) {
            runCatching {
                val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                context.packageManager.queryIntentActivities(intent,0).map { info ->
                    InstalledApp(info.loadLabel(context.packageManager).toString(),info.activityInfo.packageName)
                }.distinctBy{it.packageName}.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER){it.label})
            }.onFailure{failed=true}.getOrDefault(emptyList())
        }
    }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Pilih aplikasi terinstal")},text={Column {
        OutlinedTextField(search,{search=it},label={Text("Cari nama aplikasi")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        when {
            apps==null -> CircularProgressIndicator()
            failed -> Text("Daftar aplikasi gagal dibaca. Tutup dan coba lagi.")
            else -> {
                val filtered=apps.orEmpty().filter{it.label.contains(search,true)||it.packageName.contains(search,true)}
                if(filtered.isEmpty())Text("Tidak ada aplikasi yang cocok. Hanya aplikasi dengan ikon peluncur pada profil Android ini ditampilkan.")
                LazyColumn(Modifier.heightIn(max=380.dp)) {
                    items(filtered,key={it.packageName}) { app ->
                        ListItem(headlineContent={Text(app.label)},supportingContent={Text(app.packageName)},trailingContent={if(app.packageName==selected)Text("✓")},modifier=Modifier.clickable{onSelect(app.packageName)})
                    }
                }
            }
        }
    }},confirmButton={TextButton(onClick=onDismiss){Text("Tutup")}})
}
