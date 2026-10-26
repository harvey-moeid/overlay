package com.byga.smartoverlay.profiles

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.media.Handoffs
import com.byga.smartoverlay.ui.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable fun AccountsScreen() {
    val context=LocalContext.current; val scope=rememberCoroutineScope(); val dao=remember{BygaDatabase.get(context).records()}
    val rows by remember{dao.observe("account")}.collectAsStateWithLifecycle(initialValue=emptyList())
    var packageName by remember{mutableStateOf(targetApps.last().packageName)}; var label by remember{mutableStateOf("")}; var alias by remember{mutableStateOf("")}; var editId by remember{mutableStateOf<String?>(null)}
    var message by remember{mutableStateOf("")}; var confirm by remember{mutableStateOf<AccountProfile?>(null)}
    PremiumCard { Eyebrow("Know your destination"); Text("Satu akun.\nTujuan yang tepat.",style=MaterialTheme.typography.headlineSmall); Text("Profil hanya menyimpan label dan handle publik. Login dan perpindahan akun tetap lewat menu resmi aplikasi.",color=Muted) }
    Row(Modifier.horizontalScroll(rememberScrollState())){targetApps.forEach{a->FilterChip(packageName==a.packageName,{packageName=a.packageName},label={Text(a.name)})}}
    OutlinedTextField(packageName,{packageName=it},label={Text("Package aplikasi")},modifier=Modifier.fillMaxWidth())
    OutlinedTextField(label,{label=it},label={Text("Nama profil, mis. Instagram personal")},modifier=Modifier.fillMaxWidth())
    OutlinedTextField(alias,{alias=it},label={Text("Handle publik / alias tujuan")},modifier=Modifier.fillMaxWidth())
    Button(onClick={if(label.isBlank()||alias.isBlank()||!packageName.matches(Regex("[A-Za-z][A-Za-z0-9_]*([.][A-Za-z0-9_]+)+")))message="Package, label dan alias wajib valid" else scope.launch{val p=AccountProfile(editId?:UUID.randomUUID().toString(),packageName,label,alias);dao.save(Record("account",p.id,p.encode()));editId=null;label="";alias="";message="Profil tersimpan sebagai UNKNOWN; konfirmasi harus diulang sebelum handoff"}}){Text(if(editId==null)"Simpan profil" else "Simpan perubahan; reset konfirmasi")}
    if(message.isNotBlank())Text(message,color=Cyan)
    if(rows.isEmpty())EmptyState("Belum ada profil", "Tambahkan akun tujuan pertama. Password dan sesi login tidak disimpan.")
    PagedItems(rows){row->val p=AccountProfile.decode(row);PremiumCard{
        StatusPill(p.status,p.status=="USER_CONFIRMED")
        Text(p.label,style=MaterialTheme.typography.titleMedium);Text(p.alias.ifBlank{"Profil lama: lengkapi handle"},color=Cyan);Text(p.packageName,color=Muted)
        Text("Konfirmasi pengguna berlaku 5 menit; bukan deteksi akun otomatis.",style=MaterialTheme.typography.bodySmall,color=Muted)
        Row{TextButton(onClick={message=if(Handoffs.launch(context,p.packageName))"Buka menu akun resmi, login/pindah akun sendiri lalu kembali untuk konfirmasi" else "Aplikasi tidak tersedia"}){Text("Buka / pindah akun")};TextButton(onClick={confirm=p},enabled=p.alias.isNotBlank()&&p.packageName.isNotBlank()){Text("Konfirmasi terlihat")}}
        Row{TextButton(onClick={editId=p.id;packageName=p.packageName;label=p.label;alias=p.alias}){Text("Edit")};TextButton(onClick={scope.launch{dao.save(Record("account",p.id,p.copy(status="NEEDS_LOGIN",confirmedAt=0).encode()))}}){Text("Perlu login")};TextButton(onClick={scope.launch{dao.delete("account",p.id)}}){Text("Hapus")}}
    }}
    confirm?.let{p->AlertDialog(onDismissRequest={confirm=null},title={Text("Apakah akun tujuan sudah terlihat?")},text={Text("${p.label}\n${p.alias}\n${p.packageName}\n\nKonfirmasikan hanya setelah Anda melihat akun ini pada aplikasi tujuan. BYGA tidak membaca password atau memverifikasi login otomatis.")},confirmButton={TextButton(onClick={scope.launch{dao.save(Record("account",p.id,p.copy(status="USER_CONFIRMED",confirmedAt=System.currentTimeMillis()).encode()))};confirm=null}){Text("Ya, saya melihat akun ini")}},dismissButton={TextButton(onClick={confirm=null}){Text("Batal")}})}
}
